package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldShardMiningMathTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void beaconLevelUsesTwoToThePowerOfThreeTimesLevelEveryCycle() {
        assertEquals(0, WorldShardMiningMath.samplesPerCycle(0, 1.0D));
        assertEquals(8, WorldShardMiningMath.samplesPerCycle(1, 1.0D));
        assertEquals(64, WorldShardMiningMath.samplesPerCycle(2, 1.0D));
        assertEquals(512, WorldShardMiningMath.samplesPerCycle(3, 1.0D));
        assertEquals(4_096, WorldShardMiningMath.samplesPerCycle(4, 1.0D));
        assertEquals(8_192, WorldShardMiningMath.samplesPerCycle(4, 2.0D));
    }

    @Test
    void miningCycleRunsOncePerTwentyServerTicks() {
        assertEquals(false, WorldShardMiningMath.shouldRun(19L));
        assertEquals(true, WorldShardMiningMath.shouldRun(20L));
        assertEquals(false, WorldShardMiningMath.shouldRun(39L));
        assertEquals(true, WorldShardMiningMath.shouldRun(40L));
    }
}
