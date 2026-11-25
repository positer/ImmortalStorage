package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoRedstoneInterfaceMenu;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Two cache thresholds and a polarity switch, sharing geometry with the menu. */
public final class XianqiaoRedstoneInterfaceScreen
        extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<XianqiaoRedstoneInterfaceMenu> {
    private static final int TEXT = 0xFF404040;
    private static final int SLOT_X = 8;
    private static final int FIRST_ROW_Y = XianqiaoRedstoneInterfaceMenu.CONFIG_Y;
    private static final int SECOND_ROW_Y = 59;
    private static final int FIELD_X = 31;
    private static final int FIELD_WIDTH = 137;
    private static final int FIELD_HEIGHT = 15;
    private static final int INPUT_PADDING = 4;
    private static final String TEXT_KEY = "container.immortalstorage.xianqiao_redstone_interface.";
    private static final int MODAL_WIDTH = 176;
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
        imageHeight = XianqiaoRedstoneInterfaceMenu.HOTBAR_Y + 24;
        inventoryLabelX = 8;
        inventoryLabelY = XianqiaoRedstoneInterfaceMenu.PLAYER_INVENTORY_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        String editedHigh = configurationLoaded && high != null ? high.getValue() : null;
        String editedLow = configurationLoaded && low != null ? low.getValue() : null;
        high = numberBox(FIRST_ROW_Y, Component.translatable(TEXT_KEY + "above"));
        low = numberBox(SECOND_ROW_Y, Component.translatable(TEXT_KEY + "below"));
        high.active = false;
        low.active = false;
        polarityButton = Button.builder(Component.translatable(TEXT_KEY + "polarity"), ignored -> {
                    inverted = !inverted;
                    apply();
                })
                .bounds(leftPos + SLOT_X, topPos + SECOND_ROW_Y, 18, 18)
                .build();
        polarityButton.active = false;
        addWidget(high);
        addWidget(low);
        addWidget(polarityButton); // Keep the real click/focus target; the native button texture is not drawn.
        rebuildExternalResourceButtons();
        if (editedHigh != null && editedLow != null) {
            high.setValue(editedHigh);
            low.setValue(editedLow);
        } else {
            configurationLoaded = false;
            loadSynchronizedConfiguration();
        }
        setMainWidgetsVisible(!externalDialogOpen);
        if (externalDialogOpen) refreshExternalResourceButtons();
    }

    private EditBox numberBox(int top, Component label) {
        EditBox box = new EditBox(font, leftPos + FIELD_X + INPUT_PADDING, topPos + top + 3,
                FIELD_WIDTH - 2 * INPUT_PADDING, FIELD_HEIGHT - 3, label);
        box.setBordered(false);
        box.setTextColor(TEXT);
        box.setTextColorUneditable(0xFF626262);
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
        configurationLoaded = true;
        setMainWidgetsVisible(!externalDialogOpen);
    }

    private boolean apply() {
        if (!configurationLoaded || low == null || high == null) return false;
        try {
            long requestedLow = Long.parseLong(low.getValue());
            long requestedHigh = Long.parseLong(high.getValue());
            long appliedLow = Math.max(0L, requestedLow);
            long appliedHigh = Math.max(appliedLow, requestedHigh);
            ClientPacketDistributor.sendToServer(new ModPayloads.ConfigureXianqiaoRedstone(
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
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        ConfigurationExternalWidgets.place(children(), leftPos, topPos, imageWidth);
        VanillaGuiPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        ConfigurationStyle.header(graphics, font, leftPos, topPos, imageWidth, title);
        VanillaGuiPainter.slot(graphics, leftPos + SLOT_X, topPos + FIRST_ROW_Y, true);
        graphics.fill(leftPos + SLOT_X, topPos + FIRST_ROW_Y,
                leftPos + SLOT_X + 16, topPos + FIRST_ROW_Y + 16, 0x1F63B7BE);
        if (high.visible) {
            drawInputWell(graphics, high, FIRST_ROW_Y);
            drawInputWell(graphics, low, SECOND_ROW_Y);
            drawPolarityControl(graphics, mouseX, mouseY);
        }
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
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawThresholdLabels(graphics, FIRST_ROW_Y, "above", !inverted);
        drawThresholdLabels(graphics, SECOND_ROW_Y, "below", inverted);
        graphics.text(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    private void drawThresholdLabels(GuiGraphicsExtractor graphics, int fieldY, String label, boolean activates) {
        Component action = Component.translatable(TEXT_KEY + (activates ? "activate" : "exit"));
        int actionWidth = font.width(action);
        String caption = Component.translatable(TEXT_KEY + label).getString();
        graphics.text(font, font.plainSubstrByWidth(caption, Math.max(0, FIELD_WIDTH - actionWidth - 6)),
                FIELD_X, fieldY - 10, TEXT, false);
        graphics.text(font, action, FIELD_X + FIELD_WIDTH - actionWidth, fieldY - 10,
                activates ? 0xFF1E6C38 : 0xFF9A2828, false);
    }

    private void drawInputWell(GuiGraphicsExtractor graphics, EditBox field, int top) {
        int x = leftPos + FIELD_X, y = topPos + top;
        int outline = field.isFocused() ? 0xFF2F7178 : 0xFF8B8B8B;
        graphics.fill(x, y, x + FIELD_WIDTH, y + FIELD_HEIGHT, outline);
        graphics.fill(x + 1, y + 1, x + FIELD_WIDTH - 1, y + FIELD_HEIGHT - 1,
                field.active ? 0xFFE5E5E5 : 0xFFD2D2D2);
        if (!field.isFocused()) {
            // The native edit box paints a dark drop shadow. Retain its cursor/selection while editing,
            // but render power thresholds sharply on the idle pale wells.
            graphics.text(font, field.getValue(), field.getX(), field.getY(), TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (externalDialogOpen) renderExternalResourceDialog(graphics, mouseX, mouseY, partialTick);
        else {
            if (high.visible && high.isFocused()) high.extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (low.visible && low.isFocused()) low.extractRenderState(graphics, mouseX, mouseY, partialTick);
            renderTooltip(graphics, mouseX, mouseY);
            if (polarityButton.isMouseOver(mouseX, mouseY))
                com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, font, polarityButton.getMessage(), mouseX, mouseY);
            if (mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + 17
                    && font.width(title) > imageWidth - 12)
                com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, font, title, mouseX, mouseY);
        }
    }

    private void drawPolarityControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int color = !polarityButton.active ? 0xFF777777 : inverted ? 0xFF9A2828 : 0xFF1E6C38;
        int x = leftPos + SLOT_X;
        int y = topPos + SECOND_ROW_Y;
        boolean highlighted = polarityButton.isFocused() || polarityButton.isMouseOver(mouseX, mouseY);
        graphics.fill(x, y, x + 18, y + 18, color);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, highlighted ? 0xFFE5E5E5 : 0xFFD2D2D2);
        String glyph = inverted ? "−" : "+";
        graphics.text(font, glyph, x + (18 - font.width(glyph)) / 2, y + 5, color, false);
    }

    @Override
    protected void renderSlotContents(
            GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, String countString) {
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
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, definition.icon(), slot.x, slot.y, 0.0F, 0.0F,
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
        int y = externalDialogY() + ConfigurationStyle.CHOOSER_TOP;
        for (int index = 0; index < 5; index++) {
            final int buttonIndex = index;
            Button button = Button.builder(Component.empty(),
                            ignored -> configureExternalResource(buttonIndex))
                    .bounds(x, y + index * ConfigurationStyle.CHOOSER_ROW, MODAL_WIDTH - 20, 18)
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
        high.active = visible && configurationLoaded;
        low.visible = visible;
        low.active = visible && configurationLoaded;
        polarityButton.visible = visible;
        polarityButton.active = visible && configurationLoaded;
    }

    private void refreshExternalResourceButtons() {
        List<ResourceChannelKey> resources = menu.availableExternalResources();
        int maximum = Math.max(0, resources.size() - externalResourceButtons.size());
        externalResourceOffset = Math.max(0, Math.min(maximum, externalResourceOffset));
        for (int index = 0; index < externalResourceButtons.size(); index++) {
            int resourceIndex = externalResourceOffset + index;
            Button button = externalResourceButtons.get(index);
            button.setX(dialogX() + 10);
            button.setY(externalDialogY() + ConfigurationStyle.CHOOSER_TOP + index * ConfigurationStyle.CHOOSER_ROW);
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
        ClientPacketDistributor.sendToServer(new ModPayloads.ConfigureXianqiaoRedstoneExternalTarget(
                menu.containerId, menu.blockPos(), menu.configRevision(),
                key.channel(), key.resourceId(), XianqiaoRedstoneInterfaceMenu.DEFAULT_EXTERNAL_CACHE_AMOUNT));
        closeExternalResourceDialog();
    }

    private void renderExternalResourceDialog(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        try {
            graphics.fill(0, 0, width, height, 0x78000000);
            int x = dialogX();
            int y = externalDialogY();
            VanillaGuiPainter.panel(graphics, x, y, MODAL_WIDTH, externalDialogHeight());
            graphics.fill(x + 2, y + 2, x + MODAL_WIDTH - 2, y + 19, 0xFFD8D8D8);
            graphics.horizontalLine(x + 3, x + MODAL_WIDTH - 4, y + 19, 0xFF8B8B8B);
            graphics.text(font,
                    Component.translatable("container.immortalstorage.xianqiao_interface.external_title", 1),
                    x + 8, y + 7, TEXT, false);
            externalResourceButtons.forEach(
                    button -> button.extractRenderState(graphics, mouseX, mouseY, partialTick));
            
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private boolean clickDialogWidget(
            GuiEventListener widget, double mouseX, double mouseY, int button) {
        if (!widget.isMouseOver(mouseX, mouseY)) return false;
        setFocused(widget);
        widget.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
        return true;
    }

    private int externalDialogHeight() {
        return ConfigurationStyle.chooserHeight(menu.availableExternalResources().size());
    }

    private int externalDialogY() {
        return height / 2 - externalDialogHeight() / 2;
    }

    private int dialogX() { return (width - MODAL_WIDTH) / 2; }

    @Override public void onClose() {
        apply();
        super.onClose();
    }
}
