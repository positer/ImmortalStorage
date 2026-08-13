package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SourceVeinCoreLayoutTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void eightSegmentsLadderMapsEveryStateToTheThreeMaterials() {
        for (int segment = 0; segment < SourceVeinCoreLayout.SEGMENTS; segment++) {
            assertEquals(SourceVeinCoreLayout.Material.EMPTY,
                    SourceVeinCoreLayout.materialFor(0, segment));
            assertEquals(SourceVeinCoreLayout.Material.FULL,
                    SourceVeinCoreLayout.materialFor(7, segment));
            assertEquals(SourceVeinCoreLayout.Material.EMPTY,
                    SourceVeinCoreLayout.materialFor(-1, segment));
            assertEquals(SourceVeinCoreLayout.Material.FULL,
                    SourceVeinCoreLayout.materialFor(99, segment));
        }
        for (int state = 1; state <= 6; state++) {
            for (int segment = 0; segment < SourceVeinCoreLayout.SEGMENTS; segment++) {
                SourceVeinCoreLayout.Material expected =
                        segment < state - 1 ? SourceVeinCoreLayout.Material.FULL
                        : segment == state - 1 ? SourceVeinCoreLayout.Material.USED
                        : SourceVeinCoreLayout.Material.EMPTY;
                assertEquals(expected, SourceVeinCoreLayout.materialFor(state, segment),
                        "state=" + state + ", segment=" + segment);
            }
        }
    }

    @Test
    void segmentCentresFormTheTwoByTwoByTwoGridAroundTheBlockCentre() {
        for (int segment = 0; segment < SourceVeinCoreLayout.SEGMENTS; segment++) {
            assertEquals((segment >>> 2 & 1) == 1 ? 0.65625F : 0.34375F,
                    SourceVeinCoreLayout.centerX(segment), 0.0F, "x of " + segment);
            assertEquals((segment >>> 1 & 1) == 1 ? 0.65625F : 0.34375F,
                    SourceVeinCoreLayout.centerY(segment), 0.0F, "y of " + segment);
            assertEquals((segment & 1) == 1 ? 0.65625F : 0.34375F,
                    SourceVeinCoreLayout.centerZ(segment), 0.0F, "z of " + segment);
        }
        assertEquals(3.0F / 16.0F, SourceVeinCoreLayout.SIZE, 0.0F);
        assertEquals(SourceVeinCoreLayout.SIZE * 0.5F, SourceVeinCoreLayout.HALF_SIZE, 0.0F);
    }
}
