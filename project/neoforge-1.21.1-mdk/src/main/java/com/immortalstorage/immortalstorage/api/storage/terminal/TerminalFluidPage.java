package com.immortalstorage.immortalstorage.api.storage.terminal;

import java.util.List;

/** Revision-bound page suitable for 256-entry snapshot/delta payload chunks. */
public record TerminalFluidPage(long revision, int totalEntries, int offset, List<TerminalFluidEntry> entries) {
    public TerminalFluidPage {
        if (revision < 0L) throw new IllegalArgumentException("revision must not be negative");
        if (totalEntries < 0) throw new IllegalArgumentException("totalEntries must not be negative");
        if (offset < 0 || offset > totalEntries) throw new IllegalArgumentException("offset is outside the catalog");
        entries = List.copyOf(entries);
        if ((long) offset + entries.size() > totalEntries) {
            throw new IllegalArgumentException("page entries exceed totalEntries");
        }
    }

    public int nextOffset() {
        return offset + entries.size();
    }

    public boolean hasMore() {
        return nextOffset() < totalEntries;
    }
}
