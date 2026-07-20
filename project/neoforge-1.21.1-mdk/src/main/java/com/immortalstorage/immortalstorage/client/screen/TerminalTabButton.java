package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/** A keyboard-accessible button rendered with Minecraft's Advancement tab sprite. */
final class TerminalTabButton extends Button {
    private static final int ICON_Y = 5;
    private static final int HOVER_TINT = 0x40FFFFFF;
    private static final int DISABLED_TINT = 0x88000000;

    private final TerminalTabStyle.Side side;
    private final TerminalTabStyle.Segment segment;
    private final BooleanSupplier selected;
    private final ItemStack icon;

    TerminalTabButton(int x, int y, TerminalTabStyle.Side side, TerminalTabStyle.Segment segment,
                      ItemStack icon, Component narration, Tooltip tooltip,
                      BooleanSupplier selected, OnPress onPress) {
        super(x, y, TerminalTabStyle.WIDTH, TerminalTabStyle.HEIGHT,
                narration, onPress, DEFAULT_NARRATION);
        this.side = side;
        this.segment = segment;
        this.selected = selected;
        this.icon = icon.copyWithCount(1);
        setTooltip(tooltip);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selectedNow = this.selected.getAsBoolean();
        graphics.blitSprite(TerminalTabStyle.sprite(this.side, this.segment, selectedNow),
                getX(), getY(), getWidth(), getHeight());

        if (this.active && this.isHoveredOrFocused() && !selectedNow) {
            graphics.fill(getX() + 4, getY() + 3, getX() + getWidth() - 4,
                    getY() + getHeight() - 3, HOVER_TINT);
        }

        graphics.renderFakeItem(this.icon,
                getX() + this.side.iconInsetX(), getY() + ICON_Y);

        if (!this.active) {
            graphics.fill(getX() + 3, getY() + 2, getX() + getWidth() - 3,
                    getY() + getHeight() - 2, DISABLED_TINT);
        } else if (this.isFocused()) {
            graphics.renderOutline(getX() + 4, getY() + 3,
                    getWidth() - 8, getHeight() - 6, 0xFFFFFFFF);
        }
    }
}
