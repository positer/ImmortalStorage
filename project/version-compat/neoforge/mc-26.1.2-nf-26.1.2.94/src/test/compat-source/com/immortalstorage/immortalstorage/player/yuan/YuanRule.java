package com.immortalstorage.immortalstorage.player.yuan;

/**
 * Capacity and passive-generation rule for one resource.
 *
 * <p>A cap of {@value #DISABLED_CAP} disables the resource. A cap of
 * {@value #UNBOUNDED_CAP} is unbounded. Every other cap must be positive.
 */
public record YuanRule(long cap, int generationIntervalTicks, long generationAmount) {
    public static final long DISABLED_CAP = 0L;
    public static final long UNBOUNDED_CAP = -1L;
    public static final YuanRule DISABLED = new YuanRule(DISABLED_CAP, 0, 0L);

    public YuanRule {
        if (cap < UNBOUNDED_CAP) throw new IllegalArgumentException("cap must be -1, 0, or positive");
        if (generationIntervalTicks < 0) throw new IllegalArgumentException("generation interval must not be negative");
        if (generationAmount < 0L) throw new IllegalArgumentException("generation amount must not be negative");
        if ((generationIntervalTicks == 0) != (generationAmount == 0L)) {
            throw new IllegalArgumentException("generation interval and amount must both be enabled or disabled");
        }
        if (cap == DISABLED_CAP && (generationIntervalTicks != 0 || generationAmount != 0L)) {
            throw new IllegalArgumentException("a disabled resource cannot generate");
        }
    }

    public boolean enabled() {
        return cap != DISABLED_CAP;
    }

    public boolean unbounded() {
        return cap == UNBOUNDED_CAP;
    }

    public boolean generates() {
        return enabled() && generationIntervalTicks > 0 && generationAmount > 0L;
    }

    public YuanRule withCapMultiplier(int numerator, int denominator) {
        if (numerator <= 0 || denominator <= 0) throw new IllegalArgumentException("multiplier must be positive");
        if (cap <= 0L) return this;
        long multiplied = cap > Long.MAX_VALUE / numerator ? Long.MAX_VALUE : cap * numerator;
        long boosted = multiplied / denominator;
        return new YuanRule(Math.max(cap, boosted), generationIntervalTicks, generationAmount);
    }

    public YuanRule withGenerationMultiplier(int numerator, int denominator) {
        if (numerator <= 0 || denominator <= 0) throw new IllegalArgumentException("multiplier must be positive");
        if (!generates()) return this;
        long multiplied = generationAmount > Long.MAX_VALUE / numerator
                ? Long.MAX_VALUE : generationAmount * numerator;
        long boosted = multiplied / denominator;
        return new YuanRule(cap, generationIntervalTicks, Math.max(generationAmount, boosted));
    }
}
