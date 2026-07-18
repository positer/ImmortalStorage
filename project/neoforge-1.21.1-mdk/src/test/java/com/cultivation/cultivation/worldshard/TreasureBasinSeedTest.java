package com.cultivation.cultivation.worldshard;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class TreasureBasinSeedTest {
    private static final UUID BASIN_ID = UUID.fromString("43f845bb-34e7-42de-b91c-58dc017bbec4");
    private static final ResourceLocation DIMENSION = ResourceLocation.parse("minecraft:overworld");
    private static final ResourceLocation TABLE = ResourceLocation.parse("minecraft:chests/simple_dungeon");

    @Test
    void identicalInputsProduceTheSameNonZeroSeed() {
        long first = TreasureBasinSeed.derive(BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 7L, 11L, TABLE);
        long second = TreasureBasinSeed.derive(BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 7L, 11L, TABLE);

        assertNotEquals(0L, first);
        assertEquals(first, second);
    }

    @Test
    void cycleSourceAndTableAllParticipateInTheSeed() {
        long baseline = TreasureBasinSeed.derive(
                BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 7L, 11L, TABLE);

        assertNotEquals(baseline, TreasureBasinSeed.derive(
                BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 8L, 11L, TABLE));
        assertNotEquals(baseline, TreasureBasinSeed.derive(
                BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 7L, 12L, TABLE));
        assertNotEquals(baseline, TreasureBasinSeed.derive(
                BASIN_ID, DIMENSION, new BlockPos(1, 64, 2), 7L, 11L,
                ResourceLocation.parse("minecraft:chests/end_city_treasure")));
    }
}
