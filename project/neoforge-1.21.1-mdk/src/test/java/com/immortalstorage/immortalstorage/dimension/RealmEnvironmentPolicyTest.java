package com.immortalstorage.immortalstorage.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RealmEnvironmentPolicyTest {
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
}
