package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.MiniatureImmortalRuinMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class MiniatureImmortalRuinScreen extends AbstractContainerScreen<MiniatureImmortalRuinMenu> {
    private final List<Button> optionButtons = new ArrayList<>();
    public MiniatureImmortalRuinScreen(MiniatureImmortalRuinMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageWidth = 176; imageHeight = 132; }
    @Override protected void init() { super.init(); optionButtons.clear(); button(0, 22); button(1, 44); button(2, 66); button(3, 88); button(4, 110); }
    private void button(int id, int y) {
        Button button = Button.builder(label(id), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + 18, topPos + y, 140, 18).build();
        optionButtons.add(button);
        addRenderableWidget(button);
    }
    private Component label(int id) { return switch (id) { case 0 -> Component.literal("Players: " + on(menu.value(0))); case 1 -> Component.literal("Entity damage: " + on(menu.value(1))); case 2 -> Component.literal("Player damage: " + on(menu.value(2))); case 3 -> Component.literal("Force: " + new String[]{"None","Light","Medium","Strong","Teleport"}[menu.value(3)]); default -> Component.translatable("container.immortalstorage.miniature_ruin.warp", on(menu.value(4))); }; }
    private static String on(int value) { return value == 0 ? "Off" : "On"; }
    @Override protected void containerTick() {
        super.containerTick();
        for (int id = 0; id < optionButtons.size(); id++) {
            Button button = optionButtons.get(id);
            button.setMessage(label(id));
            button.setAlpha(id == 3 || menu.value(id) != 0 ? 1.0F : 0.35F);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0C6C6C6); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
}
