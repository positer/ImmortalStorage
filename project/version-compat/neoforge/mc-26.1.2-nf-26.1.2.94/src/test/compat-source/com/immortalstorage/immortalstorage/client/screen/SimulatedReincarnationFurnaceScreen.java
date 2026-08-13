package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedReincarnationFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Wide vanilla-style machine panel with a Xianqiao Interface matching face-preview rail. */
public final class SimulatedReincarnationFurnaceScreen
        extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<SimulatedReincarnationFurnaceMenu> {
    private static final Identifier FURNACE = Identifier.withDefaultNamespace(
            "textures/gui/container/furnace.png");
private static final List<Direction> SIDE_ORDER = List.of(
            Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    private static final int SETTINGS_WIDTH = 112;
    private final Map<Direction, FacePreviewButton> faceButtons = new EnumMap<>(Direction.class);
    private final List<Button> settingsWidgets = new ArrayList<>();
    private Button xianqiaoButton;
    private Button automaticButton;
    private boolean settingsOpen;

    public SimulatedReincarnationFurnaceScreen(SimulatedReincarnationFurnaceMenu menu,
                                                Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 187;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 26;
        inventoryLabelY = 94;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> toggleSettings())
                .bounds(leftPos + imageWidth - 22, topPos - 20, 20, 20)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.reincarnation.settings"))).build());
        buildSettingsWidgets();
        refreshSettingsWidgets();
    }

    private void buildSettingsWidgets() {
        int panelX = leftPos + imageWidth + 4;
        faceButtons.clear();
        for (int index = 0; index < SIDE_ORDER.size(); index++) {
            Direction side = SIDE_ORDER.get(index);
            FacePreviewButton button = new FacePreviewButton(
                    panelX + 12 + (index % 3) * 31, topPos + 48 + (index / 3) * 31,
                    27, Component.literal(shortSide(side)), () -> adjacentBlockPreview(side),
                    () -> menu.outputFace(side.get3DDataValue()) ? 0xFF38A85A : 0xFF777777,
                    clicked -> send(10 + side.get3DDataValue()));
            button.setTooltip(Tooltip.create(Component.translatable(
                    "container.immortalstorage.reincarnation.face." + side.get3DDataValue())));
            faceButtons.put(side, button);
            settingsWidgets.add(addRenderableWidget(button));
        }
        xianqiaoButton = addRenderableWidget(Button.builder(Component.empty(), clicked -> send(0))
                .bounds(panelX + 12, topPos + 120, 89, 18).build());
        automaticButton = addRenderableWidget(Button.builder(Component.empty(), clicked -> send(1))
                .bounds(panelX + 12, topPos + 143, 89, 18).build());
        settingsWidgets.add(xianqiaoButton);
        settingsWidgets.add(automaticButton);
        settingsWidgets.add(addRenderableWidget(Button.builder(
                Component.translatable("container.immortalstorage.reincarnation.release_xp"), clicked -> send(2))
                .bounds(panelX + 12, topPos + 166, 89, 18).build()));
    }

    private ItemStack adjacentBlockPreview(Direction side) {
        if (minecraft == null || minecraft.level == null || menu.blockPos().equals(net.minecraft.core.BlockPos.ZERO)) {
            return ItemStack.EMPTY;
        }
        ItemStack preview = new ItemStack(minecraft.level.getBlockState(menu.blockPos().relative(side)).getBlock().asItem());
        return preview.isEmpty() ? ItemStack.EMPTY : preview;
    }

    private static String shortSide(Direction side) {
        return switch (side) {
            case UP -> "U"; case DOWN -> "D"; case NORTH -> "N";
            case SOUTH -> "S"; case WEST -> "W"; case EAST -> "E";
        };
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        refreshSettingsWidgets();
    }

    private void refreshSettingsWidgets() { settingsWidgets.forEach(button -> button.visible = settingsOpen); }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        faceButtons.forEach((side, button) -> button.setAlpha(
                menu.outputFace(side.get3DDataValue()) ? 1.0F : 0.45F));
        refreshSwitchButtons();
    }

    private void refreshSwitchButtons() {
        if (xianqiaoButton == null || automaticButton == null) return;
        xianqiaoButton.setMessage(Component.translatable(menu.xianqiaoOutput()
                ? "container.immortalstorage.reincarnation.xianqiao_on"
                : "container.immortalstorage.reincarnation.xianqiao_off"));
        automaticButton.setMessage(Component.translatable(menu.automaticOutput()
                ? "container.immortalstorage.reincarnation.automatic_on"
                : "container.immortalstorage.reincarnation.automatic_off"));
        xianqiaoButton.setAlpha(menu.xianqiaoOutput() ? 1.0F : 0.45F);
        automaticButton.setAlpha(menu.automaticOutput() ? 1.0F : 0.45F);
    }

    @Override protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 17, 0xFFD8D8D8);
        graphics.horizontalLine(leftPos + 3, leftPos + imageWidth - 4, topPos + 17, 0xFF8B8B8B);
        graphics.horizontalLine(leftPos + 8, leftPos + imageWidth - 9, topPos + 91, 0xFF8B8B8B);
        graphics.horizontalLine(leftPos + 8, leftPos + imageWidth - 9, topPos + 92, 0xFFFFFFFF);
        VanillaGuiPainter.slots(graphics, leftPos, topPos, menu.slots);

        int flame = Mth.ceil(Math.min(1.0F, menu.burnTicks() / 500.0F) * 13.0F);
        VanillaGuiPainter.furnaceFlame(graphics, leftPos + 28, topPos + 47, flame, menu.burnTicks() > 0);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, FURNACE, leftPos + 91, topPos + 44, 79.0F, 34.0F, 24, 16, 256, 256);
        int progress = Mth.ceil(Math.min(1.0F, menu.progress() / 50.0F) * 24.0F);
        if (progress > 0) VanillaGuiPainter.furnaceProgress(graphics, leftPos + 91, topPos + 44, progress);

        if (settingsOpen) renderSettingsPanel(graphics);
    }

    private void renderSettingsPanel(GuiGraphicsExtractor graphics) {
        int panelX = leftPos + imageWidth + 4;
        VanillaGuiPainter.panel(graphics, panelX, topPos, SETTINGS_WIDTH, imageHeight);
        graphics.text(font, Component.translatable(
                "container.immortalstorage.reincarnation.settings"), panelX + 8, topPos + 7, 0x404040, false);
        graphics.text(font, Component.translatable(
                "container.immortalstorage.reincarnation.faces"), panelX + 8, topPos + 27, 0x404040, false);
        graphics.text(font, Component.translatable(
                "container.immortalstorage.reincarnation.switches"), panelX + 8, topPos + 108,
                0x404040, false);
    }

    @Override protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
