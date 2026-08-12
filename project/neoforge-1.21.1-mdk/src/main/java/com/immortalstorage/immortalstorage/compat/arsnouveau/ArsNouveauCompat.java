package com.immortalstorage.immortalstorage.compat.arsnouveau;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.CrystalResourceCompatHooks;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.hollingsworth.arsnouveau.api.item.IWandable;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.common.items.data.BlockFillContents;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Ars Nouveau-only Source provider, loaded reflectively behind the mod gate. */
public final class ArsNouveauCompat {
    private static final Map<XianqiaoInterfaceBlockEntity, Provider> PROVIDERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EnergyCrystalBlockEntity, CrystalProvider> CRYSTAL_PROVIDERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EnergyCrystalBlockEntity, XianqiaoArsSourceCapAdapter> CRYSTAL_CAPS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;
    private static boolean initialized;

    private static final CrystalResourceCompatHooks.Hook CRYSTAL_HOOK =
            new CrystalResourceCompatHooks.Hook() {
                @Override public boolean supports(CrystalKind kind) {
                    return kind == CrystalKind.SOURCE;
                }

                @Override public void onLoad(EnergyCrystalBlockEntity crystal, ServerLevel level) {
                    SourceManager.INSTANCE.addInterface(level, crystalProvider(crystal, level));
                }

                @Override public void onRemoved(EnergyCrystalBlockEntity crystal, ServerLevel level) {
                    CrystalProvider provider = CRYSTAL_PROVIDERS.get(crystal);
                    if (provider != null) provider.invalidate();
                }

                @Override public @Nullable Boolean pushToFace(
                        EnergyCrystalBlockEntity crystal, ServerLevel level,
                        Direction side, AtomicEnergyRefill.ResourceStore source) {
                    ISourceCap target = level.getCapability(
                            CapabilityRegistry.SOURCE_CAPABILITY,
                            crystal.getBlockPos().relative(side), side.getOpposite());
                    if (target == null || !target.canReceive()) return false;
                    long moved = 0L;
                    while (source.amount() > 0L && target.canReceive()) {
                        long room = Math.max(0L,
                                (long) target.getSourceCapacity() - target.getSource());
                        long offered = Math.min(source.amount(), Math.min(room, Integer.MAX_VALUE));
                        if (offered <= 0L) break;
                        int accepted = target.receiveSource((int) offered, false);
                        if (accepted <= 0) break;
                        long extracted = source.extract(accepted, ResourceTransferAction.EXECUTE);
                        if (extracted <= 0L) break;
                        moved += extracted;
                    }
                    return moved > 0L;
                }

                @Override public boolean acceptsInput(ItemStack stack) {
                    return isSourceContainer(stack);
                }

                @Override public @Nullable CrystalResourceCompatHooks.InputResult processInput(
                        EnergyCrystalBlockEntity crystal, ServerLevel level, ItemStack input,
                        AtomicEnergyRefill.ResourceStore local,
                        @Nullable AtomicEnergyRefill.ResourceStore external,
                        java.util.function.Predicate<ItemStack> canAcceptExtra) {
                    if (!isSourceContainer(input)) return null;
                    int maximum = sourceContainerCapacity(input);
                    int current = Math.max(0, Math.min(maximum, BlockFillContents.get(input)));
                    ItemStack output = input.copyWithCount(1);
                    if (current >= maximum) {
                        return canAcceptExtra.test(output)
                                ? CrystalResourceCompatHooks.InputResult.complete(output)
                                : new CrystalResourceCompatHooks.InputResult(false, null);
                    }
                    long moved = fillSourceItem(input, current, maximum, local);
                    current = Math.max(0, Math.min(maximum, BlockFillContents.get(input)));
                    if (external != null && current < maximum) {
                        moved += fillSourceItem(input, current, maximum, external);
                        current = Math.max(0, Math.min(maximum, BlockFillContents.get(input)));
                    }
                    if (current >= maximum) {
                        output = input.copyWithCount(1);
                        return canAcceptExtra.test(output)
                                ? CrystalResourceCompatHooks.InputResult.complete(output)
                                : new CrystalResourceCompatHooks.InputResult(moved > 0L, null);
                    }
                    return new CrystalResourceCompatHooks.InputResult(moved > 0L, null);
                }

                @Override public InteractionResult useItemOn(
                        EnergyCrystalBlockEntity crystal,
                        net.minecraft.world.entity.player.Player player,
                        ItemStack stack, InteractionHand hand, BlockHitResult hit) {
                    if (!(stack.getItem() instanceof DominionWand wand)) {
                        return InteractionResult.PASS;
                    }
                    return wand.useOn(new UseOnContext(player, hand, hit));
                }
            };

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        PROVIDERS.clear();
        CRYSTAL_PROVIDERS.clear();
        CRYSTAL_CAPS.clear();
        CrystalResourceCompatHooks.register(CRYSTAL_HOOK);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (ModBlockEntities.SOURCE_CRYSTAL != null) {
            event.registerBlockEntity(CapabilityRegistry.SOURCE_CAPABILITY,
                    ModBlockEntities.SOURCE_CRYSTAL.get(),
                    (blockEntity, direction) -> crystalCapabilityOrNull(blockEntity));
        }
        ImmortalStorageMod.LOG.info("[Compat/ArsNouveau] Registered source capability for Xianeng Source Crystal");
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        XianqiaoInterfaceCompatHooks.register(new XianqiaoInterfaceCompatHooks.Hook() {
            @Override
            public void onLoad(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
                SourceManager.INSTANCE.addInterface(level, provider(blockEntity, level));
            }

            @Override
            public void onRemoved(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
                Provider provider = PROVIDERS.get(blockEntity);
                if (provider != null) provider.invalidate();
            }

            @Override
            public InteractionResult useItemOn(
                    XianqiaoInterfaceBlockEntity blockEntity,
                    net.minecraft.world.entity.player.Player player,
                    net.minecraft.world.item.ItemStack stack,
                    InteractionHand hand, BlockHitResult hit) {
                if (!(stack.getItem() instanceof DominionWand wand)) return InteractionResult.PASS;
                return wand.useOn(new UseOnContext(player, hand, hit));
            }
        });
        ImmortalStorageMod.LOG.info(
                "[Compat/ArsNouveau] Registered Xianqiao Interface as an official Source provider");
    }

    private static Provider provider(
            XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
        synchronized (PROVIDERS) {
            return PROVIDERS.computeIfAbsent(blockEntity, key -> new Provider(key, level));
        }
    }

    public static ISourceTile sourceAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        if (level.getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity blockEntity) {
            return provider(blockEntity, level).getSource();
        }
        if (level.getBlockEntity(pos) instanceof EnergyCrystalBlockEntity crystal
                && crystal.kind() == CrystalKind.SOURCE) {
            return crystalProvider(crystal, level).getSource();
        }
        return null;
    }

    private static final class Provider implements ISpecialSourceProvider {
        private final XianqiaoInterfaceBlockEntity blockEntity;
        private final ServerLevel level;
        private final ISourceTile source;
        private boolean valid = true;

        private Provider(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
            this.blockEntity = blockEntity;
            this.level = level;
            this.source = new XianqiaoArsSourceAdapter(
                    () -> storageResolver.apply(blockEntity));
        }

        @Override
        public ISourceTile getSource() {
            return source;
        }

        @Override
        public boolean isValid() {
            // Keep the provider registered while its chunk is live even when
            // the owner is temporarily offline. Individual source operations
            // re-resolve storage and fail closed until the owner is available.
            return valid && !blockEntity.isRemoved() && blockEntity.getLevel() == level;
        }

        @Override
        public BlockPos getCurrentPos() {
            return blockEntity.getBlockPos();
        }

        private void invalidate() {
            valid = false;
        }
    }

    private static CrystalProvider crystalProvider(
            EnergyCrystalBlockEntity crystal, ServerLevel level) {
        synchronized (CRYSTAL_PROVIDERS) {
            return CRYSTAL_PROVIDERS.computeIfAbsent(crystal,
                    key -> new CrystalProvider(key, level));
        }
    }

    private static @Nullable XianqiaoArsSourceCapAdapter crystalCapabilityOrNull(
            EnergyCrystalBlockEntity crystal) {
        if (crystal.getLevel() == null) return null;
        synchronized (CRYSTAL_CAPS) {
            return CRYSTAL_CAPS.computeIfAbsent(crystal, key ->
                    new XianqiaoArsSourceCapAdapter(
                            () -> key.getLevel() instanceof ServerLevel server
                                    ? key.integrationResourceStore(server) : null));
        }
    }

    private static boolean isSourceContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && "ars_nouveau".equals(id.getNamespace())
                && id.getPath().contains("source_jar");
    }

    private static int sourceContainerCapacity(ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().contains("creative")
                ? Integer.MAX_VALUE : 10_000;
    }

    private static long fillSourceItem(ItemStack input, int current, int maximum,
                                       AtomicEnergyRefill.ResourceStore source) {
        long room = Math.max(0L, (long) maximum - current);
        long offered = Math.min(source.amount(), Math.min(room, Integer.MAX_VALUE));
        if (offered <= 0L) return 0L;
        long moved = source.extract(offered, ResourceTransferAction.EXECUTE);
        if (moved <= 0L) return 0L;
        int next = (int) Math.min((long) maximum, current + moved);
        input.set(DataComponentRegistry.BLOCK_FILL_CONTENTS.get(),
                new BlockFillContents(next));
        return moved;
    }

    private static final class CrystalProvider implements ISpecialSourceProvider {
        private final EnergyCrystalBlockEntity crystal;
        private final ServerLevel level;
        private final ISourceTile source;
        private boolean valid = true;

        private CrystalProvider(EnergyCrystalBlockEntity crystal, ServerLevel level) {
            this.crystal = crystal;
            this.level = level;
            this.source = new XianqiaoArsSourceAdapter(
                    () -> crystal.integrationResourceStore(level));
        }

        @Override public ISourceTile getSource() { return source; }
        @Override public boolean isValid() {
            return valid && !crystal.isRemoved() && crystal.getLevel() == level;
        }
        @Override public BlockPos getCurrentPos() { return crystal.getBlockPos(); }
        private void invalidate() { valid = false; }
    }

    private ArsNouveauCompat() {}
}
