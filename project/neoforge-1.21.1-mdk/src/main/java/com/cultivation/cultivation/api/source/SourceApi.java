package com.cultivation.cultivation.api.source;

import com.cultivation.cultivation.block.entity.SourceVeinBlockEntity;
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
