package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    static void header(GuiGraphics graphics, Font font, int x, int y, int width, Component title) {
        VanillaGuiPainter.panel(graphics, x, y, width, 17);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), Math.max(0, width - 12)),
                x + 6, y + 4, TEXT, false);
    }
}
