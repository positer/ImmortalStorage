package com.immortalstorage.immortalstorage.dimension;

/**
 * One authoritative policy for persisted personal-realm rates and UI steps.
 * Values are stored as integer permille so slowdown pacing is deterministic.
 */
public final class RealmTimeScalePolicy {
    public static final int NORMAL_PERMILLE = 1_000;
    public static final int ABSOLUTE_MAX_PERMILLE = 32_000;

    private static final int[] STAGE_7_STEPS = {500, 1_000, 2_000, 4_000};
    private static final int[] STAGE_8_STEPS = {200, 500, 1_000, 2_000, 4_000, 8_000};
    private static final int[] STAGE_9_STEPS = {100, 200, 500, 1_000, 2_000, 4_000, 8_000, 16_000};
    private static final int[] STAGE_10_STEPS = {0, 100, 200, 500, 1_000, 2_000, 4_000, 8_000, 16_000, 32_000};

    private RealmTimeScalePolicy() {}

    public static int minimumPermille(int stage) {
        return switch (stage) {
            case 7 -> 500;
            case 8 -> 200;
            case 9 -> 100;
            case 10 -> 0;
            default -> NORMAL_PERMILLE;
        };
    }

    public static int maximumPermille(int stage) {
        return switch (stage) {
            case 7 -> 4_000;
            case 8 -> 8_000;
            case 9 -> 16_000;
            case 10 -> ABSOLUTE_MAX_PERMILLE;
            default -> NORMAL_PERMILLE;
        };
    }

    public static int clampPermille(int stage, int requestedPermille) {
        return Math.max(minimumPermille(stage), Math.min(maximumPermille(stage), requestedPermille));
    }

    /** Move to the adjacent server-owned gear; arbitrary old values cannot strand the UI. */
    public static int stepPermille(int stage, int currentPermille, int direction) {
        int[] steps = steps(stage);
        if (direction > 0 && currentPermille < steps[0]) return steps[0];
        if (direction < 0 && currentPermille > steps[steps.length - 1]) return steps[steps.length - 1];
        int current = clampPermille(stage, currentPermille);
        if (direction < 0) {
            for (int index = steps.length - 1; index >= 0; index--) {
                if (steps[index] < current) return steps[index];
            }
            return steps[0];
        }
        if (direction > 0) {
            for (int step : steps) {
                if (step > current) return step;
            }
            return steps[steps.length - 1];
        }
        return current;
    }

    public static boolean isAllowedStep(int stage, int requestedPermille) {
        for (int step : steps(stage)) {
            if (step == requestedPermille) return true;
        }
        return false;
    }

    private static int[] steps(int stage) {
        return switch (stage) {
            case 7 -> STAGE_7_STEPS;
            case 8 -> STAGE_8_STEPS;
            case 9 -> STAGE_9_STEPS;
            case 10 -> STAGE_10_STEPS;
            default -> new int[]{NORMAL_PERMILLE};
        };
    }
}
