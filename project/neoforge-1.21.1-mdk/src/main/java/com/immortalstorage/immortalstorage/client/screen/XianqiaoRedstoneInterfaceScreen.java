package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoRedstoneInterfaceMenu;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Exact two-row vanilla-style Schmitt-trigger configuration screen. */
public final class XianqiaoRedstoneInterfaceScreen
        extends AbstractContainerScreen<XianqiaoRedstoneInterfaceMenu> {
    private static final int TEXT = 0xFF404040;
    private static final int SLOT_X = 8;
    private static final int FIRST_ROW_Y = 8;
    private static final int SECOND_ROW_Y = 30;
    private static final int FIELD_X = 80;
    private static final int FIELD_WIDTH = 62;
    private static final int MODAL_WIDTH = 176;
    private static final int MODAL_HEIGHT = 132;
    private static final float MODAL_Z = 500.0F;

    private final List<Button> externalResourceButtons = new ArrayList<>();
    private EditBox high;
    private EditBox low;
    private Button polarityButton;
    private boolean inverted;
    private boolean externalDialogOpen;
    private int externalResourceOffset;
    private boolean configurationLoaded;

    public XianqiaoRedstoneInterfaceScreen(
            XianqiaoRedstoneInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 147;
        inventoryLabelX = 8;
        inventoryLabelY = 53;
    }

    @Override
    protected void init() {
        super.init();
        high = numberBox(leftPos + FIELD_X, topPos + FIRST_ROW_Y);
        low = numberBox(leftPos + FIELD_X, topPos + SECOND_ROW_Y);
        high.active = false;
        low.active = false;
        polarityButton = Button.builder(Component.empty(), ignored -> {
                    inverted = !inverted;
                    apply();
                })
                .bounds(leftPos + SLOT_X, topPos + SECOND_ROW_Y, 18, 18)
                .build();
        polarityButton.active = false;
        addRenderableWidget(high);
        addRenderableWidget(low);
        addRenderableWidget(polarityButton);
        rebuildExternalResourceButtons();
        loadSynchronizedConfiguration();
    }

    private EditBox numberBox(int x, int y) {
        EditBox box = new EditBox(font, x, y, FIELD_WIDTH, 18, Component.empty());
        box.setFilter(valueText -> valueText.isEmpty()
                || valueText.chars().allMatch(Character::isDigit));
        box.setMaxLength(19);
        return box;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        loadSynchronizedConfiguration();
    }

    private void loadSynchronizedConfiguration() {
        if (configurationLoaded || !menu.configurationSynchronized()
                || high == null || low == null) return;
        high.setValue(Long.toString(menu.high()));
        low.setValue(Long.toString(menu.low()));
        inverted = menu.inverted();
        high.active = true;
        low.active = true;
        polarityButton.active = true;
        configurationLoaded = true;
    }

    private boolean apply() {
        if (!configurationLoaded || low == null || high == null) return false;
        try {
            long requestedLow = Long.parseLong(low.getValue());
            long requestedHigh = Long.parseLong(high.getValue());
            long appliedLow = Math.max(0L, requestedLow);
            long appliedHigh = Math.max(appliedLow, requestedHigh);
            PacketDistributor.sendToServer(new ModPayloads.ConfigureXianqiaoRedstone(
                    menu.containerId, menu.blockPos(),
                    appliedLow, appliedHigh, inverted));
            low.setValue(Long.toString(appliedLow));
            high.setValue(Long.toString(appliedHigh));
            return true;
        } catch (NumberFormatException ignored) {
            // Empty intermediate values are valid while editing.
            return false;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaGuiPainter.slot(graphics, leftPos + SLOT_X, topPos + FIRST_ROW_Y, true);
        graphics.fill(leftPos + SLOT_X, topPos + FIRST_ROW_Y,
                leftPos + SLOT_X + 16, topPos + FIRST_ROW_Y + 16, 0x1F63B7BE);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                VanillaGuiPainter.slot(graphics, leftPos + 8 + column * 18,
                        topPos + XianqiaoRedstoneInterfaceMenu.PLAYER_INVENTORY_Y + row * 18, true);
            }
        }
        for (int column = 0; column < 9; column++) {
            VanillaGuiPainter.slot(graphics, leftPos + 8 + column * 18,
                    topPos + XianqiaoRedstoneInterfaceMenu.HOTBAR_Y, true);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font,
                Component.translatable("container.immortalstorage.xianqiao_redstone_interface.above"),
                31, 14, TEXT, false);
        graphics.drawString(font,
                Component.translatable(inverted
                        ? "container.immortalstorage.xianqiao_redstone_interface.exit"
                        : "container.immortalstorage.xianqiao_redstone_interface.activate"),
                146, 14, inverted ? 0xFFAA2222 : 0xFF228822, false);
        graphics.drawString(font,
                Component.translatable("container.immortalstorage.xianqiao_redstone_interface.below"),
                31, 36, TEXT, false);
        graphics.drawString(font,
                Component.translatable(inverted
                        ? "container.immortalstorage.xianqiao_redstone_interface.activate"
                        : "container.immortalstorage.xianqiao_redstone_interface.exit"),
                146, 36, inverted ? 0xFF228822 : 0xFFAA2222, false);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawPolarityBorder(graphics);
        if (externalDialogOpen) renderExternalResourceDialog(graphics, mouseX, mouseY, partialTick);
        else renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawPolarityBorder(GuiGraphics graphics) {
        int color = inverted ? 0xFFD03030 : 0xFF30A050;
        int x = leftPos + SLOT_X;
        int y = topPos + SECOND_ROW_Y;
        graphics.fill(x, y, x + 18, y + 2, color);
        graphics.fill(x, y + 16, x + 18, y + 18, color);
        graphics.fill(x, y + 2, x + 2, y + 16, color);
        graphics.fill(x + 16, y + 2, x + 18, y + 16, color);
    }

    @Override
    protected void renderSlotContents(
            GuiGraphics graphics, ItemStack stack, Slot slot, String countString) {
        if (menu.slots.indexOf(slot) != 0 || !menu.isExternalTarget()) {
            super.renderSlotContents(graphics, stack, slot, countString);
            return;
        }
        ResourceChannelKey key = menu.getExternalTarget();
        if (key == null) return;
        ExternalResourceCatalog.Definition definition = ExternalResourceCatalog.definition(key);
        if (definition.solidColor()) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, definition.color());
        } else {
            graphics.blit(definition.icon(), slot.x, slot.y, 0.0F, 0.0F,
                    16, 16, 16, externalTextureHeight(key));
        }
    }

    private static int externalTextureHeight(ResourceChannelKey key) {
        return switch (key.channel()) {
            case "botania_mana" -> 512;
            case "ars_nouveau_source" -> 320;
            default -> 16;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (externalDialogOpen) {
            for (Button resourceButton : externalResourceButtons) {
                if (clickDialogWidget(resourceButton, mouseX, mouseY, button)) return true;
            }
            return true;
        }
        if (button == 1 && menu.getCarried().isEmpty()
                && menu.getConfiguredAmount() == 0L
                && isHovering(SLOT_X, FIRST_ROW_Y, 18, 18, mouseX, mouseY)
                && !menu.availableExternalResources().isEmpty()) {
            openExternalResourceDialog();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (externalDialogOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) closeExternalResourceDialog();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (getFocused() == high || getFocused() == low)) {
            if (apply()) setFocused(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return externalDialogOpen || super.charTyped(codePoint, modifiers);
    }

    @Override public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        return externalDialogOpen || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return externalDialogOpen || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!externalDialogOpen) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int maximum = Math.max(0, menu.availableExternalResources().size() - 5);
        int delta = scrollY < 0.0D ? 1 : scrollY > 0.0D ? -1 : 0;
        externalResourceOffset = Math.max(0, Math.min(maximum, externalResourceOffset + delta));
        refreshExternalResourceButtons();
        return true;
    }

    private void rebuildExternalResourceButtons() {
        externalResourceButtons.forEach(this::removeWidget);
        externalResourceButtons.clear();
        int x = dialogX() + 10;
        int y = dialogY() + 27;
        for (int index = 0; index < 5; index++) {
            final int buttonIndex = index;
            Button button = Button.builder(Component.empty(),
                            ignored -> configureExternalResource(buttonIndex))
                    .bounds(x, y + index * 20, MODAL_WIDTH - 20, 18)
                    .build();
            button.visible = false;
            button.active = false;
            addWidget(button);
            externalResourceButtons.add(button);
        }
    }

    private void openExternalResourceDialog() {
        externalResourceOffset = 0;
        externalDialogOpen = true;
        refreshExternalResourceButtons();
        setMainWidgetsVisible(false);
    }

    private void closeExternalResourceDialog() {
        externalDialogOpen = false;
        externalResourceButtons.forEach(button -> {
            button.visible = false;
            button.active = false;
        });
        setMainWidgetsVisible(true);
    }

    private void setMainWidgetsVisible(boolean visible) {
        high.visible = visible;
        high.active = visible;
        low.visible = visible;
        low.active = visible;
        polarityButton.visible = visible;
        polarityButton.active = visible;
    }

    private void refreshExternalResourceButtons() {
        List<ResourceChannelKey> resources = menu.availableExternalResources();
        int maximum = Math.max(0, resources.size() - externalResourceButtons.size());
        externalResourceOffset = Math.max(0, Math.min(maximum, externalResourceOffset));
        for (int index = 0; index < externalResourceButtons.size(); index++) {
            int resourceIndex = externalResourceOffset + index;
            Button button = externalResourceButtons.get(index);
            boolean present = resourceIndex < resources.size();
            button.setMessage(present
                    ? ExternalResourceCatalog.displayName(resources.get(resourceIndex))
                    : Component.empty());
            button.visible = externalDialogOpen && present;
            button.active = externalDialogOpen && present;
        }
    }

    private void configureExternalResource(int buttonIndex) {
        int resourceIndex = externalResourceOffset + buttonIndex;
        List<ResourceChannelKey> resources = menu.availableExternalResources();
        if (resourceIndex < 0 || resourceIndex >= resources.size()) return;
        ResourceChannelKey key = resources.get(resourceIndex);
        PacketDistributor.sendToServer(new ModPayloads.ConfigureXianqiaoRedstoneExternalTarget(
                menu.containerId, menu.blockPos(), menu.configRevision(),
                key.channel(), key.resourceId(), XianqiaoRedstoneInterfaceMenu.DEFAULT_EXTERNAL_CACHE_AMOUNT));
        closeExternalResourceDialog();
    }

    private void renderExternalResourceDialog(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, MODAL_Z);
        try {
            graphics.fill(0, 0, width, height, 0x78000000);
            int x = dialogX();
            int y = dialogY();
            VanillaGuiPainter.panel(graphics, x, y, MODAL_WIDTH, MODAL_HEIGHT);
            graphics.fill(x + 2, y + 2, x + MODAL_WIDTH - 2, y + 19, 0xFFD8D8D8);
            graphics.hLine(x + 3, x + MODAL_WIDTH - 4, y + 19, 0xFF8B8B8B);
            graphics.drawString(font,
                    Component.translatable("container.immortalstorage.xianqiao_interface.external_title", 1),
                    x + 8, y + 7, TEXT, false);
            externalResourceButtons.forEach(
                    button -> button.render(graphics, mouseX, mouseY, partialTick));
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    private boolean clickDialogWidget(
            GuiEventListener widget, double mouseX, double mouseY, int button) {
        if (!widget.isMouseOver(mouseX, mouseY)) return false;
        setFocused(widget);
        widget.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    private int dialogX() { return (width - MODAL_WIDTH) / 2; }
    private int dialogY() { return (height - MODAL_HEIGHT) / 2; }

    @Override public void onClose() {
        apply();
        super.onClose();
    }
}
