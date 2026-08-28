package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.AdvancedStabilizedMiniatureImmortalRuinMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Stabilized-ruin panel with the container access / split / order controls inside the settings tab. */
public final class AdvancedStabilizedMiniatureImmortalRuinScreen extends StabilizedMiniatureImmortalRuinScreen<AdvancedStabilizedMiniatureImmortalRuinMenu> {
    private Button accessButton;
    private Button splitButton;
    private Button orderButton;

    public AdvancedStabilizedMiniatureImmortalRuinScreen(AdvancedStabilizedMiniatureImmortalRuinMenu menu,
                                                         Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        if (settings) addSchedulingButtons();
    }

    private void addSchedulingButtons() {
        accessButton = addActionButton(leftPos + imageWidth + 6, topPos + 190, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(10) == 0 ? "container.immortalstorage.advanced_ruin.access_skip" : "container.immortalstorage.advanced_ruin.access_force").getString(), 16);
        splitButton = addActionButton(leftPos + imageWidth + 6, topPos + 208, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(11) == 0 ? "container.immortalstorage.advanced_ruin.split_item" : "container.immortalstorage.advanced_ruin.split_group").getString(), 17);
        orderButton = addActionButton(leftPos + imageWidth + 6, topPos + 226, 88, 18, net.minecraft.network.chat.Component.translatable(menu.value(12) == 0 ? "container.immortalstorage.advanced_ruin.order_far" : "container.immortalstorage.advanced_ruin.order_near").getString(), 18);
    }

    @Override protected int redstoneButtonY() { return topPos + 2; }

    private Button addActionButton(int x, int y, int width, int height, String label, int id) {
        Button button = Button.builder(Component.literal(label), clicked -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    @Override
    protected int faceDataIndex() { return 13; }

    /** The advanced ruin schedules containers in both modes, so the face always applies. */
    @Override
    protected boolean faceButtonVisible() { return true; }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!settings) return;
        if (accessButton != null) accessButton.setMessage(Component.translatable(
                menu.value(10) == 0
                        ? "container.immortalstorage.advanced_ruin.access_skip"
                        : "container.immortalstorage.advanced_ruin.access_force"));
        if (splitButton != null) splitButton.setMessage(Component.translatable(
                menu.value(11) == 0
                        ? "container.immortalstorage.advanced_ruin.split_item"
                        : "container.immortalstorage.advanced_ruin.split_group"));
        if (orderButton != null) orderButton.setMessage(Component.translatable(
                menu.value(12) == 0
                        ? "container.immortalstorage.advanced_ruin.order_far"
                        : "container.immortalstorage.advanced_ruin.order_near"));
    }
}
