package com.immortalstorage.immortalstorage.client.keybind;

/** Recognizes separate press-release-press pairs within a 350 ms window. */
final class DoubleTapDetector {
    private static final long WINDOW_NANOS = 350_000_000L;
    private boolean down;
    private boolean pending;
    private long firstPressNanos;

    boolean update(boolean pressed, long nowNanos) {
        if (!pressed) {
            down = false;
            return false;
        }
        if (down) return false;
        down = true;
        long elapsed = nowNanos - firstPressNanos;
        if (pending && elapsed >= 0 && elapsed <= WINDOW_NANOS) {
            pending = false;
            return true;
        }
        pending = true;
        firstPressNanos = nowNanos;
        return false;
    }

    void reset() {
        down = false;
        pending = false;
    }
}
