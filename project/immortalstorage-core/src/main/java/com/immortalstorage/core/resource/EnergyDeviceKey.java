package com.immortalstorage.core.resource;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Owner and exact world position of one staff-configured energy consumer. */
public record EnergyDeviceKey(
        UUID owner, String dimensionId, int x, int y, int z) {
    private static final Pattern DIMENSION =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public EnergyDeviceKey {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(dimensionId, "dimensionId");
        if (!DIMENSION.matcher(dimensionId).matches()) {
            throw new IllegalArgumentException("invalid dimension id: " + dimensionId);
        }
    }
}
