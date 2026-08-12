package com.immortalstorage.immortalstorage.compat.refinedstorage;

/** Quantity guards for RS's long API and its overflow-prone shared aggregation list. */
public final class RsAmountPolicy {
    /** Both supported RS 2.x and 3.x storage interfaces are long-valued. */
    private static volatile boolean longAmountApiSupported = true;

    private RsAmountPolicy() {
    }

    public static long advertised(long amount) {
        if (amount <= 0L) return 0L;
        return longAmountApiSupported || amount != Long.MAX_VALUE
                ? amount : Integer.MAX_VALUE;
    }

    static void setLongAmountApiSupported(boolean supported) {
        longAmountApiSupported = supported;
    }

    static boolean longAmountApiSupported() {
        return longAmountApiSupported;
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
