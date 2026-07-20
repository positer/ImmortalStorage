package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla-pixel-language screen with three furnace lanes in one menu. */
public class ImmortalFurnaceScreen extends AbstractContainerScreen<ImmortalFurnaceMenu> {
    private static final ResourceLocation FURNACE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final int TEXT = 0xFF404040;
    private static final int[] LANE_Y = {20, 50, 80};

    public ImmortalFurnaceScreen(ImmortalFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 195;
        imageHeight = 218;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 17;
        inventoryLabelY = 122;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        VanillaGuiPainter.panel(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 16, 0xFFD8D8D8);
        graphics.hLine(x + 2, x + imageWidth - 3, y + 16, 0xFF8B8B8B);
        graphics.hLine(x + 7, x + imageWidth - 8, y + 116, 0xFF8B8B8B);
        graphics.hLine(x + 7, x + imageWidth - 8, y + 117, 0xFFFFFFFF);
        VanillaGuiPainter.slots(graphics, x, y, menu.slots);

        for (int channel = 0; channel < LANE_Y.length; channel++) {
            int laneY = y + LANE_Y[channel];
            graphics.blit(FURNACE_TEXTURE, x + 95, laneY,
                    79.0F, 34.0F, 24, 16, 256, 256);
            int width = Mth.ceil(menu.getCookProgress(channel) * 24.0F);
            if (width > 0) {
                graphics.blitSprite(BURN_PROGRESS, 24, 16, 0, 0,
                        x + 95, laneY, width, 16);
            }
        }

        int flameProgress = Mth.ceil(menu.getLitProgress() * 13.0F);
        VanillaGuiPainter.furnaceFlame(graphics, x + 18, y + 33,
                flameProgress, menu.isLit());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        graphics.drawString(font, "I", 40, LANE_Y[0] + 5, TEXT, false);
        graphics.drawString(font, "II", 37, LANE_Y[1] + 5, TEXT, false);
        graphics.drawString(font, "III", 34, LANE_Y[2] + 5, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
