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
        RefinedStorageApiDescriptor.Probe amountProbe =
                RefinedStorageApiDescriptor.probe(RsCompat.class.getClassLoader());
        RsAmountPolicy.setLongAmountApiSupported(amountProbe.supportsLongAmounts());
        if (amountProbe.compatible()) {
            ImmortalStorageMod.LOG.info(
                    "[Compat/RS] storage amount probe passed: long insert/extract paths are active");
        } else {
            ImmortalStorageMod.LOG.warn(
                    "[Compat/RS] storage amount probe failed ({}); using the confirmed int-safe display fallback",
                    amountProbe.summary());
        }
        RsExternalResourceKeyBridges.register(ImmortalStorageRsExternalResourceKeyBridge.INSTANCE);
        InstalledAddonRsExternalResourceKeyBridges.registerPresent();
        RsExternalResourceKeyBridges.register(RegisteredRsResourceBridge.INSTANCE);
        RefinedStorageApi.INSTANCE.addExternalStorageProviderFactory(
                XianqiaoRsManagerExternalStorageProvider::new);
        RefinedStorageApi.INSTANCE.addGridResourceRepositoryMapper(
                RsExternalResource.class, RsExternalGridResourceMapper.INSTANCE);
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
                "[Compat/RS] Registered RS 2.0.9 Xianqiao storage and {} external-resource key bridges",
                RsExternalResourceKeyBridges.registered().size());
        ImmortalStorageMod.LOG.info(
                "[Compat/RS] Discovered registered addon resource types {} and storage types {}",
                RegisteredRsResourceBridge.registeredAddonResourceTypeIds(),
                RegisteredRsResourceBridge.registeredAddonStorageTypeIds());
    }

    private RsCompat() {
    }
}
