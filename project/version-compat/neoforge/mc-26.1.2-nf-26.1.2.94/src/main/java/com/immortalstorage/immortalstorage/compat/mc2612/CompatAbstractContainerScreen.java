package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    private static final Identifier SLOT_HIGHLIGHT_BACK =
            Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT =
            Identifier.withDefaultNamespace("container/slot_highlight_front");
    /** Legacy layout fields intentionally hide the target's fixed defaults. */
    protected int imageWidth = DEFAULT_IMAGE_WIDTH;
    protected int imageHeight = DEFAULT_IMAGE_HEIGHT;
    private MouseButtonEvent forwardedMouseEvent;
    private boolean forwardedDoubleClick;

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
        render(graphics, mouseX, mouseY, partialTick);
    }

    /** Compatibility lifecycle invoked by the target extractor. */
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);
        extractCompatContents(graphics, mouseX, mouseY, partialTick);
        super.extractCarriedItem(graphics, mouseX, mouseY);
        super.extractSnapbackItem(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Mirrors the official 26.1 container extraction order while resolving
     * highlights through the terminal's visual slot coordinates. Vanilla's
     * private helper instead uses buffered menu coordinates and leaves the
     * detached white cursor seen below a scrolled terminal.
     */
    private void extractCompatContents(GuiGraphicsExtractor graphics, int mouseX,
                                       int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        this.hoveredSlot = findHoveredSlot(mouseX, mouseY);
        extractCompatSlotHighlight(graphics, SLOT_HIGHLIGHT_BACK);
        extractSlots(graphics, mouseX, mouseY);
        extractCompatSlotHighlight(graphics, SLOT_HIGHLIGHT_FRONT);
        // Legacy screens submit long-count and fluid overlays from their label
        // pass.  The target extractor must therefore queue labels after item
        // models or high-rate terminal counts are hidden beneath the slot item.
        extractLabels(graphics, mouseX, mouseY);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Foreground(
                        this, graphics, mouseX, mouseY));
        graphics.pose().popMatrix();
    }

    private void extractCompatSlotHighlight(GuiGraphicsExtractor graphics, Identifier sprite) {
        if (this.hoveredSlot == null || !this.hoveredSlot.isHighlightable()) return;
        Rect2i bounds = visualSlotBounds(this.hoveredSlot);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
                bounds.getX() - this.leftPos - 4,
                bounds.getY() - this.topPos - 4, 24, 24);
    }

    private Rect2i visualSlotBounds(Slot slot) {
        if (this instanceof com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess terminal) {
            return terminal.immortalstorage$getSlotBounds(slot);
        }
        return new Rect2i(this.leftPos + slot.x, this.topPos + slot.y, 16, 16);
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

    @Override
    protected final void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected final void extractSlot(GuiGraphicsExtractor graphics, Slot slot,
                                     int mouseX, int mouseY) {
        renderSlot(graphics, slot);
    }

    protected void renderSlot(GuiGraphicsExtractor graphics, Slot slot) {
        renderSlotContents(graphics, slot.getItem(), slot, null);
    }

    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack,
                                      Slot slot, String countString) {
        if (stack.isEmpty()) {
            return;
        }
        int x = slot.x;
        int y = slot.y;
        int seed = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) {
            graphics.fakeItem(stack, x, y, seed);
        } else {
            graphics.item(stack, x, y, seed);
        }
        graphics.itemDecorations(this.font, stack, x, y, countString);
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.forwardedMouseEvent = event;
        this.forwardedDoubleClick = doubleClick;
        try {
            return mouseClicked(event.x(), event.y(), event.button());
        } finally {
            this.forwardedMouseEvent = null;
            this.forwardedDoubleClick = false;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = forwardedMouseEvent != null
                ? forwardedMouseEvent : mouseEvent(mouseX, mouseY, button);
        return super.mouseClicked(event, forwardedDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        this.forwardedMouseEvent = event;
        try {
            return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
        } finally {
            this.forwardedMouseEvent = null;
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double dragX, double dragY) {
        MouseButtonEvent event = forwardedMouseEvent != null
                ? forwardedMouseEvent : mouseEvent(mouseX, mouseY, button);
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.forwardedMouseEvent = event;
        try {
            return mouseReleased(event.x(), event.y(), event.button());
        } finally {
            this.forwardedMouseEvent = null;
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = forwardedMouseEvent != null
                ? forwardedMouseEvent : mouseEvent(mouseX, mouseY, button);
        return super.mouseReleased(event);
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

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }

    private static MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
    }
}
