package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Persistent produced units shared by manual, active, and capability extraction. */
public final class SourceVeinBuffer {
    private static final String TAG = "CachedUnits";

    private final long capacity;
    private long available;

    public SourceVeinBuffer(long capacity) {
        if (capacity <= 0L) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public long available() {
        return available;
    }

    public long room() {
        return capacity - available;
    }

    /** Materializes a non-consuming source in the same persistent cache used by every adapter. */
    public long fillToCapacityWithoutCharge() {
        long added = room();
        if (added > 0L) available = capacity;
        return added;
    }

    /** Adds one complete production cycle only after its charge is secured. */
    public boolean addCycle(long producedUnits, BooleanSupplier secureCharge) {
        Objects.requireNonNull(secureCharge, "secureCharge");
        if (producedUnits <= 0L || producedUnits > room()) return false;
        if (!secureCharge.getAsBoolean()) return false;
        available += producedUnits;
        return true;
    }

    public long extract(long requestedUnits, boolean simulate) {
        if (requestedUnits <= 0L || available <= 0L) return 0L;
        long extracted = Math.min(requestedUnits, available);
        if (!simulate) available -= extracted;
        return extracted;
    }

    /** Restores a known refused delivery without purchasing the same units again. */
    public long restore(long units) {
        if (units <= 0L || room() <= 0L) return 0L;
        long restored = Math.min(units, room());
        available += restored;
        return restored;
    }

    public void save(CompoundTag tag) {
        tag.putLong(TAG, available);
    }

    public void load(CompoundTag tag) {
        available = Math.max(0L, Math.min(capacity, tag.getLong(TAG)));
    }

    public static int manualItemBatch(VeinKind kind, int maximumStackSize) {
        Objects.requireNonNull(kind, "kind");
        if (kind.fluid || maximumStackSize <= 0) return 0;
        return kind.yuanCostPerBatch == 0L
                ? maximumStackSize
                : Math.min(maximumStackSize, kind.outputsPerBatch);
    }

    /** Converts the owner's currency cap into the largest real cache target. */
    public static long targetForChargeCapacity(long currencyCap, long unitsPerBatch,
                                               long outputsPerBatch, long bufferCapacity) {
        if (unitsPerBatch < 0L) throw new IllegalArgumentException("unitsPerBatch must not be negative");
        if (outputsPerBatch <= 0L) throw new IllegalArgumentException("outputsPerBatch must be positive");
        if (bufferCapacity <= 0L) throw new IllegalArgumentException("bufferCapacity must be positive");
        if (unitsPerBatch == 0L || currencyCap < 0L) return bufferCapacity;
        long affordableBatches = currencyCap / unitsPerBatch;
        return multiplyClamped(affordableBatches, outputsPerBatch, bufferCapacity);
    }

    /**
     * Selects one aggregate refill backed by the affordable paid production
     * batches. A final partial batch is allowed only to reach the exact target;
     * its charge still rounds up through {@link com.cultivation.cultivation.api.source.SourceChargePlan}.
     */
    public static long affordableRefill(long target, long available, long spendableUnits,
                                        long unitsPerBatch, long outputsPerBatch) {
        if (unitsPerBatch <= 0L) throw new IllegalArgumentException("unitsPerBatch must be positive");
        if (outputsPerBatch <= 0L) throw new IllegalArgumentException("outputsPerBatch must be positive");
        if (target <= 0L || available >= target || spendableUnits < unitsPerBatch) return 0L;
        long missing = target - Math.max(0L, available);
        long affordableBatches = spendableUnits / unitsPerBatch;
        return multiplyClamped(affordableBatches, outputsPerBatch, missing);
    }

    private static long multiplyClamped(long left, long right, long maximum) {
        if (left <= 0L || right <= 0L || maximum <= 0L) return 0L;
        if (left > maximum / right) return maximum;
        return Math.min(maximum, left * right);
    }
}
