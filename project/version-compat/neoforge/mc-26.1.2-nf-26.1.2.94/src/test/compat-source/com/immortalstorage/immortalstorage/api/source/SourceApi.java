package com.immortalstorage.immortalstorage.api.source;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Public source endpoint access for addons and compat bridges.
 */
public final class SourceApi {
    public static @Nullable SourceEndpoint resolve(@Nullable SourceVeinBlockEntity source) {
        return source;
    }

    private SourceApi() {}
}
