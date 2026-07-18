package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.block.entity.XianqiaoInterfaceBlockEntity;
import com.cultivation.cultivation.menu.custom.XianqiaoInterfaceMenu;
import com.cultivation.cultivation.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dependency-free vanilla fallback for the Xianqiao Interface.
 *
 * <p>This screen deliberately references only Minecraft/Cultivation classes,
 * so a standalone installation remains safe. Optional integrations may add
 * behavior around the same server-authoritative menu without being required
 * to render or operate these controls. The complete visible page is rendered
 * every frame; there is no frame counter, time gate, animation throttling, or
 * off-screen list to keep alive.</p>
 */
public final class XianqiaoInterfaceScreen
        extends AbstractContainerScreen<XianqiaoInterfaceMenu>
        implements TerminalFluidScreenAccess {
    private static final int TEXT = 0xFF404040;
    private static final int GHOST_TINT = 0x1F63B7BE;
    private static final int SCREEN_WIDTH = 176;
    private static final int SCREEN_HEIGHT = 243;
    private static final int SIDE_GRID_X = 30;
    private static final int SIDE_GRID_Y = 97;
    private static final int SIDE_BUTTON_WIDTH = 36;
    private static final int SIDE_COLUMN_STRIDE = 39;
    private static final int SIDE_ROW_STRIDE = 21;
    private static final float MODAL_Z = 500.0F;
    private static final int DIALOG_WIDTH = 176;
    private static final int DIALOG_HEIGHT = 132;
    private static final int MASK_BUTTON_SIZE = 18;
    private static final List<Direction> SIDE_ORDER = List.of(
            Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    private final Map<Direction, Button> sideButtons = new EnumMap<>(Direction.class);
    private EditBox amountInput;
    private Button applyAmountButton;
    private Button cancelAmountButton;
    private Button activePullButton;
    private Button activePushButton;
    private final Map<Direction, Button> slotFaceButtons = new EnumMap<>(Direction.class);
    private int selectedTarget;
    private long lastConfigurationRevision = Long.MIN_VALUE;
    private boolean amountDialogOpen;

    public XianqiaoInterfaceScreen(
            XianqiaoInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
        this.inventoryLabelY = 139;
    }

    @Override
    protected void init() {
        super.init();
        sideButtons.clear();
        for (int index = 0; index < SIDE_ORDER.size(); index++) {
            Direction side = SIDE_ORDER.get(index);
            int buttonX = this.leftPos + SIDE_GRID_X
                    + (index % 3) * SIDE_COLUMN_STRIDE;
            int buttonY = this.topPos + SIDE_GRID_Y
                    + (index / 3) * SIDE_ROW_STRIDE;
            Button button = this.addRenderableWidget(Button.builder(
                            sideLabel(side), ignored -> cycleSideMode(side))
                    .bounds(buttonX, buttonY, SIDE_BUTTON_WIDTH, 20)
                    .tooltip(Tooltip.create(sideTooltip(side)))
                    .build());
            sideButtons.put(side, button);
        }
        int dialogX = dialogX();
        int dialogY = dialogY();
        this.amountInput = new EditBox(this.font, dialogX + 10,
                dialogY + 31, DIALOG_WIDTH - 20, 18,
                Component.translatable("container.cultivation.xianqiao_interface.amount_input"));
        this.amountInput.setMaxLength(20);
        this.amountInput.setFilter(value -> value.isEmpty()
                || value.chars().allMatch(Character::isDigit));
        this.amountInput.setResponder(ignored -> validateAmount());
        this.applyAmountButton = Button.builder(
                        Component.translatable("container.cultivation.xianqiao_interface.apply"),
                        ignored -> applyAmount())
                .bounds(dialogX + 10, dialogY + 106, 74, 20)
                .build();
        this.cancelAmountButton = Button.builder(
                        Component.translatable("container.cultivation.xianqiao_interface.cancel"),
                        ignored -> closeAmountDialog())
                .bounds(dialogX + 92, dialogY + 106, 74, 20)
                .build();
        this.activePullButton = Button.builder(Component.empty(), ignored -> toggleActiveTransfer(true))
                .bounds(this.leftPos + 4, this.topPos + SIDE_GRID_Y, 24, 20).build();
        this.activePushButton = Button.builder(Component.empty(), ignored -> toggleActiveTransfer(false))
                .bounds(this.leftPos + 4, this.topPos + SIDE_GRID_Y + SIDE_ROW_STRIDE, 24, 20).build();
        this.addRenderableWidget(activePullButton);
        this.addRenderableWidget(activePushButton);
        slotFaceButtons.clear();
        for (int index = 0; index < SIDE_ORDER.size(); index++) {
            Direction side = SIDE_ORDER.get(index);
            Button maskButton = Button.builder(Component.empty(), ignored -> toggleSlotFace(side))
                    .bounds(dialogX + 42 + (index % 3) * 32,
                            dialogY + 62 + (index / 3) * 20, MASK_BUTTON_SIZE, MASK_BUTTON_SIZE)
                    .build();
            this.addWidget(maskButton);
            slotFaceButtons.put(side, maskButton);
        }
        this.addWidget(amountInput);
        this.addWidget(applyAmountButton);
        this.addWidget(cancelAmountButton);
        setDialogWidgetsVisible(false);
        updateControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
        if (amountDialogOpen && menu.getConfiguredAmount(selectedTarget) <= 0L) {
            closeAmountDialog();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        VanillaGuiPainter.panel(graphics, x, y, this.imageWidth, this.imageHeight);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 17, 0xFFD8D8D8);
        graphics.hLine(x + 3, x + this.imageWidth - 4, y + 17, 0xFF8B8B8B);

        for (int column = 0; column < 9; column++) {
            int slotX = x + 8 + column * 18;
            VanillaGuiPainter.slot(graphics, slotX, y + XianqiaoInterfaceMenu.CONFIG_Y, true);
            graphics.fill(slotX, y + XianqiaoInterfaceMenu.CONFIG_Y,
                    slotX + 16, y + XianqiaoInterfaceMenu.CONFIG_Y + 16, GHOST_TINT);
            VanillaGuiPainter.slot(graphics, slotX, y + XianqiaoInterfaceMenu.BUFFER_Y, true);
        }
        int selectedX = x + 8 + selectedTarget * 18;
        graphics.renderOutline(selectedX - 1, y + XianqiaoInterfaceMenu.CONFIG_Y - 1,
                18, 18, 0xFF63B7BE);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                VanillaGuiPainter.slot(graphics, x + 8 + column * 18,
                        y + XianqiaoInterfaceMenu.PLAYER_INVENTORY_Y + row * 18, true);
            }
        }
        for (int column = 0; column < 9; column++) {
            VanillaGuiPainter.slot(graphics, x + 8 + column * 18,
                    y + XianqiaoInterfaceMenu.HOTBAR_Y, true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (amountDialogOpen) {
            renderAmountDialog(graphics, mouseX, mouseY, partialTick);
        } else {
            Optional<TerminalFluidScreenAccess.FluidHover> fluidHover =
                    cultivation$getFluidAt(mouseX, mouseY);
            if (menu.getCarried().isEmpty() && fluidHover.isPresent()) {
                TerminalFluidScreenAccess.FluidHover hover = fluidHover.get();
                graphics.renderTooltip(this.font,
                        List.of(
                                hover.stack().getHoverName(),
                                Component.literal(TerminalFluidAmountFormatter.exactBuckets(
                                        hover.amountMb())),
                                Component.literal(TerminalFluidAmountFormatter.exactMillibuckets(
                                        hover.amountMb()))),
                        Optional.empty(), mouseX, mouseY);
            } else {
                renderTooltip(graphics, mouseX, mouseY);
            }
        }
    }

    /**
     * Renders mixed resource identities and synchronized totals in vanilla's
     * slot-content pass. Hover highlights and the carried stack therefore run
     * afterwards and remain visually on top.
     */
    @Override
    protected void renderSlotContents(
            GuiGraphics graphics, ItemStack stack, Slot slot, @Nullable String countString) {
        int slotIndex = this.menu.slots.indexOf(slot);
        if (slotIndex < 0 || slotIndex >= XianqiaoInterfaceMenu.PLAYER_START) {
            super.renderSlotContents(graphics, stack, slot, countString);
            return;
        }

        int resourceSlot = slotIndex % XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT;
        long amount = slotIndex < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT
                ? menu.getConfiguredAmount(resourceSlot)
                : menu.getCachedAmount(resourceSlot);
        if (amount <= 0L) return;

        if (menu.isFluidTarget(resourceSlot)) {
            FluidStack fluid = configuredFluidIdentity(resourceSlot);
            if (!fluid.isEmpty()) {
                renderFluidSprite(graphics, fluid, slot.x, slot.y);
                renderAmountOverlay(graphics, formatBuckets(amount) + "B", slot.x, slot.y);
            }
            return;
        }

        ItemStack identity = menu.getConfiguredTarget(resourceSlot);
        if (identity.isEmpty()) return;
        identity = identity.copyWithCount(1);
        int seed = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) graphics.renderFakeItem(identity, slot.x, slot.y, seed);
        else graphics.renderItem(identity, slot.x, slot.y, seed);
        graphics.renderItemDecorations(this.font, identity, slot.x, slot.y, "");
        renderAmountOverlay(graphics, Long.toString(amount), slot.x, slot.y);
    }

    /**
     * Fluid cache slots intentionally have no fake ItemStack mirror. Vanilla
     * therefore skips renderSlotContents for them; paint that one empty-slot
     * case here, still inside the ordinary slot pass and before hover/floating
     * item rendering.
     */
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        int slotIndex = this.menu.slots.indexOf(slot);
        if (slotIndex < XianqiaoInterfaceMenu.BUFFER_START
                || slotIndex >= XianqiaoInterfaceMenu.PLAYER_START
                || !slot.getItem().isEmpty()) return;
        int resourceSlot = slotIndex - XianqiaoInterfaceMenu.BUFFER_START;
        if (!menu.isFluidTarget(resourceSlot)) return;
        long amount = menu.getCachedAmount(resourceSlot);
        FluidStack fluid = configuredFluidIdentity(resourceSlot);
        if (amount <= 0L || fluid.isEmpty()) return;
        renderFluidSprite(graphics, fluid, slot.x, slot.y);
        renderAmountOverlay(graphics, formatBuckets(amount) + "B", slot.x, slot.y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, TEXT, false);
        graphics.drawString(this.font,
                Component.translatable("container.cultivation.xianqiao_interface.targets"),
                8, 19, TEXT, false);
        graphics.drawString(this.font,
                Component.translatable("container.cultivation.xianqiao_interface.buffers"),
                8, 55, TEXT, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
        graphics.drawString(this.font,
                Component.translatable("container.cultivation.xianqiao_interface.sides"),
                8, 86, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (amountDialogOpen) {
            if (clickDialogWidget(amountInput, mouseX, mouseY, button)) return true;
            if (clickDialogWidget(applyAmountButton, mouseX, mouseY, button)) return true;
            if (clickDialogWidget(cancelAmountButton, mouseX, mouseY, button)) return true;
            for (Map.Entry<Direction, Button> entry : slotFaceButtons.entrySet()) {
                if (clickDialogWidget(entry.getValue(), mouseX, mouseY, button)) {
                    toggleSlotFace(entry.getKey());
                    return true;
                }
            }
            return true;
        }
        int relativeX = (int) mouseX - this.leftPos - 8;
        int relativeY = (int) mouseY - this.topPos - XianqiaoInterfaceMenu.CONFIG_Y;
        if (relativeY >= 0 && relativeY < 18 && relativeX >= 0 && relativeX < 9 * 18) {
            int slot = relativeX / 18;
            if (relativeX % 18 < 16) {
                selectedTarget = slot;
                if (menu.getCarried().isEmpty() && menu.getConfiguredAmount(slot) > 0L) {
                    openAmountDialog(slot);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean isHovering(
            int x, int y, int width, int height, double mouseX, double mouseY) {
        return !amountDialogOpen && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (amountDialogOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeAmountDialog();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                cycleDialogFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (this.getFocused() == cancelAmountButton) closeAmountDialog();
                else applyAmount();
                return true;
            }
            GuiEventListener focused = this.getFocused();
            if (focused != null) focused.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (amountDialogOpen) {
            if (this.getFocused() == amountInput) amountInput.charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (amountDialogOpen) {
            GuiEventListener focused = this.getFocused();
            if (focused != null) {
                focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (amountDialogOpen) {
            GuiEventListener focused = this.getFocused();
            if (focused != null) focused.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double scrollX, double scrollY) {
        if (amountDialogOpen) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void cycleSideMode(Direction side) {
        if (menu.getBlockEntity() == null) return;
        XianqiaoInterfaceBlockEntity.SideMode[] modes =
                XianqiaoInterfaceBlockEntity.SideMode.values();
        int next = (menu.getSideMode(side).ordinal() + 1) % modes.length;
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceSideMode(
                menu.containerId, menu.getBlockEntity().getBlockPos(), menu.getConfigRevision(),
                side.ordinal(), next));
    }

    private void applyAmount() {
        if (amountInput == null || menu.getBlockEntity() == null || !validAmount()) return;
        long amount;
        if (menu.isFluidTarget(selectedTarget)) {
            amount = fluidInputToMillibuckets(amountInput.getValue());
            amount = Math.min(menu.getFluidTargetLimitMb(), amount);
        } else {
            try {
                amount = Long.parseLong(amountInput.getValue());
            } catch (NumberFormatException ignored) {
                amount = Long.MAX_VALUE;
            }
            amount = Math.min(menu.getItemTargetLimit(), amount);
        }
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceTargetAmount(
                menu.containerId, menu.getBlockEntity().getBlockPos(), lastConfigurationRevision,
                selectedTarget, amount));
        closeAmountDialog();
    }

    private void validateAmount() {
        if (amountInput == null || applyAmountButton == null) return;
        boolean valid = validAmount();
        amountInput.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        applyAmountButton.active = amountDialogOpen && valid && menu.getBlockEntity() != null;
    }

    private boolean validAmount() {
        if (amountInput == null || amountInput.getValue().isEmpty()) return false;
        if (menu.isFluidTarget(selectedTarget)) {
            try {
                java.math.BigDecimal buckets = new java.math.BigDecimal(amountInput.getValue());
                return buckets.signum() >= 0 && buckets.scale() <= 3
                        && buckets.movePointRight(3).stripTrailingZeros().scale() <= 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        try {
            return Long.parseLong(amountInput.getValue()) >= 0L;
        } catch (NumberFormatException ignored) {
            return amountInput.getValue().chars().allMatch(Character::isDigit);
        }
    }

    private void updateControls() {
        for (Map.Entry<Direction, Button> entry : sideButtons.entrySet()) {
            Button sideButton = entry.getValue();
            sideButton.setMessage(sideLabel(entry.getKey()));
            sideButton.setTooltip(Tooltip.create(sideTooltip(entry.getKey())));
            sideButton.visible = !amountDialogOpen;
            sideButton.active = menu.getBlockEntity() != null && !amountDialogOpen;
        }
        activePullButton.setMessage(Component.literal(menu.isActivePullEnabled() ? "抽开" : "抽关"));
        activePushButton.setMessage(Component.literal(menu.isActivePushEnabled() ? "推开" : "推关"));
        activePullButton.visible = !amountDialogOpen;
        activePushButton.visible = !amountDialogOpen;
        for (Map.Entry<Direction, Button> entry : slotFaceButtons.entrySet()) {
            boolean enabled = menu.isSlotFaceEnabled(selectedTarget, entry.getKey());
            entry.getValue().setMessage(Component.literal(shortSide(entry.getKey()) + (enabled ? "✓" : "×")));
        }
        validateAmount();
    }

    private void toggleActiveTransfer(boolean pull) {
        if (menu.getBlockEntity() == null) return;
        boolean enabled = pull ? !menu.isActivePullEnabled() : !menu.isActivePushEnabled();
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceActiveTransfer(
                menu.containerId, menu.getBlockEntity().getBlockPos(), menu.getConfigRevision(), pull, enabled));
    }

    private void toggleSlotFace(Direction side) {
        if (menu.getBlockEntity() == null || !amountDialogOpen) return;
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceSlotFaceMask(
                menu.containerId, menu.getBlockEntity().getBlockPos(), lastConfigurationRevision,
                selectedTarget, side.ordinal(), !menu.isSlotFaceEnabled(selectedTarget, side)));
    }

    private static String shortSide(Direction side) {
        return switch (side) {
            case UP -> "上";
            case DOWN -> "下";
            case NORTH -> "北";
            case SOUTH -> "南";
            case WEST -> "西";
            case EAST -> "东";
        };
    }

    private void openAmountDialog(int slot) {
        if (menu.getConfiguredAmount(slot) <= 0L) return;
        selectedTarget = slot;
        lastConfigurationRevision = menu.getConfigRevision();
        boolean fluid = menu.isFluidTarget(slot);
        amountInput.setMaxLength(fluid ? fluidInputMaxLength() : 20);
        amountInput.setFilter(fluid
                ? XianqiaoInterfaceScreen::validBucketInputShape
                : value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        amountInput.setValue(fluid
                ? formatBuckets(menu.getConfiguredAmount(slot))
                : Long.toString(menu.getConfiguredAmount(slot)));
        amountDialogOpen = true;
        setDialogWidgetsVisible(true);
        this.setFocused(amountInput);
        amountInput.moveCursorToEnd(false);
        validateAmount();
        updateControls();
    }

    private void closeAmountDialog() {
        amountDialogOpen = false;
        if (isDialogWidget(this.getFocused())) this.setFocused(null);
        setDialogWidgetsVisible(false);
        updateControls();
    }

    private void setDialogWidgetsVisible(boolean visible) {
        if (amountInput != null) {
            amountInput.visible = visible;
            amountInput.active = visible;
        }
        if (applyAmountButton != null) {
            applyAmountButton.visible = visible;
            applyAmountButton.active = visible;
        }
        if (cancelAmountButton != null) {
            cancelAmountButton.visible = visible;
            cancelAmountButton.active = visible;
        }
        for (Button button : slotFaceButtons.values()) {
            button.visible = visible;
            button.active = visible;
        }
    }

    private void renderAmountDialog(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, MODAL_Z);
        try {
            graphics.fill(0, 0, this.width, this.height, 0x78000000);
            int x = dialogX();
            int y = dialogY();
            VanillaGuiPainter.panel(graphics, x, y, DIALOG_WIDTH, DIALOG_HEIGHT);
            graphics.fill(x + 2, y + 2, x + DIALOG_WIDTH - 2, y + 19, 0xFFD8D8D8);
            graphics.hLine(x + 3, x + DIALOG_WIDTH - 4, y + 19, 0xFF8B8B8B);
            graphics.drawString(font,
                    Component.translatable("container.cultivation.xianqiao_interface.amount_title",
                            selectedTarget + 1), x + 8, y + 7, TEXT, false);
            graphics.drawString(font, amountSummary(), x + 10, y + 22, TEXT, false);
            amountInput.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font,
                    Component.translatable("container.cultivation.xianqiao_interface.slot_faces"),
                    x + 8, y + 52, TEXT, false);
            for (Button button : slotFaceButtons.values()) button.render(graphics, mouseX, mouseY, partialTick);
            applyAmountButton.render(graphics, mouseX, mouseY, partialTick);
            cancelAmountButton.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    /** True while the amount editor exclusively owns rendering and input. */
    public boolean isAmountDialogOpen() {
        return amountDialogOpen;
    }

    private boolean clickDialogWidget(
            GuiEventListener widget, double mouseX, double mouseY, int button) {
        if (!widget.isMouseOver(mouseX, mouseY)) return false;
        this.setFocused(widget);
        widget.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    private void cycleDialogFocus(boolean backwards) {
        GuiEventListener[] widgets = {amountInput, applyAmountButton, cancelAmountButton};
        int current = -1;
        for (int index = 0; index < widgets.length; index++) {
            if (widgets[index] == this.getFocused()) {
                current = index;
                break;
            }
        }
        int step = backwards ? -1 : 1;
        int next = Math.floorMod(current + step, widgets.length);
        this.setFocused(widgets[next]);
    }

    private boolean isDialogWidget(@Nullable GuiEventListener listener) {
        return listener == amountInput
                || listener == applyAmountButton
                || listener == cancelAmountButton;
    }

    private Component amountSummary() {
        long amount = parsedAmountForSummary();
        if (!menu.isFluidTarget(selectedTarget)) {
            return Component.translatable("container.cultivation.xianqiao_interface.item_amount_hint",
                    amount, menu.getItemTargetLimit());
        }
        long millibuckets = fluidInputToMillibuckets(amountInput.getValue());
        String buckets = validAmount() ? amountInput.getValue() : "?";
        return Component.translatable("container.cultivation.xianqiao_interface.fluid_amount_hint",
                buckets, millibuckets, menu.getFluidTargetLimitMb());
    }

    private long parsedAmountForSummary() {
        if (amountInput == null || amountInput.getValue().isEmpty()) return 0L;
        try {
            return Long.parseLong(amountInput.getValue());
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean validBucketInputShape(String value) {
        if (value.isEmpty()) return true;
        int dot = value.indexOf('.');
        if (dot != value.lastIndexOf('.')) return false;
        String whole = dot < 0 ? value : value.substring(0, dot);
        String fractional = dot < 0 ? "" : value.substring(dot + 1);
        return !whole.isEmpty() && whole.chars().allMatch(Character::isDigit)
                && fractional.length() <= 3 && fractional.chars().allMatch(Character::isDigit);
    }

    private static long fluidInputToMillibuckets(String value) {
        try {
            return new java.math.BigDecimal(value).movePointRight(3).longValueExact();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static String formatBuckets(long amountMb) {
        return java.math.BigDecimal.valueOf(amountMb).movePointLeft(3)
                .stripTrailingZeros().toPlainString();
    }

    /**
     * Leaves room for the configured whole-bucket limit, a decimal point, and
     * the three mB digits accepted by the amount editor. The server still
     * performs the authoritative clamp when the request arrives.
     */
    private int fluidInputMaxLength() {
        long maximumMb = Integer.toUnsignedLong(menu.getFluidTargetLimitMb());
        int wholeDigits = Long.toString(Math.max(0L, maximumMb / 1_000L)).length();
        return Math.max(5, wholeDigits + 4);
    }

    @Override
    public Optional<TerminalFluidScreenAccess.FluidHover> cultivation$getFluidAt(
            double mouseX, double mouseY) {
        for (TerminalFluidScreenAccess.FluidHover hover : cultivation$getVisibleFluids()) {
            if (TerminalLayout.containsHalfOpen(hover.bounds(), mouseX, mouseY)) {
                return Optional.of(hover);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<TerminalFluidScreenAccess.FluidHover> cultivation$getVisibleFluids() {
        if (amountDialogOpen) return List.of();
        List<TerminalFluidScreenAccess.FluidHover> result = new ArrayList<>();
        for (int slot = 0; slot < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT; slot++) {
            if (!menu.isFluidTarget(slot)) continue;
            FluidStack fluid = configuredFluidIdentity(slot);
            if (fluid.isEmpty()) continue;
            int x = this.leftPos + 8 + slot * 18;
            long desired = menu.getConfiguredAmount(slot);
            if (desired > 0L) {
                result.add(new TerminalFluidScreenAccess.FluidHover(
                        fluid, desired, new Rect2i(x,
                        this.topPos + XianqiaoInterfaceMenu.CONFIG_Y, 16, 16)));
            }
            long cached = menu.getCachedAmount(slot);
            if (cached > 0L) {
                result.add(new TerminalFluidScreenAccess.FluidHover(
                        fluid, cached, new Rect2i(x,
                        this.topPos + XianqiaoInterfaceMenu.BUFFER_Y, 16, 16)));
            }
        }
        return List.copyOf(result);
    }

    private FluidStack configuredFluidIdentity(int slot) {
        if (this.minecraft == null || this.minecraft.level == null) return FluidStack.EMPTY;
        ItemStack display = menu.getConfiguredTarget(slot);
        CustomData customData = display.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var marker = customData.copyTag();
        if (!marker.contains(XianqiaoInterfaceMenu.FLUID_DISPLAY_TAG)) return FluidStack.EMPTY;
        return FluidStack.parseOptional(this.minecraft.level.registryAccess(),
                marker.getCompound(XianqiaoInterfaceMenu.FLUID_DISPLAY_TAG));
    }

    private void renderFluidSprite(GuiGraphics graphics, FluidStack stack, int x, int y) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluidType());
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null || this.minecraft == null) return;
        TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
        int tint = extensions.getTintColor(stack);
        graphics.setColor(((tint >>> 16) & 0xFF) / 255.0F,
                ((tint >>> 8) & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F,
                ((tint >>> 24) & 0xFF) / 255.0F);
        graphics.blit(x, y, 0, 16, 16, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderAmountOverlay(GuiGraphics graphics, String label, int x, int y) {
        int width = this.font.width(label);
        float scale = Math.min(0.666F, 15.0F / Math.max(1, width));
        graphics.pose().pushPose();
        // Match vanilla item-decoration depth: above the resource sprite but
        // below the later hover-highlight and floating-carried-item passes.
        graphics.pose().translate(x + 17.0F, y + 17.0F, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, label, -width, -this.font.lineHeight, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private int dialogX() {
        return this.leftPos + (this.imageWidth - DIALOG_WIDTH) / 2;
    }

    private int dialogY() {
        return this.topPos + (this.imageHeight - DIALOG_HEIGHT) / 2;
    }

    private Component sideLabel(Direction side) {
        return Component.translatable("container.cultivation.xianqiao_interface.side_mode",
                Component.translatable("container.cultivation.xianqiao_interface.side." + side.getName()),
                modeLabel(menu.getSideMode(side)));
    }

    private Component sideTooltip(Direction side) {
        return Component.translatable("container.cultivation.xianqiao_interface.side_tooltip",
                Component.translatable("container.cultivation.xianqiao_interface.side_full." + side.getName()),
                modeLabel(menu.getSideMode(side)));
    }

    private static Component modeLabel(XianqiaoInterfaceBlockEntity.SideMode mode) {
        return Component.translatable("container.cultivation.xianqiao_interface.mode."
                + mode.name().toLowerCase(java.util.Locale.ROOT));
    }
}
