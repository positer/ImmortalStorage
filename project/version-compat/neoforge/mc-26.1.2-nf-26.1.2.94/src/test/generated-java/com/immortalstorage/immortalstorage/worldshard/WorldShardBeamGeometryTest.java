package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardBeamGeometryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void firstGenericOpaqueBlockNaturallyTerminatesTheVisualBeam() {
        assertEquals(1, WorldShardBeamPath.renderHeightFromBlockingY(64, 65));
        assertEquals(16, WorldShardBeamPath.renderHeightFromBlockingY(64, 80));
    }

    @Test
    void noOpaqueBlockKeepsTheNormalTallBeam() {
        assertEquals(WorldShardBeamPath.OPEN_SKY_RENDER_HEIGHT,
                WorldShardBeamPath.renderHeightFromBlockingY(64, -1));
    }
}
