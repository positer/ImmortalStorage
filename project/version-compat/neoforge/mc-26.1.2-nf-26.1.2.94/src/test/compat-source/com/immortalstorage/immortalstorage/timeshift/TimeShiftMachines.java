package com.immortalstorage.immortalstorage.timeshift;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Read-side helper for ImmortalStorage's own machines.
 *
 * <p>Extra ticker calls alone do not accelerate a machine that is paced by the
 * world clock instead of by how often its ticker ran: the immortal furnace engine
 * drops every call that carries a game time it already saw, and the Spirit
 * Forging / interface families gate their work behind {@code gameTime %
 * frequency}. Those machines therefore read the staff's multiplier here and fold
 * it into their own pacing (processing multiplier or throttle gate) rather than
 * depending on extra ticker invocations.</p>
 */
public final class TimeShiftMachines {
    /** The staff multiplier currently applied to {@code pos}, or 1 when it carries no record. */
    public static int multiplierAt(ServerLevel level, BlockPos pos) {
        TimeShiftEntry record = TimeShiftStore.get(level).get(pos);
        if (record == null) return 1;
        int boostMultiplier = TimeShiftTiers.boostMultiplier(ImmortalStorageConfig.TIME_SHIFT_BOOST_LOG2.get());
        return record.effectiveMultiplier(System.currentTimeMillis(), boostMultiplier);
    }

    /** True when the block is currently under a staff acceleration above 1x. */
    public static boolean isShifted(ServerLevel level, BlockPos pos) {
        return multiplierAt(level, pos) > 1;
    }

    /**
     * Effective world-clock period for a machine that gates on {@code gameTime % period}.
     *
     * <p>Inside a realm the same game time is presented several times per real tick, so a
     * fixed period would be evaluated on exactly the same value every pass and a machine
     * could never outpace its own slot. Dividing the period by the realm multiplier makes
     * the gate fire proportionally more often, which is the realm acceleration's intent.</p>
     */
    public static int gatedPeriod(ServerLevel level, int frequency) {
        if (frequency <= 1) return 1;
        int realm = com.immortalstorage.immortalstorage.dimension.RealmHelper.accelerationMultiplier(level);
        return realm <= 1 ? frequency : Math.max(1, frequency / realm);
    }

    /** {@code base * staff}, clamped so a machine cannot overflow its progress counter. */
    public static int combine(int baseMultiplier, int staffMultiplier) {
        long combined = (long) Math.max(1, baseMultiplier) * (long) Math.max(1, staffMultiplier);
        return (int) Math.min(Integer.MAX_VALUE, combined);
    }

    private TimeShiftMachines() {}
}
