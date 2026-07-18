package com.cultivation.core.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * O(1) finite long-resource ledger shared by optional energy, chemical, mana,
 * source and soul adapters.
 *
 * <p>The ledger is deliberately independent of Minecraft registries and NBT.
 * A version adapter persists {@link #snapshot()} using the registry format
 * available in that Minecraft range. Simulation never changes either the map
 * or its revision.</p>
 */
public final class LongResourceLedger {
    private final Map<ResourceChannelKey, Long> amounts = new LinkedHashMap<>();
    private long revision;

    public long revision() {
        return revision;
    }

    public long amount(ResourceChannelKey key) {
        Objects.requireNonNull(key, "key");
        return amounts.getOrDefault(key, 0L);
    }

    public long insert(ResourceChannelKey key, long requested, ResourceTransferAction action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
        if (requested <= 0L) return 0L;
        long stored = amounts.getOrDefault(key, 0L);
        long accepted = Math.min(requested, Long.MAX_VALUE - stored);
        if (accepted > 0L && action.executes()) {
            amounts.put(key, stored + accepted);
            advanceRevision();
        }
        return accepted;
    }

    public long extract(ResourceChannelKey key, long requested, ResourceTransferAction action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
        if (requested <= 0L) return 0L;
        long stored = amounts.getOrDefault(key, 0L);
        long extracted = Math.min(stored, requested);
        if (extracted > 0L && action.executes()) {
            long remaining = stored - extracted;
            if (remaining == 0L) amounts.remove(key);
            else amounts.put(key, remaining);
            advanceRevision();
        }
        return extracted;
    }

    /** Immutable insertion-ordered snapshot; zero-valued entries are never retained. */
    public List<ResourceChannelEntry> snapshot() {
        List<ResourceChannelEntry> entries = new ArrayList<>(amounts.size());
        amounts.forEach((key, amount) -> entries.add(new ResourceChannelEntry(key, amount)));
        return List.copyOf(entries);
    }

    /**
     * Restores persisted finite balances without manufacturing revision events.
     * Duplicate keys are saturating-added so malformed legacy data cannot wrap.
     */
    public void restore(List<ResourceChannelEntry> entries, long restoredRevision) {
        Objects.requireNonNull(entries, "entries");
        amounts.clear();
        for (ResourceChannelEntry entry : entries) {
            Objects.requireNonNull(entry, "entry");
            if (entry.amount() == 0L) continue;
            long current = amounts.getOrDefault(entry.key(), 0L);
            long merged = Long.MAX_VALUE - current < entry.amount()
                    ? Long.MAX_VALUE : current + entry.amount();
            amounts.put(entry.key(), merged);
        }
        revision = Math.max(0L, restoredRevision);
    }

    private void advanceRevision() {
        if (revision < Long.MAX_VALUE) revision++;
    }
}
