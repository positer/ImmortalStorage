package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
 * <p>This screen deliberately references only Minecraft/ImmortalStorage classes,
 * so a standalone installation remains safe. Optional integrations may add
 * behavior around the same server-authoritative menu without being required
 * to render or operate these controls. The complete visible page is rendered
 * every frame; there is no frame counter, time gate, animation throttling, or
 * off-screen list to keep alive.</p>
 */
public class XianqiaoInterfaceScreen
        extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<XianqiaoInterfaceMenu>
        implements TerminalFluidScreenAccess {
    private static final int TEXT = 0xFF404040;
    private static final int GHOST_TINT = 0x1F63B7BE;
    private static final int SCREEN_WIDTH = 176;
    private static final int SCREEN_HEIGHT = 243;
    private static final int SIDE_GRID_X = 56;
    private static final int SIDE_GRID_Y = 97;
    private static final int SIDE_BUTTON_WIDTH = 20;
    private static final int SIDE_COLUMN_STRIDE = 22;
    private static final int SIDE_ROW_STRIDE = 21;
    private static final float MODAL_Z = 500.0F;
    private static final int DIALOG_WIDTH = 176;
    private static final int DIALOG_HEIGHT = 132;
    private static final int MASK_BUTTON_SIZE = 18;
    private static final List<Direction> SIDE_ORDER = List.of(
            Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST);
    private final Map<Direction, FacePreviewButton> sideButtons = new EnumMap<>(Direction.class);
    private EditBox amountInput;
    private Button applyAmountButton;
    private Button cancelAmountButton;
    private Button activePullButton;
    private Button activePushButton;
    private final Map<Direction, FacePreviewButton> slotFaceButtons = new EnumMap<>(Direction.class);
    private final List<Button> externalResourceButtons = new ArrayList<>();
    private int selectedTarget;
    private long lastConfigurationRevision = Long.MIN_VALUE;
    private boolean amountDialogOpen;
    private boolean externalDialogOpen;
    private int externalResourceOffset;

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
            FacePreviewButton button = new FacePreviewButton(
                    buttonX, buttonY, SIDE_BUTTON_WIDTH, Component.literal(shortSide(side)),
                    () -> sideButtonPreview(side), () -> interfaceModeColor(side),
                    ignored -> cycleSideMode(side));
            button.setTooltip(Tooltip.create(sideTooltip(side)));
            this.addRenderableWidget(button);
            sideButtons.put(side, button);
        }
        int dialogX = dialogX();
        int dialogY = dialogY();
        this.amountInput = new EditBox(this.font, dialogX + 10,
                dialogY + 31, DIALOG_WIDTH - 20, 18,
                Component.translatable("container.immortalstorage.xianqiao_interface.amount_input"));
        this.amountInput.setMaxLength(20);
        this.amountInput.setFilter(value -> value.isEmpty()
                || value.chars().allMatch(Character::isDigit));
        this.amountInput.setResponder(ignored -> validateAmount());
        this.applyAmountButton = Button.builder(
                        Component.translatable("container.immortalstorage.xianqiao_interface.apply"),
                        ignored -> applyAmount())
                .bounds(dialogX + 10, dialogY + 106, 74, 20)
                .build();
        this.cancelAmountButton = Button.builder(
                        Component.translatable("container.immortalstorage.xianqiao_interface.cancel"),
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
            FacePreviewButton maskButton = new FacePreviewButton(
                    dialogX + 42 + (index % 3) * 32,
                    dialogY + 62 + (index / 3) * 20, MASK_BUTTON_SIZE,
                    Component.literal(shortSide(side)), () -> adjacentBlockPreview(side),
                    () -> 0xFF777777, ignored -> toggleSlotFace(side));
            this.addWidget(maskButton);
            slotFaceButtons.put(side, maskButton);
        }
        this.addWidget(amountInput);
        this.addWidget(applyAmountButton);
        this.addWidget(cancelAmountButton);
        setDialogWidgetsVisible(false);
        rebuildExternalResourceButtons();
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
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        VanillaGuiPainter.panel(graphics, x, y, this.imageWidth, this.imageHeight);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 17, 0xFFD8D8D8);
        graphics.horizontalLine(x + 3, x + this.imageWidth - 4, y + 17, 0xFF8B8B8B);

        for (int column = 0; column < 9; column++) {
            int slotX = x + 8 + column * 18;
            VanillaGuiPainter.slot(graphics, slotX, y + XianqiaoInterfaceMenu.CONFIG_Y, true);
            graphics.fill(slotX, y + XianqiaoInterfaceMenu.CONFIG_Y,
                    slotX + 16, y + XianqiaoInterfaceMenu.CONFIG_Y + 16, GHOST_TINT);
            VanillaGuiPainter.slot(graphics, slotX, y + XianqiaoInterfaceMenu.BUFFER_Y, true);
        }
        int selectedX = x + 8 + selectedTarget * 18;
        graphics.outline(selectedX - 1, y + XianqiaoInterfaceMenu.CONFIG_Y - 1,
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
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (amountDialogOpen) {
            renderAmountDialog(graphics, mouseX, mouseY, partialTick);
        } else if (externalDialogOpen) {
            renderExternalResourceDialog(graphics, mouseX, mouseY, partialTick);
        } else {
            Optional<ExternalResourceHover> externalHover = externalResourceHover();
            Optional<TerminalFluidScreenAccess.FluidHover> fluidHover =
                    immortalstorage$getFluidAt(mouseX, mouseY);
            if (menu.getCarried().isEmpty() && externalHover.isPresent()) {
                ExternalResourceHover hover = externalHover.get();
                ExternalResourceCatalog.Definition definition =
                        ExternalResourceCatalog.definition(hover.key());
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(ExternalResourceCatalog.displayName(hover.key()));
                tooltip.add(Component.literal(Long.toString(hover.amount())
                        + (definition.unit().isBlank() ? "" : " " + definition.unit())));
                com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, this.font, tooltip, mouseX, mouseY);
            } else if (menu.getCarried().isEmpty() && fluidHover.isPresent()) {
                TerminalFluidScreenAccess.FluidHover hover = fluidHover.get();
                com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, this.font, List.of(
                                hover.stack().getHoverName(),
                                Component.literal(TerminalFluidAmountFormatter.exactBuckets(
                                        hover.amountMb())),
                                Component.literal(TerminalFluidAmountFormatter.exactMillibuckets(
                                        hover.amountMb()))), mouseX, mouseY);
            } else {
                renderTooltip(graphics, mouseX, mouseY);
            }
        }
    }

    private Optional<ExternalResourceHover> externalResourceHover() {
        if (this.hoveredSlot == null) return Optional.empty();
        int slotIndex = this.menu.slots.indexOf(this.hoveredSlot);
        if (slotIndex < 0 || slotIndex >= XianqiaoInterfaceMenu.PLAYER_START) {
            return Optional.empty();
        }
        int resourceSlot = slotIndex % XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT;
        if (!menu.isExternalTarget(resourceSlot)) return Optional.empty();
        ResourceChannelKey key = menu.getExternalTarget(resourceSlot);
        if (key == null) return Optional.empty();
        long amount = slotIndex < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT
                ? menu.getConfiguredAmount(resourceSlot) : menu.getCachedAmount(resourceSlot);
        return amount > 0L ? Optional.of(new ExternalResourceHover(key, amount)) : Optional.empty();
    }

    private record ExternalResourceHover(ResourceChannelKey key, long amount) {}

    /**
     * Renders mixed resource identities and synchronized totals in vanilla's
     * slot-content pass. Hover highlights and the carried stack therefore run
     * afterwards and remain visually on top.
     */
    @Override
    protected void renderSlotContents(
            GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, @Nullable String countString) {
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

        if (menu.isExternalTarget(resourceSlot)) {
            ResourceChannelKey key = menu.getExternalTarget(resourceSlot);
            if (key != null) {
                renderExternalResource(graphics, key, amount, slot.x, slot.y);
            }
            return;
        }

        ItemStack identity = menu.getConfiguredTarget(resourceSlot);
        if (identity.isEmpty()) return;
        identity = identity.copyWithCount(1);
        int seed = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) graphics.fakeItem(identity, slot.x, slot.y, seed);
        else graphics.item(identity, slot.x, slot.y, seed);
        graphics.itemDecorations(this.font, identity, slot.x, slot.y, "");
        renderAmountOverlay(graphics, Long.toString(amount), slot.x, slot.y);
    }

    /**
     * Fluid cache slots intentionally have no fake ItemStack mirror. Vanilla
     * therefore skips renderSlotContents for them; paint that one empty-slot
     * case here, still inside the ordinary slot pass and before hover/floating
     * item rendering.
     */
    @Override
    protected void renderSlot(GuiGraphicsExtractor graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        int slotIndex = this.menu.slots.indexOf(slot);
        if (slotIndex < XianqiaoInterfaceMenu.BUFFER_START
                || slotIndex >= XianqiaoInterfaceMenu.PLAYER_START
                || !slot.getItem().isEmpty()) return;
        int resourceSlot = slotIndex - XianqiaoInterfaceMenu.BUFFER_START;
        long amount = menu.getCachedAmount(resourceSlot);
        if (menu.isExternalTarget(resourceSlot)) {
            ResourceChannelKey key = menu.getExternalTarget(resourceSlot);
            if (amount > 0L && key != null) {
                renderExternalResource(graphics, key, amount, slot.x, slot.y);
            }
            return;
        }
        if (!menu.isFluidTarget(resourceSlot)) return;
        FluidStack fluid = configuredFluidIdentity(resourceSlot);
        if (amount <= 0L || fluid.isEmpty()) return;
        renderFluidSprite(graphics, fluid, slot.x, slot.y);
        renderAmountOverlay(graphics, formatBuckets(amount) + "B", slot.x, slot.y);
    }

    private void renderExternalResource(
            GuiGraphicsExtractor graphics, ResourceChannelKey key, long amount, int x, int y) {
        ExternalResourceCatalog.Definition definition = ExternalResourceCatalog.definition(key);
        if (definition.solidColor()) {
            graphics.fill(x, y, x + 16, y + 16, definition.color());
        } else {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, definition.icon(), x, y, 16, 16, 0.0F, 0.0F, 16, externalTextureHeight(key));
        }
        renderAmountOverlay(graphics, Long.toString(amount), x, y);
    }

    private static int externalTextureHeight(ResourceChannelKey key) {
        return switch (key.channel()) {
            case "botania_mana" -> 512;
            case "ars_nouveau_source" -> 320;
            default -> 16;
        };
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, TEXT, false);
        graphics.text(this.font,
                Component.translatable("container.immortalstorage.xianqiao_interface.targets"),
                8, 19, TEXT, false);
        graphics.text(this.font,
                Component.translatable("container.immortalstorage.xianqiao_interface.buffers"),
                8, 55, TEXT, false);
        graphics.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, TEXT, false);
        graphics.text(this.font,
                Component.translatable("container.immortalstorage.xianqiao_interface.sides"),
                8, 86, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (externalDialogOpen) {
            for (Button resourceButton : externalResourceButtons) {
                if (clickDialogWidget(resourceButton, mouseX, mouseY, button)) return true;
            }
            return true;
        }
        if (amountDialogOpen) {
            if (clickDialogWidget(amountInput, mouseX, mouseY, button)) return true;
            if (clickDialogWidget(applyAmountButton, mouseX, mouseY, button)) return true;
            if (clickDialogWidget(cancelAmountButton, mouseX, mouseY, button)) return true;
            for (Map.Entry<Direction, FacePreviewButton> entry : slotFaceButtons.entrySet()) {
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
                    // Empty-hand primary click follows the normal menu slot
                    // path. The authoritative menu then clears the target and
                    // atomically returns its real cache before discarding the
                    // identity. Secondary click remains the amount editor.
                    if (button == 0) {
                        return super.mouseClicked(mouseX, mouseY, button);
                    }
                    if (button == 1) {
                        openAmountDialog(slot);
                        return true;
                    }
                } else if (menu.getCarried().isEmpty() && button == 1
                        && !menu.availableExternalResources().isEmpty()) {
                    openExternalResourceDialog(slot);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean isHovering(
            int x, int y, int width, int height, double mouseX, double mouseY) {
        return !amountDialogOpen && !externalDialogOpen
                && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (externalDialogOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeExternalResourceDialog();
                return true;
            }
            return true;
        }
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
            if (focused != null) focused.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (externalDialogOpen) return true;
        if (amountDialogOpen) {
            if (this.getFocused() == amountInput) amountInput.charTyped(new net.minecraft.client.input.CharacterEvent(codePoint));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (externalDialogOpen) return true;
        if (amountDialogOpen) {
            GuiEventListener focused = this.getFocused();
            if (focused != null) {
                focused.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), dragX, dragY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (externalDialogOpen) return true;
        if (amountDialogOpen) {
            GuiEventListener focused = this.getFocused();
            if (focused != null) focused.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double scrollX, double scrollY) {
        if (externalDialogOpen) {
            int maximum = Math.max(0, menu.availableExternalResources().size() - 5);
            int delta = scrollY < 0.0D ? 1 : scrollY > 0.0D ? -1 : 0;
            int next = Math.max(0, Math.min(maximum, externalResourceOffset + delta));
            if (next != externalResourceOffset) {
                externalResourceOffset = next;
                refreshExternalResourceButtons();
            }
            return true;
        }
        if (amountDialogOpen) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void cycleSideMode(Direction side) {
        if (menu.getBlockEntity() == null) return;
        XianqiaoInterfaceBlockEntity.SideMode[] modes =
                XianqiaoInterfaceBlockEntity.SideMode.values();
        int next = (menu.getSideMode(side).ordinal() + 1) % modes.length;
        ClientPacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceSideMode(
                menu.containerId, menu.getBlockEntity().getBlockPos(), menu.getConfigRevision(),
                side.ordinal(), next));
    }

    private void applyAmount() {
        if (amountInput == null || menu.getBlockEntity() == null || !validAmount()) return;
        long amount;
        if (menu.isFluidTarget(selectedTarget)) {
            amount = fluidInputToMillibuckets(amountInput.getValue());
            amount = Math.min(menu.getFluidTargetLimitMb(), amount);
        } else if (menu.isExternalTarget(selectedTarget)) {
            try {
                amount = Long.parseLong(amountInput.getValue());
            } catch (NumberFormatException ignored) {
                amount = Long.MAX_VALUE;
            }
            ResourceChannelKey key = menu.getExternalTarget(selectedTarget);
            amount = ExternalResourceChannels.clampCacheAmount(key, amount);
        } else {
            try {
                amount = Long.parseLong(amountInput.getValue());
            } catch (NumberFormatException ignored) {
                amount = Long.MAX_VALUE;
            }
            amount = Math.min(menu.getItemTargetLimit(), amount);
        }
        ClientPacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceTargetAmount(
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
        for (Map.Entry<Direction, FacePreviewButton> entry : sideButtons.entrySet()) {
            Button sideButton = entry.getValue();
            sideButton.setMessage(sideLabel(entry.getKey()));
            sideButton.setTooltip(Tooltip.create(sideTooltip(entry.getKey())));
            sideButton.visible = !amountDialogOpen && !externalDialogOpen;
            sideButton.active = menu.getBlockEntity() != null
                    && !amountDialogOpen && !externalDialogOpen;
        }
        activePullButton.setMessage(Component.translatable(
                "container.immortalstorage.xianqiao_interface.active_pull"));
        activePushButton.setMessage(Component.translatable(
                "container.immortalstorage.xianqiao_interface.active_push"));
        activePullButton.setAlpha(menu.isActivePullEnabled() ? 1.0F : 0.35F);
        activePushButton.setAlpha(menu.isActivePushEnabled() ? 1.0F : 0.35F);
        activePullButton.setTooltip(Tooltip.create(Component.translatable(
                "container.immortalstorage.xianqiao_interface.active_pull_tooltip",
                Component.translatable(menu.isActivePullEnabled()
                        ? "container.immortalstorage.xianqiao_interface.state_on"
                        : "container.immortalstorage.xianqiao_interface.state_off"))));
        activePushButton.setTooltip(Tooltip.create(Component.translatable(
                "container.immortalstorage.xianqiao_interface.active_push_tooltip",
                Component.translatable(menu.isActivePushEnabled()
                        ? "container.immortalstorage.xianqiao_interface.state_on"
                        : "container.immortalstorage.xianqiao_interface.state_off"))));
        activePullButton.visible = !amountDialogOpen && !externalDialogOpen;
        activePushButton.visible = !amountDialogOpen && !externalDialogOpen;
        for (Map.Entry<Direction, FacePreviewButton> entry : slotFaceButtons.entrySet()) {
            boolean enabled = menu.isSlotFaceEnabled(selectedTarget, entry.getKey());
            entry.getValue().setAlpha(enabled ? 1.0F : 0.35F);
            entry.getValue().setTooltip(Tooltip.create(Component.translatable(
                    "container.immortalstorage.xianqiao_interface.slot_face_tooltip",
                    fullSideName(entry.getKey()), shortSide(entry.getKey()),
                    Component.translatable(enabled
                            ? "container.immortalstorage.xianqiao_interface.slot_face_open"
                            : "container.immortalstorage.xianqiao_interface.slot_face_closed"))));
        }
        validateAmount();
    }

    private void toggleActiveTransfer(boolean pull) {
        if (menu.getBlockEntity() == null) return;
        boolean enabled = pull ? !menu.isActivePullEnabled() : !menu.isActivePushEnabled();
        ClientPacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceActiveTransfer(
                menu.containerId, menu.getBlockEntity().getBlockPos(), menu.getConfigRevision(), pull, enabled));
    }

    private void toggleSlotFace(Direction side) {
        if (menu.getBlockEntity() == null || !amountDialogOpen) return;
        ClientPacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceSlotFaceMask(
                menu.containerId, menu.getBlockEntity().getBlockPos(), lastConfigurationRevision,
                selectedTarget, side.ordinal(), !menu.isSlotFaceEnabled(selectedTarget, side)));
    }

    private static String shortSide(Direction side) {
        return switch (side) {
            case UP -> "U";
            case DOWN -> "D";
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
        };
    }

    private String fullSideName(Direction side) {
        return Component.translatable(
                "container.immortalstorage.xianqiao_interface.side_full." + side.getName()).getString();
    }

    private ItemStack adjacentBlockPreview(Direction side) {
        XianqiaoInterfaceBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getLevel() == null) return ItemStack.EMPTY;
        ItemStack preview = new ItemStack(blockEntity.getLevel().getBlockState(
                blockEntity.getBlockPos().relative(side)).getBlock().asItem());
        return preview.isEmpty() ? ItemStack.EMPTY : preview;
    }

    /** Side-button preview; subclasses may suppress the adjacent-block icon. */
    protected ItemStack sideButtonPreview(Direction side) {
        return adjacentBlockPreview(side);
    }

    private int interfaceModeColor(Direction side) {
        return switch (menu.getSideMode(side)) {
            case PULL -> 0xFF38A85A;
            case PUSH -> 0xFFD44A4A;
            case DISABLED -> 0xFF777777;
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

    private void rebuildExternalResourceButtons() {
        for (Button button : externalResourceButtons) removeWidget(button);
        externalResourceButtons.clear();
        int x = dialogX() + 10;
        int y = dialogY() + 27;
        int width = DIALOG_WIDTH - 20;
        for (int index = 0; index < 5; index++) {
            final int buttonIndex = index;
            Button button = Button.builder(Component.empty(),
                            ignored -> configureExternalResourceButton(buttonIndex))
                    .bounds(x, y + index * 20, width, 18)
                    .build();
            button.visible = false;
            button.active = false;
            addWidget(button);
            externalResourceButtons.add(button);
        }
        refreshExternalResourceButtons();
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

    private void configureExternalResourceButton(int buttonIndex) {
        List<ResourceChannelKey> resources = menu.availableExternalResources();
        int resourceIndex = externalResourceOffset + buttonIndex;
        if (resourceIndex >= 0 && resourceIndex < resources.size()) {
            configureExternalResource(resources.get(resourceIndex));
        }
    }

    private void openExternalResourceDialog(int slot) {
        selectedTarget = slot;
        lastConfigurationRevision = menu.getConfigRevision();
        externalResourceOffset = 0;
        externalDialogOpen = true;
        refreshExternalResourceButtons();
        updateControls();
    }

    private void closeExternalResourceDialog() {
        externalDialogOpen = false;
        setExternalDialogWidgetsVisible(false);
        updateControls();
    }

    private void setExternalDialogWidgetsVisible(boolean visible) {
        if (visible) {
            refreshExternalResourceButtons();
            return;
        }
        for (Button button : externalResourceButtons) {
            button.visible = false;
            button.active = false;
        }
    }

    private void configureExternalResource(ResourceChannelKey key) {
        if (menu.getBlockEntity() == null || key == null) return;
        ClientPacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceExternalTarget(
                menu.containerId, menu.getBlockEntity().getBlockPos(), lastConfigurationRevision,
                selectedTarget, key.channel(), key.resourceId(),
                XianqiaoInterfaceMenu.DEFAULT_EXTERNAL_CACHE_AMOUNT));
        closeExternalResourceDialog();
    }

    private void renderExternalResourceDialog(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        try {
            graphics.fill(0, 0, this.width, this.height, 0x78000000);
            int x = dialogX();
            int y = dialogY();
            VanillaGuiPainter.panel(graphics, x, y, DIALOG_WIDTH, DIALOG_HEIGHT);
            graphics.fill(x + 2, y + 2, x + DIALOG_WIDTH - 2, y + 19, 0xFFD8D8D8);
            graphics.horizontalLine(x + 3, x + DIALOG_WIDTH - 4, y + 19, 0xFF8B8B8B);
            graphics.text(font,
                    Component.translatable("container.immortalstorage.xianqiao_interface.external_title",
                            selectedTarget + 1), x + 8, y + 7, TEXT, false);
            for (Button button : externalResourceButtons) {
                button.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
            
        } finally {
            graphics.pose().popMatrix();
        }
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
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        try {
            graphics.fill(0, 0, this.width, this.height, 0x78000000);
            int x = dialogX();
            int y = dialogY();
            VanillaGuiPainter.panel(graphics, x, y, DIALOG_WIDTH, DIALOG_HEIGHT);
            graphics.fill(x + 2, y + 2, x + DIALOG_WIDTH - 2, y + 19, 0xFFD8D8D8);
            graphics.horizontalLine(x + 3, x + DIALOG_WIDTH - 4, y + 19, 0xFF8B8B8B);
            graphics.text(font,
                    Component.translatable("container.immortalstorage.xianqiao_interface.amount_title",
                            selectedTarget + 1), x + 8, y + 7, TEXT, false);
            graphics.text(font, amountSummary(), x + 10, y + 22, TEXT, false);
            amountInput.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.text(font,
                    Component.translatable("container.immortalstorage.xianqiao_interface.slot_faces"),
                    x + 8, y + 52, TEXT, false);
            for (Button button : slotFaceButtons.values()) button.extractRenderState(graphics, mouseX, mouseY, partialTick);
            applyAmountButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
            cancelAmountButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
            
        } finally {
            graphics.pose().popMatrix();
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
        widget.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
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
        if (menu.isExternalTarget(selectedTarget)) {
            ResourceChannelKey key = menu.getExternalTarget(selectedTarget);
            return Component.translatable(
                    "container.immortalstorage.xianqiao_interface.external_amount_hint",
                    amount, key == null ? "?" : key.resourceId());
        }
        if (!menu.isFluidTarget(selectedTarget)) {
            return Component.translatable("container.immortalstorage.xianqiao_interface.item_amount_hint",
                    amount, menu.getItemTargetLimit());
        }
        long millibuckets = fluidInputToMillibuckets(amountInput.getValue());
        String buckets = validAmount() ? amountInput.getValue() : "?";
        return Component.translatable("container.immortalstorage.xianqiao_interface.fluid_amount_hint",
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
    public Optional<TerminalFluidScreenAccess.FluidHover> immortalstorage$getFluidAt(
            double mouseX, double mouseY) {
        for (TerminalFluidScreenAccess.FluidHover hover : immortalstorage$getVisibleFluids()) {
            if (TerminalLayout.containsHalfOpen(hover.bounds(), mouseX, mouseY)) {
                return Optional.of(hover);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<TerminalFluidScreenAccess.FluidHover> immortalstorage$getVisibleFluids() {
        if (amountDialogOpen || externalDialogOpen) return List.of();
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
        return com.immortalstorage.immortalstorage.compat.mc2612.CompatCodec.parseFluidStack(this.minecraft.level.registryAccess(),
                marker.getCompoundOrEmpty(XianqiaoInterfaceMenu.FLUID_DISPLAY_TAG));
    }

    private void renderFluidSprite(GuiGraphicsExtractor graphics, FluidStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        graphics.fakeItem(stack.getFluidType().getBucket(stack), x, y);
    }

    private void renderAmountOverlay(GuiGraphicsExtractor graphics, String label, int x, int y) {
        int width = this.font.width(label);
        float scale = Math.min(0.666F, 15.0F / Math.max(1, width));
        graphics.pose().pushMatrix();
        // Match vanilla item-decoration depth: above the resource sprite but
        // below the later hover-highlight and floating-carried-item passes.
        graphics.pose().translate(x + 17.0F, y + 17.0F);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, label, -width, -this.font.lineHeight, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    private int dialogX() {
        return this.leftPos + (this.imageWidth - DIALOG_WIDTH) / 2;
    }

    private int dialogY() {
        return this.topPos + (this.imageHeight - DIALOG_HEIGHT) / 2;
    }

    private Component sideLabel(Direction side) {
        return Component.literal(shortSide(side));
    }

    private Component sideTooltip(Direction side) {
        return Component.translatable("container.immortalstorage.xianqiao_interface.side_tooltip",
                Component.translatable("container.immortalstorage.xianqiao_interface.side_full." + side.getName()),
                modeLabel(menu.getSideMode(side)));
    }

    private static Component modeLabel(XianqiaoInterfaceBlockEntity.SideMode mode) {
        return Component.translatable("container.immortalstorage.xianqiao_interface.mode."
                + mode.name().toLowerCase(java.util.Locale.ROOT));
    }
}
