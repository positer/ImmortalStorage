package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import net.minecraft.resources.ResourceLocation;

/** Exact Refined Storage 2.0.9 optional bootstrap. */
public final class RsCompat {
    private static final ResourceLocation STORAGE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "xianqiao_exchange");
    private static final ResourceLocation EXTERNAL_RESOURCE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "xianqiao_external");
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        var registry = RefinedStorageApi.INSTANCE.getStorageTypeRegistry();
        if (registry.get(STORAGE_TYPE_ID).isEmpty()) {
            registry.register(STORAGE_TYPE_ID, RsXianqiaoStorageType.INSTANCE);
        }
        var resources = RefinedStorageApi.INSTANCE.getResourceTypeRegistry();
        if (resources.get(EXTERNAL_RESOURCE_TYPE_ID).isEmpty()) {
            resources.register(EXTERNAL_RESOURCE_TYPE_ID, RsExternalResourceType.INSTANCE);
        }
        initialized = true;
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Registered RS 2.0.9 Xianqiao storage and external-resource types");
    }

    private RsCompat() {
    }
}
