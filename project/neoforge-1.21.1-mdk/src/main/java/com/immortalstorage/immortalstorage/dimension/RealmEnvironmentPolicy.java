package com.immortalstorage.immortalstorage.dimension;

/** Persistent, server-authoritative day and weather locks for personal realms. */
public final class RealmEnvironmentPolicy {
    public static final int CLEAR = 0;
    public static final int RAIN = 1;
    public static final int THUNDER = 2;
    public static final int SNOW = 3;
    public static final int WEATHER_MODE_COUNT = 4;
    public static final long DAY_TIME = 1_000L;
    public static final long NIGHT_TIME = 18_000L;

    public static int sanitizeWeatherMode(int mode) {
        return mode >= CLEAR && mode < WEATHER_MODE_COUNT ? mode : CLEAR;
    }

    public static int nextWeatherMode(int mode) {
        return (sanitizeWeatherMode(mode) + 1) % WEATHER_MODE_COUNT;
    }

    public static long lockedDayTime(boolean daytime) {
        return daytime ? DAY_TIME : NIGHT_TIME;
    }

    private RealmEnvironmentPolicy() {}
}
