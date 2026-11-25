package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Movable configuration window. Move its origin, never its slots or server menu coordinates. */
public abstract class ConfigurationWindowScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private int windowOffsetX, windowOffsetY, layoutWidth, layoutHeight;
    private boolean movingWindow;
    private int previousFootprint;
    private double grabX, grabY;
    protected ConfigurationWindowScreen(M menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void init() {
        super.init();
        int centeredX = leftPos, centeredY = topPos;
        leftPos = ConfigurationWindowBounds.x(leftPos + windowOffsetX, width, Math.max(imageWidth, layoutWidth));
        topPos = ConfigurationWindowBounds.y(topPos + windowOffsetY, height, Math.max(imageHeight, layoutHeight));
        windowOffsetX = leftPos - centeredX; windowOffsetY = topPos - centeredY;
        movingWindow = false;
        previousFootprint = 0;
    }
    @Override protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ConfigurationExternalWidgets.place(children(), leftPos, topPos, imageWidth);
        // Existing controls retain their geometry and authoritative payloads. Side pages share one framed surface.
        int right = leftPos + imageWidth;
        int sideBottom = topPos + configurationPanelMinimumHeight();
        boolean sidePage = false;
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && followsWindow(widget)
                && widget.getX() >= leftPos + imageWidth) {
            sidePage = true;
            right = Math.max(right, widget.getX() + widget.getWidth() + 8);
            sideBottom = Math.max(sideBottom, widget.getY() + widget.getHeight() + 8);
        }
        if (sidePage) right = Math.max(right, leftPos + imageWidth + configurationPanelWidth());
        layoutWidth = right - leftPos;
        layoutHeight = Math.max(imageHeight, sidePage ? sideBottom - topPos : 0);
        if (layoutWidth != previousFootprint) {
            // On expansion the background, real slots and live controls move together.
            int moveX = ConfigurationWindowBounds.x(leftPos, width, layoutWidth) - leftPos;
            if (moveX != 0) {
                for (var child : children()) if (child instanceof AbstractWidget widget && followsWindow(widget))
                    widget.setX(widget.getX() + moveX);
                leftPos += moveX;
                right += moveX;
                windowOffsetX += moveX;
            }
            previousFootprint = layoutWidth;
        }
        // The native title row and body share a frame; the control row above stays transparent.
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (sidePage) {
            VanillaGuiPainter.panel(graphics, leftPos + imageWidth + 2, topPos,
                    right - leftPos - imageWidth - 2, sideBottom - topPos);
        }
        renderMachineBackground(graphics, partialTick, mouseX, mouseY);
        if (sidePage) ConfigurationStyle.header(graphics, font, leftPos + imageWidth + 2, topPos,
                right - leftPos - imageWidth - 2, configurationSideTitle());
        ConfigurationStyle.header(graphics, font, leftPos, topPos, imageWidth, configurationMachineTitle());
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ConfigurationStyle.TEXT, false);
    }
    @Override protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && widget.isMouseOver(mouseX, mouseY)) return;
        if (mouseY < topPos || mouseY >= topPos + 17) return;
        Component machine = configurationMachineTitle();
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth && font.width(machine) > imageWidth - 16)
            graphics.renderTooltip(font, machine, mouseX, mouseY);
        int sideX = leftPos + imageWidth + 2, sideWidth = windowRight() - sideX;
        Component side = configurationSideTitle();
        if (sideWidth > 0 && mouseX >= sideX && mouseX < sideX + sideWidth && font.width(side) > sideWidth - 16)
            graphics.renderTooltip(font, side, mouseX, mouseY);
    }
    protected Component configurationMachineTitle() { return title; }
    protected Component configurationSideTitle() { return Component.translatable("container.immortalstorage.configuration"); }
    protected abstract void renderMachineBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);
    @Override public boolean mouseClicked(double x, double y, int button) {
        // Existing tabs/controls take precedence; only the unoccupied title bar starts a move.
        for (var child : children()) if (child instanceof AbstractWidget widget && widget.visible && widget.isMouseOver(x, y))
            return super.mouseClicked(x, y, button);
        boolean titleRow = y >= topPos && y < topPos + 17 && x >= leftPos && x < windowRight();
        if (button == 0 && titleRow) {
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
    protected int configurationPanelMinimumHeight() { return imageHeight; }
    protected boolean followsWindow(AbstractWidget widget) { return true; }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && movingWindow) { movingWindow = false; return true; }
        return super.mouseReleased(x, y, button);
    }
}
