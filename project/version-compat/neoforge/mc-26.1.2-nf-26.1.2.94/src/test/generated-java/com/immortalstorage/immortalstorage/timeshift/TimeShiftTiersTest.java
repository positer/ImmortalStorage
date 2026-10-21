package com.immortalstorage.immortalstorage.timeshift;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** ToolMaterial arithmetic contract for the Guixu Time-Shift Staff. */
final class TimeShiftTiersTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void permanentTiersAreTwoToThePowerOfTierPlusOne() {
        assertEquals(2, TimeShiftTiers.permanentMultiplier(0));
        assertEquals(4, TimeShiftTiers.permanentMultiplier(1));
        assertEquals(8, TimeShiftTiers.permanentMultiplier(2));
        assertEquals(16, TimeShiftTiers.permanentMultiplier(3));
        assertEquals(32, TimeShiftTiers.permanentMultiplier(4));
    }

    @Test
    void tierIndexIsClampedIntoTheFiveAuthoredTiers() {
        assertEquals(0, TimeShiftTiers.clampTier(-9));
        assertEquals(4, TimeShiftTiers.clampTier(99));
        assertEquals(2, TimeShiftTiers.clampTier(2));
    }

    @Test
    void sneakScrollWrapsInBothDirections() {
        assertEquals(1, TimeShiftTiers.cycle(0, 1));
        assertEquals(4, TimeShiftTiers.cycle(0, -1));
        assertEquals(0, TimeShiftTiers.cycle(4, 1));
        assertEquals(3, TimeShiftTiers.cycle(4, -1));
        assertEquals(1, TimeShiftTiers.cycle(2, -1));
        assertEquals(2, TimeShiftTiers.cycle(2, 0));
    }

    @Test
    void boostWinsWhileItIsStillInTheFutureAndThenFallsBackToTheTier() {
        int boost = TimeShiftTiers.boostMultiplier(TimeShiftTiers.DEFAULT_BOOST_LOG2);
        assertEquals(1024, boost);
        assertEquals(1024, TimeShiftTiers.effectiveMultiplier(1, true, 2_000L, 1_000L, boost));
        assertEquals(4, TimeShiftTiers.effectiveMultiplier(1, true, 1_000L, 1_000L, boost));
        assertEquals(4, TimeShiftTiers.effectiveMultiplier(1, false, 0L, 1_000L, boost));
        assertEquals(32, TimeShiftTiers.effectiveMultiplier(4, true, 1L, 2L, boost));
    }

    @Test
    void anExpiredBoostKeepsThePreviouslyAppliedTierVisible() {
        TimeShiftEntry applied = TimeShiftEntry.permanent(3);
        TimeShiftEntry boosted = applied.withBoost(5_000L);
        assertEquals(1024, boosted.effectiveMultiplier(4_000L, 1024));
        assertEquals(16, boosted.effectiveMultiplier(5_000L, 1024));
        assertEquals(3, boosted.withoutBoost().tier());
        assertEquals(16, boosted.withoutBoost().permanentMultiplier());
    }

    @Test
    void extraTicksAreTheMultiplierMinusTheVanillaPass() {
        assertEquals(0, TimeShiftTiers.extraTicks(1));
        assertEquals(1, TimeShiftTiers.extraTicks(2));
        assertEquals(1023, TimeShiftTiers.extraTicks(1024));
        assertEquals(0, TimeShiftTiers.extraTicks(0));
    }

    @Test
    void labelsMatchTheMultiplierShownInTheWorld() {
        assertEquals("16x", TimeShiftTiers.label(16));
        assertEquals("1024x", TimeShiftTiers.label(1024));
        assertEquals(3, TimeShiftTiers.tierOfLabel(16));
        assertEquals(0, TimeShiftTiers.tierOfLabel(2));
    }
}
