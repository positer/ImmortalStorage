package com.immortalstorage.immortalstorage.client.screen;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.menu.custom.SourceFluxValue;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinMenu;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.EnumMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

/** Vanilla-widget face configuration; the source cache is intentionally hidden. */
public class SourceVeinScreen extends com.immortalstorage.immortalstorage.compat.mc2612.CompatAbstractContainerScreen<SourceVeinMenu> {
    private static final int TEXT = 0xFF404040;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 6;
    private static final int TITLE_MAX_WIDTH = 160;
    private static final String ELLIPSIS = "\u2026";
    private static final Direction[] SIDE_BUTTONS = {
            Direction.UP, Direction.NORTH, Direction.DOWN,
            Direction.WEST, Direction.SOUTH, Direction.EAST
    };
    private final Map<Direction, FacePreviewButton> sideButtons = new EnumMap<>(Direction.class);
    private EditBox fluxInput;
    private Button applyFluxButton;
    private long lastSyncedFlux;

    public SourceVeinScreen(SourceVeinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 112;
    }

    @Override
    protected void init() {
        super.init();
        this.sideButtons.clear();
        this.lastSyncedFlux = this.menu.getFluxLimit();
        this.fluxInput = new EditBox(this.font, this.leftPos + 48, this.topPos + 22, 84, 18,
                Component.translatable("container.immortalstorage.source_vein.flux_input"));
        this.fluxInput.setMaxLength(128);
        this.fluxInput.setHint(Component.translatable("container.immortalstorage.source_vein.flux_input_hint"));
        this.fluxInput.setValue(Long.toString(this.lastSyncedFlux));
        this.applyFluxButton = Button.builder(
                        Component.translatable("container.immortalstorage.source_vein.flux_apply"),
                        ignored -> applyFluxLimit())
                .bounds(this.leftPos + 136, this.topPos + 22, 32, 18)
                .build();
        this.fluxInput.setResponder(this::validateFluxInput);
        this.addRenderableWidget(this.fluxInput);
        this.addRenderableWidget(this.applyFluxButton);
        validateFluxInput(this.fluxInput.getValue());
        for (int index = 0; index < SIDE_BUTTONS.length; index++) {
            Direction side = SIDE_BUTTONS[index];
            int x = this.leftPos + 56 + (index % 3) * 22;
            int y = this.topPos + 61 + (index / 3) * 21;
            FacePreviewButton button = new FacePreviewButton(
                    x, y, 20, Component.literal(shortSide(side)),
                    () -> adjacentBlockPreview(side), () -> sourceModeColor(side),
                    ignored -> cycleMode(side));
            button.setTooltip(Tooltip.create(sideTooltip(side)));
            this.addRenderableWidget(button);
            this.sideButtons.put(side, button);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonMessages();
        long synchronizedFlux = this.menu.getFluxLimit();
        if (this.fluxInput != null && !this.fluxInput.isFocused()
                && synchronizedFlux != this.lastSyncedFlux) {
            this.lastSyncedFlux = synchronizedFlux;
            this.fluxInput.setValue(Long.toString(synchronizedFlux));
        }
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        VanillaGuiPainter.panel(graphics, x, y, this.imageWidth, this.imageHeight);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 18, 0xFFD8D8D8);
        graphics.horizontalLine(x + 3, x + this.imageWidth - 4, y + 18, 0xFF858585);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderTitleTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, fittedTitle(), TITLE_X, TITLE_Y, TEXT, false);
        graphics.text(this.font,
                Component.translatable("container.immortalstorage.source_vein.flux"), 8, 27, TEXT, false);
        graphics.text(this.font,
                Component.translatable("container.immortalstorage.source_vein.sides"), 8, 48, TEXT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.fluxInput != null && this.fluxInput.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applyFluxLimit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inputWasFocused = this.fluxInput != null && this.fluxInput.isFocused();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (inputWasFocused && this.fluxInput != null && !this.fluxInput.isFocused()
                && (this.applyFluxButton == null || !this.applyFluxButton.isMouseOver(mouseX, mouseY))) {
            applyFluxLimit();
        }
        return handled;
    }

    @Override
    public void removed() {
        if (this.fluxInput != null && this.fluxInput.isFocused()) applyFluxLimit();
        super.removed();
    }

    private Component fittedTitle() {
        if (this.font.width(this.title) <= TITLE_MAX_WIDTH) return this.title;
        int textWidth = Math.max(0, TITLE_MAX_WIDTH - this.font.width(ELLIPSIS));
        String fitted = this.font.plainSubstrByWidth(this.title.getString(), textWidth);
        return Component.literal(fitted + ELLIPSIS).withStyle(this.title.getStyle());
    }

    private void renderTitleTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.font.width(this.title) <= TITLE_MAX_WIDTH) return;
        if (this.isHovering(TITLE_X, TITLE_Y, TITLE_MAX_WIDTH, this.font.lineHeight, mouseX, mouseY)) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(graphics, this.font, this.title, mouseX, mouseY);
        }
    }

    private void cycleMode(Direction side) {
        if (this.menu.getBlockEntity() == null) return;
        int next = SourceVeinBlockEntity.SourceSideMode
                .byId(this.menu.getSideModeId(side)).next().persistedId();
        ClientPacketDistributor.sendToServer(new ModPayloads.SetSourceSideMode(
                this.menu.getBlockEntity().getBlockPos(), side.ordinal(), next));
    }

    private void applyFluxLimit() {
        if (this.fluxInput == null || this.menu.getBlockEntity() == null) return;
        SourceFluxValue.ParseResult parsed = SourceFluxValue.parse(this.fluxInput.getValue());
        if (!parsed.valid()) {
            validateFluxInput(this.fluxInput.getValue());
            return;
        }
        this.fluxInput.setValue(Long.toString(parsed.value()));
        ClientPacketDistributor.sendToServer(new ModPayloads.SetSourceFluxLimit(
                this.menu.containerId, this.menu.getBlockEntity().getBlockPos(), parsed.value()));
        this.fluxInput.setFocused(false);
        validateFluxInput(this.fluxInput.getValue());
    }

    private void validateFluxInput(String value) {
        if (this.fluxInput == null || this.applyFluxButton == null) return;
        SourceFluxValue.ParseResult parsed = SourceFluxValue.parse(value);
        if (parsed.saturated()) {
            this.fluxInput.setValue(Long.toString(parsed.value()));
            return;
        }
        this.fluxInput.setTextColor(parsed.valid() ? 0xFFE0E0E0 : 0xFFFF5555);
        this.applyFluxButton.active = parsed.valid() && parsed.value() != this.menu.getFluxLimit();
        Component tooltip = parsed.valid()
                ? Component.translatable("container.immortalstorage.source_vein.flux_input_tooltip",
                        SourceFluxValue.MIN_VALUE, SourceFluxValue.MAX_VALUE)
                : switch (parsed.error()) {
                    case EMPTY -> Component.translatable("container.immortalstorage.source_vein.flux_error_empty");
                    case NOT_AN_INTEGER -> Component.translatable("container.immortalstorage.source_vein.flux_error_integer");
                    case OUT_OF_RANGE -> Component.translatable("container.immortalstorage.source_vein.flux_error_range",
                            SourceFluxValue.MIN_VALUE, SourceFluxValue.MAX_VALUE);
                    case NONE -> Component.empty();
                };
        this.fluxInput.setTooltip(Tooltip.create(tooltip));
    }

    private void updateButtonMessages() {
        for (Map.Entry<Direction, FacePreviewButton> entry : this.sideButtons.entrySet()) {
            entry.getValue().setTooltip(Tooltip.create(sideTooltip(entry.getKey())));
        }
    }

    private ItemStack adjacentBlockPreview(Direction side) {
        SourceVeinBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getLevel() == null) return ItemStack.EMPTY;
        ItemStack preview = new ItemStack(blockEntity.getLevel().getBlockState(
                blockEntity.getBlockPos().relative(side)).getBlock().asItem());
        return preview.isEmpty() ? ItemStack.EMPTY : preview;
    }

    private int sourceModeColor(Direction side) {
        return switch (SourceVeinBlockEntity.SourceSideMode.byId(menu.getSideModeId(side))) {
            case DISABLED -> 0xFF777777;
            case PUSH -> 0xFFD44A4A;
            case BYPASS_PUSH -> 0xFF9A4BC2;
        };
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

    private Component sideLabel(Direction side) {
        SourceVeinBlockEntity.SourceSideMode mode = SourceVeinBlockEntity.SourceSideMode
                .byId(this.menu.getSideModeId(side));
        return Component.translatable("container.immortalstorage.source_vein.side_mode",
                Component.translatable(sideKey(side)), modeLabel(mode));
    }

    private Component sideTooltip(Direction side) {
        SourceVeinBlockEntity.SourceSideMode mode = SourceVeinBlockEntity.SourceSideMode
                .byId(this.menu.getSideModeId(side));
        net.minecraft.network.chat.MutableComponent tooltip = Component.translatable(
                "container.immortalstorage.source_vein.side_tooltip",
                Component.translatable(fullSideKey(side)), modeLabel(mode));
        if (this.menu.isSideFaulted(side)) {
            tooltip.append("\n").append(Component.translatable(
                    "container.immortalstorage.source_vein.side_tooltip_fault",
                    this.menu.getSideUncertainInFlight(side)));
        }
        return tooltip;
    }

    private static String sideKey(Direction side) {
        return "container.immortalstorage.source_vein.side." + side.getName();
    }

    private static String fullSideKey(Direction side) {
        return "container.immortalstorage.source_vein.side_full." + side.getName();
    }

    private static Component modeLabel(SourceVeinBlockEntity.SourceSideMode mode) {
        return switch (mode) {
            case DISABLED -> Component.translatable("container.immortalstorage.source_vein.mode.off");
            case PUSH -> Component.translatable("container.immortalstorage.source_vein.mode.push");
            case BYPASS_PUSH -> Component.translatable("container.immortalstorage.source_vein.mode.bypass_push");
        };
    }
}
