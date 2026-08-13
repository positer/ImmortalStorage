package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.menu.custom.SourceVeinManagerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact full 8x9 member grid; all slots remain real server slots. */
public final class SourceVeinManagerScreen extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<SourceVeinManagerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;

    public SourceVeinManagerScreen(SourceVeinManagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 250;
        inventoryLabelY = 163;
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.horizontalLine(x, x + imageWidth - 1, y, LIGHT);
        graphics.verticalLine(x, y, y + imageHeight - 1, LIGHT);
        graphics.horizontalLine(x, x + imageWidth - 1, y + imageHeight - 1, DARK);
        graphics.verticalLine(x + imageWidth - 1, y, y + imageHeight - 1, DARK);
        drawSlots(graphics, x + 7, y + 17, 9, 8);
        drawSlots(graphics, x + 7, y + 173, 9, 3);
        drawSlots(graphics, x + 7, y + 231, 9, 1);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static void drawSlots(GuiGraphicsExtractor graphics, int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) for (int column = 0; column < columns; column++) {
            int sx = x + column * 18;
            int sy = y + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            graphics.horizontalLine(sx, sx + 17, sy, DARK);
            graphics.verticalLine(sx, sy, sy + 17, DARK);
            graphics.horizontalLine(sx + 1, sx + 17, sy + 17, LIGHT);
            graphics.verticalLine(sx + 17, sy + 1, sy + 17, LIGHT);
        }
    }
}
