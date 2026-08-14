package com.immortalstorage.immortalstorage.worldshard;

public final class TreasureBasinSchedule {
    public static final long INTERVAL_TICKS = 2_400L;

    private TreasureBasinSchedule() {
    }

    public static boolean shouldRun(long gameTime) {
        return gameTime > 0L && gameTime % INTERVAL_TICKS == 0L;
    }

    /**
     * Advances a basin's schedule progress by {@code speed} and reports how
     * many full rolls that completes and the progress carried into the next
     * interval.  This is the acceleration primitive used by the reinforcement
     * plugin: a stronger plugin raises {@code speed} so rolls complete more
     * often without multiplying each roll's drop count.
     */
    public static Advance advance(long progress, long speed, long interval) {
        if (interval <= 0L) throw new IllegalArgumentException("interval must be positive");
        long effectiveSpeed = Math.max(0L, speed);
        long total = progress > Long.MAX_VALUE - effectiveSpeed
                ? Long.MAX_VALUE : progress + effectiveSpeed;
        return new Advance(total / interval, total % interval);
    }

    public record Advance(long rolls, long remainder) {}
}
