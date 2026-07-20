package com.cultivation.cultivation.compat.ae2;

import appeng.api.stacks.AEKey;
import com.cultivation.core.resource.ResourceChannelKey;
import org.jetbrains.annotations.Nullable;

/**
 * Optional bidirectional identity bridge between an addon-defined AE2 key and
 * Cultivation's loader-neutral external-resource ledger.
 *
 * <p>Implementations belong to isolated compatibility packages and are only
 * registered when every required mod is present.</p>
 */
public interface Ae2ExternalKeyBridge {
    /** Higher priority wins when multiple installed bridges represent one logical resource. */
    default int priority() {
        return 0;
    }

    @Nullable ResourceChannelKey toResourceKey(AEKey key);

    @Nullable AEKey toAeKey(ResourceChannelKey key);
}
