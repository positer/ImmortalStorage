package com.immortalstorage.immortalstorage.client.render;

/** Pure client-side timing helpers for source-vein floating displays. */
final class SourceVeinAnimation {
    private static final double NANOS_PER_TICK = 50_000_000.0D;
    private static final double MAX_RENDER_TICKS = 8.0D;
    private static final double MAX_SPEED = 64.0D;
    private static final long PAUSE_NANOS = 250_000_000L;

    private SourceVeinAnimation() {
    }

    static double continuousTime(long gameTime, float partialTick) {
        double clampedPartial = Math.max(0.0D, Math.min(1.0D, partialTick));
        return gameTime + clampedPartial;
    }

    static double realTime() {
        return System.nanoTime() / NANOS_PER_TICK;
    }

    /**
     * A render-local clock that follows logical game time without replaying a
     * multi-tick update as a slow catch-up animation.  The multiplier is
     * measured over the complete wall-clock interval since the previous
     * logical advance, then applied immediately to subsequent render frames.
     * This keeps normal speed at 1x and an accelerated realm at its real
     * multiple without a smoothing tail or a one-frame logical-time snap.
     */
    static final class Clock {
        private boolean initialized;
        private long lastNanos;
        private long lastLogicalAdvanceNanos;
        private double lastLogicalTime;
        private double animationTime;
        private double speed = 1.0D;

        double sample(double logicalTime) {
            return sampleAt(System.nanoTime(), logicalTime);
        }

        double sampleAt(long nowNanos, double logicalTime) {
            if (!initialized) {
                initialized = true;
                lastNanos = nowNanos;
                lastLogicalAdvanceNanos = nowNanos;
                lastLogicalTime = logicalTime;
                animationTime = logicalTime;
                return animationTime;
            }

            long elapsedNanos = Math.max(0L, nowNanos - lastNanos);
            double renderTicks = clamp(elapsedNanos / NANOS_PER_TICK, 0.0D, MAX_RENDER_TICKS);
            double logicalDelta = logicalTime - lastLogicalTime;
            if (logicalDelta < -0.5D) {
                // A world/level change resets the logical clock.  Do not let
                // the old level's phase leak into the new level.
                lastNanos = nowNanos;
                lastLogicalAdvanceNanos = nowNanos;
                lastLogicalTime = logicalTime;
                animationTime = logicalTime;
                speed = 1.0D;
                return animationTime;
            }

            long logicalElapsedNanos = Math.max(0L, nowNanos - lastLogicalAdvanceNanos);
            if (logicalElapsedNanos > 0L && logicalDelta > 0.000001D) {
                double observedSpeed = logicalDelta
                        / (logicalElapsedNanos / NANOS_PER_TICK);
                speed = clamp(observedSpeed, 0.0D, MAX_SPEED);
                lastLogicalAdvanceNanos = nowNanos;
            } else if (nowNanos - lastLogicalAdvanceNanos > PAUSE_NANOS
                    && logicalDelta <= 0.000001D) {
                // A paused client must not continue to drift forever just
                // because render frames are still being submitted.
                speed = 0.0D;
            }

            animationTime += renderTicks * speed;
            lastNanos = nowNanos;
            lastLogicalTime = logicalTime;
            return animationTime;
        }

        double speed() {
            return speed;
        }
    }

    static float bob(double animationTime) {
        return (float) (Math.sin(animationTime * 0.085D) * 0.035D);
    }

    static float rotationDegrees(double animationTime, double degreesPerTick) {
        // Keep the value small before converting to float.  This avoids the
        // precision loss that becomes visible as a periodic rotation jump in
        // long-running worlds or during low-FPS frame delivery.
        return (float) ((animationTime * degreesPerTick) % 360.0D);
    }

    /**
     * A deterministic per-display attitude.  It is derived from the source
     * identity/position rather than regenerated from wall time, so a render
     * frame can never move the pivot or randomly reorient the model.
     */
    static Orientation orientation(long seed) {
        long mixed = mix64(seed ^ 0x9E3779B97F4A7C15L);
        return new Orientation(
                randomRange(mixed, 0.0F, 360.0F),
                randomRange(mix64(mixed + 0x632BE59BD9B4E019L), -24.0F, 24.0F),
                randomRange(mix64(mixed + 0xC6BC279692B5CC83L), -24.0F, 24.0F));
    }

    private static float randomRange(long seed, float minimum, float maximum) {
        double unit = (mix64(seed) >>> 11) * 0x1.0p-53;
        return (float) (minimum + (maximum - minimum) * unit);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    record Orientation(float yaw, float pitch, float roll) {
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
