package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure aggregation/query logic kept independent from menu slot mechanics. */
public final class TerminalEntryCatalog {
    private final Map<TerminalEntryKey, Long> stableIds = new HashMap<>();
    private long nextEntryId = 1L;
    private long revision;
    private long sourceRevision = Long.MIN_VALUE;
    private List<VirtualSnapshot> sourceVirtualEntries = List.of();
    private long fullScanCount;
    private int fingerprint;
    private boolean initialized;
    private List<TerminalEntry> entries = List.of();

    public boolean rebuildIfChanged(List<ItemStack> backing) {
        return rebuildIfChanged(backing, List.of());
    }

    public boolean rebuildIfChanged(List<ItemStack> backing, List<TerminalVirtualEntry> virtualEntries) {
        boolean changed = rebuild(backing, virtualEntries);
        sourceRevision = Long.MIN_VALUE;
        sourceVirtualEntries = virtualSnapshot(virtualEntries);
        return changed;
    }

    /**
     * Rebuilds only after the authoritative storage revision changes.
     * Repeated menu ticks with the same revision are constant-time.
     */
    public boolean rebuildIfStale(List<ItemStack> backing, long storageRevision) {
        return rebuildIfStale(backing, storageRevision, List.of());
    }

    public boolean rebuildIfStale(List<ItemStack> backing, long storageRevision,
                                  List<TerminalVirtualEntry> virtualEntries) {
        if (storageRevision < 0L) throw new IllegalArgumentException("storage revision must not be negative");
        List<VirtualSnapshot> nextVirtualEntries = virtualSnapshot(virtualEntries);
        if (initialized && sourceRevision == storageRevision
                && sourceVirtualEntries.equals(nextVirtualEntries)) return false;
        boolean changed = rebuild(backing, virtualEntries);
        // Commit source cursors only after a successful rebuild. A malformed
        // source must never poison the constant-time stale check.
        sourceRevision = storageRevision;
        sourceVirtualEntries = nextVirtualEntries;
        return changed;
    }

    /**
     * Revision-gated rebuild from the data-owned logical summary. This avoids
     * every open menu repeating the physical storage aggregation pass.
     */
    public boolean rebuildSummariesIfStale(List<StorageItemSummary> backing, long storageRevision,
                                           List<TerminalVirtualEntry> virtualEntries) {
        if (backing == null) throw new IllegalArgumentException("backing summaries are required");
        if (storageRevision < 0L) throw new IllegalArgumentException("storage revision must not be negative");
        List<VirtualSnapshot> nextVirtualEntries = virtualSnapshot(virtualEntries);
        if (initialized && sourceRevision == storageRevision
                && sourceVirtualEntries.equals(nextVirtualEntries)) return false;
        fullScanCount++;
        Map<TerminalEntryKey, MutableEntry> grouped = new HashMap<>();
        for (StorageItemSummary summary : backing) {
            TerminalEntryKey key = TerminalEntryKey.of(summary.prototype());
            MutableEntry entry = grouped.computeIfAbsent(key, ignored ->
                    new MutableEntry(stableIds.computeIfAbsent(key, unused -> nextEntryId++),
                            summary.prototype()));
            entry.amount = saturatingAdd(entry.amount, summary.amount());
        }
        applyVirtualEntries(grouped, virtualEntries);
        boolean changed = commit(grouped);
        sourceRevision = storageRevision;
        sourceVirtualEntries = nextVirtualEntries;
        return changed;
    }

    private boolean rebuild(List<ItemStack> backing, List<TerminalVirtualEntry> virtualEntries) {
        Objects.requireNonNull(backing, "backing");
        Objects.requireNonNull(virtualEntries, "virtualEntries");
        fullScanCount++;
        Map<TerminalEntryKey, MutableEntry> grouped = new HashMap<>();
        for (ItemStack stack : backing) {
            if (stack.isEmpty()) continue;
            TerminalEntryKey key = TerminalEntryKey.of(stack);
            MutableEntry entry = grouped.computeIfAbsent(key, ignored ->
                    new MutableEntry(stableIds.computeIfAbsent(key, unused -> nextEntryId++), stack.copyWithCount(1)));
            entry.amount = saturatingAdd(entry.amount, stack.getCount());
        }
        applyVirtualEntries(grouped, virtualEntries);
        return commit(grouped);
    }

    private void applyVirtualEntries(Map<TerminalEntryKey, MutableEntry> grouped,
                                     List<TerminalVirtualEntry> virtualEntries) {
        for (TerminalVirtualEntry virtualEntry : virtualEntries) {
            TerminalEntryKey key = TerminalEntryKey.of(virtualEntry.prototype());
            MutableEntry entry = grouped.computeIfAbsent(key, ignored ->
                    new MutableEntry(stableIds.computeIfAbsent(key, unused -> nextEntryId++),
                            virtualEntry.prototype()));
            // A virtual entry is the authoritative logical total. Physical
            // stacks with the same key remain ordinary persisted objects, but
            // must never be added to (and overflow) the virtual projection.
            entry.amount = virtualEntry.amount();
        }
    }

    private boolean commit(Map<TerminalEntryKey, MutableEntry> grouped) {
        // Stale revisions are rejected by the menu protocol, so identities no
        // longer present do not need an unbounded lifetime reservation.
        stableIds.keySet().retainAll(grouped.keySet());
        List<TerminalEntry> rebuilt = new ArrayList<>(grouped.size());
        for (MutableEntry entry : grouped.values()) {
            rebuilt.add(new TerminalEntry(entry.id, entry.prototype, entry.amount));
        }
        rebuilt.sort(Comparator.comparingLong(TerminalEntry::entryId));
        int nextFingerprint = fingerprint(rebuilt);
        if (initialized && nextFingerprint == fingerprint && sameEntries(entries, rebuilt)) return false;
        entries = List.copyOf(rebuilt);
        fingerprint = nextFingerprint;
        initialized = true;
        revision++;
        return true;
    }

    /** Number of O(n) backing-list passes, exposed for deterministic performance tests. */
    public long fullScanCount() {
        return fullScanCount;
    }

    public long revision() {
        return revision;
    }

    public List<TerminalEntry> entries(TerminalQuery query) {
        TerminalQuery effective = query == null ? TerminalQuery.DEFAULT : query;
        String search = effective.normalizedText();
        List<TerminalEntry> filtered = new ArrayList<>();
        for (TerminalEntry entry : entries) {
            if (matches(entry, search)) filtered.add(entry);
        }
        Comparator<TerminalEntry> comparator = comparator(effective.sortOrder())
                .thenComparingLong(TerminalEntry::entryId);
        if (effective.sortDirection() == TerminalQuery.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);
        return List.copyOf(filtered);
    }

    public TerminalEntry find(long entryId) {
        for (TerminalEntry entry : entries) {
            if (entry.entryId() == entryId) return entry;
        }
        return null;
    }

    private static boolean matches(TerminalEntry entry, String search) {
        if (search.isEmpty()) return true;
        ItemStack stack = entry.displayStack();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
        if (search.startsWith("@")) return itemId.getNamespace().contains(search.substring(1));
        if (search.startsWith("#")) {
            String needle = search.substring(1);
            return com.immortalstorage.immortalstorage.compat.mc2612.CompatTags.getTags(stack).anyMatch(tag -> tag.location().toString().toLowerCase(java.util.Locale.ROOT).contains(needle));
        }
        return name.contains(search) || itemId.toString().contains(search);
    }

    private static Comparator<TerminalEntry> comparator(TerminalQuery.SortOrder order) {
        return switch (order) {
            case AMOUNT -> Comparator.comparingLong(TerminalEntry::amount);
            case NAME -> Comparator.comparing(entry -> entry.displayStack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
            case MOD_ID -> Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.displayStack().getItem()).getNamespace());
        };
    }

    private static int fingerprint(List<TerminalEntry> entries) {
        int hash = 1;
        for (TerminalEntry entry : entries) {
            hash = 31 * hash + ItemStack.hashItemAndComponents(entry.displayStack());
            hash = 31 * hash + Long.hashCode(entry.amount());
        }
        return hash;
    }

    private static List<VirtualSnapshot> virtualSnapshot(List<TerminalVirtualEntry> virtualEntries) {
        Objects.requireNonNull(virtualEntries, "virtualEntries");
        List<VirtualSnapshot> snapshot = new ArrayList<>(virtualEntries.size());
        for (TerminalVirtualEntry entry : virtualEntries) {
            snapshot.add(new VirtualSnapshot(TerminalEntryKey.of(entry.prototype()), entry.amount()));
        }
        return List.copyOf(snapshot);
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static boolean sameEntries(List<TerminalEntry> left, List<TerminalEntry> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            TerminalEntry a = left.get(i);
            TerminalEntry b = right.get(i);
            if (a.entryId() != b.entryId() || a.amount() != b.amount()
                    || !ItemStack.isSameItemSameComponents(a.displayStack(), b.displayStack())) {
                return false;
            }
        }
        return true;
    }

    private static final class MutableEntry {
        private final long id;
        private final ItemStack prototype;
        private long amount;

        private MutableEntry(long id, ItemStack prototype) {
            this.id = id;
            this.prototype = prototype;
        }
    }

    private record VirtualSnapshot(TerminalEntryKey key, long amount) {}
}
