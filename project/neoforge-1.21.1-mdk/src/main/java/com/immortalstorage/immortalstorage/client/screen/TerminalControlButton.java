package com.immortalstorage.immortalstorage.client.screen;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Terminal action with native keyboard focus/narration and an inset frame instead of a stock texture. */
final class TerminalControlButton extends Button {
    private final Supplier<String> shortLabel;

    TerminalControlButton(int x, int y, int width, int height, Component label, OnPress action, Tooltip help) {
        this(x, y, width, height, label, action, help, null);
    }

    TerminalControlButton(int x, int y, int width, int height, Component label,
                          OnPress action, Tooltip help, Supplier<String> shortLabel) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.shortLabel = shortLabel;
        setTooltip(help);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean highlighted = this.active && isHoveredOrFocused();
        graphics.fill(x, y, x + w, y + h, highlighted ? 0xFF4B6958 : 0xFF898989);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, highlighted ? 0xFFEBF1EC : 0xFFD9D9D9);
        Font font = Minecraft.getInstance().font;
        String label = this.shortLabel == null ? getMessage().getString() : this.shortLabel.get();
        int available = w - (this.shortLabel == null ? 6 : 2);
        if (font.width(label) > available) {
            String ellipsis = "\u2026";
            label = font.plainSubstrByWidth(label, Math.max(0, available - font.width(ellipsis))) + ellipsis;
        }
        graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2,
                this.active ? 0xFF373737 : 0xFF747474, false);
    }
}
