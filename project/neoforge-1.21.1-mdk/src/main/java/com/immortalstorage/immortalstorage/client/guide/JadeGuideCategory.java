package com.immortalstorage.immortalstorage.client.guide;

import net.minecraft.resources.ResourceLocation;

public record JadeGuideCategory(String id, String titleKey, ResourceLocation iconId) {
    public JadeGuideCategory {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Category id is required");
        if (titleKey == null || titleKey.isBlank()) throw new IllegalArgumentException("Category title is required");
        if (iconId == null) throw new IllegalArgumentException("Category icon is required");
    }
}
