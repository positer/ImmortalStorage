package com.immortalstorage.immortalstorage.client.keybind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleTapDetectorTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private final DoubleTapDetector detector = new DoubleTapDetector();

    @Test
    void singlePressAndHoldingNeverBoost() {
        assertFalse(detector.update(true, 0));
        assertFalse(detector.update(true, 100_000_000));
        assertFalse(detector.update(true, 400_000_000));
        assertFalse(detector.update(false, 500_000_000));
    }

    @Test
    void quickPressReleasePressBoostsEvenBetweenGameTicks() {
        assertFalse(detector.update(true, 0));
        assertFalse(detector.update(false, 10_000_000));
        assertTrue(detector.update(true, 20_000_000));
        assertFalse(detector.update(true, 30_000_000));
    }

    @Test
    void successiveBoostsRequireSeparatePairs() {
        assertFalse(detector.update(true, 0));
        assertFalse(detector.update(false, 50_000_000));
        assertTrue(detector.update(true, 100_000_000));
        assertFalse(detector.update(false, 150_000_000));
        assertFalse(detector.update(true, 200_000_000));
        assertFalse(detector.update(false, 250_000_000));
        assertTrue(detector.update(true, 300_000_000));
    }

    @Test
    void acceptsTheWindowBoundary() {
        assertFalse(detector.update(true, 0));
        assertFalse(detector.update(false, 100_000_000));
        assertTrue(detector.update(true, 350_000_000));
    }

    @Test
    void latePressStartsANewPair() {
        assertFalse(detector.update(true, 0));
        assertFalse(detector.update(false, 100_000_000));
        assertFalse(detector.update(true, 350_000_001));
        assertFalse(detector.update(false, 400_000_000));
        assertTrue(detector.update(true, 450_000_000));
    }

    @Test
    void leavingFlightOrGameplayDiscardsThePendingTapAndHeldState() {
        assertFalse(detector.update(true, 0));
        detector.reset();
        assertFalse(detector.update(true, 100_000_000));
        assertFalse(detector.update(false, 150_000_000));
        assertTrue(detector.update(true, 200_000_000));
    }
}
