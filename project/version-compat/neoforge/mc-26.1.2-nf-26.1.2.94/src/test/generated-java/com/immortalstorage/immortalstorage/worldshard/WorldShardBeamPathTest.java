package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardBeamPathTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void clearColumnHasNoBlockingHeight() {
        assertEquals(-1, WorldShardBeamPath.firstBlockingY(64, 320, y -> false));
    }

    @Test
    void scanStartsAboveMinerAndStopsAtFirstOpaqueBlock() {
        List<Integer> visited = new ArrayList<>();

        int blocker = WorldShardBeamPath.firstBlockingY(64, 320, y -> {
            visited.add(y);
            return y == 80;
        });

        assertEquals(80, blocker);
        assertEquals(65, visited.getFirst());
        assertEquals(80, visited.getLast());
    }

    @Test
    void beaconOpacityThresholdAllowsTransparentBlocksAndBedrock() {
        assertFalse(WorldShardBeamPath.blocksBeaconBeam(0, false));
        assertFalse(WorldShardBeamPath.blocksBeaconBeam(14, false));
        assertTrue(WorldShardBeamPath.blocksBeaconBeam(15, false));
        assertFalse(WorldShardBeamPath.blocksBeaconBeam(15, true));
    }
}
