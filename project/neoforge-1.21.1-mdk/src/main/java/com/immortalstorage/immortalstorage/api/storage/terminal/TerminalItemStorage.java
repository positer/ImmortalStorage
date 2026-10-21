package com.immortalstorage.immortalstorage.api.storage.terminal;

import java.util.List;

/**
 * Native long-valued item surface for optional storage integrations. Unlike
 * {@code IItemHandler}, amounts are logical totals and are not truncated to an
 * {@code ItemStack} count. Identity always includes complete Data Components.
 */
public interface TerminalItemStorage {
    long revision();

    /** Immutable point-in-time identity/amount snapshot. */
    List<StorageItemSummary> snapshot();

    /**
     * Snapshot restricted to items that physically exist in Xianqiao.
     *
     * <p>An exchange cell is a storage reader, so it must never project the source veins'
     * convertible budget; the exchange itself is a recipe the cell exposes separately. The
     * Xianqiao terminal keeps using {@link #snapshot()}, which does project that budget.</p>
     */
    default List<StorageItemSummary> snapshotRealOnly() {
        return snapshot();
    }

    /** Returns the amount accepted (or that would be accepted). */
    long insert(TerminalEntryKey key, long amount, TerminalStorageAction action);

    /** Returns the amount extracted (or that would be extracted). */
    long extract(TerminalEntryKey key, long amount, TerminalStorageAction action);
}
