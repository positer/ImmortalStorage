package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.SpiritCompressorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Both modes process left to right and share the simulated-machine settings controls. */
public final class SpiritCompressorScreen extends AbstractContainerScreen<SpiritCompressorMenu> {
    private static final List<Direction> SIDE_ORDER = List.of(
            Direction.UP, Direction.NORTH, Direction.DOWN, Direction.WEST, Direction.SOUTH, Direction.EAST);
    private static final int SETTINGS_WIDTH = 112;
    private final Map<Direction, FacePreviewButton> faceButtons = new EnumMap<>(Direction.class);
    private final List<Button> settingsWidgets = new ArrayList<>();
    private boolean settingsOpen;
    private Button redstone;
    private Button xianqiao;
    public SpiritCompressorScreen(SpiritCompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable(menu.reversed()
                ? "container.immortalstorage.spirit_decompressor" : "container.immortalstorage.spirit_compressor"));
        imageWidth = 176; imageHeight = 182;
    }
    @Override protected void init() {
        super.init(); settingsWidgets.clear(); faceButtons.clear();
        int panelX = leftPos + imageWidth + 4;
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> { settingsOpen = !settingsOpen; refresh(); })
                .bounds(leftPos + imageWidth - 22, topPos - 20, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("container.immortalstorage.reincarnation.settings"))).build());
        redstone = addRenderableWidget(MachineRedstoneModeButton.create(panelX + 12, topPos + 18, menu));
        settingsWidgets.add(redstone);
        for (int index = 0; index < SIDE_ORDER.size(); index++) {
            Direction side = SIDE_ORDER.get(index);
            FacePreviewButton button = new FacePreviewButton(
                    panelX + 12 + index % 3 * 31, topPos + 48 + index / 3 * 31, 27,
                    Component.literal(shortSide(side)), () -> adjacentBlockPreview(side),
                    () -> modeColor(menu.value(side.ordinal())), clicked -> send(side.ordinal()));
            faceButtons.put(side, button); settingsWidgets.add(addRenderableWidget(button));
        }
        xianqiao = addRenderableWidget(Button.builder(Component.empty(), clicked -> send(6))
                .bounds(panelX + 12, topPos + 120, 89, 18).build());
        settingsWidgets.add(xianqiao); refresh();
    }
    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private ItemStack adjacentBlockPreview(Direction side) {
        if (minecraft == null || minecraft.level == null) return ItemStack.EMPTY;
        // Skip the structure shell to show the actual automation neighbour.
        return new ItemStack(minecraft.level.getBlockState(menu.blockPos().relative(side, 2)).getBlock());
    }
    private static String shortSide(Direction side) {
        return switch (side) { case UP -> "U"; case DOWN -> "D"; case NORTH -> "N";
            case SOUTH -> "S"; case WEST -> "W"; case EAST -> "E"; };
    }
    private static int modeColor(int mode) {
        return switch (mode) { case 1 -> 0xFF38A85A; case 2 -> 0xFFE69A38;
            case 3 -> 0xFF38AEC7; case 4 -> 0xFF9F60CA; default -> 0xFF777777; };
    }
    private void refresh() {
        for (Button button : settingsWidgets) button.visible = settingsOpen;
        faceButtons.forEach((side, button) -> {
            int mode = menu.value(side.ordinal()); button.setAlpha(mode == 0 ? .45F : 1F);
            button.setTooltip(Tooltip.create(Component.translatable("container.immortalstorage.spirit_compressor.face",
                    Component.translatable("container.immortalstorage.spirit_compressor.side." + side.ordinal()),
                    Component.translatable("container.immortalstorage.spirit_compressor.mode." + mode))));
        });
        xianqiao.setMessage(Component.translatable(menu.value(6) != 0
                ? "container.immortalstorage.reincarnation.xianqiao_on"
                : "container.immortalstorage.reincarnation.xianqiao_off"));
        xianqiao.setAlpha(menu.value(6) != 0 ? 1F : .45F);
        MachineRedstoneModeButton.refresh(redstone, menu);
    }
    @Override protected void containerTick() { super.containerTick(); refresh(); }
    @Override protected void renderBg(GuiGraphics graphics, float tick, int mx, int my) {
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (settingsOpen) {
            int panelX = leftPos + imageWidth + 4;
            VanillaGuiPainter.panel(graphics, panelX, topPos, SETTINGS_WIDTH, imageHeight);
            graphics.drawString(font, Component.translatable("container.immortalstorage.reincarnation.settings"), panelX + 8, topPos + 7, 0x404040, false);
            graphics.drawString(font, Component.translatable("container.immortalstorage.reincarnation.faces"), panelX + 8, topPos + 38, 0x404040, false);
            graphics.drawString(font, Component.translatable("container.immortalstorage.reincarnation.switches"), panelX + 8, topPos + 108, 0x404040, false);
        }
        for (var slot : menu.slots) VanillaGuiPainter.slot(graphics, leftPos + slot.x, topPos + slot.y, true);
        int arrowX = menu.reversed() ? 60 : 94;
        graphics.fill(leftPos + arrowX, topPos + 52, leftPos + arrowX + 21, topPos + 58, 0xFF888888);
        for (int i = 0; i < 9; i++) graphics.fill(leftPos + arrowX + 21 + i, topPos + 47 + i,
                leftPos + arrowX + 22 + i, topPos + 63 - i, 0xFF888888);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mx, int my) {
        graphics.drawString(font, title, 8, 7, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.immortalstorage.spirit_compressor."
                + (menu.value(7) == 0 ? "invalid" : "formed")), 8, 84, 0x404040, false);
    }
    @Override public void render(GuiGraphics graphics, int mx, int my, float tick) {
        super.render(graphics, mx, my, tick); renderTooltip(graphics, mx, my);
    }
}
