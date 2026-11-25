package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinStatus;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;

/** Vanilla three-row chest geometry with a read-only status strip. */
public final class TreasureBasinScreen extends ConfigurationWindowScreen<TreasureBasinMenu> {
    private static final Identifier CHEST_TEXTURE = Identifier.withDefaultNamespace(
            "textures/gui/container/generic_54.png");
    private static final int TEXT = 0xFF404040;
    private static final int STATUS_TOP = 17;
    private static final int STATUS_HEIGHT = 18;
    private static final int CHEST_ROWS_HEIGHT = 54;
    @Override protected int configurationPanelWidth() { return 126; }
    private final List<Button> settingsWidgets = new ArrayList<>();
    private boolean settingsOpen;
    private Button redstoneModeButton;

    public TreasureBasinScreen(TreasureBasinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 92;
    }

    @Override protected void init() {
        super.init();
        settingsWidgets.clear();
        redstoneModeButton = addRenderableWidget(MachineRedstoneModeButton.create(
                leftPos + imageWidth + 10, topPos + 17, menu));
        redstoneModeButton.setWidth(108);
        redstoneModeButton.visible = false;
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> toggleSettings())
                .bounds(leftPos + imageWidth - 22, topPos - 20, 20, 20)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.configuration"))).build());
        int railX = leftPos + imageWidth + 10;
        for (int index = 0; index < ConfigurationStyle.FACES.size(); index++) {
            Direction side = ConfigurationStyle.FACES.get(index);
            final int face = side.get3DDataValue();
            Button button = new FacePreviewButton(railX + (index % 3) * 37,
                    topPos + 35 + (index / 3) * 37, 34,
                    ConfigurationStyle.faceLabel(side),
                    () -> net.minecraft.world.item.ItemStack.EMPTY,
                    () -> menu.outputFace(face) ? ConfigurationStyle.ENABLED : ConfigurationStyle.DISABLED,
                    clicked -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10 + face));
            button.setTooltip(Tooltip.create(Component.translatable(
                    "container.immortalstorage.reincarnation.face." + face)));
            settingsWidgets.add(addRenderableWidget(button));
        }
        settingsWidgets.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(railX, topPos + 110, 108, 18).build()));
        settingsWidgets.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }).bounds(railX, topPos + 132, 108, 18).build()));
        refreshSettings();
        refreshOutputButtons();
    }

    private void refreshOutputButtons() {
        if (settingsWidgets.size() < 8) return;
        settingsWidgets.get(6).setMessage(Component.translatable(
                "container.immortalstorage.reincarnation." + (menu.xianqiaoOutput() ? "xianqiao_on" : "xianqiao_off")));
        settingsWidgets.get(7).setMessage(Component.translatable(
                "container.immortalstorage.reincarnation." + (menu.automaticOutput() ? "automatic_on" : "automatic_off")));
        settingsWidgets.get(6).setAlpha(menu.xianqiaoOutput() ? 1.0F : 0.45F);
        settingsWidgets.get(7).setAlpha(menu.automaticOutput() ? 1.0F : 0.45F);
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
        refreshSettings();
        refreshOutputButtons();
    }

    private void refreshSettings() {
        settingsWidgets.forEach(widget -> widget.visible = settingsOpen);
        if (redstoneModeButton != null) redstoneModeButton.visible = settingsOpen;
        menu.setSettingsVisible(settingsOpen);
    }

    @Override protected void containerTick() {
        super.containerTick();
        MachineRedstoneModeButton.refresh(redstoneModeButton, menu);
        refreshOutputButtons();
    }

    @Override
    protected void renderMachineBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Preserve the runtime vanilla chest frame and slot pixels. Only an
        // 18px #C6C6C6 information band is inserted between title and cache.
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, CHEST_TEXTURE, x, y, 0.0F, 0.0F,
                imageWidth, STATUS_TOP, 256, 256);
        graphics.fill(x, y + STATUS_TOP, x + imageWidth,
                y + STATUS_TOP + STATUS_HEIGHT, 0xFFC6C6C6);
        graphics.verticalLine(x, y + STATUS_TOP,
                y + STATUS_TOP + STATUS_HEIGHT - 1, 0xFFFFFFFF);
        graphics.verticalLine(x + imageWidth - 1, y + STATUS_TOP,
                y + STATUS_TOP + STATUS_HEIGHT - 1, 0xFF373737);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, CHEST_TEXTURE, x, y + STATUS_TOP + STATUS_HEIGHT,
                0.0F, STATUS_TOP, imageWidth, CHEST_ROWS_HEIGHT, 256, 256);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, CHEST_TEXTURE, x,
                y + STATUS_TOP + STATUS_HEIGHT + CHEST_ROWS_HEIGHT,
                0.0F, 126.0F, imageWidth, 96, 256, 256);
        if (settingsOpen) {
            VanillaGuiPainter.slot(
                    graphics, leftPos + 190, topPos + 160, true);
        }
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font,
                Component.translatable("container.immortalstorage.treasure_basin.mode", modeName()),
                8, 17, TEXT, false);

        Component status = statusName();
        graphics.text(font, status, 8, 26, TEXT, false);

        Component cache = Component.translatable("container.immortalstorage.treasure_basin.cache",
                menu.getFilledSlots(), menu.getCacheCapacity());
        graphics.text(font, cache, imageWidth - 8 - font.width(cache), 26, TEXT, false);
        graphics.text(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, TEXT, false);
        if (settingsOpen) {
            int panelX = imageWidth + 4;
            graphics.text(font, Component.translatable(
                    "container.immortalstorage.configuration.reinforcement_short"), panelX + 34, 165, TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component modeName() {
        Identifier mode = menu.getActiveMode();
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
