package com.cultivation.cultivation.compat.ifsouls;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.buuz135.industrialforegoingsouls.capabilities.SoulCapabilities;
import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.entity.ModBlockEntities;
import com.cultivation.cultivation.block.entity.XianqiaoInterfaceBlockEntity;
import com.cultivation.core.resource.AtomicEnergyRefill;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/** Industrial Foregoing Souls-only bootstrap, loaded after its exact mod gate. */
public final class IndustrialForegoingSoulsCompat {
    private static volatile Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> storageResolver = ignored -> null;

    public static void installBridge(Function<XianqiaoInterfaceBlockEntity,
            AtomicEnergyRefill.ResourceStore> resolver) {
        storageResolver = resolver == null ? ignored -> null : resolver;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(SoulCapabilities.BLOCK,
                ModBlockEntities.XIANQIAO_INTERFACE.get(),
                IndustrialForegoingSoulsCompat::handlerOrNull);
        CultivationMod.LOG.info(
                "[Compat/IndustrialForegoingSouls] Registered owner-bound Xianqiao Interface soul capability");
    }

    private static @Nullable ISoulHandler handlerOrNull(
            XianqiaoInterfaceBlockEntity blockEntity, @Nullable Direction side) {
        AtomicEnergyRefill.ResourceStore current = storageResolver.apply(blockEntity);
        if (current == null || side == null) return null;
        return new XianqiaoSoulHandler(
                () -> storageResolver.apply(blockEntity),
                () -> mode(blockEntity.getSideMode(side)));
    }

    private static XianqiaoSoulHandler.Mode mode(
            XianqiaoInterfaceBlockEntity.SideMode sideMode) {
        return switch (sideMode) {
            case PULL -> XianqiaoSoulHandler.Mode.PULL;
            case PUSH -> XianqiaoSoulHandler.Mode.PUSH;
            case DISABLED -> XianqiaoSoulHandler.Mode.DISABLED;
        };
    }

    private IndustrialForegoingSoulsCompat() {
    }
}
