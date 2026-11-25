package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.RuinFilterLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import com.immortalstorage.immortalstorage.network.ModPayloads;

import java.util.ArrayList;
import java.util.List;

/** Vanilla six-row chest with a compact settings tab and server-backed +/- controls. */
public class StabilizedMiniatureImmortalRuinScreen<M extends StabilizedMiniatureImmortalRuinMenu> extends ConfigurationWindowScreen<M> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    protected boolean settings;
    private boolean filtersOpen;
    private boolean syncingValues;
    private final List<EditBox> valueBoxes = new ArrayList<>();
    private Button previewButton;
    private Button enabledButton;
    private final List<Button> faceButtons = new ArrayList<>();
    private Button redstoneModeButton;

    public StabilizedMiniatureImmortalRuinScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override protected net.minecraft.network.chat.Component configurationSideTitle() {
        return filtersOpen ? net.minecraft.network.chat.Component.translatable("container.immortalstorage.stabilized_ruin.filter")
                : super.configurationSideTitle();
    }

    @Override protected int configurationPanelWidth() { return filtersOpen ? 108 : 140; }
    @Override protected int configurationPanelMinimumHeight() {
        return filtersOpen ? RuinFilterLayout.MINIMUM_PANEL_HEIGHT : super.configurationPanelMinimumHeight();
    }

    @Override
    protected void init() {
        super.init();
        redstoneModeButton = addRenderableWidget(MachineRedstoneModeButton.create(
                redstoneButtonX(), redstoneButtonY(), menu));
        redstoneModeButton.setWidth(122);
        redstoneModeButton.visible = settings;
        menu.setPluginVisible(filtersOpen);
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> { settings = !settings; filtersOpen = false; refreshWidgets(); })
                .bounds(leftPos + 4, topPos - 18, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▦"), button -> {
                    filtersOpen = !filtersOpen; settings = false; refreshWidgets();
                }).bounds(leftPos + 26, topPos - 18, 20, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                        "container.immortalstorage.stabilized_ruin.filter"))).build());
        if (settings) addSettingsButtons();
        if (filtersOpen) addFilterButtons();
    }

    private void refreshWidgets() { clearWidgets(); init(); }

    protected int redstoneButtonX() { return leftPos + imageWidth + 10; }
    protected int redstoneButtonY() { return topPos + 162; }

    private void addSettingsButtons() {
        valueBoxes.clear();
        for (int index = 0; index < 6; index++) {
            int y = topPos + 18 + index * 18;
            addActionButton(leftPos + imageWidth + 36, y, 16, 16, "-", index * 2);
            addActionButton(leftPos + imageWidth + 116, y, 16, 16, "+", index * 2 + 1);
            int valueIndex = index;
            EditBox box = new InsetNumberBox(font, leftPos + imageWidth + 55, y, 58, 16, Component.empty());
            box.setValue(Integer.toString(menu.value(index)));
            box.setFilter(text -> text.matches("-?\\d{0,5}"));
            box.setResponder(text -> {
                if (syncingValues) return;
                if (text.isEmpty() || "-".equals(text)) return;
                try { PacketDistributor.sendToServer(new ModPayloads.SetStabilizedRuinValue(
                        menu.containerId, valueIndex, Integer.parseInt(text))); } catch (NumberFormatException ignored) { }
            });
            valueBoxes.add(box);
            addRenderableWidget(box);
        }
        previewButton = addActionButton(leftPos + imageWidth + 10, topPos + 126, 59, 16, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.preview").getString(), 12);
        enabledButton = addActionButton(leftPos + imageWidth + 73, topPos + 126, 59, 16, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.enabled").getString(), 13);
        addActionButton(leftPos + imageWidth + 36, topPos + 144, 16, 16, "-", 14);
        addActionButton(leftPos + imageWidth + 116, topPos + 144, 16, 16, "+", 15);
        EditBox frequency = new InsetNumberBox(font, leftPos + imageWidth + 55, topPos + 144, 58, 16, Component.empty());
        frequency.setValue(Integer.toString(menu.value(6)));
        frequency.setFilter(text -> text.matches("\\d{0,5}"));
        frequency.setResponder(text -> {
            if (syncingValues) return;
            if (text.isEmpty()) return;
            try { PacketDistributor.sendToServer(new ModPayloads.SetStabilizedRuinValue(
                    menu.containerId, 6, Integer.parseInt(text))); } catch (NumberFormatException ignored) { }
        });
        valueBoxes.add(frequency);
        addRenderableWidget(frequency);
    }

    private void addFilterButtons() {
        int panelX = leftPos + imageWidth + 10;
        var ruin = filterEntity();
        Component nbt = Component.translatable(ruin != null && ruin.filterMatchComponents()
                ? "container.immortalstorage.stabilized_ruin.nbt_on"
                : "container.immortalstorage.stabilized_ruin.nbt_off");
        Component list = Component.translatable(ruin == null || ruin.filterWhitelist()
                ? "container.immortalstorage.stabilized_ruin.whitelist"
                : "container.immortalstorage.stabilized_ruin.blacklist");
        addRenderableWidget(Button.builder(nbt, button -> PacketDistributor.sendToServer(
                        new ModPayloads.ToggleStabilizedRuinFilterMode(menu.containerId, 0)))
                .bounds(panelX, topPos + 92, 90, 18).build());
        addRenderableWidget(Button.builder(list, button -> PacketDistributor.sendToServer(
                        new ModPayloads.ToggleStabilizedRuinFilterMode(menu.containerId, 1)))
                .bounds(panelX, topPos + 112, 90, 18).build());
        faceButtons.clear();
        faceButtons.addAll(RuinFaceGrid.add(this::sendFaceToggle, 20, panelX, topPos + RuinFilterLayout.FACE_GRID_Y));
        for (Button button : faceButtons) addRenderableWidget(button);
        RuinFaceGrid.sync(faceButtons, menu.value(faceDataIndex()), faceButtonVisible());
    }

    /** Sends a menu-button click that toggles one Direction bit in the face mask. */
    protected void sendFaceToggle(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** Data-slot index exposing the interaction face mask (one bit per Direction ordinal). */
    protected int faceDataIndex() { return 10; }

    /** Face only applies to container transfers; the plain stabilized ruin uses it when reversed. */
    protected boolean faceButtonVisible() { return menu.value(9) != 0; }

    public com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity filterEntity() {
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getBlockEntity(menu.blockPos()) instanceof
                com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin : null;
    }

    public net.minecraft.client.renderer.Rect2i filterSlotBounds(int slot) {
        return new net.minecraft.client.renderer.Rect2i(leftPos + imageWidth + 10 + (slot % 5) * 18,
                topPos + 16 + (slot / 5) * 18, 18, 18);
    }
    public boolean filtersOpen() { return filtersOpen; }

    public void setGhostFilter(int slot, net.minecraft.world.item.ItemStack stack) {
        PacketDistributor.sendToServer(new ModPayloads.SetStabilizedRuinFilter(
                menu.containerId, slot, stack.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : stack.copyWithCount(1)));
    }

    private Button addActionButton(int x, int y, int width, int height, String label, int id) {
        Button button = Button.builder(Component.literal(label), clicked -> {
            if (id >= 0 && id < 12 && id / 2 < valueBoxes.size()) {
                valueBoxes.get(id / 2).setFocused(false);
            } else if ((id == 14 || id == 15) && valueBoxes.size() > 6) {
                valueBoxes.get(6).setFocused(false);
            }
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MachineRedstoneModeButton.refresh(redstoneModeButton, menu);
        if (!settings) return;
        syncingValues = true;
        for (int index = 0; index < valueBoxes.size(); index++) {
            EditBox box = valueBoxes.get(index);
            String authoritative = Integer.toString(menu.value(index));
            if (!box.isFocused() && !authoritative.equals(box.getValue())) box.setValue(authoritative);
        }
        syncingValues = false;
        if (previewButton != null) previewButton.setAlpha(menu.value(7) != 0 ? 1.0F : 0.35F);
        if (enabledButton != null) enabledButton.setAlpha(menu.value(8) != 0 ? 1.0F : 0.35F);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (filtersOpen) {
            var ruin = filterEntity();
            for (int slot = 0; slot < 20; slot++) {
                var bounds = filterSlotBounds(slot);
                VanillaGuiPainter.slot(graphics, bounds.getX() + 1, bounds.getY() + 1, true);
                if (ruin != null && !ruin.filter(slot).isEmpty()) graphics.renderItem(
                        ruin.filter(slot), bounds.getX() + 1, bounds.getY() + 1);
            }
            if (!faceButtons.isEmpty()) {
                RuinFaceGrid.sync(faceButtons, menu.value(faceDataIndex()), faceButtonVisible());
            }
            VanillaGuiPainter.slot(graphics, leftPos + RuinFilterLayout.REINFORCEMENT_X,
                    topPos + RuinFilterLayout.REINFORCEMENT_Y, true);
            graphics.drawString(font, Component.translatable("container.immortalstorage.configuration.reinforcement_short"),
                    leftPos + RuinFilterLayout.REINFORCEMENT_X + 22,
                    topPos + RuinFilterLayout.REINFORCEMENT_Y + 5, ConfigurationStyle.TEXT, false);
        }
        if (!settings) return;
        String[] labels = {"x", "y", "z", "+x", "+y", "+z"};
        for (int i = 0; i < labels.length; i++) graphics.drawString(font, labels[i], leftPos + imageWidth + 10, topPos + 22 + i * 18, ConfigurationStyle.TEXT, false);
        graphics.drawString(font, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.tick").getString(), leftPos + imageWidth + 10, topPos + 148, ConfigurationStyle.TEXT, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (filtersOpen && button == 0) {
            for (int slot = 0; slot < 20; slot++) {
                var bounds = filterSlotBounds(slot);
                if (mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                        && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight()) {
                    setGhostFilter(slot, menu.getCarried());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
