package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerDisplayState;

/** Stable 3x2 indicator layout and the specified 72-slot to eight-state map. */
public final class SourceVeinManagerIndicatorLayout {
    public static final int COLUMNS = 3;
    public static final int ROWS = 2;
    public static final int INDICATORS = COLUMNS * ROWS;
    public static final int CAPACITY = SourceVeinManagerDisplayState.CAPACITY;
    public static final int MAX_STATE = SourceVeinManagerDisplayState.MAX_STATE;
    public static final float INDICATOR_SIZE = 0.15F;

    public static final int BLACK = 0x090909;
    public static final int BLUE = 0x3F78DA;
    public static final int RED = 0xC84343;

    /** state=ceil(filled*7/72), with zero kept as its own all-black state. */
    public static int stateForFilled(int filled) {
        return SourceVeinManagerDisplayState.stateForFilled(filled);
    }

    public static int colorFor(int state, int indicatorIndex) {
        checkIndicator(indicatorIndex);
        int clampedState = Math.max(0, Math.min(MAX_STATE, state));
        if (clampedState == 0) return BLACK;
        if (clampedState == MAX_STATE) return RED;
        int progressIndex = clampedState - 1;
        if (indicatorIndex < progressIndex) return RED;
        if (indicatorIndex == progressIndex) return BLUE;
        return BLACK;
    }

    public static float centerX(int indicatorIndex) {
        checkIndicator(indicatorIndex);
        return 0.20F + (indicatorIndex % COLUMNS) * 0.30F;
    }

    public static float centerY(int indicatorIndex) {
        checkIndicator(indicatorIndex);
        return indicatorIndex < COLUMNS ? 0.70F : 0.27F;
    }

    private static void checkIndicator(int indicatorIndex) {
        if (indicatorIndex < 0 || indicatorIndex >= INDICATORS) {
            throw new IndexOutOfBoundsException("indicatorIndex=" + indicatorIndex);
        }
    }

    private SourceVeinManagerIndicatorLayout() {}
}
