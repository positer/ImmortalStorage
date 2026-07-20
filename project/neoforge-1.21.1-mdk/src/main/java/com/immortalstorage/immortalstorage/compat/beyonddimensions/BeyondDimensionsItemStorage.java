package com.immortalstorage.immortalstorage.compat.beyonddimensions;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Item-and-components adapter over Beyond Dimensions 0.7.24 UnifiedStorage. */
public final class BeyondDimensionsItemStorage implements TerminalItemStorage {
    private final UnifiedStorage storage;
    private final BeyondDimensionsRevisionTracker revisions;
    private final Runnable onChanged;

    public BeyondDimensionsItemStorage(UnifiedStorage storage, Runnable onChanged) {
        this(storage, new BeyondDimensionsRevisionTracker(storage), onChanged);
    }

    BeyondDimensionsItemStorage(
            UnifiedStorage storage,
            BeyondDimensionsRevisionTracker revisions,
            Runnable onChanged) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.revisions = Objects.requireNonNull(revisions, "revisions");
        this.onChanged = onChanged == null ? () -> {} : onChanged;
    }

    @Override
    public long revision() {
        return revisions.revision();
    }

    @Override
    public List<StorageItemSummary> snapshot() {
        List<StorageItemSummary> result = new ArrayList<>();
        for (KeyAmount entry : storage.getStorage()) {
            if (entry.amount() <= 0L || !(entry.key() instanceof ItemStackKey itemKey)) continue;
            ItemStack prototype = itemKey.copyStackWithCount(1L);
            if (!prototype.isEmpty()) result.add(new StorageItemSummary(prototype, entry.amount()));
        }
        return List.copyOf(result);
    }

    @Override
    public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (key == null || amount <= 0L || action == null) return 0L;
        // UnifiedStorage.insert returns the remainder, not the accepted amount.
        KeyAmount remainder = storage.insert(
                new ItemStackKey(key.prototype()), amount, !action.executes());
        long accepted = acceptedFromRemainder(amount, remainder.amount());
        if (accepted > 0L && action.executes()) onChanged.run();
        return accepted;
    }

    @Override
    public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (key == null || amount <= 0L || action == null) return 0L;
        KeyAmount extracted = storage.extract(
                new ItemStackKey(key.prototype()), amount, !action.executes(), false);
        long actual = boundedAmount(amount, extracted.amount());
        if (actual > 0L && action.executes()) onChanged.run();
        return actual;
    }

    private static long acceptedFromRemainder(long requested, long remainder) {
        return requested - boundedAmount(requested, remainder);
    }

    private static long boundedAmount(long requested, long amount) {
        return Math.min(requested, Math.max(0L, amount));
    }
}
