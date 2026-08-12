package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.server.level.ServerLevel;

/**
 * Coalesces client/menu synchronization for machines that may run many logical
 * ticks during one rendered frame.  This never limits processing or transfer;
 * it only avoids sending the same intermediate progress state repeatedly.
 */
final class MachineTickSync {
    static final long FAST_SYNC_INTERVAL_TICKS = 4L;

    private MachineTickSync() {
    }

    static boolean due(ServerLevel level, long lastSyncTick) {
        if (level == null) return false;
        return due(level.getGameTime(), lastSyncTick);
    }

    /**
     * Checks both clocks used by a block entity: the world clock and the
     * number of state-changing ticker invocations.  Vanilla and the
     * personal-realm ticker advance the world clock, while acceleration mods
     * commonly call the same ticker repeatedly without advancing it.  The
     * invocation clock keeps accelerated machines visually current without
     * making a normal tick send more than one update for every interval.
     */
    static boolean due(ServerLevel level, long lastSyncTick,
                       long invocation, long lastSyncInvocation) {
        if (level == null) return false;
        return due(level.getGameTime(), lastSyncTick, invocation, lastSyncInvocation);
    }

    static boolean due(long currentTick, long lastSyncTick) {
        if (lastSyncTick == Long.MIN_VALUE) return true;
        long elapsed = currentTick - lastSyncTick;
        return elapsed < 0L || elapsed >= FAST_SYNC_INTERVAL_TICKS;
    }

    static boolean due(long currentTick, long lastSyncTick,
                       long invocation, long lastSyncInvocation) {
        if (lastSyncTick == Long.MIN_VALUE || lastSyncInvocation == Long.MIN_VALUE) return true;
        long elapsed = currentTick - lastSyncTick;
        long invocations = invocation - lastSyncInvocation;
        return elapsed < 0L || invocations < 0L
                || elapsed >= FAST_SYNC_INTERVAL_TICKS
                || invocations >= FAST_SYNC_INTERVAL_TICKS;
    }
}
