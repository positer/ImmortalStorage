package com.immortalstorage.immortalstorage.progression;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import net.minecraft.resources.ResourceLocation;

public final class TribulationPolicy {
    public static final int FIRST_STAGE = 6;
    public static final int FINAL_STAGE = 10;

    private TribulationPolicy() {}

    public static int configuredMaximumStage() {
        return clampMaximumStage(ImmortalStorageConfig.NORMAL_PROGRESSION_MAX_STAGE.get());
    }

    public static int clampMaximumStage(int configured) {
        return Math.max(1, Math.min(FINAL_STAGE, configured));
    }

    public static boolean allowsNormalAdvance(int currentStage, int nextStage, int maximumStage) {
        int maximum = clampMaximumStage(maximumStage);
        return nextStage == currentStage + 1 && nextStage <= maximum;
    }

    public static boolean canStart(int currentStage, int maximumStage) {
        return currentStage >= FIRST_STAGE
                && currentStage < FINAL_STAGE
                && allowsNormalAdvance(currentStage, currentStage + 1, maximumStage);
    }

    public static ResourceLocation defaultTargetId(int currentStage) {
        return switch (currentStage) {
            case 6 -> ResourceLocation.withDefaultNamespace("zombie");
            case 7 -> ResourceLocation.withDefaultNamespace("wither_skeleton");
            case 8 -> ResourceLocation.withDefaultNamespace("vindicator");
            case 9 -> ResourceLocation.withDefaultNamespace("warden");
            default -> ResourceLocation.withDefaultNamespace("zombie");
        };
    }

    public static String configuredTargetId(int currentStage) {
        return switch (currentStage) {
            case 6 -> ImmortalStorageConfig.TRIBULATION_TARGET_STAGE_6.get();
            case 7 -> ImmortalStorageConfig.TRIBULATION_TARGET_STAGE_7.get();
            case 8 -> ImmortalStorageConfig.TRIBULATION_TARGET_STAGE_8.get();
            case 9 -> ImmortalStorageConfig.TRIBULATION_TARGET_STAGE_9.get();
            default -> defaultTargetId(currentStage).toString();
        };
    }

    public static boolean requiresBlindness(int currentStage) {
        return currentStage >= 8;
    }

    public static boolean requiresWither(int currentStage) {
        return currentStage >= 9;
    }
}
