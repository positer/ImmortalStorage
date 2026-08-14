package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.worldshard.TreasureBasinSchedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The reinforcement plugin accelerates the Treasure Basin's schedule instead
 * of multiplying one roll's drop count: a stronger plugin completes rolls more
 * often while every roll keeps its vanilla drop size.
 */
final class TreasureBasinMultiplierTest {
    @Test
    void aPluginAdvancesTheScheduleWithoutTouchingDropCount() {
        // 4x plugin: every tick advances 4 ticks of schedule progress.
        TreasureBasinSchedule.Advance advance = TreasureBasinSchedule.advance(
                0L, 4, TreasureBasinSchedule.INTERVAL_TICKS);

        assertEquals(0L, advance.rolls());
        assertEquals(4L, advance.remainder());
    }

    @Test
    void aStrongerPluginCompletesMoreRollsInTheSameWindow() {
        // 256x plugin can cross one full interval from an almost-complete bar.
        long almostDone = TreasureBasinSchedule.INTERVAL_TICKS - 128L;
        TreasureBasinSchedule.Advance advance = TreasureBasinSchedule.advance(
                almostDone, 256, TreasureBasinSchedule.INTERVAL_TICKS);

        assertEquals(1L, advance.rolls(), "crossing the interval completes exactly one roll");
        assertEquals(128L, advance.remainder(), "the leftover 128 ticks carry forward");
    }

    @Test
    void aDefaultBasinWithNoPluginStillRunsAtOneX() {
        TreasureBasinSchedule.Advance advance = TreasureBasinSchedule.advance(
                2_399L, 1, TreasureBasinSchedule.INTERVAL_TICKS);

        assertEquals(1L, advance.rolls());
        assertEquals(0L, advance.remainder());
    }

    @Test
    void negativeOrZeroIntervalIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TreasureBasinSchedule.advance(0L, 1, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> TreasureBasinSchedule.advance(0L, 1, -1L));
    }
}
