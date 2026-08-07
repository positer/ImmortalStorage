package com.immortalstorage.immortalstorage.compat.ifsouls;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.buuz135.industrialforegoingsouls.capabilities.SoulCapabilities;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.concurrent.atomic.AtomicBoolean;

/** Industrial Foregoing Souls-only bootstrap, loaded after its exact mod gate. */
public final class IndustrialForegoingSoulsCompat {
    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;
    private static final AtomicBoolean ACTIVE_TRANSFER_HOOK_INSTALLED = new AtomicBoolean();

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
        if (ACTIVE_TRANSFER_HOOK_INSTALLED.compareAndSet(false, true)) {
            XianqiaoInterfaceCompatHooks.register(new XianqiaoInterfaceCompatHooks.Hook() {
                @Override
                public void serverTick(
                        XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
                    activeTransferTick(blockEntity, level);
                }
            });
        }
    }

    private static void activeTransferTick(
            XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
        for (Direction side : Direction.values()) {
            XianqiaoInterfaceBlockEntity.SideMode mode = blockEntity.getSideMode(side);
            if (mode == XianqiaoInterfaceBlockEntity.SideMode.DISABLED) continue;
            AtomicEnergyRefill.ResourceStore storage = blockEntity.resolveExternalResourceCache(
                    ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL, side);
            if (storage == null) continue;
            ISoulHandler adjacent = level.getCapability(
                    SoulCapabilities.BLOCK,
                    blockEntity.getBlockPos().relative(side), side.getOpposite());
            if (adjacent == null) continue;
            if (mode == XianqiaoInterfaceBlockEntity.SideMode.PUSH
                    && blockEntity.isActivePushEnabled()) {
                SoulActiveTransfer.push(storage, adjacent);
            } else if (mode == XianqiaoInterfaceBlockEntity.SideMode.PULL
                    && blockEntity.isActivePullEnabled()) {
                SoulActiveTransfer.pull(adjacent, storage);
            }
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(SoulCapabilities.BLOCK,
                ModBlockEntities.XIANQIAO_INTERFACE.get(),
                IndustrialForegoingSoulsCompat::handlerOrNull);
        event.registerBlockEntity(SoulCapabilities.BLOCK,
                ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get(),
                IndustrialForegoingSoulsCompat::handlerOrNull);
        ImmortalStorageMod.LOG.info(
                "[Compat/IndustrialForegoingSouls] Registered owner-bound Xianqiao Interface soul capability");
    }

    private static @Nullable ISoulHandler handlerOrNull(
            XianqiaoInterfaceBlockEntity blockEntity, @Nullable Direction side) {
        AtomicEnergyRefill.ResourceStore current = blockEntity.resolveExternalResourceCache(
                ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL);
        if (current == null) return null;
        return new XianqiaoSoulHandler(
                () -> blockEntity.resolveExternalResourcePipeStore(
                        ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL, side));
    }

    private IndustrialForegoingSoulsCompat() {
    }
}
