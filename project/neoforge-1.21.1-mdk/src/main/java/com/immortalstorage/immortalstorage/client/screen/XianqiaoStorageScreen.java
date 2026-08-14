package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.compat.TerminalExternalResourceCompatHooks;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XianqiaoStorageScreen extends AbstractTerminalScreen<XianqiaoStorageMenu>
        implements TerminalFluidScreenAccess {
    private static final int REALM_WIDTH = 104;
    private static final int REALM_HEIGHT = 160;
    /** Small vanilla panel seam; the removed channel rail no longer reserves 32 px. */
    private static final int REALM_GAP = 4;
    private static final int REALM_TIME_BUTTON_SIZE = 20;
    private static final int REALM_TIME_SIDE_INSET = 8;
    private static final int REALM_TIME_LABEL_Y = 43;
    private static final int REALM_TIME_ROW_Y = 53;

    private boolean realmVisible;
    private boolean furnaceVisible;
    private boolean smithingVisible;
    private boolean stonecutterVisible;
    private final TerminalStonecutterGui stonecutterGui = new TerminalStonecutterGui();
    private Button stonecutterToggleButton;
    private TerminalTabButton craftModuleButton;
    private TerminalTabButton furnaceModuleButton;
    private TerminalTabButton realmModuleButton;
    private TerminalTabButton smithingModuleButton;
    private Button tribulateButton;
    private Button slowerTimeButton;
    private Button fasterTimeButton;
    private Button dayNightButton;
    private Button weatherButton;
    private Button autoFurnaceFuelButton;
    private Button autoFurnaceFillButton;
    private Button handAutoRefillButton;
    private Button magnetButton;
    private Button sortInventoryButton;
    private Button depositInventoryButton;
    private Button withdrawInventoryButton;
    private final long[] cachedItemAmounts = new long[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];
    private final String[] cachedItemAmountLabels = new String[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];
    private final int[] cachedItemAmountWidths = new int[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];
    private final long[] cachedFluidAmounts = new long[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];
    private final String[] cachedFluidAmountLabels = new String[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];
    private final int[] cachedFluidAmountWidths = new int[XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS];

    public XianqiaoStorageScreen(XianqiaoStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int railX = this.leftPos + TerminalLayout.MODULE_RAIL_X;
        Component craftLabel = Component.translatable("container.immortalstorage.terminal.craft");
        Component furnaceLabel = Component.translatable("container.immortalstorage.terminal.furnace");
        Component realmLabel = Component.translatable("container.immortalstorage.terminal.realm");
        Component smithingLabel = Component.translatable("container.immortalstorage.terminal.smithing");
        this.craftModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(0),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(0, 4),
                new ItemStack(Items.CRAFTING_TABLE), craftLabel, Tooltip.create(craftLabel),
                () -> this.craftingVisible, button -> selectModule(0)));
        this.smithingModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(1),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(1, 4),
                new ItemStack(Items.SMITHING_TABLE), smithingLabel, Tooltip.create(smithingLabel),
                () -> this.smithingVisible, button -> selectModule(3)));
        this.furnaceModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(2),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(2, 4),
                new ItemStack(ModBlocks.IMMORTAL_FURNACE.get()), furnaceLabel, Tooltip.create(furnaceLabel),
                () -> this.furnaceVisible, button -> selectModule(2)));
        this.realmModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(3),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(3, 4),
                new ItemStack(Items.ENDER_EYE), realmLabel, Tooltip.create(realmLabel),
                () -> this.realmVisible, button -> selectModule(1)));

        boolean terminalUnlocked = this.menu.isCraftingUnlocked();
        this.craftModuleButton.active = terminalUnlocked;
        this.furnaceModuleButton.active = terminalUnlocked;
        this.smithingModuleButton.active = this.menu.isSmithingUnlocked();
        this.stonecutterToggleButton = this.addRenderableWidget(Button.builder(
                        stonecutterToggleLabel(), button -> requestMenuButton(XianqiaoStorageMenu.SMITHING_VIEW_BUTTON))
                .bounds(this.leftPos + 30, this.topPos + TerminalLayout.craftGridY(this.imageHeight) - 8, 76, 16)
                .tooltip(Tooltip.create(Component.translatable("container.immortalstorage.terminal.stonecutter_toggle_hint")))
                .build());
        updateStonecutterToggle();
        this.tribulateButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("container.immortalstorage.terminal.tribulate"),
                        button -> PacketDistributor.sendToServer(new ModPayloads.TriggerTribulation(this.menu.containerId)))
                .bounds(this.leftPos + this.imageWidth + REALM_GAP + 20,
                        this.topPos + 99, 68, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.immortalstorage.terminal.tribulate_hint")))
                .build());
        int environmentX = this.leftPos + this.imageWidth + REALM_GAP + 8;
        this.dayNightButton = this.addRenderableWidget(Button.builder(dayNightLabel(),
                        button -> PacketDistributor.sendToServer(
                                new ModPayloads.RealmEnvironment(this.menu.containerId, 0)))
                .bounds(environmentX, this.topPos + 78, 42, 18)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.day_night_hint"))).build());
        this.weatherButton = this.addRenderableWidget(Button.builder(weatherLabel(),
                        button -> PacketDistributor.sendToServer(
                                new ModPayloads.RealmEnvironment(this.menu.containerId, 1)))
                .bounds(environmentX + 46, this.topPos + 78, 42, 18)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.weather_hint"))).build());
        int realmPanelX = this.leftPos + this.imageWidth + REALM_GAP;
        int timeControlY = this.topPos + REALM_TIME_ROW_Y;
        this.slowerTimeButton = this.addRenderableWidget(Button.builder(Component.literal("-"),
                        button -> PacketDistributor.sendToServer(new ModPayloads.TimeFlow(this.menu.containerId, -1)))
                .bounds(realmPanelX + REALM_TIME_SIDE_INSET, timeControlY,
                        REALM_TIME_BUTTON_SIZE, REALM_TIME_BUTTON_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.immortalstorage.terminal.time_slower")))
                .build());
        this.fasterTimeButton = this.addRenderableWidget(Button.builder(Component.literal("+"),
                        button -> PacketDistributor.sendToServer(new ModPayloads.TimeFlow(this.menu.containerId, 1)))
                .bounds(realmPanelX + REALM_WIDTH - REALM_TIME_SIDE_INSET - REALM_TIME_BUTTON_SIZE,
                        timeControlY, REALM_TIME_BUTTON_SIZE, REALM_TIME_BUTTON_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.immortalstorage.terminal.time_faster")))
                .build());
        this.autoFurnaceFuelButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFuelLabel(), button -> requestMenuButton(
                                XianqiaoStorageMenu.AUTO_FURNACE_FUEL_BUTTON))
                .bounds(this.leftPos + 92, this.topPos + this.imageHeight - 179, 76, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.auto_fuel_hint")))
                .build());
        this.autoFurnaceFillButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFillLabel(), button -> requestMenuButton(
                                XianqiaoStorageMenu.AUTO_FURNACE_FILL_BUTTON))
                .bounds(this.leftPos + 16, this.topPos + this.imageHeight - 179, 72, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.auto_fill_hint")))
                .build());
        this.handAutoRefillButton = this.addRenderableWidget(Button.builder(
                        handAutoRefillLabel(), button -> requestMenuButton(
                                XianqiaoStorageMenu.HAND_AUTO_REFILL_BUTTON))
                .bounds(this.leftPos + this.imageWidth + REALM_GAP + 8, this.topPos + 120, 88, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.hand_refill_hint")))
                .build());
        this.magnetButton = this.addRenderableWidget(Button.builder(magnetLabel(),
                        button -> requestMenuButton(XianqiaoStorageMenu.MAGNET_BUTTON))
                .bounds(this.leftPos + this.imageWidth + REALM_GAP + 8, this.topPos + 139, 88, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.magnet_hint"))).build());
        int inventoryActionsY = this.topPos + TerminalLayout.inventoryY(this.imageHeight)
                - (TerminalLayout.SLOT_SIZE + TerminalInventoryActionButton.SIZE) / 2;
        int inventoryActionsX = this.leftPos + this.imageWidth - 30;
        this.sortInventoryButton = this.addRenderableWidget(new TerminalInventoryActionButton(
                inventoryActionsX, inventoryActionsY, TerminalInventoryActionButton.Icon.WRENCH,
                Component.translatable("container.immortalstorage.terminal.inventory_sort"),
                Tooltip.create(Component.translatable("container.immortalstorage.terminal.inventory_sort_hint")),
                button -> requestMenuButton(XianqiaoStorageMenu.SORT_PLAYER_INVENTORY_BUTTON)));
        this.depositInventoryButton = this.addRenderableWidget(new TerminalInventoryActionButton(
                inventoryActionsX + TerminalInventoryActionButton.SPACING, inventoryActionsY,
                TerminalInventoryActionButton.Icon.DEPOSIT,
                Component.translatable("container.immortalstorage.terminal.inventory_deposit"),
                Tooltip.create(Component.translatable("container.immortalstorage.terminal.inventory_deposit_hint")),
                button -> requestMenuButton(XianqiaoStorageMenu.DEPOSIT_PLAYER_INVENTORY_BUTTON)));
        this.withdrawInventoryButton = this.addRenderableWidget(new TerminalInventoryActionButton(
                inventoryActionsX + TerminalInventoryActionButton.SPACING * 2, inventoryActionsY,
                TerminalInventoryActionButton.Icon.WITHDRAW,
                Component.translatable("container.immortalstorage.terminal.inventory_withdraw"),
                Tooltip.create(Component.translatable("container.immortalstorage.terminal.inventory_withdraw_hint")),
                button -> requestMenuButton(XianqiaoStorageMenu.WITHDRAW_FILTERED_BUTTON)));
        updateRealmWidgets();
    }

    private Component magnetLabel() { return Component.translatable(this.menu.getData().isMagnetEnabled()
            ? "container.immortalstorage.terminal.magnet_on"
            : "container.immortalstorage.terminal.magnet_off"); }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderTerminalChrome(graphics, true);
        renderStorageSlotsClipped(graphics, storageSlotCountInternal());
        renderFluidStorage(graphics);
        renderExternalResourceStorage(graphics);
        if (this.furnaceVisible) {
            VanillaGuiPainter.terminalFurnaceModule(graphics, this.leftPos, this.topPos, this.imageHeight,
                    this.menu.getFurnaceLitProgress(), new int[] {
                            this.menu.getFurnaceBurnProgress(0),
                            this.menu.getFurnaceBurnProgress(1),
                            this.menu.getFurnaceBurnProgress(2)
                    }, this.menu.isFurnaceLit());
        }
        if (this.smithingVisible) VanillaGuiPainter.terminalSmithingModule(
                graphics, this.leftPos, this.topPos, this.imageHeight);
        if (this.stonecutterVisible) {
            VanillaGuiPainter.terminalStonecutterModule(
                    graphics, this.leftPos, this.topPos, this.imageHeight);
            this.stonecutterGui.render(graphics, this, stonecutterAbsoluteSlotY(),
                    mouseX, mouseY, this.menu.stonecutterSelectedIndex(), this.menu.stonecutterRecipes());
        }
        if (this.realmVisible) {
            int panelX = this.leftPos + this.imageWidth + REALM_GAP;
            VanillaGuiPainter.panel(graphics, panelX, this.topPos + 18, REALM_WIDTH, REALM_HEIGHT);
            VanillaGuiPainter.moduleTitle(graphics, this.font,
                    Component.translatable("container.immortalstorage.terminal.management").getString(),
                    panelX + 8, this.topPos + 26);
            Component timeScaleTitle = Component.translatable(
                    "container.immortalstorage.terminal.time_scale_title");
            graphics.drawString(this.font, timeScaleTitle,
                    panelX + (REALM_WIDTH - this.font.width(timeScaleTitle)) / 2,
                    this.topPos + REALM_TIME_LABEL_Y, 0xFF404040, false);
            String timeScale = String.format(java.util.Locale.ROOT, "%.1fx", this.menu.getData().getTimeScale());
            Component timeScaleValue = Component.literal(timeScale);
            graphics.drawString(this.font, timeScaleValue,
                    panelX + (REALM_WIDTH - this.font.width(timeScaleValue)) / 2,
                    this.topPos + REALM_TIME_ROW_Y
                            + (REALM_TIME_BUTTON_SIZE - this.font.lineHeight) / 2,
                    0xFF404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.stonecutterVisible && this.stonecutterGui.renderTooltip(
                graphics, this, stonecutterAbsoluteSlotY(), mouseX, mouseY,
                this.menu.stonecutterRecipes())) {
            return;
        }
        Optional<FluidHover> fluidHover = immortalstorage$getFluidAt(mouseX, mouseY);
        if (fluidHover.isPresent()) {
            FluidHover hover = fluidHover.get();
            graphics.renderTooltip(this.font,
                    List.of(
                            hover.stack().getHoverName(),
                            Component.literal(TerminalFluidAmountFormatter.exactBuckets(hover.amountMb())),
                            Component.literal(TerminalFluidAmountFormatter.exactMillibuckets(hover.amountMb()))),
                    Optional.empty(), mouseX, mouseY);
        } else {
            var external = externalCellAt(mouseX, mouseY);
            if (external != null) {
                var definition = com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog
                        .definition(external.key());
                graphics.renderTooltip(this.font, List.of(
                        com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog.displayName(external.key()),
                        Component.literal(Long.toString(external.amount())
                                + (definition.unit().isEmpty() ? "" : " " + definition.unit()))),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
            if (!renderExactStorageTooltip(graphics, mouseX, mouseY)) this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, TerminalLayout.TITLE_X, TerminalLayout.TITLE_Y,
                0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
        renderStorageAmountOverlays(graphics);
        renderFluidAmountOverlays(graphics);
        renderExternalResourceAmountOverlays(graphics);
    }

    @Override
    protected long storageAmountAt(int relativeIndex, Slot slot) {
        return this.menu.aggregatedAmountAtSlot(relativeIndex);
    }

    @Override
    protected String storageAmountLabel(int relativeIndex, long amount) {
        return cachedItemAmountLabel(relativeIndex, amount);
    }

    @Override
    protected int storageAmountLabelWidth(int relativeIndex, long amount, String label) {
        return this.cachedItemAmountWidths[relativeIndex];
    }

    @Override
    protected float storageAmountScale(int relativeIndex, long amount, int labelWidth) {
        return Math.min(0.666F, 15.0F / Math.max(1, labelWidth));
    }

    @Override
    protected float storageAmountBottomOffset(int relativeIndex, long amount) {
        return 17.0F;
    }

    @Override
    protected int totalStorageRows() {
        return this.menu.getTotalRows();
    }

    @Override
    protected void onViewportChanged(int rows, int baseRow) {
        super.onViewportChanged(rows, baseRow);
    }

    @Override
    protected int storageSlotStart() {
        return 0;
    }

    @Override
    protected int storageSlotCount() {
        return Math.min(XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS, this.menu.slots.size());
    }

    @Override
    protected int craftingSlotStart() {
        return XianqiaoStorageMenu.CRAFT_START;
    }

    @Override
    protected int craftingResultSlot() {
        return XianqiaoStorageMenu.CRAFT_RESULT_SLOT;
    }

    @Override protected int craftMatchComponentsButtonId() {
        return XianqiaoStorageMenu.CRAFT_MATCH_COMPONENTS_BUTTON;
    }

    @Override protected boolean craftMatchComponentsEnabled() {
        return this.menu.isCraftAutofillMatchComponents();
    }

    @Override
    protected int playerInventoryStart() {
        return XianqiaoStorageMenu.PLAYER_START;
    }

    @Override
    protected int visualSlotX(int menuIndex, Slot slot) {
        if (menuIndex == XianqiaoStorageMenu.FURNACE_FUEL_SLOT) return TerminalLayout.FURNACE_FUEL_X;
        if (menuIndex == XianqiaoStorageMenu.FURNACE_PLUGIN_SLOT) return TerminalLayout.FURNACE_PLUGIN_X;
        if (XianqiaoStorageMenu.isFurnaceInputSlotIndex(menuIndex)) return TerminalLayout.FURNACE_INPUT_X;
        if (XianqiaoStorageMenu.isFurnaceResultSlotIndex(menuIndex)) return TerminalLayout.FURNACE_RESULT_X;
        int visual = super.visualSlotX(menuIndex, slot);
        return menuIndex >= XianqiaoStorageMenu.PLAYER_START ? visual + 22 : visual;
    }

    @Override
    protected int visualSlotY(int menuIndex, Slot slot) {
        if (XianqiaoStorageMenu.isSmithingSlotIndex(menuIndex)) {
            return TerminalLayout.craftGridY(this.imageHeight) + 18;
        }
        if (XianqiaoStorageMenu.isStonecutterSlotIndex(menuIndex)) {
            return TerminalLayout.craftGridY(this.imageHeight) + 18;
        }
        if (menuIndex >= XianqiaoStorageMenu.ARMOR_START && menuIndex < XianqiaoStorageMenu.ARMOR_END) {
            return TerminalLayout.inventoryY(this.imageHeight)
                    + (menuIndex - XianqiaoStorageMenu.ARMOR_START) * TerminalLayout.SLOT_PITCH;
        }
        if (menuIndex == XianqiaoStorageMenu.FURNACE_FUEL_SLOT) return TerminalLayout.furnaceFuelY(this.imageHeight);
        if (menuIndex == XianqiaoStorageMenu.FURNACE_PLUGIN_SLOT) return TerminalLayout.furnacePluginY(this.imageHeight);
        int channel = XianqiaoStorageMenu.furnaceChannelForSlot(menuIndex);
        if (XianqiaoStorageMenu.isFurnaceInputSlotIndex(menuIndex)) {
            return TerminalLayout.furnaceInputY(this.imageHeight, channel);
        }
        if (XianqiaoStorageMenu.isFurnaceResultSlotIndex(menuIndex)) {
            return TerminalLayout.furnaceResultY(this.imageHeight, channel);
        }
        return super.visualSlotY(menuIndex, slot);
    }

    @Override
    protected boolean shouldRenderMenuSlot(int menuIndex) {
        if (XianqiaoStorageMenu.isSmithingSlotIndex(menuIndex)) return this.smithingVisible;
        if (XianqiaoStorageMenu.isStonecutterSlotIndex(menuIndex)) return this.stonecutterVisible;
        if (XianqiaoStorageMenu.isFurnaceSlotIndex(menuIndex)) {
            return this.furnaceVisible;
        }
        return super.shouldRenderMenuSlot(menuIndex);
    }

    @Override
    protected void setLocalQuery(TerminalQuery query) {
        this.menu.setTerminalQuery(query);
    }

    @Override
    protected void setLocalActiveModule(int module) {
        this.menu.applyClientActiveModule(module);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.stonecutterVisible && button == 0
                && this.stonecutterGui.mouseClicked(mouseX, mouseY, this,
                stonecutterAbsoluteSlotY(), this.menu.stonecutterRecipes())) {
            return true;
        }
        if (button == 0 || button == 1) {
            FluidCell cell = fluidCellAt(mouseX, mouseY);
            if (cell != null) {
                if (cell.entry() != null) {
                    PacketDistributor.sendToServer(new ModPayloads.TerminalFluidEntryAction(
                            this.menu.containerId, this.menu.fluidRevision(),
                            button == 1 ? 0L : cell.entry().entryId(), button == 1));
                    consumeCustomSlotRelease(button);
                    return true;
                }
                if (button == 1 && this.menu.displayedEntryAtSlot(cell.index()) == null
                        && carriedIsFluidContainer()) {
                    PacketDistributor.sendToServer(new ModPayloads.TerminalFluidEntryAction(
                            this.menu.containerId, this.menu.fluidRevision(), 0L, true));
                    consumeCustomSlotRelease(button);
                    return true;
                }
                var external = this.menu.displayedExternalEntryAtIndex(cell.index());
                if (external != null) {
                    PacketDistributor.sendToServer(new ModPayloads.TerminalExternalResourceEntryAction(
                            this.menu.containerId, this.menu.externalRevision(),
                            button == 1 ? 0L : external.entryId(), button == 1));
                    consumeCustomSlotRelease(button);
                    return true;
                }
                if (button == 1 && this.menu.displayedEntryAtSlot(cell.index()) == null
                        && carriedIsChemicalContainer()) {
                    PacketDistributor.sendToServer(new ModPayloads.TerminalExternalResourceEntryAction(
                            this.menu.containerId, this.menu.externalRevision(), 0L, true));
                    consumeCustomSlotRelease(button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public Optional<FluidHover> immortalstorage$getFluidAt(double mouseX, double mouseY) {
        FluidCell cell = fluidCellAt(mouseX, mouseY);
        if (cell == null || cell.entry() == null) return Optional.empty();
        return Optional.of(new FluidHover(cell.entry().displayStack(), cell.entry().amountMb(), cell.bounds()));
    }

    @Override
    public List<FluidHover> immortalstorage$getVisibleFluids() {
        List<FluidHover> visible = new ArrayList<>();
        var window = visibleBufferedRows();
        for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
            for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                int index = row * TerminalLayout.COLUMNS + column;
                var entry = this.menu.displayedFluidEntryAtIndex(index);
                if (entry == null) continue;
                Rect2i bounds = fluidBounds(index);
                if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                    visible.add(new FluidHover(entry.displayStack(), entry.amountMb(), bounds));
                }
            }
        }
        return List.copyOf(visible);
    }

    @Override
    public List<Rect2i> immortalstorage$getExtraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        int railHeight = TerminalLayout.railHeight(4);
        areas.add(new Rect2i(this.leftPos + TerminalLayout.MODULE_RAIL_X, this.topPos,
                TerminalLayout.TAB_WIDTH, railHeight));
        if (this.realmVisible) {
            areas.add(new Rect2i(this.leftPos + this.imageWidth + REALM_GAP, this.topPos + 18,
                    REALM_WIDTH, REALM_HEIGHT));
        }
        return List.copyOf(areas);
    }

    private int storageSlotCountInternal() {
        int buffered = this.menu.bufferedRowCount() * TerminalLayout.COLUMNS;
        return Math.min(buffered, this.menu.slots.size());
    }

    private void renderFluidStorage(GuiGraphics graphics) {
        Rect2i clip = storageBounds();
        boolean needsScissor = fractionalScrollOffset() != 0;
        if (needsScissor) {
            graphics.enableScissor(clip.getX(), clip.getY(),
                    clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
        }
        var window = visibleBufferedRows();
        for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
            for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                int index = row * TerminalLayout.COLUMNS + column;
                var entry = this.menu.displayedFluidEntryAtIndex(index);
                if (entry == null) continue;
                int x = fluidVisualX(index);
                int y = fluidVisualY(index);
                renderFluidSprite(graphics, entry.displayStack(), x, y);
            }
        }
        if (needsScissor) {
            graphics.disableScissor();
        }
    }

    private void renderExternalResourceStorage(GuiGraphics graphics) {
        Rect2i clip = storageBounds();
        graphics.enableScissor(clip.getX(), clip.getY(),
                clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
        try {
            var window = visibleBufferedRows();
            for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
                for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                    int index = row * TerminalLayout.COLUMNS + column;
                    var entry = this.menu.displayedExternalEntryAtIndex(index);
                    if (entry == null) continue;
                    var definition = com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog.definition(entry.key());
                    if (definition.solidColor()) {
                        graphics.fill(fluidVisualX(index), fluidVisualY(index),
                                fluidVisualX(index) + 16, fluidVisualY(index) + 16,
                                definition.color());
                    } else {
                        graphics.blit(definition.icon(), fluidVisualX(index), fluidVisualY(index),
                                0.0F, 0.0F, 16, 16, 16,
                                externalTextureHeight(entry.key()));
                    }
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private static int externalTextureHeight(
            com.immortalstorage.core.resource.ResourceChannelKey key) {
        return switch (key.channel()) {
            case "botania_mana" -> 512;
            case "ars_nouveau_source" -> 320;
            default -> 16;
        };
    }

    private void renderExternalResourceAmountOverlays(GuiGraphics graphics) {
        Rect2i clip = storageBounds();
        boolean needsScissor = fractionalScrollOffset() != 0;
        if (needsScissor) enableStorageContentScissor(graphics, clip);
        try {
            var window = visibleBufferedRows();
            for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
                for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                    int index = row * TerminalLayout.COLUMNS + column;
                    var entry = this.menu.displayedExternalEntryAtIndex(index);
                    if (entry == null || entry.amount() <= 0L) continue;
                    renderFluidAmount(graphics, index, entry.amount(),
                            fluidVisualX(index) - this.leftPos, fluidVisualY(index) - this.topPos);
                }
            }
        } finally {
            if (needsScissor) graphics.disableScissor();
        }
    }

    private void renderFluidAmountOverlays(GuiGraphics graphics) {
        Rect2i clip = storageBounds();
        boolean needsScissor = fractionalScrollOffset() != 0;
        if (needsScissor) {
            enableStorageContentScissor(graphics, clip);
        }
        try {
            var window = visibleBufferedRows();
            for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
                for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                    int index = row * TerminalLayout.COLUMNS + column;
                    var entry = this.menu.displayedFluidEntryAtIndex(index);
                    if (entry == null || entry.amountMb() <= 0L) continue;
                    renderFluidAmount(graphics, index, entry.amountMb(),
                            fluidVisualX(index) - this.leftPos, fluidVisualY(index) - this.topPos);
                }
            }
            graphics.flush();
        } finally {
            if (needsScissor) {
                graphics.disableScissor();
            }
        }
    }

    private void renderFluidSprite(GuiGraphics graphics, FluidStack stack, int x, int y) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluidType());
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null || this.minecraft == null) return;
        TextureAtlasSprite sprite = this.minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
        int tint = extensions.getTintColor(stack);
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float red = ((tint >>> 16) & 0xFF) / 255.0F;
        float green = ((tint >>> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        graphics.setColor(red, green, blue, alpha);
        graphics.blit(x, y, 0, TerminalLayout.SLOT_SIZE, TerminalLayout.SLOT_SIZE, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderFluidAmount(GuiGraphics graphics, int viewIndex, long amountMb, int x, int y) {
        String label = cachedFluidAmountLabel(viewIndex, amountMb);
        int width = this.cachedFluidAmountWidths[viewIndex];
        float scale = Math.min(0.666F, 15.0F / width);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17.0F, y + 17.0F, TerminalLayout.STORAGE_AMOUNT_Z);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, label, -width, -this.font.lineHeight, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private int fluidVisualX(int index) {
        return this.leftPos + TerminalLayout.STORAGE_X
                + index % TerminalLayout.COLUMNS * TerminalLayout.SLOT_PITCH;
    }

    private int fluidVisualY(int index) {
        int visualRow = index / TerminalLayout.COLUMNS + this.menu.fluidBufferedBaseRow() - baseRow();
        return this.topPos + TerminalLayout.STORAGE_Y
                + visualRow * TerminalLayout.SLOT_PITCH - visualFractionalOffset();
    }

    private Rect2i fluidBounds(int index) {
        return storageCellBounds(index);
    }

    private FluidCell fluidCellAt(double mouseX, double mouseY) {
        StorageViewCell cell = storageCellAt(mouseX, mouseY);
        if (cell == null) return null;
        return new FluidCell(cell.viewIndex(),
                this.menu.displayedFluidEntryAtIndex(cell.viewIndex()), cell.bounds());
    }

    private com.immortalstorage.immortalstorage.api.storage.terminal.TerminalExternalResourceEntry externalCellAt(
            double mouseX, double mouseY) {
        StorageViewCell cell = storageCellAt(mouseX, mouseY);
        return cell == null ? null : this.menu.displayedExternalEntryAtIndex(cell.viewIndex());
    }

    private String cachedItemAmountLabel(int viewIndex, long amount) {
        if (this.cachedItemAmountLabels[viewIndex] == null || this.cachedItemAmounts[viewIndex] != amount) {
            String label = TerminalAmountFormatter.format(amount);
            this.cachedItemAmounts[viewIndex] = amount;
            this.cachedItemAmountLabels[viewIndex] = label;
            this.cachedItemAmountWidths[viewIndex] = this.font.width(label);
        }
        return this.cachedItemAmountLabels[viewIndex];
    }

    private String cachedFluidAmountLabel(int viewIndex, long amountMb) {
        if (this.cachedFluidAmountLabels[viewIndex] == null || this.cachedFluidAmounts[viewIndex] != amountMb) {
            String label = TerminalFluidAmountFormatter.format(amountMb);
            this.cachedFluidAmounts[viewIndex] = amountMb;
            this.cachedFluidAmountLabels[viewIndex] = label;
            this.cachedFluidAmountWidths[viewIndex] = Math.max(1, this.font.width(label));
        }
        return this.cachedFluidAmountLabels[viewIndex];
    }

    private boolean carriedIsFluidContainer() {
        ItemStack carried = this.menu.getCarried();
        return !carried.isEmpty()
                && FluidUtil.getFluidHandler(carried.copyWithCount(1)).isPresent();
    }

    private boolean carriedIsChemicalContainer() {
        ItemStack carried = this.menu.getCarried();
        return !carried.isEmpty()
                && TerminalExternalResourceCompatHooks.isContainer(carried.copyWithCount(1));
    }

    private void updateRealmWidgets() {
        if (this.tribulateButton != null) {
            this.tribulateButton.visible = this.realmVisible;
            this.tribulateButton.active = this.realmVisible && !this.menu.getData().isTribulationActive();
        }
        boolean timeVisible = this.realmVisible && this.menu.getData().getStage() >= 7;
        if (this.slowerTimeButton != null) {
            this.slowerTimeButton.visible = timeVisible;
            this.slowerTimeButton.active = timeVisible;
        }
        if (this.fasterTimeButton != null) {
            this.fasterTimeButton.visible = timeVisible;
            this.fasterTimeButton.active = timeVisible;
        }
        if (this.dayNightButton != null) {
            this.dayNightButton.visible = this.realmVisible;
            this.dayNightButton.active = this.realmVisible;
            this.dayNightButton.setMessage(dayNightLabel());
        }
        if (this.weatherButton != null) {
            this.weatherButton.visible = this.realmVisible;
            this.weatherButton.active = this.realmVisible;
            this.weatherButton.setMessage(weatherLabel());
        }
        if (this.autoFurnaceFuelButton != null) {
            this.autoFurnaceFuelButton.visible = this.furnaceVisible;
            this.autoFurnaceFuelButton.active = this.furnaceVisible;
            this.autoFurnaceFuelButton.setMessage(autoFurnaceFuelLabel());
        }
        if (this.autoFurnaceFillButton != null) {
            this.autoFurnaceFillButton.visible = this.furnaceVisible;
            this.autoFurnaceFillButton.active = this.furnaceVisible;
            this.autoFurnaceFillButton.setMessage(autoFurnaceFillLabel());
        }
        if (this.handAutoRefillButton != null) {
            this.handAutoRefillButton.visible = this.realmVisible;
            this.handAutoRefillButton.active = this.realmVisible;
            this.handAutoRefillButton.setMessage(handAutoRefillLabel());
        }
        if (this.magnetButton != null) {
            this.magnetButton.visible = this.realmVisible;
            this.magnetButton.active = this.realmVisible;
            this.magnetButton.setMessage(magnetLabel());
        }
    }

    private Component autoFurnaceFuelLabel() {
        return Component.translatable(this.menu.isFurnaceAutoConsume()
                ? "container.immortalstorage.terminal.auto_fuel_on"
                : "container.immortalstorage.terminal.auto_fuel_off");
    }

    private Component autoFurnaceFillLabel() {
        return Component.translatable(this.menu.isFurnaceAutoFill()
                ? "container.immortalstorage.terminal.auto_fill_on"
                : "container.immortalstorage.terminal.auto_fill_off");
    }

    private Component handAutoRefillLabel() {
        return Component.translatable(this.menu.isHandAutoRefill()
                ? "container.immortalstorage.terminal.hand_refill_on"
                : "container.immortalstorage.terminal.hand_refill_off");
    }

    private Component dayNightLabel() {
        return Component.translatable(this.menu.getData().isRealmDaytime()
                ? "container.immortalstorage.terminal.day"
                : "container.immortalstorage.terminal.night");
    }

    private Component weatherLabel() {
        return Component.translatable("container.immortalstorage.terminal.weather."
                + this.menu.getData().getRealmWeatherMode());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        reconcileServerViewport(this.menu.getVisibleRows(), this.menu.getBaseRow());
        int module = this.menu.getActiveModule();
        boolean moduleThreeOpen = this.smithingVisible || this.stonecutterVisible;
        if ((module == 0) != this.craftingVisible || (module == 1) != this.realmVisible
                || (module == 2) != this.furnaceVisible || (module == 3) != moduleThreeOpen
                || (module == 3 && this.smithingVisible != this.menu.isSmithingViewActive())) applyModuleState(module);
        updateRealmWidgets();
    }

    @Override
    protected int railControlOffset() {
        return TerminalLayout.railControlOffset(4);
    }

    @Override
    protected int adjustTerminalLeftPos(int centeredLeftPos) {
        if (!this.realmVisible) return centeredLeftPos;
        return TerminalLayout.compositeLeft(this.width, this.imageWidth,
                TerminalLayout.MODULE_RAIL_X, REALM_GAP, REALM_WIDTH);
    }

    private void selectModule(int module) {
        int next = this.menu.getActiveModule() == module ? -1 : module;
        requestMenuButton(module);
        this.menu.applyClientActiveModule(next);
        applyModuleState(next);
    }

    private void applyModuleState(int module) {
        boolean realmChanged = this.realmVisible != (module == 1);
        this.realmVisible = module == 1;
        this.furnaceVisible = module == 2;
        this.smithingVisible = module == 3 && this.menu.isSmithingViewActive();
        this.stonecutterVisible = module == 3 && !this.menu.isSmithingViewActive();
        boolean rebuilt = setWorkspaceState(module == 0, module == 0 || module == 2 || module == 3);
        if (realmChanged && !rebuilt && this.minecraft != null) {
            this.rebuildWidgets();
        }
        updateStonecutterToggle();
        updateRealmWidgets();
    }

    private int stonecutterSlotY() {
        return TerminalLayout.craftGridY(this.imageHeight) + 18;
    }

    private int stonecutterAbsoluteSlotY() {
        return this.topPos + stonecutterSlotY();
    }

    private Component stonecutterToggleLabel() {
        return Component.translatable(this.menu.isSmithingViewActive()
                ? "container.immortalstorage.terminal.stonecutter_switch"
                : "container.immortalstorage.terminal.smithing_switch");
    }

    private void updateStonecutterToggle() {
        if (this.stonecutterToggleButton == null) return;
        boolean moduleThree = this.smithingVisible || this.stonecutterVisible;
        this.stonecutterToggleButton.visible = moduleThree && this.menu.isSmithingUnlocked();
        this.stonecutterToggleButton.active = this.stonecutterToggleButton.visible;
        this.stonecutterToggleButton.setMessage(stonecutterToggleLabel());
        if (!moduleThree) this.stonecutterGui.reset();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.stonecutterVisible && this.stonecutterGui.mouseDragged(
                mouseY, stonecutterAbsoluteSlotY(), this.menu.stonecutterRecipeCount())) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.stonecutterVisible && this.stonecutterGui.mouseScrolled(
                scrollY, this.menu.stonecutterRecipeCount())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private record FluidCell(int index,
                             com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidEntry entry,
                             Rect2i bounds) {}
}
