package com.immortalstorage.immortalstorage.compat.arsnouveau;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Ars Nouveau-only Source provider, loaded reflectively behind the mod gate. */
public final class ArsNouveauCompat {
    private static final Map<XianqiaoInterfaceBlockEntity, Provider> PROVIDERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;
    private static boolean initialized;

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        PROVIDERS.clear();
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
        if (level == null || pos == null || !(level.getBlockEntity(pos)
                instanceof XianqiaoInterfaceBlockEntity blockEntity)) return null;
        return provider(blockEntity, level).getSource();
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

    private ArsNouveauCompat() {}
}
