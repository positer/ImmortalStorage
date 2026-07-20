package com.immortalstorage.immortalstorage.player.yuan;

/** Stage-derived caps and generation rules for physical yuan item totals. */
public record YuanProfile(YuanRule trueYuan, YuanRule immortalYuan) {
    public static final YuanProfile DISABLED = new YuanProfile(YuanRule.DISABLED, YuanRule.DISABLED);
    private static final YuanRule POST_ASCENSION_TRUE_YUAN =
            new YuanRule(YuanRule.UNBOUNDED_CAP, 0, 0L);

    public YuanProfile {
        if (trueYuan == null || immortalYuan == null) throw new IllegalArgumentException("yuan rules are required");
    }

    public YuanRule rule(YuanKind kind) {
        return kind == YuanKind.TRUE ? trueYuan : immortalYuan;
    }

    public static YuanProfile forStage(int stage, boolean spiritCore) {
        return forStage(stage, spiritCore,
                com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.get());
    }

    public static YuanProfile forStage(int stage, boolean spiritCore, boolean infiniteStageTen) {
        return switch (stage) {
            case 1 -> trueOnly(64L, 1200, 2L);
            case 2 -> trueOnly(128L, 1200, 4L);
            case 3 -> trueOnly(256L, 1200, 8L);
            case 4 -> trueOnly(512L, 600, 8L);
            case 5 -> trueOnly(1024L, 300, 8L);
            case 6 -> postAscension(64L, 150, 2L);
            case 7 -> postAscension(256L, 100, 5L);
            case 8 -> postAscension(1024L, 50, 16L);
            case 9 -> postAscension(YuanRule.UNBOUNDED_CAP, 20, 32L);
            case 10 -> stageTen(infiniteStageTen);
            default -> DISABLED;
        };
    }

    private static YuanProfile stageTen(boolean infiniteStageTen) {
        if (infiniteStageTen) {
            return new YuanProfile(POST_ASCENSION_TRUE_YUAN,
                    new YuanRule(YuanRule.UNBOUNDED_CAP, 0, 0L));
        }
        // Default stage ten generation is intentionally eight times stage nine.
        return new YuanProfile(POST_ASCENSION_TRUE_YUAN,
                new YuanRule(YuanRule.UNBOUNDED_CAP, 20, 256L));
    }

    private static YuanProfile trueOnly(long cap, int interval, long amount) {
        return new YuanProfile(new YuanRule(cap, interval, amount), YuanRule.DISABLED);
    }

    private static YuanProfile postAscension(long immortalCap, int interval, long amount) {
        return new YuanProfile(POST_ASCENSION_TRUE_YUAN,
                new YuanRule(immortalCap, interval, amount));
    }
}
