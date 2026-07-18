package com.cultivation.core.resource;

import java.util.Objects;

/** Direction and per-tick ceiling chosen for one device using the spirit staff. */
public record EnergyDeviceBinding(
        EnergyDeviceKey key, BlockSide inputSide, long inputLimitPerTick) {
    public EnergyDeviceBinding {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(inputSide, "inputSide");
        if (inputLimitPerTick <= 0L) {
            throw new IllegalArgumentException("inputLimitPerTick must be positive");
        }
    }

    public long limitRequest(long requested) {
        return requested <= 0L ? 0L : Math.min(requested, inputLimitPerTick);
    }
}
