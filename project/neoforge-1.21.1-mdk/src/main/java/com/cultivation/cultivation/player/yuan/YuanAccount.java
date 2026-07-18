package com.cultivation.cultivation.player.yuan;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Persistent scheduling state for yuan generation and legacy-balance migration.
 *
 * <p>This object is deliberately <strong>not</strong> a yuan inventory. Runtime
 * yuan exists only as item stacks owned by the player or their active personal
 * storage. The two legacy-pending fields are a one-way migration buffer for old
 * saves whose yuan was stored as numeric balances; callers must drain them and
 * materialize the returned amounts as item stacks.
 */
public final class YuanAccount {
    private static final int NBT_VERSION = 4;
    private static final int TRUE_TO_IMMORTAL_RATIO = 16;

    private static final String LEGACY_TRUE_PENDING = "legacyTruePending";
    private static final String LEGACY_IMMORTAL_PENDING = "legacyImmortalPending";

    private YuanProfile profile = YuanProfile.DISABLED;
    private int trueProgress;
    private int immortalProgress;
    private long legacyTruePending;
    private long legacyImmortalPending;

    public YuanProfile profile() {
        return profile;
    }

    public int progress(YuanKind kind) {
        if (kind == null || !profile.rule(kind).generates()) return 0;
        return storedProgress(kind);
    }

    public void configure(YuanProfile newProfile) {
        profile = newProfile == null ? YuanProfile.DISABLED : newProfile;
        trueProgress = clampStoredProgress(trueProgress, profile.trueYuan());
        immortalProgress = clampStoredProgress(immortalProgress, profile.immortalYuan());
    }

    /**
     * Calculates generation completed during {@code elapsedTicks}.
     *
     * <p>The result is a materialization request, not a deposited balance. It is
     * intentionally independent of both the profile cap and legacy migration
     * state; the owning item inventory applies its live total-cap constraint.
     */
    public YuanGeneration advanceGeneration(int elapsedTicks) {
        if (elapsedTicks <= 0) return YuanGeneration.NONE;
        long generatedTrue = advanceOne(YuanKind.TRUE, elapsedTicks);
        long generatedImmortal = advanceOne(YuanKind.IMMORTAL, elapsedTicks);
        return generatedTrue == 0L && generatedImmortal == 0L
                ? YuanGeneration.NONE : new YuanGeneration(generatedTrue, generatedImmortal);
    }

    /**
     * Performs the one-shot ascension conversion. Incomplete groups are
     * discarded at that boundary and are never retained for later deposits.
     */
    public long convertTrueToImmortal(long trueAmount) {
        return trueAmount <= 0L ? 0L : trueAmount / TRUE_TO_IMMORTAL_RATIO;
    }

    /**
     * Returns and clears one old numeric balance awaiting item migration.
     * Repeated drains are idempotent and return zero.
     */
    public long drainLegacyBalance(YuanKind kind) {
        if (kind == YuanKind.TRUE) {
            long pending = legacyTruePending;
            legacyTruePending = 0L;
            return pending;
        }
        if (kind == YuanKind.IMMORTAL) {
            long pending = legacyImmortalPending;
            legacyImmortalPending = 0L;
            return pending;
        }
        return 0L;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", NBT_VERSION);
        tag.putInt("trueProgress", trueProgress);
        tag.putInt("immortalProgress", immortalProgress);
        tag.putLong(LEGACY_TRUE_PENDING, legacyTruePending);
        tag.putLong(LEGACY_IMMORTAL_PENDING, legacyImmortalPending);
        return tag;
    }

    public void load(CompoundTag tag, YuanProfile activeProfile) {
        if (tag == null) {
            resetPersistentState();
        } else {
            trueProgress = Math.max(0, tag.getInt("trueProgress"));
            immortalProgress = Math.max(0, tag.getInt("immortalProgress"));
            legacyTruePending = readLegacyPending(tag, LEGACY_TRUE_PENDING, "trueBalance");
            legacyImmortalPending = readLegacyPending(tag, LEGACY_IMMORTAL_PENDING, "immortalBalance");
        }
        configure(activeProfile);
    }

    public void loadLegacy(long legacyTrueYuan, long legacyImmortalYuan, YuanProfile activeProfile) {
        trueProgress = 0;
        immortalProgress = 0;
        legacyTruePending = nonNegative(legacyTrueYuan);
        legacyImmortalPending = nonNegative(legacyImmortalYuan);
        configure(activeProfile);
    }

    private long advanceOne(YuanKind kind, int elapsedTicks) {
        YuanRule rule = profile.rule(kind);
        if (!rule.generates()) return 0L;

        long accumulated = (long) storedProgress(kind) + elapsedTicks;
        long periods = accumulated / rule.generationIntervalTicks();
        setProgress(kind, (int) (accumulated % rule.generationIntervalTicks()));
        return periods <= 0L ? 0L : saturatingMultiply(periods, rule.generationAmount());
    }

    private void setProgress(YuanKind kind, int value) {
        if (kind == YuanKind.TRUE) trueProgress = value;
        else immortalProgress = value;
    }

    private int storedProgress(YuanKind kind) {
        return kind == YuanKind.TRUE ? trueProgress : immortalProgress;
    }

    private void resetPersistentState() {
        trueProgress = 0;
        immortalProgress = 0;
        legacyTruePending = 0L;
        legacyImmortalPending = 0L;
    }

    private static long readLegacyPending(CompoundTag tag, String pendingKey, String oldBalanceKey) {
        if (tag.contains(pendingKey, Tag.TAG_ANY_NUMERIC)) {
            return nonNegative(tag.getLong(pendingKey));
        }
        return nonNegative(tag.getLong(oldBalanceKey));
    }

    private static int clampStoredProgress(int value, YuanRule rule) {
        if (!rule.generates()) return Math.max(0, value);
        return Math.min(Math.max(0, value), rule.generationIntervalTicks() - 1);
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }
}
