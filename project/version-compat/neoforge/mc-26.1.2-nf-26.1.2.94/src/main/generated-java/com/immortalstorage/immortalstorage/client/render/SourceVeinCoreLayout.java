package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerDisplayState;

/**
 * Geometry of the manager's rotating source core.  The core is a rigid 2x2x2
 * block of 3x3x3 cubes (eight segments) that spins as one body around the
 * block centre [8,8,8].  Segment material follows the same eight-state ladder
 * the old north-face indicator used: state 0 leaves every segment empty, state
 * 7 fills every segment, and each intermediate state advances one more segment
 * from empty to used and on to full.
 */
public final class SourceVeinCoreLayout {
    public static final int SEGMENTS = 8;
    public static final float SIZE = 3.0F / 16.0F;
    public static final float HALF_SIZE = SIZE * 0.5F;

    /** The 2x2x2 grid places segment centres at 5.5/16 and 10.5/16. */
    private static final float LOW = 5.5F / 16.0F;
    private static final float HIGH = 10.5F / 16.0F;

    public enum Material { EMPTY, USED, FULL }

    /** Segment index bit 2 selects X, bit 1 Y, bit 0 Z. */
    public static float centerX(int segment) { return offset(segment >>> 2); }
    public static float centerY(int segment) { return offset(segment >>> 1 & 1); }
    public static float centerZ(int segment) { return offset(segment & 1); }

    public static Material materialFor(int state, int segment) {
        int clampedState = Math.max(0, Math.min(SourceVeinManagerDisplayState.MAX_STATE, state));
        if (clampedState == 0) return Material.EMPTY;
        if (clampedState == SourceVeinManagerDisplayState.MAX_STATE) return Material.FULL;
        if (segment < clampedState - 1) return Material.FULL;
        if (segment == clampedState - 1) return Material.USED;
        return Material.EMPTY;
    }

    private static float offset(int bit) {
        return bit == 0 ? LOW : HIGH;
    }

    private SourceVeinCoreLayout() {}
}
