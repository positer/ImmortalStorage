package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEKey;
import com.immortalstorage.core.resource.ResourceChannelKey;
import org.jetbrains.annotations.Nullable;

/** Direct bridge for ImmortalStorage's built-in external resource key. */
final class ImmortalStorageExternalResourceKeyBridge implements Ae2ExternalKeyBridge {
    static final ImmortalStorageExternalResourceKeyBridge INSTANCE =
            new ImmortalStorageExternalResourceKeyBridge();

    private ImmortalStorageExternalResourceKeyBridge() {}

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
        return key instanceof ImmortalStorageExternalResourceKey external ? external.resource() : null;
    }

    @Override
    public AEKey toAeKey(ResourceChannelKey key) {
        return new ImmortalStorageExternalResourceKey(key);
    }
}
