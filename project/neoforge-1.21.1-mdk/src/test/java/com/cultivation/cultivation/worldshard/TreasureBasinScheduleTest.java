package com.cultivation.cultivation.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinScheduleTest {
    @Test
    void runsExactlyOnceEveryTwoThousandFourHundredServerTicks() {
        assertFalse(TreasureBasinSchedule.shouldRun(0L));
        assertFalse(TreasureBasinSchedule.shouldRun(2_399L));
        assertTrue(TreasureBasinSchedule.shouldRun(2_400L));
        assertFalse(TreasureBasinSchedule.shouldRun(2_401L));
        assertTrue(TreasureBasinSchedule.shouldRun(4_800L));
    }

    @Test
    void scheduleDoesNotAcceptOrDependOnMinerLevel() {
        assertTrue(TreasureBasinSchedule.shouldRun(2_400L));
        assertFalse(TreasureBasinSchedule.shouldRun(-2_400L));
    }
}
