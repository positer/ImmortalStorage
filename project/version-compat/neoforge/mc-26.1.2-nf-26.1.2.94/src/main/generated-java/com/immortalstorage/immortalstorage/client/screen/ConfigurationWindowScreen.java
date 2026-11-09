package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Movable configuration window. Move its origin, never its slots or server menu coordinates. */
public abstract class ConfigurationWindowScreen<M extends AbstractContainerMenu> extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<M> {
    private int windowOffsetX, windowOffsetY, layoutWidth, layoutHeight;
    private boolean movingWindow;
    private double grabX, grabY;
    protected ConfigurationWindowScreen(M menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void init() {
        super.init();
        int centeredX = leftPos, centeredY = topPos;
        leftPos = ConfigurationWindowBounds.x(leftPos + windowOffsetX, width, Math.max(imageWidth, layoutWidth));
        topPos = ConfigurationWindowBounds.y(topPos + windowOffsetY, height, Math.max(imageHeight, layoutHeight));
        windowOffsetX = leftPos - centeredX; windowOffsetY = topPos - centeredY;
        movingWindow = false;
    }
    @Override protected final void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        // Existing controls retain their geometry and authoritative payloads. Side pages share one framed surface.
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        boolean sidePage = false;
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && followsWindow(widget)
                && widget.getX() >= leftPos + imageWidth) {
            sidePage = true;
            right = Math.max(right, widget.getX() + widget.getWidth() + 8);
            bottom = Math.max(bottom, widget.getY() + widget.getHeight() + 8);
        }
        if (sidePage) right = Math.max(right, leftPos + imageWidth + configurationPanelWidth());
        layoutWidth = right - leftPos;
        layoutHeight = bottom - topPos;
        // Only the title and body share a frame; the control row above stays transparent.
        VanillaGuiPainter.panel(graphics, leftPos, topPos - 17, imageWidth, imageHeight + 17);
        if (sidePage) {
            VanillaGuiPainter.panel(graphics, leftPos + imageWidth + 2, topPos - 17,
                    right - leftPos - imageWidth - 2, bottom - topPos + 17);
        }
        renderMachineBackground(graphics, partialTick, mouseX, mouseY);
        if (sidePage) ConfigurationStyle.header(graphics, font, leftPos + imageWidth + 2, topPos - 17,
                right - leftPos - imageWidth - 2, Component.translatable("container.immortalstorage.configuration"));
        ConfigurationStyle.header(graphics, font, leftPos, topPos - 17, imageWidth, title);
    }
    @Override protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ConfigurationStyle.TEXT, false);
    }
    protected abstract void renderMachineBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY);
    @Override public boolean mouseClicked(double x, double y, int button) {
        // Existing tabs/controls take precedence; only the unoccupied title bar starts a move.
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && widget.isMouseOver(x, y))
            return super.mouseClicked(x, y, button);
        boolean externalTitle = y >= topPos - 17 && y < topPos && x >= leftPos && x < windowRight();
        boolean nativeTitle = y >= topPos && y < topPos + 16 && x >= leftPos && x < leftPos + imageWidth;
        if (button == 0 && (externalTitle || nativeTitle)) {
            movingWindow = true; grabX = x - leftPos; grabY = y - topPos; return true;
        }
        return super.mouseClicked(x, y, button);
    }
    private int windowRight() {
        int right = leftPos + Math.max(imageWidth, layoutWidth);
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && followsWindow(widget)) {
            right = Math.max(right, widget.getX() + widget.getWidth() + 8);
            if (widget.getX() >= leftPos + imageWidth)
                right = Math.max(right, leftPos + imageWidth + configurationPanelWidth());
        }
        return right;
    }
    private int windowBottom() {
        int bottom = topPos + imageHeight;
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && followsWindow(widget))
            bottom = Math.max(bottom, widget.getY() + widget.getHeight());
        return bottom;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!movingWindow || button != 0) return super.mouseDragged(x, y, button, dx, dy);
        int nextX = ConfigurationWindowBounds.x((int)Math.round(x - grabX), width, windowRight() - leftPos);
        layoutHeight = windowBottom() - topPos;
        int nextY = ConfigurationWindowBounds.y((int)Math.round(y - grabY), height, layoutHeight);
        int moveX = nextX - leftPos, moveY = nextY - topPos;
        leftPos = nextX; topPos = nextY;
        windowOffsetX += moveX; windowOffsetY += moveY;
        for (var child : children()) if (child instanceof AbstractWidget widget && followsWindow(widget)) {
            widget.setX(widget.getX() + moveX); widget.setY(widget.getY() + moveY);
        }
        return true;
    }
    protected int configurationPanelWidth() { return 132; }
    protected boolean followsWindow(AbstractWidget widget) { return true; }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && movingWindow) { movingWindow = false; return true; }
        return super.mouseReleased(x, y, button);
    }
}
