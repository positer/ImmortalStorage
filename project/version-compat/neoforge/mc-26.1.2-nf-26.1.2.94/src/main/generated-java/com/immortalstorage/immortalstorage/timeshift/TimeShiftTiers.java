package com.immortalstorage.immortalstorage.timeshift;

/**
 * Pure tier arithmetic shared by the staff item, the block store and the contract tests.
 *
 * <p>Five permanent tiers map to 2x..32x.  The temporary boost is a separate
 * multiplier (1024x by default) that never changes the stored permanent tier,
 * so an expired boost always falls back to exactly the tier that was applied.</p>
 */
public final class TimeShiftTiers {
    public static final int COUNT = 5;
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = COUNT - 1;
    /** Base-2 exponent of the default 1024x boost multiplier. */
    public static final int DEFAULT_BOOST_LOG2 = 10;

    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    /** Sneak+scroll cycles through every tier in both directions. */
    public static int cycle(int tier, int delta) {
        int step = delta == 0 ? 0 : (delta > 0 ? 1 : -1);
        return Math.floorMod(clampTier(tier) + step, COUNT);
    }

    /** Permanent multiplier of a tier: tier 0..4 -> 2x, 4x, 8x, 16x, 32x. */
    public static int permanentMultiplier(int tier) {
        return 1 << (clampTier(tier) + 1);
    }

    public static int boostMultiplier(int boostLog2) {
        int clamped = Math.max(1, Math.min(30, boostLog2));
        return 1 << clamped;
    }

    /**
     * Effective multiplier: an active boost wins, otherwise the permanent tier.
     * A boost whose end time is not in the future is treated as expired; the
     * store clears the flag on the next server tick.
     */
    public static int effectiveMultiplier(int tier, boolean boosted, long boostEndEpochMillis,
                                          long nowEpochMillis, int boostMultiplier) {
        if (boosted && nowEpochMillis < boostEndEpochMillis) return Math.max(1, boostMultiplier);
        return permanentMultiplier(tier);
    }

    /** Extra ticks on top of the vanilla tick that already ran in this pass. */
    public static int extraTicks(int effectiveMultiplier) {
        return Math.max(0, effectiveMultiplier - 1);
    }

    public static String label(int multiplier) {
        return multiplier + "x";
    }

    public static int tierOfLabel(int multiplier) {
        for (int tier = MIN_TIER; tier <= MAX_TIER; tier++) {
            if (permanentMultiplier(tier) == multiplier) return tier;
        }
        return MIN_TIER;
    }

    private TimeShiftTiers() {}
}
