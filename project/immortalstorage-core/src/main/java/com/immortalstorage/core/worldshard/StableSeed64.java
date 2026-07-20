package com.immortalstorage.core.worldshard;

import java.util.Objects;
import java.util.UUID;

/**
 * Loader-independent, stable 64-bit seed derivation shared by every platform
 * adapter.  Its output is part of the persisted generation contract.
 */
public final class StableSeed64 {
    private static final long INITIAL = 0x6A09E667F3BCC909L;

    private StableSeed64() {
    }

    public static long derive(UUID identity, String dimension, long packedPosition,
                              long cycle, long sourceSeed, String sourceKey) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(sourceKey, "sourceKey");
        long state = INITIAL;
        state = mix(state, identity.getMostSignificantBits());
        state = mix(state, identity.getLeastSignificantBits());
        state = mixString(state, dimension);
        state = mix(state, packedPosition);
        state = mix(state, cycle);
        state = mix(state, sourceSeed);
        state = mixString(state, sourceKey);
        long result = avalanche(state);
        return result == 0L ? 0x9E3779B97F4A7C15L : result;
    }

    private static long mixString(long state, String value) {
        long mixed = mix(state, value.length());
        for (int index = 0; index < value.length(); index++) {
            mixed = mix(mixed, value.charAt(index));
        }
        return mixed;
    }

    private static long mix(long state, long value) {
        long mixed = state ^ (value + 0x9E3779B97F4A7C15L);
        mixed = Long.rotateLeft(mixed, 27);
        return mixed * 0x3C79AC492BA7B653L + 0x1C69B3F74AC4AE35L;
    }

    private static long avalanche(long value) {
        value ^= value >>> 27;
        value *= 0x3C79AC492BA7B653L;
        value ^= value >>> 33;
        value *= 0x1C69B3F74AC4AE35L;
        return value ^ value >>> 27;
    }
}
