package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import org.jetbrains.annotations.Nullable;

/** Built-in no-addon and legacy fallback for every Xianqiao external resource. */
final class ImmortalStorageRsExternalResourceKeyBridge implements RsExternalResourceKeyBridge {
    static final ImmortalStorageRsExternalResourceKeyBridge INSTANCE =
            new ImmortalStorageRsExternalResourceKeyBridge();

    private ImmortalStorageRsExternalResourceKeyBridge() {}

    @Override public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override public @Nullable ResourceChannelKey toResourceKey(ResourceKey key) {
        return key instanceof RsExternalResource external ? external.resource() : null;
    }

    @Override public ResourceKey toRsKey(ResourceChannelKey key) {
        return new RsExternalResource(key);
    }
}
