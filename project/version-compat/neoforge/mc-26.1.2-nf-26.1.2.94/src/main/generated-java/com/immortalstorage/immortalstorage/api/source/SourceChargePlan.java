package com.immortalstorage.immortalstorage.api.source;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Currency key and batch conversion used to price one source resource. */
public record SourceChargePlan(Identifier providerId, long unitsPerBatch, long outputsPerBatch) {
    public SourceChargePlan {
        Objects.requireNonNull(providerId, "providerId");
        if (unitsPerBatch < 0) throw new IllegalArgumentException("unitsPerBatch must be non-negative");
        if (outputsPerBatch <= 0) throw new IllegalArgumentException("outputsPerBatch must be positive");
    }

    public boolean isFree() {
        return unitsPerBatch == 0;
    }

    public long requiredUnits(long outputs) {
        if (outputs <= 0 || isFree()) return 0;
        long batches = 1 + (outputs - 1) / outputsPerBatch;
        return batches > Long.MAX_VALUE / unitsPerBatch ? Long.MAX_VALUE : batches * unitsPerBatch;
    }
}
