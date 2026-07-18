package com.cultivation.cultivation.client.guide;

import net.minecraft.resources.ResourceLocation;

public record JadeGuideChapter(
        String id,
        String categoryId,
        int order,
        int minimumStage,
        String titleKey,
        String summaryKey,
        String bodyKey,
        String keywordsKey,
        String lockSummaryKey,
        ResourceLocation iconId) {
    public JadeGuideChapter {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Chapter id is required");
        if (categoryId == null || categoryId.isBlank()) throw new IllegalArgumentException("Category id is required");
        if (minimumStage < 0) throw new IllegalArgumentException("Minimum stage cannot be negative");
        if (titleKey == null || summaryKey == null || bodyKey == null || keywordsKey == null) {
            throw new IllegalArgumentException("Chapter localization keys are required");
        }
        if (lockSummaryKey == null || lockSummaryKey.isBlank()) {
            throw new IllegalArgumentException("Locked chapters need a discoverable summary");
        }
        if (iconId == null) throw new IllegalArgumentException("Chapter icon is required");
    }

    public boolean isUnlocked(int stage) {
        return stage >= minimumStage;
    }
}
