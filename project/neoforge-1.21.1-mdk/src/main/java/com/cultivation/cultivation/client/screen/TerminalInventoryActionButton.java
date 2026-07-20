package com.cultivation.cultivation.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Compact inventory action button with a readable pixel icon and tooltip. */
final class TerminalInventoryActionButton extends Button {
    enum Icon { WRENCH, DEPOSIT, WITHDRAW }

    private final Icon icon;

    TerminalInventoryActionButton(
            int x, int y, Icon icon, Component narration, Tooltip tooltip, OnPress onPress) {
        super(x, y, 16, 16, narration, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(tooltip);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                        this.active
                                ? this.isHoveredOrFocused() ? "widget/button_highlighted" : "widget/button"
                                : "widget/button_disabled"),
                getX(), getY(), getWidth(), getHeight());
        int x = getX();
        int y = getY();
        switch (this.icon) {
            case WRENCH -> renderWrench(graphics, x, y);
            case DEPOSIT -> renderArrow(graphics, x, y, 0xFF32D35A, true);
            case WITHDRAW -> renderArrow(graphics, x, y, 0xFFE14A4A, false);
        }
    }

    private static void renderWrench(GuiGraphics graphics, int x, int y) {
        int dark = 0xFF303030;
        int light = 0xFFD0D0D0;
        graphics.fill(x + 5, y + 9, x + 10, y + 11, dark);
        graphics.fill(x + 6, y + 8, x + 11, y + 10, light);
        graphics.fill(x + 9, y + 5, x + 11, y + 9, light);
        graphics.fill(x + 10, y + 4, x + 12, y + 6, light);
        graphics.fill(x + 4, y + 10, x + 6, y + 12, light);
        graphics.fill(x + 5, y + 10, x + 6, y + 11, dark);
    }

    private static void renderArrow(GuiGraphics graphics, int x, int y, int color, boolean up) {
        int shadow = 0xFF303030;
        int tipY = up ? y + 4 : y + 11;
        int stemTop = up ? y + 7 : y + 5;
        int stemBottom = up ? y + 12 : y + 9;
        graphics.fill(x + 7, stemTop, x + 10, stemBottom, shadow);
        graphics.fill(x + 8, stemTop, x + 9, stemBottom, color);
        for (int row = 0; row < 3; row++) {
            int yy = up ? tipY + row : tipY - row;
            graphics.fill(x + 7 - row, yy, x + 10 + row, yy + 1, color);
        }
    }
}
