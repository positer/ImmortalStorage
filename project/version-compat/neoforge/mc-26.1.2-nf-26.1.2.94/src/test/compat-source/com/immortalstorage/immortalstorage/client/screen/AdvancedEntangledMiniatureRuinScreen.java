package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.block.entity.AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.menu.custom.AdvancedEntangledMiniatureRuinMenu;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Merged advanced ruin storage with the stabilized settings/filter panels, a side selector and scheduling buttons. */
public final class AdvancedEntangledMiniatureRuinScreen extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<AdvancedEntangledMiniatureRuinMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int SIDE_SPAN = 100;
    private static final int PER_SIDE = 13;
    private boolean settings;
    private boolean filtersOpen;
    private boolean syncingValues;
    private int activeSide;
    private final List<EditBox> valueBoxes = new ArrayList<>();
    private Button previewButton;
    private Button enabledButton;
    private Button accessButton;
    private Button splitButton;
    private Button orderButton;
    private final List<Button> faceButtons = new ArrayList<>();

    public AdvancedEntangledMiniatureRuinScreen(AdvancedEntangledMiniatureRuinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        menu.setPluginVisible(filtersOpen);
        addRenderableWidget(Button.builder(Component.literal("⚙"), button -> { settings = !settings; filtersOpen = false; refreshWidgets(); })
                .bounds(leftPos + 4, topPos - 20, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▦"), button -> {
                    filtersOpen = !filtersOpen; settings = false; refreshWidgets();
                }).bounds(leftPos + 26, topPos - 20, 20, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                        "container.immortalstorage.stabilized_ruin.filter"))).build());
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> { activeSide = 0; refreshWidgets(); })
                .bounds(leftPos + 48, topPos - 20, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> { activeSide = 1; refreshWidgets(); })
                .bounds(leftPos + 70, topPos - 20, 20, 20).build());
        if (settings) addSettingsButtons();
        if (filtersOpen) addFilterButtons();
    }

    private void refreshWidgets() { clearWidgets(); init(); }

    private int base() { return activeSide * PER_SIDE; }

    private void addSettingsButtons() {
        valueBoxes.clear();
        for (int index = 0; index < 6; index++) {
            int y = topPos + 18 + index * 18;
            addActionButton(leftPos + imageWidth + 6, y, 16, 16, "-", index * 2);
            addActionButton(leftPos + imageWidth + 78, y, 16, 16, "+", index * 2 + 1);
            int valueIndex = index;
            EditBox box = new EditBox(font, leftPos + imageWidth + 25, y, 50, 16, Component.empty());
            box.setValue(Integer.toString(menu.value(base() + index)));
            box.setFilter(text -> text.matches("-?\\d{0,5}"));
            box.setResponder(text -> {
                if (syncingValues) return;
                if (text.isEmpty() || "-".equals(text)) return;
                try { ClientPacketDistributor.sendToServer(new ModPayloads.SetEntangledRuinValue(
                        menu.containerId, activeSide, valueIndex, Integer.parseInt(text))); } catch (NumberFormatException ignored) { }
            });
            valueBoxes.add(box);
            addRenderableWidget(box);
        }
        previewButton = addActionButton(leftPos + imageWidth + 6, topPos + 130, 88, 18, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.preview").getString(), 12);
        enabledButton = addActionButton(leftPos + imageWidth + 6, topPos + 150, 88, 18, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.enabled").getString(), 13);
        addActionButton(leftPos + imageWidth + 6, topPos + 170, 20, 18, "-", 14);
        addActionButton(leftPos + imageWidth + 74, topPos + 170, 20, 18, "+", 15);
        EditBox frequency = new EditBox(font, leftPos + imageWidth + 29, topPos + 170, 42, 18, Component.empty());
        frequency.setValue(Integer.toString(menu.value(base() + 6)));
        frequency.setFilter(text -> text.matches("\\d{0,5}"));
        frequency.setResponder(text -> {
            if (syncingValues) return;
            if (text.isEmpty()) return;
            try { ClientPacketDistributor.sendToServer(new ModPayloads.SetEntangledRuinValue(
                    menu.containerId, activeSide, 6, Integer.parseInt(text))); } catch (NumberFormatException ignored) { }
        });
        valueBoxes.add(frequency);
        addRenderableWidget(frequency);
        accessButton = addActionButton(leftPos + imageWidth + 6, topPos + 192, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(base() + 9) == 0 ? "container.immortalstorage.advanced_ruin.access_skip" : "container.immortalstorage.advanced_ruin.access_force").getString(), 16);
        splitButton = addActionButton(leftPos + imageWidth + 6, topPos + 210, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(base() + 10) == 0 ? "container.immortalstorage.advanced_ruin.split_item" : "container.immortalstorage.advanced_ruin.split_group").getString(), 17);
        orderButton = addActionButton(leftPos + imageWidth + 6, topPos + 228, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(base() + 11) == 0 ? "container.immortalstorage.advanced_ruin.order_far" : "container.immortalstorage.advanced_ruin.order_near").getString(), 18);
    }

    private void addFilterButtons() {
        int panelX = leftPos + imageWidth + 6;
        var ruin = filterEntity();
        Component nbt = Component.translatable(ruin != null && ruin.filterMatchComponents(activeSide)
                ? "container.immortalstorage.stabilized_ruin.nbt_on"
                : "container.immortalstorage.stabilized_ruin.nbt_off");
        Component list = Component.translatable(ruin == null || ruin.filterWhitelist(activeSide)
                ? "container.immortalstorage.stabilized_ruin.whitelist"
                : "container.immortalstorage.stabilized_ruin.blacklist");
        addRenderableWidget(Button.builder(nbt, button -> ClientPacketDistributor.sendToServer(
                        new ModPayloads.ToggleEntangledRuinFilterMode(menu.containerId, activeSide, 0)))
                .bounds(panelX, topPos + 92, 88, 18).build());
        addRenderableWidget(Button.builder(list, button -> ClientPacketDistributor.sendToServer(
                        new ModPayloads.ToggleEntangledRuinFilterMode(menu.containerId, activeSide, 1)))
                .bounds(panelX, topPos + 112, 88, 18).build());
        faceButtons.clear();
        faceButtons.addAll(RuinFaceGrid.add(this::sendFaceToggle, activeSide * SIDE_SPAN + 20, panelX, topPos + 132));
        for (Button button : faceButtons) addRenderableWidget(button);
        RuinFaceGrid.sync(faceButtons, menu.value(base() + 12), true);
    }

    private void sendFaceToggle(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    public AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity filterEntity() {
        if (minecraft == null || minecraft.level == null) return null;
        return minecraft.level.getBlockEntity(menu.blockPos()) instanceof
                AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin : null;
    }

    public net.minecraft.client.renderer.Rect2i filterSlotBounds(int slot) {
        return new net.minecraft.client.renderer.Rect2i(leftPos + imageWidth + 6 + (slot % 5) * 18,
                topPos + 16 + (slot / 5) * 18, 18, 18);
    }

    public void setGhostFilter(int slot, net.minecraft.world.item.ItemStack stack) {
        ClientPacketDistributor.sendToServer(new ModPayloads.SetEntangledRuinFilter(
                menu.containerId, activeSide, slot,
                stack.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : stack.copyWithCount(1)));
    }

    private Button addActionButton(int x, int y, int width, int height, String label, int localId) {
        int id = activeSide * SIDE_SPAN + localId;
        Button button = Button.builder(Component.literal(label), clicked -> {
            if (localId >= 0 && localId < 12 && localId / 2 < valueBoxes.size()) {
                valueBoxes.get(localId / 2).setFocused(false);
            } else if ((localId == 14 || localId == 15) && valueBoxes.size() > 6) {
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
        if (!settings) return;
        syncingValues = true;
        for (int index = 0; index < valueBoxes.size(); index++) {
            EditBox box = valueBoxes.get(index);
            String authoritative = Integer.toString(menu.value(base() + index));
            if (!box.isFocused() && !authoritative.equals(box.getValue())) box.setValue(authoritative);
        }
        syncingValues = false;
        if (previewButton != null) previewButton.setAlpha(menu.value(base() + 7) != 0 ? 1.0F : 0.35F);
        if (enabledButton != null) enabledButton.setAlpha(menu.value(base() + 8) != 0 ? 1.0F : 0.35F);
        if (accessButton != null) accessButton.setMessage(Component.translatable(menu.value(base() + 9) == 0
                ? "container.immortalstorage.advanced_ruin.access_skip"
                : "container.immortalstorage.advanced_ruin.access_force"));
        if (splitButton != null) splitButton.setMessage(Component.translatable(menu.value(base() + 10) == 0
                ? "container.immortalstorage.advanced_ruin.split_item"
                : "container.immortalstorage.advanced_ruin.split_group"));
        if (orderButton != null) orderButton.setMessage(Component.translatable(menu.value(base() + 11) == 0
                ? "container.immortalstorage.advanced_ruin.order_far"
                : "container.immortalstorage.advanced_ruin.order_near"));
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F, 256, 256);
        if (filtersOpen) {
            var ruin = filterEntity();
            for (int slot = 0; slot < 20; slot++) {
                var bounds = filterSlotBounds(slot);
                VanillaGuiPainter.slot(graphics, bounds.getX(), bounds.getY(), true);
                if (ruin != null && !ruin.filter(activeSide, slot).isEmpty()) graphics.item(
                        ruin.filter(activeSide, slot), bounds.getX() + 1, bounds.getY() + 1);
            }
            if (!faceButtons.isEmpty()) {
                RuinFaceGrid.sync(faceButtons, menu.value(base() + 12), true);
            }
            VanillaGuiPainter.slot(graphics, leftPos + 188, topPos + 198, true);
            graphics.text(font, Component.translatable("container.immortalstorage.reinforcement_plugin"),
                    leftPos + 210, topPos + 203, 0xFFFFFF, true);
        }
        if (!settings) return;
        String[] labels = {"x", "y", "z", "+x", "+y", "+z"};
        for (int i = 0; i < labels.length; i++) graphics.text(font, labels[i], leftPos + imageWidth + 98, topPos + 22 + i * 18, 0xFFFFFF, true);
        graphics.text(font, net.minecraft.network.chat.Component.translatable("container.immortalstorage.ruin.tick").getString(), leftPos + imageWidth + 98, topPos + 175, 0xFFFFFF, true);
        graphics.text(font, net.minecraft.network.chat.Component.translatable(activeSide == 0 ? "container.immortalstorage.ruin.side_normal" : "container.immortalstorage.ruin.side_reversed").getString(), leftPos + imageWidth + 6, topPos + 2, 0xFFFFFF, true);
    }

    @Override protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }

    @Override public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
