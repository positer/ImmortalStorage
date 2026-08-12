package com.immortalstorage.immortalstorage.worldshard;

public final class WorldShardMiningMath {
    public static final int CYCLE_TICKS = 20;
    public static final int MAX_LEVEL = 4;

    private WorldShardMiningMath() {
    }

    public static boolean shouldRun(long gameTime) {
        return Math.floorMod(gameTime, CYCLE_TICKS) == 0L;
    }

    public static int samplesPerCycle(int level, double multiplier) {
        if (level < 1 || level > MAX_LEVEL || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return 0;
        }
        long base = 1L << (3 * level);
        double scaled = base * multiplier;
        if (scaled >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) Math.round(scaled));
    }
}
