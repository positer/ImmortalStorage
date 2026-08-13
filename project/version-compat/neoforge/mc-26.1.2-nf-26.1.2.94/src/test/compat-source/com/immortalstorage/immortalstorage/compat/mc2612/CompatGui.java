package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Official 26.1 deferred GUI extractor calls used by the migrated screens. */
public final class CompatGui {
    private CompatGui() {
    }

    public static void renderTooltip(GuiGraphicsExtractor graphics, Font font,
                                     List<Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
    }

    public static void renderTooltip(GuiGraphicsExtractor graphics, Font font,
                                     Component line, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, line, mouseX, mouseY);
    }

    public static void blitTexture(GuiGraphicsExtractor graphics, Identifier texture,
                                   int x, int y, int width, int height,
                                   float u, float v, int textureWidth, int textureHeight) {
        // 26.1's extractor takes destination end coordinates and normalized UV
        // end coordinates.  The legacy GuiGraphics contract used x/y plus
        // width/height and pixel-space u/v, so both pairs must be expanded.
        graphics.blit(texture, x, y, x + width, y + height,
                u / textureWidth, (u + width) / textureWidth,
                v / textureHeight, (v + height) / textureHeight);
    }

    public static void blitTexture(GuiGraphicsExtractor graphics, Identifier texture,
                                   int x, int y, float u, float v,
                                   int width, int height, int textureWidth, int textureHeight) {
        blitTexture(graphics, texture, x, y, width, height, u, v, textureWidth, textureHeight);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite,
                                  int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite,
                                  int x, int y, int width, int height, int tint) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite,
                                  int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite,
                                  int x, int y, int width, int height, int tint) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite,
                                  int spriteWidth, int spriteHeight, int u, int v,
                                  int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height,
                u, v, spriteWidth, spriteHeight);
    }
}
