package com.immortalstorage.immortalstorage.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmTimeScalePolicyTest {
    @Test
    void stageRangesMatchProgressionAndStageTenCapsAtThirtyTwo() {
        assertEquals(1000, RealmTimeScalePolicy.clampPermille(6, 0));
        assertEquals(500, RealmTimeScalePolicy.clampPermille(7, 0));
        assertEquals(200, RealmTimeScalePolicy.clampPermille(8, 0));
        assertEquals(100, RealmTimeScalePolicy.clampPermille(9, 0));
        assertEquals(0, RealmTimeScalePolicy.clampPermille(10, -1));
        assertEquals(32000, RealmTimeScalePolicy.clampPermille(10, Integer.MAX_VALUE));
    }

    @Test
    void stageTenButtonsVisitEveryRequiredSlowdownStep() {
        assertEquals(500, RealmTimeScalePolicy.stepPermille(10, 1000, -1));
        assertEquals(200, RealmTimeScalePolicy.stepPermille(10, 500, -1));
        assertEquals(100, RealmTimeScalePolicy.stepPermille(10, 200, -1));
        assertEquals(0, RealmTimeScalePolicy.stepPermille(10, 100, -1));
        assertEquals(100, RealmTimeScalePolicy.stepPermille(10, 0, 1));
        assertEquals(32000, RealmTimeScalePolicy.stepPermille(10, 16000, 1));
        assertEquals(32000, RealmTimeScalePolicy.stepPermille(10, 32000, 1));
    }

    @Test
    void nonGearValuesMoveToTheNextOrderedGear() {
        assertEquals(500, RealmTimeScalePolicy.stepPermille(10, 700, -1));
        assertEquals(1000, RealmTimeScalePolicy.stepPermille(10, 700, 1));
        assertEquals(200, RealmTimeScalePolicy.stepPermille(8, 100, 1));
    }

    @Test
    void administratorSpeedCommandCanAcceptOnlyExactStageGears() {
        assertTrue(RealmTimeScalePolicy.isAllowedStep(7, 500));
        assertTrue(RealmTimeScalePolicy.isAllowedStep(8, 200));
        assertTrue(RealmTimeScalePolicy.isAllowedStep(10, 0));
        assertTrue(RealmTimeScalePolicy.isAllowedStep(10, 32_000));
        assertFalse(RealmTimeScalePolicy.isAllowedStep(8, 100));
        assertFalse(RealmTimeScalePolicy.isAllowedStep(9, 32_000));
        assertFalse(RealmTimeScalePolicy.isAllowedStep(6, 2_000));
    }
}
