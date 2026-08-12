package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.menu.custom.EnergyCrystalMenu;
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
import java.util.Optional;

/**
 * Simulated-machine UI with the output grid footprint converted into a single
 * Mekanism-inspired FE memory meter.  The left machine geometry remains shared
 * with the two simulated machines, including the static processing arrow.
 */
public final class EnergyCrystalScreen extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<EnergyCrystalMenu> {
    private static final Identifier FURNACE = Identifier.withDefaultNamespace(
            "textures/gui/container/furnace.png");
    private static final List<Direction> SIDE_ORDER = List.of(
            Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    private static final int SETTINGS_WIDTH = 112;
    private static final int TANK_X = 132;
    private static final int TANK_Y = 26;
    private static final int TANK_WIDTH = 72;
    private static final int TANK_HEIGHT = 54;
    private final Map<Direction, FacePreviewButton> faceButtons = new EnumMap<>(Direction.class);
    private final List<Button> settingsWidgets = new ArrayList<>();
    private Button xianqiaoButton;
    private Button automaticButton;
    private boolean settingsOpen;

    public EnergyCrystalScreen(EnergyCrystalMenu menu, Inventory inventory, Component title) {
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
                .tooltip(Tooltip.create(Component.translatable(menu.uiKey("settings")))).build());
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
                    menu.uiKey("face." + side.get3DDataValue()))));
            faceButtons.put(side, button);
            settingsWidgets.add(addRenderableWidget(button));
        }
        xianqiaoButton = addRenderableWidget(Button.builder(Component.empty(), clicked -> send(0))
                .bounds(panelX + 12, topPos + 120, 89, 18).build());
        automaticButton = addRenderableWidget(Button.builder(Component.empty(), clicked -> send(1))
                .bounds(panelX + 12, topPos + 143, 89, 18).build());
        settingsWidgets.add(xianqiaoButton);
        settingsWidgets.add(automaticButton);
    }

    private ItemStack adjacentBlockPreview(Direction side) {
        if (minecraft == null || minecraft.level == null
                || menu.blockPos().equals(net.minecraft.core.BlockPos.ZERO)) return ItemStack.EMPTY;
        ItemStack preview = new ItemStack(minecraft.level.getBlockState(menu.blockPos().relative(side))
                .getBlock().asItem());
        return preview.isEmpty() ? ItemStack.EMPTY : preview;
    }

    private static String shortSide(Direction side) {
        return switch (side) {
            case UP -> "U";
            case DOWN -> "D";
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
        };
    }

    private void toggleSettings() {
        settingsOpen = !settingsOpen;
        refreshSettingsWidgets();
    }

    private void refreshSettingsWidgets() {
        settingsWidgets.forEach(button -> button.visible = settingsOpen);
    }

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
                ? menu.uiKey("xianqiao_on") : menu.uiKey("xianqiao_off")));
        automaticButton.setMessage(Component.translatable(menu.automaticOutput()
                ? menu.uiKey("automatic_on") : menu.uiKey("automatic_off")));
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

        int burnDuration = Math.max(1, menu.burnDuration());
        int flame = Mth.ceil(Math.min(1.0F, menu.burnTicks() / (float) burnDuration) * 13.0F);
        VanillaGuiPainter.furnaceFlame(graphics, leftPos + 28, topPos + 47, flame,
                menu.burnTicks() > 0);
        // Deliberately no burn/process overlay on the arrow: the crystal arrow
        // is a fixed direction cue, not a progress indicator.
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, FURNACE, leftPos + 91, topPos + 44, 79.0F, 34.0F,
                24, 16, 256, 256);
        renderEnergyTank(graphics);
        if (settingsOpen) renderSettingsPanel(graphics);
    }

    private void renderEnergyTank(GuiGraphicsExtractor graphics) {
        int x = leftPos + TANK_X;
        int y = topPos + TANK_Y;
        graphics.fill(x, y, x + TANK_WIDTH, y + TANK_HEIGHT, 0xFF20252A);
        graphics.fill(x + 2, y + 2, x + TANK_WIDTH - 2, y + TANK_HEIGHT - 2, 0xFF6C747B);
        graphics.fill(x + 4, y + 4, x + TANK_WIDTH - 4, y + TANK_HEIGHT - 4, 0xFF3A454D);

        long capacity = Math.max(1L, menu.energyCapacity());
        long stored = Math.max(0L, Math.min(capacity, menu.storedEnergy()));
        int innerHeight = TANK_HEIGHT - 8;
        int filled = (int) Math.min(innerHeight, Math.round(innerHeight * (stored / (double) capacity)));
        if (filled > 0) {
            int fillTop = y + 4 + innerHeight - filled;
            graphics.fill(x + 4, fillTop, x + TANK_WIDTH - 4, y + 4 + innerHeight,
                    0xFFC1E7EC);
            graphics.fill(x + 6, fillTop + 1, x + TANK_WIDTH - 6,
                    Math.min(y + 4 + innerHeight, fillTop + 3), 0xFFE8FAFC);
        }

        // Mekanism-like level marks: a full-width dark mark at the centre and
        // short left ticks at the quarter boundaries remain visible over fill.
        int innerLeft = x + 3;
        int innerRight = x + TANK_WIDTH - 3;
        int innerTop = y + 3;
        int innerBottom = y + TANK_HEIGHT - 3;
        graphics.horizontalLine(innerLeft, innerRight, innerTop, 0xFF171B1E);
        graphics.horizontalLine(innerLeft, innerRight, innerBottom, 0xFFE8F2F3);
        for (int mark = 1; mark < 4; mark++) {
            int markY = innerTop + Math.round((innerBottom - innerTop) * mark / 4.0F);
            graphics.horizontalLine(innerLeft, x + 10, markY, 0xFF512B36);
        }
        graphics.horizontalLine(innerLeft, innerRight, y + TANK_HEIGHT / 2, 0xFF512B36);
    }

    private void renderSettingsPanel(GuiGraphicsExtractor graphics) {
        int panelX = leftPos + imageWidth + 4;
        VanillaGuiPainter.panel(graphics, panelX, topPos, SETTINGS_WIDTH, imageHeight);
        graphics.text(font, Component.translatable(
                menu.uiKey("settings")), panelX + 8, topPos + 7,
                0x404040, false);
        graphics.text(font, Component.translatable(
                menu.uiKey("faces")), panelX + 8, topPos + 27,
                0x404040, false);
        graphics.text(font, Component.translatable(
                menu.uiKey("switches")),
                panelX + 8, topPos + 108, 0x404040, false);
    }

    private boolean tankHovered(double mouseX, double mouseY) {
        return mouseX >= leftPos + TANK_X && mouseX < leftPos + TANK_X + TANK_WIDTH
                && mouseY >= topPos + TANK_Y && mouseY < topPos + TANK_Y + TANK_HEIGHT;
    }

    @Override protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                0x404040, false);
    }

    @Override public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tankHovered(mouseX, mouseY)) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, font, List.of(
                    Component.translatable(menu.uiKey("resource")),
                    Component.translatable(menu.uiKey("stored"),
                            format(menu.storedEnergy())),
                    Component.translatable(menu.uiKey("capacity"),
                            format(menu.energyCapacity()))), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private static String format(long amount) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, amount));
    }
}
