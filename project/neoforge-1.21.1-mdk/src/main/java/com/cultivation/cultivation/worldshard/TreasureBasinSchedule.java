package com.cultivation.cultivation.worldshard;

public final class TreasureBasinSchedule {
    public static final long INTERVAL_TICKS = 2_400L;

    private TreasureBasinSchedule() {
    }

    public static boolean shouldRun(long gameTime) {
        return gameTime > 0L && gameTime % INTERVAL_TICKS == 0L;
    }
}
