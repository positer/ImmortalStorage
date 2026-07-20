package com.immortalstorage.core.amount;

/** Amount semantics shared by finite stores and creative/sink-like non-consuming resources. */
public enum ResourceAmountPolicy {
    CONSUMED,
    UNCHANGED;

    public long reportedLong(long stored) {
        return this == UNCHANGED ? Long.MAX_VALUE : Math.max(0L, stored);
    }

    public int reportedInt(long stored) {
        return this == UNCHANGED ? Integer.MAX_VALUE : LongAmountBridge.saturatingInt(stored);
    }

    public long extractable(long stored, long requested) {
        if (requested <= 0L) return 0L;
        return this == UNCHANGED ? requested : Math.min(Math.max(0L, stored), requested);
    }

    public long afterExtract(long stored, long extracted) {
        if (stored < 0L || extracted < 0L) throw new IllegalArgumentException("amounts must be non-negative");
        return this == UNCHANGED ? stored : Math.max(0L, stored - extracted);
    }

    public long insertable(long stored, long requested) {
        if (stored < 0L || requested < 0L) throw new IllegalArgumentException("amounts must be non-negative");
        return requested;
    }

    public long afterInsert(long stored, long inserted) {
        if (stored < 0L || inserted < 0L) throw new IllegalArgumentException("amounts must be non-negative");
        if (this == UNCHANGED) return stored;
        return Long.MAX_VALUE - stored < inserted ? Long.MAX_VALUE : stored + inserted;
    }

    /** Maximum logical output a native long API may expose during one server tick. */
    public long longPerTickCeiling(long stored) {
        return reportedLong(stored);
    }

    /** Maximum logical output an int-only adapter may expose during one server tick. */
    public int intPerTickCeiling(long stored) {
        return reportedInt(stored);
    }
}
