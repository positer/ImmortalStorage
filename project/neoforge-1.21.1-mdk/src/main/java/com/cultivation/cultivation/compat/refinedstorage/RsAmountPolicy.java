package com.cultivation.cultivation.compat.refinedstorage;

/** Quantity guards for RS's long API and its overflow-prone shared aggregation list. */
public final class RsAmountPolicy {
    private RsAmountPolicy() {
    }

    public static long advertised(long amount) {
        if (amount <= 0L) return 0L;
        return amount == Long.MAX_VALUE ? Integer.MAX_VALUE : amount;
    }

    public static long saturatedSum(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public static long boundedTransfer(long transferred, long requested) {
        if (transferred <= 0L || requested <= 0L) return 0L;
        return Math.min(transferred, requested);
    }
}
