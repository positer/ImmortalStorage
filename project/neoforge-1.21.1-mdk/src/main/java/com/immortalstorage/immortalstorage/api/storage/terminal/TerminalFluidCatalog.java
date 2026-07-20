package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Revision-gated fluid directory with stable entry ids and bounded pages.
 * Repeated refreshes at the same authoritative source revision are O(1).
 */
public final class TerminalFluidCatalog {
    public static final int MAX_PAGE_SIZE = 256;

    private final Map<TerminalFluidKey, Long> stableIds = new HashMap<>();
    private long nextEntryId = 1L;
    private long revision;
    private long sourceRevision = Long.MIN_VALUE;
    private long fullScanCount;
    private boolean initialized;
    private List<TerminalFluidEntry> entries = List.of();

    public boolean rebuildIfStale(Map<TerminalFluidKey, Long> backing, long storageRevision) {
        if (backing == null) throw new IllegalArgumentException("backing fluid amounts are required");
        if (storageRevision < 0L) throw new IllegalArgumentException("storage revision must not be negative");
        if (initialized && sourceRevision == storageRevision) return false;
        sourceRevision = storageRevision;
        fullScanCount++;

        List<TerminalFluidEntry> rebuilt = new ArrayList<>(backing.size());
        for (Map.Entry<TerminalFluidKey, Long> amount : backing.entrySet()) {
            if (amount.getKey() == null || amount.getValue() == null || amount.getValue() <= 0L) continue;
            long id = stableIds.computeIfAbsent(amount.getKey(), ignored -> allocateEntryId());
            rebuilt.add(new TerminalFluidEntry(id, amount.getKey().prototype(), amount.getValue()));
        }
        rebuilt.sort(Comparator.comparingLong(TerminalFluidEntry::entryId));
        if (initialized && sameEntries(entries, rebuilt)) return false;
        entries = List.copyOf(rebuilt);
        initialized = true;
        if (revision < Long.MAX_VALUE) revision++;
        return true;
    }

    public long revision() {
        return revision;
    }

    /** Number of O(n) backing-map passes, exposed for deterministic performance tests. */
    public long fullScanCount() {
        return fullScanCount;
    }

    public int size() {
        return entries.size();
    }

    /** Full filtered/sorted view; callers can page the immutable result for payload chunks. */
    public List<TerminalFluidEntry> entries(TerminalQuery query) {
        TerminalQuery effective = query == null ? TerminalQuery.DEFAULT : query;
        String search = effective.normalizedText();
        List<TerminalFluidEntry> filtered = new ArrayList<>();
        for (TerminalFluidEntry entry : entries) {
            if (matches(entry, search)) filtered.add(entry);
        }
        Comparator<TerminalFluidEntry> comparator = comparator(effective.sortOrder())
                .thenComparingLong(TerminalFluidEntry::entryId);
        if (effective.sortDirection() == TerminalQuery.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);
        return List.copyOf(filtered);
    }

    public TerminalFluidEntry find(long entryId) {
        for (TerminalFluidEntry entry : entries) {
            if (entry.entryId() == entryId) return entry;
        }
        return null;
    }

    public TerminalFluidPage page(int offset, int limit) {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (limit <= 0 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        int safeOffset = Math.min(offset, entries.size());
        int end = Math.min(entries.size(), safeOffset + limit);
        return new TerminalFluidPage(revision, entries.size(), safeOffset, entries.subList(safeOffset, end));
    }

    private long allocateEntryId() {
        if (nextEntryId <= 0L) throw new IllegalStateException("terminal fluid entry id space exhausted");
        return nextEntryId++;
    }

    private static boolean matches(TerminalFluidEntry entry, String search) {
        if (search.isEmpty()) return true;
        FluidStack stack = entry.displayStack();
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String descriptionId = stack.getDescriptionId().toLowerCase(Locale.ROOT);
        if (search.startsWith("@")) return fluidId.getNamespace().contains(search.substring(1));
        if (search.startsWith("#")) {
            String needle = search.substring(1);
            return stack.getTags().anyMatch(tag -> tag.location().toString()
                    .toLowerCase(Locale.ROOT).contains(needle))
                    || descriptionId.contains(needle)
                    || stack.getComponentsPatch().toString().toLowerCase(Locale.ROOT).contains(needle);
        }
        return name.contains(search) || fluidId.toString().contains(search)
                || descriptionId.contains(search)
                || stack.getComponentsPatch().toString().toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<TerminalFluidEntry> comparator(TerminalQuery.SortOrder order) {
        return switch (order) {
            case AMOUNT -> Comparator.comparingLong(TerminalFluidEntry::amountMb);
            case NAME -> Comparator.comparing(entry -> entry.displayStack().getHoverName().getString(),
                    String.CASE_INSENSITIVE_ORDER);
            case MOD_ID -> Comparator.comparing(entry -> BuiltInRegistries.FLUID
                    .getKey(entry.displayStack().getFluid()).getNamespace());
        };
    }

    private static boolean sameEntries(List<TerminalFluidEntry> left, List<TerminalFluidEntry> right) {
        if (left.size() != right.size()) return false;
        for (int index = 0; index < left.size(); index++) {
            TerminalFluidEntry a = left.get(index);
            TerminalFluidEntry b = right.get(index);
            if (a.entryId() != b.entryId() || a.amountMb() != b.amountMb()
                    || !FluidStack.isSameFluidSameComponents(a.displayStack(), b.displayStack())) {
                return false;
            }
        }
        return true;
    }
}
