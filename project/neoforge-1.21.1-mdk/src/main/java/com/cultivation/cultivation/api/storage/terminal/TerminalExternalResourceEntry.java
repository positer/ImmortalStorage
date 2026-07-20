package com.cultivation.cultivation.api.storage.terminal;

import com.cultivation.core.resource.ResourceChannelKey;

import java.util.Objects;

/** One exact long-valued external resource displayed in the unified terminal directory. */
public record TerminalExternalResourceEntry(long entryId, ResourceChannelKey key, long amount) {
    public TerminalExternalResourceEntry {
        if (entryId == 0L) throw new IllegalArgumentException("entryId must be non-zero");
        Objects.requireNonNull(key, "key");
        if (amount < 0L) throw new IllegalArgumentException("amount must be non-negative");
    }
}
