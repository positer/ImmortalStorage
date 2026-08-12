package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinScheduleTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

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
