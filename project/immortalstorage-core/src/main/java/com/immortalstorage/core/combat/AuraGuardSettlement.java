package com.immortalstorage.core.combat;

/**
 * Pure settlement math for Spiritual Aura Guard.
 *
 * <p>One True Yuan pays one point of health damage. One Immortal Yuan is
 * converted only when required and yields sixty-four True Yuan. Fractional
 * value is retained as fixed-point credit so repeated small hits preserve the
 * exact long-term 1:1 exchange rate.</p>
 */
public final class AuraGuardSettlement {
    public static final long CREDIT_SCALE = 1_000_000L;
    public static final long TRUE_PER_IMMORTAL = 64L;

    private AuraGuardSettlement() {
    }

    public static Result settle(double requestedDamage, long storedTrueYuan,
                                long storedImmortalYuan, long priorCredit) {
        long damageCredit = damageCredit(requestedDamage);
        long credit = clampCredit(priorCredit);
        long trueAvailable = Math.max(0L, storedTrueYuan);
        long immortalAvailable = Math.max(0L, storedImmortalYuan);

        if (damageCredit == 0L) {
            return new Result(0.0D, 0.0D, 0L, 0L, credit, 0L);
        }

        long remaining = Math.max(0L, damageCredit - credit);
        long trueNeeded = ceilDiv(remaining, CREDIT_SCALE);
        long trueSpent = Math.min(trueAvailable, trueNeeded);
        long supplied = saturatingMultiply(trueSpent, CREDIT_SCALE);
        remaining = Math.max(0L, remaining - supplied);

        long immortalNeeded = ceilDiv(remaining,
                saturatingMultiply(TRUE_PER_IMMORTAL, CREDIT_SCALE));
        long immortalSpent = Math.min(immortalAvailable, immortalNeeded);
        long convertedTrue = saturatingMultiply(immortalSpent, TRUE_PER_IMMORTAL);
        supplied = saturatingAdd(supplied, saturatingMultiply(convertedTrue, CREDIT_SCALE));

        long totalCover = saturatingAdd(credit, supplied);
        long blockedCredit = Math.min(damageCredit, totalCover);
        long healthDamageCredit = damageCredit - blockedCredit;
        long endingCredit = Math.max(0L, totalCover - blockedCredit);
        long unusedConvertedTrue = endingCredit / CREDIT_SCALE;
        long fractionalCredit = endingCredit % CREDIT_SCALE;

        return new Result(blockedCredit / (double) CREDIT_SCALE,
                healthDamageCredit / (double) CREDIT_SCALE, trueSpent,
                immortalSpent, fractionalCredit, unusedConvertedTrue);
    }

    private static long damageCredit(double damage) {
        if (!Double.isFinite(damage) || damage <= 0.0D) return 0L;
        double scaled = Math.ceil(damage * CREDIT_SCALE - 1.0E-9D);
        return scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) scaled);
    }

    private static long clampCredit(long credit) {
        return Math.min(Math.max(0L, credit), CREDIT_SCALE - 1L);
    }

    private static long ceilDiv(long value, long divisor) {
        if (value <= 0L) return 0L;
        return 1L + (value - 1L) / divisor;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long saturatingAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    public record Result(double blockedDamage, double healthDamage,
                         long trueYuanSpent,
                         long immortalYuanSpent, long endingCredit,
                         long convertedTrueYuanRemainder) {
        public Result {
            if (!Double.isFinite(blockedDamage) || blockedDamage < 0.0D
                    || !Double.isFinite(healthDamage) || healthDamage < 0.0D
                    || trueYuanSpent < 0L || immortalYuanSpent < 0L
                    || endingCredit < 0L || endingCredit >= CREDIT_SCALE
                    || convertedTrueYuanRemainder < 0L) {
                throw new IllegalArgumentException("invalid aura guard settlement");
            }
        }

        public boolean fullyBlocked(double requestedDamage) {
            return healthDamage <= 1.0E-6D;
        }
    }
}
