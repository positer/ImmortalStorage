package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineTickSyncTest {
    @Test
    void coalescingOnlyControlsSynchronizationCadence() {
        assertEquals(4L, MachineTickSync.FAST_SYNC_INTERVAL_TICKS);
        assertTrue(MachineTickSync.due(100L, Long.MIN_VALUE));
        assertTrue(MachineTickSync.due(104L, 100L));
        assertFalse(MachineTickSync.due(103L, 100L));
    }

    @Test
    void aFreshMachineAndAClockRollbackSynchronizeImmediately() {
        assertTrue(MachineTickSync.due(9L, 10L));
    }

    @Test
    void repeatedTickerInvocationsWithoutWorldTimeAdvanceStillSynchronize() {
        assertFalse(MachineTickSync.due(100L, 100L, 3L, 0L));
        assertTrue(MachineTickSync.due(100L, 100L, 4L, 0L));
    }

    @Test
    void eitherClockCanWakeTheSynchronizer() {
        assertTrue(MachineTickSync.due(104L, 100L, 1L, 0L));
        assertTrue(MachineTickSync.due(100L, 100L, 5L, 0L));
    }
}
