package com.cultivation.core.resource;

import java.util.Objects;

/** Immutable six-bit mask stored with one mixed external-resource cache slot. */
public record ResourceFaceMask(int bits) {
    private static final int ALL_BITS = (1 << ResourceFace.values().length) - 1;

    public ResourceFaceMask {
        bits &= ALL_BITS;
    }

    public static ResourceFaceMask none() {
        return new ResourceFaceMask(0);
    }

    public static ResourceFaceMask all() {
        return new ResourceFaceMask(ALL_BITS);
    }

    public static ResourceFaceMask fromBits(int bits) {
        return new ResourceFaceMask(bits);
    }

    public boolean includes(ResourceFace face) {
        Objects.requireNonNull(face, "face");
        return (bits & (1 << face.ordinal())) != 0;
    }

    public ResourceFaceMask with(ResourceFace face, boolean enabled) {
        Objects.requireNonNull(face, "face");
        int bit = 1 << face.ordinal();
        return new ResourceFaceMask(enabled ? bits | bit : bits & ~bit);
    }
}
