package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.mojang.blaze3d.vertex.PoseStack;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.network.chat.Component;


import java.util.List;

/** Client-only RS grid representation using ImmortalStorage-owned artwork. */
final class RsExternalResourceRendering implements ResourceRendering {
    static final RsExternalResourceRendering INSTANCE = new RsExternalResourceRendering();

    @Override public String formatAmount(long amount, boolean withUnits) {
        return Long.toString(Math.max(0L, amount));
    }
    @Override public Component getDisplayName(ResourceKey resource) {
        if (!(resource instanceof RsExternalResource external)) return Component.empty();
        return ExternalResourceCatalog.displayName(external.resource());
    }
    @Override public List<Component> getTooltip(ResourceKey resource) {
        Component name = getDisplayName(resource);
        return name.getString().isEmpty() ? List.of() : List.of(name);
    }
    @Override public void render(ResourceKey resource, GuiGraphicsExtractor graphics, int x, int y) {
        if (!(resource instanceof RsExternalResource external)) return;
        ResourceChannelKey key = external.resource();
        ExternalResourceCatalog.Definition definition = ExternalResourceCatalog.definition(key);
        if (definition.solidColor()) {
            // Match Xianqiao and AE2 exactly: chemicals are represented by their sampled
            // registry colour, while only non-solid resources use an icon texture.
            graphics.fill(x, y, x + 16, y + 16, definition.color());
        } else {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, definition.icon(), x, y, 16, 16, 0.0F, 0.0F, 16, externalTextureHeight(key));
        }
    }
    @Override public void render(
            ResourceKey resource, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector,
            int light, long seed) {
        // RS uses the GuiGraphicsExtractor path for grid entries; no world model is required.
    }

    private RsExternalResourceRendering() {}

    private static int externalTextureHeight(ResourceChannelKey key) {
        return switch (key.channel()) {
            case "botania_mana" -> 512;
            case "ars_nouveau_source" -> 320;
            default -> 16;
        };
    }
}
