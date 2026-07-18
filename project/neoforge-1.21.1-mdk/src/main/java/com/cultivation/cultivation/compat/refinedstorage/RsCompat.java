package com.cultivation.cultivation.compat.refinedstorage;

import com.cultivation.cultivation.CultivationMod;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import net.minecraft.resources.ResourceLocation;

/** Exact Refined Storage 2.0.9 optional bootstrap. */
public final class RsCompat {
    private static final ResourceLocation STORAGE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, "xianqiao_exchange");
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        var registry = RefinedStorageApi.INSTANCE.getStorageTypeRegistry();
        if (registry.get(STORAGE_TYPE_ID).isEmpty()) {
            registry.register(STORAGE_TYPE_ID, RsXianqiaoStorageType.INSTANCE);
        }
        initialized = true;
        CultivationMod.LOG.info(
                "[Compat/RS] Registered RS 2.0.9 Xianqiao exchange-disk storage type");
    }

    private RsCompat() {
    }
}
