package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Half-size inventory action button with a readable pixel icon and tooltip. */
final class TerminalInventoryActionButton extends Button {
    static final int SIZE = 8;
    static final int SPACING = 10;

    enum Icon { WRENCH, DEPOSIT, WITHDRAW }

    private final Icon icon;

    TerminalInventoryActionButton(
            int x, int y, Icon icon, Component narration, Tooltip tooltip, OnPress onPress) {
        super(x, y, SIZE, SIZE, narration, onPress, DEFAULT_NARRATION);
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
        graphics.fill(x + 2, y + 5, x + 5, y + 6, dark);
        graphics.fill(x + 3, y + 4, x + 6, y + 5, light);
        graphics.fill(x + 5, y + 2, x + 6, y + 4, light);
        graphics.fill(x + 2, y + 5, x + 3, y + 7, light);
    }

    private static void renderArrow(GuiGraphics graphics, int x, int y, int color, boolean up) {
        int shadow = 0xFF303030;
        int tipY = up ? y + 2 : y + 6;
        int stemTop = up ? y + 4 : y + 2;
        int stemBottom = up ? y + 7 : y + 5;
        graphics.fill(x + 3, stemTop, x + 5, stemBottom, shadow);
        graphics.fill(x + 4, stemTop, x + 5, stemBottom, color);
        for (int row = 0; row < 2; row++) {
            int yy = up ? tipY + row : tipY - row;
            graphics.fill(x + 3 - row, yy, x + 5 + row, yy + 1, color);
        }
    }
}
