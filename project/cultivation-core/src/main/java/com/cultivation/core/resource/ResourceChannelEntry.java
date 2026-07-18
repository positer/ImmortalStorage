package com.cultivation.core.resource;

import java.util.Objects;

/** Immutable snapshot row from a {@link LongResourceLedger}. */
public record ResourceChannelEntry(ResourceChannelKey key, long amount) {
    public ResourceChannelEntry {
        Objects.requireNonNull(key, "key");
        if (amount < 0L) throw new IllegalArgumentException("amount must be non-negative");
    }
}
