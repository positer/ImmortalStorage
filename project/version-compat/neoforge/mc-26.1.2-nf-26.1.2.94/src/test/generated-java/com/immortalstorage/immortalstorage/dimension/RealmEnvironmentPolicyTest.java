package com.immortalstorage.immortalstorage.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RealmEnvironmentPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void dayAndNightUseStableVanillaTimes() {
        assertEquals(1_000L, RealmEnvironmentPolicy.lockedDayTime(true));
        assertEquals(18_000L, RealmEnvironmentPolicy.lockedDayTime(false));
    }

    @Test void weatherCyclesClearRainThunderSnowAndBackToClear() {
        int mode = RealmEnvironmentPolicy.CLEAR;
        mode = RealmEnvironmentPolicy.nextWeatherMode(mode);
        assertEquals(RealmEnvironmentPolicy.RAIN, mode);
        mode = RealmEnvironmentPolicy.nextWeatherMode(mode);
        assertEquals(RealmEnvironmentPolicy.THUNDER, mode);
        mode = RealmEnvironmentPolicy.nextWeatherMode(mode);
        assertEquals(RealmEnvironmentPolicy.SNOW, mode);
        assertEquals(RealmEnvironmentPolicy.CLEAR, RealmEnvironmentPolicy.nextWeatherMode(mode));
    }

    @Test void unknownWeatherModesFailClosedToClear() {
        assertEquals(RealmEnvironmentPolicy.CLEAR, RealmEnvironmentPolicy.sanitizeWeatherMode(-1));
        assertEquals(RealmEnvironmentPolicy.CLEAR, RealmEnvironmentPolicy.sanitizeWeatherMode(4));
    }

    @Test void snowNeverEnablesRainingOrThundering() {
        assertFalse(RealmEnvironmentPolicy.requiresRain(RealmEnvironmentPolicy.SNOW));
        assertFalse(RealmEnvironmentPolicy.requiresThunder(RealmEnvironmentPolicy.SNOW));
    }

    @Test void rainEnablesRainingButNotThundering() {
        assertTrue(RealmEnvironmentPolicy.requiresRain(RealmEnvironmentPolicy.RAIN));
        assertFalse(RealmEnvironmentPolicy.requiresThunder(RealmEnvironmentPolicy.RAIN));
    }

    @Test void thunderEnablesRainingAndThundering() {
        assertTrue(RealmEnvironmentPolicy.requiresRain(RealmEnvironmentPolicy.THUNDER));
        assertTrue(RealmEnvironmentPolicy.requiresThunder(RealmEnvironmentPolicy.THUNDER));
    }

    @Test void clearEnablesNeitherRainingNorThundering() {
        assertFalse(RealmEnvironmentPolicy.requiresRain(RealmEnvironmentPolicy.CLEAR));
        assertFalse(RealmEnvironmentPolicy.requiresThunder(RealmEnvironmentPolicy.CLEAR));
    }
}
