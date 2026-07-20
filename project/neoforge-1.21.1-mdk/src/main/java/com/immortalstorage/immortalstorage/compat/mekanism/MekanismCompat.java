package com.immortalstorage.immortalstorage.compat.mekanism;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.SpiritStaffWrenchCompat;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.IConfigurable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Optional;

/** Mekanism-only capability bootstrap, loaded only after the exact mod gate. */
public final class MekanismCompat {
    private static final BlockCapability<IStrictEnergyHandler, @Nullable Direction> STRICT_ENERGY =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "strict_energy_handler"),
                    IStrictEnergyHandler.class);
    private static final BlockCapability<IChemicalHandler, @Nullable Direction> CHEMICAL =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"),
                    IChemicalHandler.class);
    private static final ItemCapability<IChemicalHandler, Void> CHEMICAL_ITEM =
            ItemCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"),
                    IChemicalHandler.class);

    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;
    private static final AtomicBoolean WRENCH_BRIDGE_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean ACTIVE_TRANSFER_HOOK_INSTALLED = new AtomicBoolean();

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        ExternalResourceCatalog.registerDefinitionProvider(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID,
                        "mekanism_chemical_definitions"),
                MekanismCompat::chemicalDefinition);
        ExternalResourceCatalog.register(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "mekanism_chemicals"),
                () -> XianqiaoMekanismChemicalAdapter.chemicalsSnapshot().stream()
                        .map(XianqiaoMekanismChemicalAdapter::key).toList());
        if (WRENCH_BRIDGE_INSTALLED.compareAndSet(false, true)) {
            SpiritStaffWrenchCompat.register(MekanismCompat::configureWithSpiritStaff);
        }
        if (ACTIVE_TRANSFER_HOOK_INSTALLED.compareAndSet(false, true)) {
            XianqiaoInterfaceCompatHooks.register(new XianqiaoInterfaceCompatHooks.Hook() {
                @Override
                public void serverTick(
                        XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
                    activeChemicalTransferTick(blockEntity, level);
                }

                @Override
                public Optional<XianqiaoInterfaceCompatHooks.ContainedExternalResource>
                        containedExternalResource(ItemStack stack) {
                    IChemicalHandler handler = stack.copyWithCount(1).getCapability(CHEMICAL_ITEM);
                    if (handler == null) return Optional.empty();
                    for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
                        ChemicalStack chemical = handler.getChemicalInTank(tank);
                        if (chemical != null && !chemical.isEmpty()) {
                            return Optional.of(new XianqiaoInterfaceCompatHooks.ContainedExternalResource(
                                    XianqiaoMekanismChemicalAdapter.key(chemical.getChemical()),
                                    chemical.getAmount()));
                        }
                    }
                    return Optional.empty();
                }
            });
        }
    }

    private static ExternalResourceCatalog.Definition chemicalDefinition(ResourceChannelKey key) {
        if (key == null || !ExternalResourceChannels.MEKANISM_CHEMICAL_CHANNEL
                .equals(key.channel())) return null;
        ResourceLocation id = ResourceLocation.tryParse(key.resourceId());
        if (id == null) return null;
        var chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
        if (chemical == null || MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical)
                .is(MekanismAPI.EMPTY_CHEMICAL_KEY)) return null;
        return new ExternalResourceCatalog.Definition(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID,
                        "textures/gui/external_resource/ae2_chemical.png"),
                "mB", 0xFF000000 | chemical.getColorRepresentation(),
                chemical.getTextComponent(), true);
    }

    private static void activeChemicalTransferTick(
            XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
        for (Direction side : Direction.values()) {
            XianqiaoInterfaceBlockEntity.SideMode mode = blockEntity.getSideMode(side);
            if (mode == XianqiaoInterfaceBlockEntity.SideMode.DISABLED) continue;
            var standardEnergy = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    blockEntity.getBlockPos().relative(side), side.getOpposite());
            if (standardEnergy == null) {
                IStrictEnergyHandler strictEnergy = level.getCapability(
                        STRICT_ENERGY, blockEntity.getBlockPos().relative(side), side.getOpposite());
                AtomicEnergyRefill.ResourceStore energy = blockEntity.resolveExternalResourceFaceStore(
                        ExternalResourceChannels.FE, side);
                if (strictEnergy != null && energy != null) {
                    if (mode == XianqiaoInterfaceBlockEntity.SideMode.PUSH
                            && blockEntity.isActivePushEnabled()) {
                        MekanismEnergyActiveTransfer.push(energy, strictEnergy);
                    } else if (mode == XianqiaoInterfaceBlockEntity.SideMode.PULL
                            && blockEntity.isActivePullEnabled()) {
                        MekanismEnergyActiveTransfer.pull(strictEnergy, energy);
                    }
                }
            }
            IChemicalHandler adjacent = level.getCapability(
                    CHEMICAL, blockEntity.getBlockPos().relative(side), side.getOpposite());
            if (adjacent == null) continue;
            if (mode == XianqiaoInterfaceBlockEntity.SideMode.PUSH
                    && blockEntity.isActivePushEnabled()) {
                MekanismChemicalActiveTransfer.push(
                        key -> blockEntity.resolveExternalResourceCache(key, side), adjacent);
            } else if (mode == XianqiaoInterfaceBlockEntity.SideMode.PULL
                    && blockEntity.isActivePullEnabled()) {
                MekanismChemicalActiveTransfer.pull(
                        adjacent, key -> blockEntity.resolveExternalResourceFaceStore(key, side));
            }
        }
    }

    private static InteractionResult configureWithSpiritStaff(
            UseOnContext context, ServerPlayer player) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof IConfigurable configurable)) {
            return InteractionResult.PASS;
        }
        return player.isShiftKeyDown()
                ? configurable.onSneakRightClick(player)
                : configurable.onRightClick(player);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(STRICT_ENERGY, ModBlockEntities.XIANQIAO_INTERFACE.get(),
                MekanismCompat::handlerOrNull);
        event.registerBlockEntity(CHEMICAL, ModBlockEntities.XIANQIAO_INTERFACE.get(),
                MekanismCompat::chemicalHandlerOrNull);
        ImmortalStorageMod.LOG.info(
                "[Compat/Mekanism] Registered owner-bound Xianqiao Interface strict energy and chemical capabilities");
    }

    private static @Nullable IStrictEnergyHandler handlerOrNull(
            XianqiaoInterfaceBlockEntity blockEntity, @Nullable Direction side) {
        AtomicEnergyRefill.ResourceStore current =
                blockEntity.resolveExternalResourcePipeStore(ExternalResourceChannels.FE, side);
        if (current == null) return null;
        return new XianqiaoMekanismEnergyAdapter(
                () -> blockEntity.resolveExternalResourcePipeStore(
                        ExternalResourceChannels.FE, side));
    }

    private static @Nullable IChemicalHandler chemicalHandlerOrNull(
            XianqiaoInterfaceBlockEntity blockEntity, @Nullable Direction side) {
        return new XianqiaoMekanismChemicalAdapter(
                key -> blockEntity.resolveExternalResourcePipeStore(key, side));
    }

    private MekanismCompat() {
    }
}
