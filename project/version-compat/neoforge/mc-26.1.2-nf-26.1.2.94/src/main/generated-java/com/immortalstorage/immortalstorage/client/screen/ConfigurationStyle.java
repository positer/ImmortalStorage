package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import java.util.List;

/** Shared frame, text palette and face order for machine configuration surfaces. */
final class ConfigurationStyle {
    static final int TEXT = 0xFF404040;
    static final int ENABLED = 0xFF38A85A;
    static final int DISABLED = 0xFF777777;
    static final List<Direction> FACES = List.of(Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    private ConfigurationStyle() {}
    static void header(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, Component title) {
        // Erase the body's old top bevel so the joined surface has no internal seam.
        graphics.fill(x + 2, y + 2, x + width - 2, y + 19, 0xFFC6C6C6);
        graphics.verticalLine(x, y, y + 20, 0xFFFFFFFF);
        graphics.verticalLine(x + 1, y, y + 20, 0xFFD8D8D8);
        graphics.verticalLine(x + width - 2, y, y + 20, 0xFF8B8B8B);
        graphics.verticalLine(x + width - 1, y, y + 20, 0xFF373737);
        graphics.text(font, font.plainSubstrByWidth(title.getString(), Math.max(0, width - 12)),
                x + 6, y + 4, TEXT, false);
    }
}
