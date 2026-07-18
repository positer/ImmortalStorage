package com.cultivation.cultivation.compat.mekanism;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.entity.ModBlockEntities;
import com.cultivation.cultivation.block.entity.XianqiaoInterfaceBlockEntity;
import com.cultivation.cultivation.compat.SpiritStaffWrenchCompat;
import com.cultivation.core.resource.AtomicEnergyRefill;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.IConfigurable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;

/** Mekanism-only capability bootstrap, loaded only after the exact mod gate. */
public final class MekanismCompat {
    private static final BlockCapability<IStrictEnergyHandler, @Nullable Direction> STRICT_ENERGY =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "strict_energy_handler"),
                    IStrictEnergyHandler.class);

    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;
    private static final AtomicBoolean WRENCH_BRIDGE_INSTALLED = new AtomicBoolean();

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        if (WRENCH_BRIDGE_INSTALLED.compareAndSet(false, true)) {
            SpiritStaffWrenchCompat.register(MekanismCompat::configureWithSpiritStaff);
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
        CultivationMod.LOG.info(
                "[Compat/Mekanism] Registered owner-bound Xianqiao Interface strict energy capability");
    }

    private static @Nullable IStrictEnergyHandler handlerOrNull(
            XianqiaoInterfaceBlockEntity blockEntity, @Nullable Direction side) {
        AtomicEnergyRefill.ResourceStore current = storageResolver.apply(blockEntity);
        if (current == null || side == null) return null;
        return new XianqiaoMekanismEnergyAdapter(
                () -> storageResolver.apply(blockEntity),
                () -> mode(blockEntity.getSideMode(side)));
    }

    private static XianqiaoMekanismEnergyAdapter.Mode mode(
            XianqiaoInterfaceBlockEntity.SideMode sideMode) {
        return switch (sideMode) {
            case PULL -> XianqiaoMekanismEnergyAdapter.Mode.PULL;
            case PUSH -> XianqiaoMekanismEnergyAdapter.Mode.PUSH;
            case DISABLED -> XianqiaoMekanismEnergyAdapter.Mode.DISABLED;
        };
    }

    private MekanismCompat() {
    }
}
