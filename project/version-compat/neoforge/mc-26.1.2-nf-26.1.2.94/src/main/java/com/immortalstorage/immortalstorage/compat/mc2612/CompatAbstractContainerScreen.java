package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Official 26.1.2 screen adapter for the existing ImmortalStorage screen
 * contract.  NeoForge/Minecraft now extracts GUI render state instead of
 * exposing the old immediate {@code renderBg}/{@code renderSlot} lifecycle;
 * this class keeps that lifecycle in one audited target adapter and feeds it
 * through the official extractor.
 */
public abstract class CompatAbstractContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    /** Legacy layout fields intentionally hide the target's fixed defaults. */
    protected int imageWidth = DEFAULT_IMAGE_WIDTH;
    protected int imageHeight = DEFAULT_IMAGE_HEIGHT;
    private boolean layoutDiagnosticsLogged;

    protected CompatAbstractContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected CompatAbstractContainerScreen(T menu, Inventory inventory, Component title,
                                            int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        // The target superclass positions itself using its final default
        // dimensions. Recompute the public layout position from the migrated
        // screen dimensions after its official initialization hook.
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX,
                                   int mouseY, float partialTick) {
        if (!this.layoutDiagnosticsLogged && this.minecraft != null) {
            var window = this.minecraft.getWindow();
            ImmortalStorageMod.LOG.info(
                    "[ui-layout] screen={} screenSize={}x{} extractor={}x{} window={}x{} guiScale={} "
                            + "leftTop={}x{} image={}x{} mouse={}x{}",
                    this.getClass().getSimpleName(), this.width, this.height,
                    graphics.guiWidth(), graphics.guiHeight(),
                    window.getWidth(), window.getHeight(), window.getGuiScale(),
                    this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                    mouseX, mouseY);
            this.layoutDiagnosticsLogged = true;
        }
        render(graphics, mouseX, mouseY, partialTick);
    }

    /** Compatibility lifecycle invoked by the target extractor. */
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderBg(graphics, partialTick, mouseX, mouseY);
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                renderSlot(graphics, slot);
            }
        }
        // Legacy container screens receive their labels in local GUI
        // coordinates.  The 26.1.2 extractor normally supplies the
        // left/top translation before invoking extractLabels; this adapter
        // invokes the legacy hook directly, so reproduce that translation
        // explicitly.  Without it, titles, inventory labels, and terminal
        // amount overlays are painted at the top-left of the window while
        // the panel and widgets remain centered.
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        renderLabels(graphics, mouseX, mouseY);
        graphics.pose().popMatrix();
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    protected void renderBackground(GuiGraphicsExtractor graphics, int mouseX,
                                    int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick,
                            int mouseX, int mouseY) {
    }

    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    protected void renderSlot(GuiGraphicsExtractor graphics, Slot slot) {
        renderSlotContents(graphics, slot.getItem(), slot, null);
    }

    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack,
                                      Slot slot, String countString) {
        if (stack.isEmpty()) {
            return;
        }
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        int seed = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) {
            graphics.fakeItem(stack, x, y, seed);
        } else {
            graphics.item(stack, x, y, seed);
        }
        graphics.itemDecorations(this.font, stack, x, y, countString);
    }

    protected void renderSlotHighlight(GuiGraphicsExtractor graphics, Slot slot,
                                       int mouseX, int mouseY, float partialTick) {
        if (slot.isHighlightable() && slot == this.hoveredSlot) {
            graphics.fill(this.leftPos + slot.x, this.topPos + slot.y,
                    this.leftPos + slot.x + 16, this.topPos + slot.y + 16,
                    0x80FFFFFF);
        }
    }

    protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot slot = findHoveredSlot(mouseX, mouseY);
        this.hoveredSlot = slot;
        if (slot != null && !slot.getItem().isEmpty() && this.menu.getCarried().isEmpty()) {
            graphics.setTooltipForNextFrame(this.font,
                    getTooltipFromContainerItem(slot.getItem()), Optional.empty(), mouseX, mouseY);
        }
    }

    private Slot findHoveredSlot(double mouseX, double mouseY) {
        if (this instanceof com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess terminal) {
            Slot slot = terminal.immortalstorage$getSlotAt(mouseX, mouseY);
            if (slot != null) {
                return slot;
            }
        }
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()
                    && mouseX >= this.leftPos + slot.x && mouseX < this.leftPos + slot.x + 16
                    && mouseY >= this.topPos + slot.y && mouseY < this.topPos + slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    protected boolean hasShiftDown() {
        return keyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || keyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    protected boolean hasControlDown() {
        return keyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || keyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    protected boolean hasAltDown() {
        return keyDown(GLFW.GLFW_KEY_LEFT_ALT) || keyDown(GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private boolean keyDown(int key) {
        return this.minecraft != null && InputConstants.isKeyDown(this.minecraft.getWindow(), key);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseEvent(mouseX, mouseY, button), false);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        return super.mouseDragged(mouseEvent(mouseX, mouseY, button), dragX, dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseEvent(mouseX, mouseY, button));
    }

    /**
     * Keep outside-click handling on the same migrated dimensions used by
     * rendering and widget placement.  The official target superclass stores
     * fixed final dimensions, while legacy screens resize the hidden adapter
     * fields for their actual layouts.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY,
                                        int left, int top) {
        return mouseX < this.leftPos
                || mouseX >= this.leftPos + this.imageWidth
                || mouseY < this.topPos
                || mouseY >= this.topPos + this.imageHeight;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }

    private static MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
    }
}
