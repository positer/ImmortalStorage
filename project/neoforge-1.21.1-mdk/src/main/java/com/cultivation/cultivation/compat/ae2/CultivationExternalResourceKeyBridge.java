package com.cultivation.cultivation.compat.ae2;

import appeng.api.stacks.AEKey;
import com.cultivation.core.resource.ResourceChannelKey;
import org.jetbrains.annotations.Nullable;

/** Direct bridge for Cultivation's built-in external resource key. */
final class CultivationExternalResourceKeyBridge implements Ae2ExternalKeyBridge {
    static final CultivationExternalResourceKeyBridge INSTANCE =
            new CultivationExternalResourceKeyBridge();

    private CultivationExternalResourceKeyBridge() {}

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
        return key instanceof CultivationExternalResourceKey external ? external.resource() : null;
    }

    @Override
    public AEKey toAeKey(ResourceChannelKey key) {
        return new CultivationExternalResourceKey(key);
    }
}
