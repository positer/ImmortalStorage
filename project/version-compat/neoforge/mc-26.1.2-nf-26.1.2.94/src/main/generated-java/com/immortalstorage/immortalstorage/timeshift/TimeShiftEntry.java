package com.immortalstorage.immortalstorage.timeshift;

/**
 * One accelerated block. {@code boostEndEpochMillis} is wall-clock time, not
 * game time, because the 1024x window is specified in the player's real time.
 */
public record TimeShiftEntry(int tier, boolean boosted, long boostEndEpochMillis) {
    public static final long NO_BOOST = 0L;

    public TimeShiftEntry {
        tier = TimeShiftTiers.clampTier(tier);
    }

    public static TimeShiftEntry permanent(int tier) {
        return new TimeShiftEntry(tier, false, NO_BOOST);
    }

    public TimeShiftEntry withBoost(long endEpochMillis) {
        return new TimeShiftEntry(tier, true, endEpochMillis);
    }

    public TimeShiftEntry withoutBoost() {
        return new TimeShiftEntry(tier, false, NO_BOOST);
    }

    public boolean boostActive(long nowEpochMillis) {
        return boosted && nowEpochMillis < boostEndEpochMillis;
    }

    public boolean expired(long nowEpochMillis) {
        return boosted && nowEpochMillis >= boostEndEpochMillis;
    }

    public int permanentMultiplier() {
        return TimeShiftTiers.permanentMultiplier(tier);
    }

    public int effectiveMultiplier(long nowEpochMillis, int boostMultiplier) {
        return TimeShiftTiers.effectiveMultiplier(tier, boosted, boostEndEpochMillis, nowEpochMillis, boostMultiplier);
    }
}
