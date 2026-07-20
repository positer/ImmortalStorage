package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SourceVeinManagerIndicatorLayoutTest {
    @Test
    void seventyTwoSlotsMapToExactlyEightSpecifiedStates() {
        assertEquals(0, SourceVeinManagerIndicatorLayout.stateForFilled(0));
        assertEquals(1, SourceVeinManagerIndicatorLayout.stateForFilled(1));
        assertEquals(1, SourceVeinManagerIndicatorLayout.stateForFilled(10));
        assertEquals(2, SourceVeinManagerIndicatorLayout.stateForFilled(11));
        assertEquals(6, SourceVeinManagerIndicatorLayout.stateForFilled(61));
        assertEquals(7, SourceVeinManagerIndicatorLayout.stateForFilled(62));
        assertEquals(7, SourceVeinManagerIndicatorLayout.stateForFilled(72));
        assertEquals(7, SourceVeinManagerIndicatorLayout.stateForFilled(Integer.MAX_VALUE));
    }

    @Test
    void statesUseBlackBlueRedProgressionWithoutBakedMemberModels() {
        for (int index = 0; index < 6; index++) {
            assertEquals(SourceVeinManagerIndicatorLayout.BLACK,
                    SourceVeinManagerIndicatorLayout.colorFor(0, index));
            assertEquals(SourceVeinManagerIndicatorLayout.RED,
                    SourceVeinManagerIndicatorLayout.colorFor(7, index));
        }
        for (int state = 1; state <= 6; state++) {
            for (int index = 0; index < 6; index++) {
                int expected = index < state - 1 ? SourceVeinManagerIndicatorLayout.RED
                        : index == state - 1 ? SourceVeinManagerIndicatorLayout.BLUE
                        : SourceVeinManagerIndicatorLayout.BLACK;
                assertEquals(expected, SourceVeinManagerIndicatorLayout.colorFor(state, index),
                        "state=" + state + ", index=" + index);
            }
        }
    }

    @Test
    void indicatorPositionsStayInsideTheSixBookshelfBays() {
        for (int column = 0; column < 3; column++) {
            assertEquals(SourceVeinManagerIndicatorLayout.centerX(column),
                    SourceVeinManagerIndicatorLayout.centerX(column + 3));
        }
        assertThrows(IndexOutOfBoundsException.class,
                () -> SourceVeinManagerIndicatorLayout.centerX(-1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> SourceVeinManagerIndicatorLayout.centerY(6));
    }
}
