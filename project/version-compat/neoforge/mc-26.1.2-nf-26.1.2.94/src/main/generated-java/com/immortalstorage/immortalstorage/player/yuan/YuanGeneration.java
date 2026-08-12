package com.immortalstorage.immortalstorage.player.yuan;

/** Amount scheduled or physically materialized during one generation advance. */
public record YuanGeneration(long trueYuan, long immortalYuan) {
    public static final YuanGeneration NONE = new YuanGeneration(0L, 0L);

    public YuanGeneration {
        if (trueYuan < 0L || immortalYuan < 0L) throw new IllegalArgumentException("generated amounts must not be negative");
    }

    public boolean changed() {
        return trueYuan > 0L || immortalYuan > 0L;
    }
}
