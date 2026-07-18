package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.menu.custom.TreasureBasinMenu;
import com.cultivation.cultivation.worldshard.TreasureBasinStatus;
import com.cultivation.cultivation.worldshard.WorldShardMinerModes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla three-row chest geometry with a read-only status strip. */
public final class TreasureBasinScreen extends AbstractContainerScreen<TreasureBasinMenu> {
    private static final ResourceLocation CHEST_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");
    private static final int TEXT = 0xFF404040;
    private static final int STATUS_TOP = 17;
    private static final int STATUS_HEIGHT = 18;
    private static final int CHEST_ROWS_HEIGHT = 54;

    public TreasureBasinScreen(TreasureBasinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 92;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Preserve the runtime vanilla chest frame and slot pixels. Only an
        // 18px #C6C6C6 information band is inserted between title and cache.
        graphics.blit(CHEST_TEXTURE, x, y, 0.0F, 0.0F,
                imageWidth, STATUS_TOP, 256, 256);
        graphics.fill(x, y + STATUS_TOP, x + imageWidth,
                y + STATUS_TOP + STATUS_HEIGHT, 0xFFC6C6C6);
        graphics.vLine(x, y + STATUS_TOP,
                y + STATUS_TOP + STATUS_HEIGHT - 1, 0xFFFFFFFF);
        graphics.vLine(x + imageWidth - 1, y + STATUS_TOP,
                y + STATUS_TOP + STATUS_HEIGHT - 1, 0xFF373737);
        graphics.blit(CHEST_TEXTURE, x, y + STATUS_TOP + STATUS_HEIGHT,
                0.0F, STATUS_TOP, imageWidth, CHEST_ROWS_HEIGHT, 256, 256);
        graphics.blit(CHEST_TEXTURE, x,
                y + STATUS_TOP + STATUS_HEIGHT + CHEST_ROWS_HEIGHT,
                0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font,
                Component.translatable("container.cultivation.treasure_basin.mode", modeName()),
                8, 17, TEXT, false);

        Component status = statusName();
        graphics.drawString(font, status, 8, 26, TEXT, false);

        Component cache = Component.translatable("container.cultivation.treasure_basin.cache",
                menu.getFilledSlots(), menu.getCacheCapacity());
        graphics.drawString(font, cache, imageWidth - 8 - font.width(cache), 26, TEXT, false);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component modeName() {
        ResourceLocation mode = menu.getActiveMode();
        if (mode == null) {
            return Component.translatable("container.cultivation.treasure_basin.mode.none");
        }
        if (WorldShardMinerModes.OVERWORLD.equals(mode)) {
            return Component.translatable("container.cultivation.treasure_basin.mode.overworld");
        }
        if (WorldShardMinerModes.NETHER.equals(mode)) {
            return Component.translatable("container.cultivation.treasure_basin.mode.nether");
        }
        if (WorldShardMinerModes.END.equals(mode)) {
            return Component.translatable("container.cultivation.treasure_basin.mode.end");
        }
        return Component.literal(mode.toString());
    }

    private Component statusName() {
        TreasureBasinStatus status = menu.getOperatingStatus();
        return switch (status) {
            case ACTIVE -> Component.translatable(
                    "container.cultivation.treasure_basin.running");
            case CALIBRATING -> Component.translatable(
                    "container.cultivation.treasure_basin.calibrating");
            case CACHE_FULL -> Component.translatable(
                    "container.cultivation.treasure_basin.cache_full");
            case STORAGE_UNAVAILABLE -> Component.translatable(
                    "container.cultivation.treasure_basin.storage_unavailable");
            case INACTIVE -> Component.translatable(
                    "container.cultivation.treasure_basin.stopped");
        };
    }
}
