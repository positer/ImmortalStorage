package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinStatus;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;

/** Vanilla three-row chest geometry with a read-only status strip. */
public final class TreasureBasinScreen extends AbstractContainerScreen<TreasureBasinMenu> {
    private static final ResourceLocation CHEST_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");
    private static final int TEXT = 0xFF404040;
    private static final int STATUS_TOP = 17;
    private static final int STATUS_HEIGHT = 18;
    private static final int CHEST_ROWS_HEIGHT = 54;
    private static final int SETTINGS_WIDTH = 126;
    private final List<Button> settingsWidgets = new ArrayList<>();
    private boolean settingsOpen;

    public TreasureBasinScreen(TreasureBasinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 92;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> toggleSettings())
                .bounds(leftPos + imageWidth - 22, topPos - 20, 20, 20)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.treasure_basin.settings"))).build());
        int panelX = leftPos + imageWidth + 4;
        for (Direction side : Direction.values()) {
            Button face = Button.builder(Component.literal(side.getName().substring(0, 1).toUpperCase()),
                    button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                            10 + side.get3DDataValue()))
                    .bounds(panelX + 8 + (side.get3DDataValue() % 3) * 34,
                            topPos + 34 + (side.get3DDataValue() / 3) * 34, 30, 30).build();
            settingsWidgets.add(addRenderableWidget(face));
        }
        settingsWidgets.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(panelX + 8, topPos + 108, 110, 20).build()));
        settingsWidgets.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }).bounds(panelX + 8, topPos + 132, 110, 20).build()));
        refreshSettings();
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
        refreshSettings();
    }

    private void refreshSettings() {
        settingsWidgets.forEach(widget -> widget.visible = settingsOpen);
        menu.setSettingsVisible(settingsOpen);
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
        if (settingsOpen) {
            int panelX = leftPos + imageWidth + 4;
            VanillaGuiPainter.panel(
                    graphics, panelX, topPos, SETTINGS_WIDTH, imageHeight);
            VanillaGuiPainter.slot(
                    graphics, leftPos + 190, topPos + 160, true);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font,
                Component.translatable("container.immortalstorage.treasure_basin.mode", modeName()),
                8, 17, TEXT, false);

        Component status = statusName();
        graphics.drawString(font, status, 8, 26, TEXT, false);

        Component cache = Component.translatable("container.immortalstorage.treasure_basin.cache",
                menu.getFilledSlots(), menu.getCacheCapacity());
        graphics.drawString(font, cache, imageWidth - 8 - font.width(cache), 26, TEXT, false);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, TEXT, false);
        if (settingsOpen) {
            int panelX = imageWidth + 4;
            graphics.drawString(font, Component.translatable(
                    "container.immortalstorage.treasure_basin.settings"), panelX + 8, 8, TEXT, false);
            graphics.drawString(font, Component.translatable(
                    "container.immortalstorage.treasure_basin.xianqiao_output",
                    Component.translatable(menu.xianqiaoOutput() ? "options.on" : "options.off")), panelX + 12, 114, TEXT, false);
            graphics.drawString(font, Component.translatable(
                    "container.immortalstorage.treasure_basin.automatic_output",
                    Component.translatable(menu.automaticOutput() ? "options.on" : "options.off")), panelX + 12, 138, TEXT, false);
            graphics.drawString(font, Component.translatable(
                    "container.immortalstorage.reinforcement_plugin"), panelX + 34, 165, TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component modeName() {
        ResourceLocation mode = menu.getActiveMode();
        if (mode == null) {
            return Component.translatable("container.immortalstorage.treasure_basin.mode.none");
        }
        if (WorldShardMinerModes.OVERWORLD.equals(mode)) {
            return Component.translatable("container.immortalstorage.treasure_basin.mode.overworld");
        }
        if (WorldShardMinerModes.NETHER.equals(mode)) {
            return Component.translatable("container.immortalstorage.treasure_basin.mode.nether");
        }
        if (WorldShardMinerModes.END.equals(mode)) {
            return Component.translatable("container.immortalstorage.treasure_basin.mode.end");
        }
        return Component.literal(mode.toString());
    }

    private Component statusName() {
        TreasureBasinStatus status = menu.getOperatingStatus();
        return switch (status) {
            case ACTIVE -> Component.translatable(
                    "container.immortalstorage.treasure_basin.running");
            case CALIBRATING -> Component.translatable(
                    "container.immortalstorage.treasure_basin.calibrating");
            case CACHE_FULL -> Component.translatable(
                    "container.immortalstorage.treasure_basin.cache_full");
            case STORAGE_UNAVAILABLE -> Component.translatable(
                    "container.immortalstorage.treasure_basin.storage_unavailable");
            case INACTIVE -> Component.translatable(
                    "container.immortalstorage.treasure_basin.stopped");
        };
    }
}
