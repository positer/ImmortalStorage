package com.cultivation.cultivation.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardOperatingStatusTest {
    @Test
    void inactiveStructureNeverReportsAWorkingState() {
        assertEquals(WorldShardMinerStatus.INACTIVE,
                WorldShardMinerStatus.resolve(false, false, false));
    }

    @Test
    void eachActiveExternalMachineStopsOnlyWhenItsOwnCacheIsFull() {
        assertEquals(WorldShardMinerStatus.ACTIVE,
                WorldShardMinerStatus.resolve(true, false, false));
        WorldShardMinerStatus full = WorldShardMinerStatus.resolve(true, true, false);
        assertEquals(WorldShardMinerStatus.CACHE_FULL, full);
        assertTrue(full.blocksOutput());
    }

    @Test
    void personalRealmDoesNotSilentlyFallBackWhenUnifiedStorageIsUnavailable() {
        WorldShardMinerStatus unavailable = WorldShardMinerStatus.resolve(true, false, true);
        assertEquals(WorldShardMinerStatus.STORAGE_UNAVAILABLE, unavailable);
        assertTrue(unavailable.blocksOutput());
    }

    @Test
    void minerAndBasinCacheFullStatesDoNotPauseEachOther() {
        WorldShardMinerStatus minerFull = WorldShardMinerStatus.resolve(true, true, false);
        TreasureBasinStatus basinHealthy = TreasureBasinStatus.resolve(true, true, false, false);
        assertEquals(WorldShardMinerStatus.CACHE_FULL, minerFull);
        assertEquals(TreasureBasinStatus.ACTIVE, basinHealthy);

        WorldShardMinerStatus minerHealthy = WorldShardMinerStatus.resolve(true, false, false);
        TreasureBasinStatus basinFull = TreasureBasinStatus.resolve(true, true, true, false);
        assertEquals(WorldShardMinerStatus.ACTIVE, minerHealthy);
        assertEquals(TreasureBasinStatus.CACHE_FULL, basinFull);
    }

    @Test
    void attachedBasinWithoutSelectableLootReportsCalibrating() {
        assertEquals(TreasureBasinStatus.CALIBRATING,
                TreasureBasinStatus.resolve(true, false, false, false));
        assertEquals(TreasureBasinStatus.INACTIVE,
                TreasureBasinStatus.resolve(false, false, true, true));
    }
}
