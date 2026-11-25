package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Shared frame, text palette and face order for machine configuration surfaces. */
final class ConfigurationStyle {
    static final int TEXT = 0xFF404040;
    static final int ENABLED = 0xFF38A85A;
    static final int DISABLED = 0xFF777777;
    static final List<Direction> FACES = List.of(Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    static final int CHOOSER_TOP = 27;
    static final int CHOOSER_ROW = 20;
    private ConfigurationStyle() {}

    static int chooserHeight(int choices) {
        return 32 + CHOOSER_ROW * Math.max(1, Math.min(5, choices));
    }

    static void amountSummary(GuiGraphicsExtractor graphics, Font font, Component summary, int x, int y, int width) {
        List<FormattedCharSequence> lines = font.split(summary, width);
        for (int i = 0; i < Math.min(lines.size(), 3); i++) {
            graphics.text(font, lines.get(i), x, y + i * 9, TEXT, false);
        }
    }
    static Component faceLabel(Direction side) {
        return Component.translatable("container.immortalstorage.configuration.face." + side.getName());
    }
    static void header(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, Component title) {
        // Reuse the body's native title row; no extra header height or internal seam.
        graphics.fill(x + 2, y + 2, x + width - 2, y + 17, 0xFFC6C6C6);
        graphics.verticalLine(x, y, y + 18, 0xFFFFFFFF);
        graphics.verticalLine(x + 1, y, y + 18, 0xFFD8D8D8);
        graphics.verticalLine(x + width - 2, y, y + 18, 0xFF8B8B8B);
        graphics.verticalLine(x + width - 1, y, y + 18, 0xFF373737);
        int available = Math.max(0, width - 16);
        String text = title.getString();
        if (font.width(text) > available) {
            String ellipsis = "\u2026";
            text = font.plainSubstrByWidth(text, Math.max(0, available - font.width(ellipsis))) + ellipsis;
        }
        graphics.text(font, text, x + 8, y + 5, TEXT, false);
    }
}
