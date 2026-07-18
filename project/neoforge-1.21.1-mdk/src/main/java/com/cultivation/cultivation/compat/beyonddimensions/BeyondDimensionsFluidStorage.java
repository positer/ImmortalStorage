package com.cultivation.cultivation.compat.beyonddimensions;

import com.cultivation.cultivation.api.storage.terminal.TerminalFluidKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Fluid-and-components, long-mB adapter over Beyond Dimensions UnifiedStorage. */
public final class BeyondDimensionsFluidStorage implements TerminalFluidStorage {
    private final UnifiedStorage storage;
    private final BeyondDimensionsRevisionTracker revisions;
    private final Runnable onChanged;

    public BeyondDimensionsFluidStorage(UnifiedStorage storage, Runnable onChanged) {
        this(storage, new BeyondDimensionsRevisionTracker(storage), onChanged);
    }

    BeyondDimensionsFluidStorage(
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
    public Map<TerminalFluidKey, Long> snapshot() {
        LinkedHashMap<TerminalFluidKey, Long> result = new LinkedHashMap<>();
        for (KeyAmount entry : storage.getStorage()) {
            if (entry.amount() <= 0L || !(entry.key() instanceof FluidStackKey fluidKey)) continue;
            FluidStack prototype = fluidKey.copyStackWithCount(1L);
            if (!prototype.isEmpty()) {
                result.merge(TerminalFluidKey.of(prototype), entry.amount(),
                        BeyondDimensionsFluidStorage::saturatingAdd);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public long insert(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (key == null || amountMb <= 0L || action == null) return 0L;
        KeyAmount remainder = storage.insert(
                new FluidStackKey(key.prototype()), amountMb, !action.executes());
        long accepted = acceptedFromRemainder(amountMb, remainder.amount());
        if (accepted > 0L && action.executes()) onChanged.run();
        return accepted;
    }

    @Override
    public long extract(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (key == null || amountMb <= 0L || action == null) return 0L;
        KeyAmount extracted = storage.extract(
                new FluidStackKey(key.prototype()), amountMb, !action.executes(), false);
        long actual = boundedAmount(amountMb, extracted.amount());
        if (actual > 0L && action.executes()) onChanged.run();
        return actual;
    }

    private static long acceptedFromRemainder(long requested, long remainder) {
        return requested - boundedAmount(requested, remainder);
    }

    private static long boundedAmount(long requested, long amount) {
        return Math.min(requested, Math.max(0L, amount));
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
