package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import org.jetbrains.annotations.Nullable;

/** Bidirectional identity bridge between an RS resource key and the Xianqiao ledger. */
interface RsExternalResourceKeyBridge {
    /** Higher priority wins, allowing an installed addon's native key to replace the fallback key. */
    default int priority() {
        return 0;
    }

    @Nullable ResourceChannelKey toResourceKey(ResourceKey key);

    @Nullable ResourceKey toRsKey(ResourceChannelKey key);
}
