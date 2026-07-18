package com.cultivation.cultivation.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardBeamGeometryTest {
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
