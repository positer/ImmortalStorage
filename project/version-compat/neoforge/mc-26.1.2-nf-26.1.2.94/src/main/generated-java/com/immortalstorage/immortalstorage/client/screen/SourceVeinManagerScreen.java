package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.menu.custom.SourceVeinManagerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact full 12x6 member grid; all slots remain real server slots. */
public final class SourceVeinManagerScreen extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<SourceVeinManagerMenu> {

    public SourceVeinManagerScreen(SourceVeinManagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 216;
        inventoryLabelX = 35;
        inventoryLabelY = 128;
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        ConfigurationExternalWidgets.place(children(), leftPos, topPos, imageWidth);
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        ConfigurationStyle.header(graphics, font, leftPos, topPos, imageWidth, title);
        VanillaGuiPainter.slots(graphics, leftPos, topPos, menu.slots);
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ConfigurationStyle.TEXT, false);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void init() {
        super.init();
        topPos = ConfigurationWindowBounds.y(topPos, height, imageHeight);
    }
}
