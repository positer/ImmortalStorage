package com.cultivation.cultivation.progression;

/** Pure progression-boundary rules shared by events, items, and tests. */
public final class CultivationProgressionRules {
    public static final long JADE_INITIATION_TICKS = 24_000L;
    public static final int IMMORTAL_PILL_ASCENSION_BLASTS = 5;

    private CultivationProgressionRules() {}

    public enum AdvancementSource {
        JADE_SLEEP,
        JADE_CARRIED,
        IMMORTAL_PILL,
        ASCENSION_DAN,
        OTHER
    }

    public static boolean shouldInitiateWithJade(int stage, boolean hasJade,
                                                  long continuousCarryTicks, boolean sleeping) {
        return stage == 0 && hasJade
                && (sleeping || continuousCarryTicks >= JADE_INITIATION_TICKS);
    }

    /**
     * Only the normal, fully prepared stage-five Immortal Pill path has the
     * cosmetic TNT sequence. Stage setters deliberately have no such effect,
     * so Ascension Dan and operator commands remain side-effect free.
     */
    public static int cosmeticTntBlastCount(int fromStage, int toStage, AdvancementSource source) {
        return fromStage == 5 && toStage == 6 && source == AdvancementSource.IMMORTAL_PILL
                ? IMMORTAL_PILL_ASCENSION_BLASTS
                : 0;
    }

    public static boolean allowsNormalAdvance(int fromStage, int toStage) {
        return TribulationPolicy.allowsNormalAdvance(
                fromStage, toStage, TribulationPolicy.configuredMaximumStage());
    }

    public static boolean allowsNormalTargetStage(int targetStage) {
        return targetStage >= 1 && targetStage <= TribulationPolicy.configuredMaximumStage();
    }
}
