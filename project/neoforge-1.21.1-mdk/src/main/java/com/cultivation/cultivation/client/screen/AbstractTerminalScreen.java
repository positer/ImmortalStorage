package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.network.ModPayloads;
import com.cultivation.cultivation.api.storage.terminal.TerminalQuery;
import com.cultivation.cultivation.api.storage.terminal.TerminalAction;
import com.cultivation.cultivation.api.storage.terminal.StorageTerminalView;
import com.cultivation.cultivation.api.storage.terminal.TerminalViewport;
import com.cultivation.cultivation.menu.custom.KongqiaoMenu;
import com.cultivation.cultivation.menu.custom.XianqiaoStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractTerminalScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements TerminalScreenAccess {
    private static final long SCROLL_ANIMATION_MS = 100L;

    protected EditBox searchBox;
    protected Button fewerRowsButton;
    protected Button moreRowsButton;
    protected Button sortButton;
    protected Button sortDirectionButton;
    protected Button craftClearButton;
    protected Button craftMatchComponentsButton;
    protected int visibleRows;
    protected boolean craftingVisible;
    protected boolean workspaceExpanded;

    protected double scrollPx;
    protected double scrollStartPx;
    protected double targetScrollPx;
    private long scrollStartTime;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;
    private int handledVisualSlotButton = -1;
    private TerminalQuery.SortOrder sortOrder = TerminalQuery.SortOrder.AMOUNT;
    private TerminalQuery.SortDirection sortDirection = TerminalQuery.SortDirection.DESCENDING;
    private String retainedSearch = "";
    private boolean applyingExternalSearch;
    private final IdentityHashMap<Slot, Integer> slotMenuIndices = new IdentityHashMap<>();
    private final Map<Long, int[]> slotIndicesByLogicalPosition = new HashMap<>();
    private Rect2i cachedStorageBounds;
    private TerminalViewport.BufferedRowWindow cachedVisibleBufferedRows =
            new TerminalViewport.BufferedRowWindow(0, 0);
    private int cachedWindowBufferBase = Integer.MIN_VALUE;
    private int cachedWindowBufferedRows = Integer.MIN_VALUE;
    private int cachedWindowViewBase = Integer.MIN_VALUE;
    private int cachedWindowVisibleRows = Integer.MIN_VALUE;
    private int cachedWindowFractionalOffset = Integer.MIN_VALUE;

    protected AbstractTerminalScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = TerminalLayout.WIDTH;
        configureLayout(false);
    }

    @Override
    public void onClose() {
        Screen returnScreen = TerminalReturnNavigation.take();
        super.onClose();
        if (returnScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(returnScreen);
        }
    }

    @Override
    protected void init() {
        configureLayout(this.workspaceExpanded);
        super.init();
        this.leftPos = adjustTerminalLeftPos(this.leftPos);
        rebuildSlotIndexCaches();
        this.cachedStorageBounds = TerminalLayout.storageBounds(this, this.visibleRows);
        this.searchBox = new EditBox(this.font,
                this.leftPos + TerminalLayout.SEARCH_X,
                this.topPos + TerminalLayout.SEARCH_Y,
                TerminalLayout.SEARCH_WIDTH,
                TerminalLayout.SEARCH_HEIGHT,
                Component.translatable("container.cultivation.terminal.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Component.translatable("container.cultivation.terminal.search_hint"));
        this.searchBox.setResponder(value -> { });
        this.searchBox.setValue(this.retainedSearch);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        this.craftClearButton = this.addRenderableWidget(Button.builder(Component.literal("×"),
                        button -> requestMenuButton(10))
                .bounds(this.leftPos + 81, this.topPos + this.imageHeight - 159, 10, 10)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.cultivation.terminal.clear_crafting")))
                .build());
        this.craftClearButton.visible = this.craftingVisible;
        this.craftMatchComponentsButton = this.addRenderableWidget(Button.builder(
                        craftMatchComponentsLabel(), button -> requestMenuButton(craftMatchComponentsButtonId()))
                .bounds(this.leftPos + 81, this.topPos + this.imageHeight - 147, 10, 10)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.cultivation.terminal.craft_match_components_hint")))
                .build());
        this.craftMatchComponentsButton.visible = this.craftingVisible;

        if (this.menu instanceof StorageTerminalView terminal) {
            this.sortOrder = terminal.query().sortOrder();
            this.sortDirection = terminal.query().sortDirection();
        }

        int railX = this.leftPos + TerminalLayout.railControlX();
        int controlY = this.topPos + railControlOffset();
        this.sortButton = this.addRenderableWidget(Button.builder(sortLabel(), button -> cycleSort())
                .bounds(railX, controlY, TerminalLayout.CONTROL_SIZE, TerminalLayout.CONTROL_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(sortTooltip()))
                .build());
        this.sortDirectionButton = this.addRenderableWidget(Button.builder(directionLabel(), button -> toggleDirection())
                .bounds(railX, controlY + TerminalLayout.CONTROL_SIZE,
                        TerminalLayout.CONTROL_SIZE, TerminalLayout.CONTROL_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.cultivation.terminal.sort_direction")))
                .build());
        this.fewerRowsButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> changeRows(-1))
                .bounds(railX, controlY + TerminalLayout.CONTROL_SIZE * 2,
                        TerminalLayout.CONTROL_SIZE, TerminalLayout.CONTROL_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.cultivation.terminal.rows_less")))
                .build());
        this.moreRowsButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> changeRows(1))
                .bounds(railX, controlY + TerminalLayout.CONTROL_SIZE * 3,
                        TerminalLayout.CONTROL_SIZE, TerminalLayout.CONTROL_SIZE)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("container.cultivation.terminal.rows_more")))
                .build());
        updateRowButtons();
        synchronizeViewport();
    }

    protected final void configureLayout(boolean expanded) {
        this.workspaceExpanded = expanded;
        this.visibleRows = TerminalLayout.effectiveRows(TerminalLayout.configuredRows(),
                TerminalLayout.currentScreenHeight(), expanded, maximumContentRows());
        this.imageHeight = TerminalLayout.imageHeight(this.visibleRows, expanded);
        this.inventoryLabelX = 30;
        this.inventoryLabelY = TerminalLayout.inventoryY(this.imageHeight) - 11;
    }

    /** Xianqiao is directory-backed; finite Kongqiao overrides this with its physical row count. */
    protected int maximumContentRows() {
        return TerminalLayout.MAX_ROWS;
    }

    protected final void setCraftingVisible(boolean visible) {
        int activeModule = visible ? 0 : -1;
        if (this.menu instanceof StorageTerminalView terminal && !terminal.isCraftingUnlocked()) {
            activeModule = -1;
        }
        setLocalActiveModule(activeModule);
        setWorkspaceState(activeModule == 0, activeModule == 0);
    }

    protected final boolean setWorkspaceState(boolean craftVisible, boolean expanded) {
        if (this.craftingVisible == craftVisible && this.workspaceExpanded == expanded) return false;
        this.craftingVisible = craftVisible;
        this.workspaceExpanded = expanded;
        if (this.minecraft != null) {
            if (this.searchBox != null) {
                this.retainedSearch = this.searchBox.getValue();
            }
            this.rebuildWidgets();
        }
        return true;
    }

    /** Allows an embedded side panel to center the complete composite without changing terminal geometry. */
    protected int adjustTerminalLeftPos(int centeredLeftPos) {
        return centeredLeftPos;
    }

    protected final void requestMenuButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    protected abstract int craftMatchComponentsButtonId();

    protected abstract boolean craftMatchComponentsEnabled();

    private Component craftMatchComponentsLabel() {
        return Component.literal(craftMatchComponentsEnabled() ? "N" : "n");
    }

    protected final void renderTerminalChrome(GuiGraphics graphics, boolean xianqiao) {
        tickScrollAnimation();
        VanillaGuiPainter.terminalPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                this.visibleRows, totalStorageRows(), this.workspaceExpanded, this.craftingVisible,
                xianqiao, scrollFraction());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.craftMatchComponentsButton != null) {
            this.craftMatchComponentsButton.setMessage(craftMatchComponentsLabel());
        }
        if (this.searchBox == null || this.searchBox.isFocused()) {
            return;
        }
        RecipeViewerSearchSync.externalText().filter(text -> !text.equals(this.searchBox.getValue())).ifPresent(text -> {
            this.applyingExternalSearch = true;
            this.retainedSearch = text;
            this.searchBox.setValue(text);
            this.applyingExternalSearch = false;
            sendQuery(new TerminalQuery(text, this.sortOrder, this.sortDirection), false);
        });
    }

    protected final void renderStorageSlotsClipped(GuiGraphics graphics, int storageSlotCount) {
        Rect2i clip = storageBounds();
        boolean needsScissor = fractionalScrollOffset() != 0;
        if (needsScissor) {
            graphics.enableScissor(clip.getX(), clip.getY(),
                    clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
        }
        TerminalViewport.BufferedRowWindow window = visibleBufferedRows();
        int available = Math.max(0, Math.min(storageSlotCount,
                this.menu.slots.size() - storageSlotStart()));
        for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
            for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                int relative = row * TerminalLayout.COLUMNS + column;
                if (relative >= available) break;
                int menuIndex = storageSlotStart() + relative;
                Slot slot = this.menu.slots.get(menuIndex);
                if (slot.isActive()) {
                    VanillaGuiPainter.slot(graphics, this.leftPos + visualSlotX(menuIndex, slot),
                            this.topPos + visualSlotY(menuIndex, slot), true);
                }
            }
        }
        if (needsScissor) {
            graphics.disableScissor();
        }
    }

    /**
     * Draws storage amounts only after the complete container item pass. Vanilla
     * composes a slot item at Z=250 and its decorations at Z=300; submitting a
     * terminal's long-count text from {@link #renderSlot} can therefore leave it
     * in the same buffered layer as a later item model. This dedicated pass
     * commits those buffers first and keeps every terminal amount at a stable
     * foreground depth while preserving smooth-scroll clipping.
     */
    protected final void renderStorageAmountOverlays(GuiGraphics graphics) {
        graphics.flush();
        Rect2i clip = storageBounds();
        boolean needsScissor = fractionalScrollOffset() != 0;
        if (needsScissor) {
            graphics.enableScissor(clip.getX(), clip.getY(),
                    clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
        }
        try {
            TerminalViewport.BufferedRowWindow window = visibleBufferedRows();
            int available = Math.max(0, Math.min(storageSlotCount(),
                    this.menu.slots.size() - storageSlotStart()));
            for (int row = window.fromInclusive(); row < window.toExclusive(); row++) {
                for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                    int relative = row * TerminalLayout.COLUMNS + column;
                    if (relative >= available) break;
                    int menuIndex = storageSlotStart() + relative;
                    Slot slot = this.menu.slots.get(menuIndex);
                    if (!slot.isActive() || slot.getItem().isEmpty()) continue;
                    long amount = storageAmountAt(relative, slot);
                    if (amount <= 1L) continue;
                    int x = visualSlotX(menuIndex, slot);
                    int y = visualSlotY(menuIndex, slot);
                    if (!intersectsStorageViewport(this.leftPos + x, this.topPos + y)) continue;
                    String label = storageAmountLabel(relative, amount);
                    int width = storageAmountLabelWidth(relative, amount, label);
                    float scale = storageAmountScale(relative, amount, width);
                    graphics.pose().pushPose();
                    graphics.pose().translate(x + 17.0F,
                            y + storageAmountBottomOffset(relative, amount),
                            TerminalLayout.STORAGE_AMOUNT_Z);
                    graphics.pose().scale(scale, scale, 1.0F);
                    graphics.drawString(this.font, label, -width, -this.font.lineHeight,
                            0xFFFFFFFF, true);
                    graphics.pose().popPose();
                }
            }
            graphics.flush();
        } finally {
            if (needsScissor) {
                graphics.disableScissor();
            }
        }
    }

    protected long storageAmountAt(int relativeIndex, Slot slot) {
        return slot.getItem().getCount();
    }

    protected String storageAmountLabel(int relativeIndex, long amount) {
        return Long.toString(amount);
    }

    protected int storageAmountLabelWidth(int relativeIndex, long amount, String label) {
        return this.font.width(label);
    }

    protected float storageAmountScale(int relativeIndex, long amount, int labelWidth) {
        return 1.0F;
    }

    protected float storageAmountBottomOffset(int relativeIndex, long amount) {
        return 18.0F;
    }

    protected final TerminalViewport.BufferedRowWindow visibleBufferedRows() {
        int bufferBase = baseRow();
        int bufferedRows = Math.min(TerminalViewport.MAX_ROWS, this.visibleRows + 1);
        if (this.menu instanceof StorageTerminalView terminal) {
            bufferBase = terminal.bufferedBaseRow();
            bufferedRows = terminal.bufferedRowCount();
        }
        int viewBase = baseRow();
        int fractionalOffset = fractionalScrollOffset();
        if (bufferBase != this.cachedWindowBufferBase
                || bufferedRows != this.cachedWindowBufferedRows
                || viewBase != this.cachedWindowViewBase
                || this.visibleRows != this.cachedWindowVisibleRows
                || fractionalOffset != this.cachedWindowFractionalOffset) {
            this.cachedVisibleBufferedRows = TerminalViewport.intersectingBufferedRows(
                    bufferBase, bufferedRows, viewBase, this.visibleRows, fractionalOffset);
            this.cachedWindowBufferBase = bufferBase;
            this.cachedWindowBufferedRows = bufferedRows;
            this.cachedWindowViewBase = viewBase;
            this.cachedWindowVisibleRows = this.visibleRows;
            this.cachedWindowFractionalOffset = fractionalOffset;
        }
        return this.cachedVisibleBufferedRows;
    }

    protected final Rect2i storageBounds() {
        if (this.cachedStorageBounds == null) {
            this.cachedStorageBounds = TerminalLayout.storageBounds(this, this.visibleRows);
        }
        return this.cachedStorageBounds;
    }

    protected abstract int totalStorageRows();

    protected abstract int storageSlotStart();

    protected abstract int storageSlotCount();

    protected abstract int craftingSlotStart();

    protected abstract int craftingResultSlot();

    protected abstract int playerInventoryStart();

    protected abstract void setLocalQuery(TerminalQuery query);

    protected abstract void setLocalActiveModule(int module);

    protected int railControlOffset() {
        return TerminalLayout.railControlOffset(1);
    }

    protected void onBaseRowChanged(int baseRow) {
        onViewportChanged(this.visibleRows, baseRow);
    }

    protected void onViewportChanged(int rows, int baseRow) {
        if (this.menu instanceof KongqiaoMenu kongqiao) {
            kongqiao.setViewport(rows, baseRow);
        } else if (this.menu instanceof XianqiaoStorageMenu xianqiao) {
            xianqiao.setViewport(rows, baseRow);
        }
        PacketDistributor.sendToServer(new ModPayloads.SetTerminalViewport(rows, baseRow));
    }

    protected final int baseRow() {
        return TerminalLayout.baseRow(this.scrollPx);
    }

    protected final void setServerBaseRow(int baseRow) {
        double px = Math.max(0, baseRow) * (double) TerminalLayout.SLOT_PITCH;
        this.scrollPx = this.scrollStartPx = this.targetScrollPx = Math.min(px, maxScrollPx());
        this.scrollStartTime = System.currentTimeMillis();
    }

    protected final void reconcileServerViewport(int rows, int baseRow) {
        if (rows != this.visibleRows) {
            synchronizeViewport();
            return;
        }
        int clampedBase = TerminalViewport.clampBaseRow(baseRow, this.visibleRows, totalStorageRows());
        if (clampedBase != baseRow()) {
            onViewportChanged(this.visibleRows, baseRow());
        }
    }

    protected final int fractionalScrollOffset() {
        return TerminalLayout.fractionalScrollOffset(this.scrollPx);
    }

    protected final int visualFractionalOffset() {
        return fractionalScrollOffset();
    }

    protected int visualSlotX(int menuIndex, Slot slot) {
        if (isStorageMenuIndex(menuIndex)) {
            int relative = menuIndex - storageSlotStart();
            return TerminalLayout.STORAGE_X + relative % TerminalLayout.COLUMNS * TerminalLayout.SLOT_PITCH;
        }
        if (menuIndex >= craftingSlotStart() && menuIndex < craftingSlotStart() + 9) {
            int relative = menuIndex - craftingSlotStart();
            return TerminalLayout.craftInputSlotX(relative % 3);
        }
        if (menuIndex == craftingResultSlot()) {
            return TerminalLayout.craftResultSlotBounds(this.imageHeight).getX();
        }
        if (menuIndex >= playerInventoryStart()) {
            int relative = menuIndex - playerInventoryStart();
            return 8 + (relative < 27 ? relative : relative - 27) % 9 * TerminalLayout.SLOT_PITCH;
        }
        return slot.x;
    }

    protected int visualSlotY(int menuIndex, Slot slot) {
        if (isStorageMenuIndex(menuIndex)) {
            int relative = menuIndex - storageSlotStart();
            int effectiveBase = baseRow();
            if (this.menu instanceof StorageTerminalView terminal) {
                effectiveBase = terminal.bufferedBaseRow();
            }
            return TerminalLayout.STORAGE_Y
                    + TerminalLayout.visualStorageRow(relative, effectiveBase, baseRow()) * TerminalLayout.SLOT_PITCH
                    - visualFractionalOffset();
        }
        if (menuIndex >= craftingSlotStart() && menuIndex < craftingSlotStart() + 9) {
            int relative = menuIndex - craftingSlotStart();
            return TerminalLayout.craftInputSlotY(this.imageHeight, relative / 3);
        }
        if (menuIndex == craftingResultSlot()) {
            return TerminalLayout.craftResultSlotBounds(this.imageHeight).getY();
        }
        if (menuIndex >= playerInventoryStart()) {
            int relative = menuIndex - playerInventoryStart();
            return relative < 27
                    ? TerminalLayout.inventoryY(this.imageHeight) + relative / 9 * TerminalLayout.SLOT_PITCH
                    : TerminalLayout.hotbarY(this.imageHeight);
        }
        return slot.y;
    }

    protected boolean shouldRenderMenuSlot(int menuIndex) {
        if (isStorageMenuIndex(menuIndex)) {
            int relative = menuIndex - storageSlotStart();
            return visibleBufferedRows().contains(relative / TerminalLayout.COLUMNS);
        }
        if (menuIndex >= craftingSlotStart() && menuIndex <= craftingResultSlot()) {
            return this.craftingVisible;
        }
        return true;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        int menuIndex = menuIndexOf(slot);
        if (!shouldRenderMenuSlot(menuIndex)) {
            return;
        }
        int visualX = visualSlotX(menuIndex, slot);
        int visualY = visualSlotY(menuIndex, slot);
        if (isStorageMenuIndex(menuIndex)) {
            Rect2i clip = storageBounds();
            int absoluteX = this.leftPos + visualX;
            int absoluteY = this.topPos + visualY;
            if (!intersectsStorageViewport(absoluteX, absoluteY)) {
                return;
            }
            boolean needsScissor = storageCellRequiresScissor(absoluteX, absoluteY);
            if (needsScissor) {
                graphics.enableScissor(clip.getX(), clip.getY(),
                        clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
            }
            graphics.pose().pushPose();
            graphics.pose().translate(visualX - slot.x, visualY - slot.y, 0.0F);
            super.renderSlot(graphics, slot);
            graphics.pose().popPose();
            if (needsScissor) {
                graphics.disableScissor();
            }
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(visualX - slot.x, visualY - slot.y, 0.0F);
        super.renderSlot(graphics, slot);
        graphics.pose().popPose();
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot,
                                      @Nullable String countString) {
        int menuIndex = menuIndexOf(slot);
        if (!isStorageMenuIndex(menuIndex)) {
            super.renderSlotContents(graphics, stack, slot, countString);
            return;
        }
        int seed = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) {
            graphics.renderFakeItem(stack, slot.x, slot.y, seed);
        } else {
            graphics.renderItem(stack, slot.x, slot.y, seed);
        }
        // Keep durability/cooldown/mod decorators in the item pass, but reserve
        // the amount itself for renderStorageAmountOverlays().
        graphics.renderItemDecorations(this.font, stack, slot.x, slot.y, "");
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        int menuIndex = menuIndexOf(slot);
        if (!shouldRenderMenuSlot(menuIndex)) {
            return;
        }
        boolean needsScissor = false;
        if (isStorageMenuIndex(menuIndex)) {
            Rect2i clip = storageBounds();
            int absoluteX = this.leftPos + visualSlotX(menuIndex, slot);
            int absoluteY = this.topPos + visualSlotY(menuIndex, slot);
            needsScissor = storageCellRequiresScissor(absoluteX, absoluteY);
            if (needsScissor) {
                graphics.enableScissor(clip.getX(), clip.getY(),
                        clip.getX() + clip.getWidth(), clip.getY() + clip.getHeight());
            }
        }
        graphics.pose().pushPose();
        graphics.pose().translate(visualSlotX(menuIndex, slot) - slot.x,
                visualSlotY(menuIndex, slot) - slot.y, 0.0F);
        super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
        if (needsScissor) {
            graphics.disableScissor();
        }
    }

    protected final String searchText() {
        return this.searchBox == null ? "" : this.searchBox.getValue();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        boolean overTerminal = TerminalLayout.terminalContains(
                this.leftPos, this.topPos, this.imageHeight, mouseX, mouseY);
        if (!overTerminal) {
            for (Rect2i extraArea : cultivation$getExtraAreas()) {
                if (TerminalLayout.containsHalfOpen(extraArea, mouseX, mouseY)) {
                    overTerminal = true;
                    break;
                }
            }
        }
        if (vertical != 0.0D && overTerminal && maxScrollPx() > 0.0D) {
            animateTo(this.targetScrollPx + TerminalLayout.wheelScrollDelta(vertical));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScrollPx() > 0.0D && isOverScrollbar(mouseX, mouseY)) {
            beginScrollbarDrag(mouseY);
            return true;
        }
        Slot visualSlot = cultivation$getSlotAt(mouseX, mouseY);
        if (isSelfHandledStorageSlot(visualSlot)) {
            this.handledVisualSlotButton = button;
            sendXianqiaoEntryAction((XianqiaoStorageMenu) this.menu, visualSlot, button);
            return true;
        }
        // isHovering(...) maps the menu's fixed baseline slot positions onto the
        // current dynamic terminal layout, so vanilla can own PICKUP/QUICK_MOVE
        // and, critically, the complete QUICK_CRAFT mouse-down/drag/release state.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isSelfHandledStorageSlot(@Nullable Slot visualSlot) {
        return visualSlot != null
                && isStorageMenuIndex(menuIndexOf(visualSlot))
                && this.menu instanceof XianqiaoStorageMenu;
    }

    protected final void consumeCustomSlotRelease(int button) {
        this.handledVisualSlotButton = button;
    }

    private void sendXianqiaoEntryAction(XianqiaoStorageMenu xianqiao, Slot visualSlot, int button) {
        if (button != 0 && button != 1) {
            return;
        }
        var entry = xianqiao.displayedEntryAtSlot(visualSlot.index);
        TerminalAction action;
        long entryId;
        if (entry == null) {
            if (this.menu.getCarried().isEmpty()) {
                return;
            }
            entryId = 0L;
            action = button == 1 ? TerminalAction.INSERT_ONE : TerminalAction.INSERT_CARRIED;
        } else {
            entryId = entry.entryId();
            if (hasShiftDown() && button == 0) {
                action = TerminalAction.QUICK_MOVE_TO_PLAYER;
            } else if (this.menu.getCarried().isEmpty()) {
                action = button == 1 ? TerminalAction.PICKUP_ONE : TerminalAction.PICKUP_STACK;
            } else {
                action = button == 1 ? TerminalAction.INSERT_ONE : TerminalAction.INSERT_CARRIED;
            }
        }
        PacketDistributor.sendToServer(new ModPayloads.TerminalEntryAction(
                this.menu.containerId, xianqiao.viewport().revision(), entryId, action.ordinal()));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar && button == 0) {
            updateScrollbarDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        if (button == this.handledVisualSlotButton) {
            this.handledVisualSlotButton = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public List<Rect2i> cultivation$getExtraAreas() {
        return List.of();
    }

    @Override
    public Slot cultivation$getSlotAt(double mouseX, double mouseY) {
        StorageViewCell storageCell = storageCellAt(mouseX, mouseY);
        if (storageCell != null) {
            int menuIndex = storageSlotStart() + storageCell.viewIndex();
            if (menuIndex >= 0 && menuIndex < this.menu.slots.size()) {
                return this.menu.slots.get(menuIndex);
            }
        }
        int nonStorageStart = Math.min(this.menu.slots.size(), storageSlotStart() + storageSlotCount());
        for (int menuIndex = nonStorageStart; menuIndex < this.menu.slots.size(); menuIndex++) {
            Slot slot = this.menu.slots.get(menuIndex);
            if (!slot.isActive()) {
                continue;
            }
            if (!shouldRenderMenuSlot(menuIndex)) {
                continue;
            }
            int y = this.topPos + visualSlotY(menuIndex, slot);
            int x = this.leftPos + visualSlotX(menuIndex, slot);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public Rect2i cultivation$getSlotBounds(Slot slot) {
        int menuIndex = menuIndexOf(slot);
        int x = this.leftPos + visualSlotX(menuIndex, slot);
        int y = this.topPos + visualSlotY(menuIndex, slot);
        if (isStorageMenuIndex(menuIndex)) {
            return TerminalLayout.clippedSlotBounds(
                    storageBounds(), x, y);
        }
        return new Rect2i(x, y, TerminalLayout.SLOT_SIZE, TerminalLayout.SLOT_SIZE);
    }

    protected final StorageViewCell storageCellAt(double mouseX, double mouseY) {
        Rect2i viewport = storageBounds();
        if (!TerminalLayout.containsHalfOpen(viewport, mouseX, mouseY)) return null;

        double localX = mouseX - (this.leftPos + TerminalLayout.STORAGE_X);
        int column = (int) Math.floor(localX / TerminalLayout.SLOT_PITCH);
        double withinColumn = localX - column * (double) TerminalLayout.SLOT_PITCH;
        if (column < 0 || column >= TerminalLayout.COLUMNS
                || withinColumn < 0.0D || withinColumn >= TerminalLayout.SLOT_SIZE) return null;

        double shiftedY = mouseY - (this.topPos + TerminalLayout.STORAGE_Y)
                + visualFractionalOffset();
        int visualRow = (int) Math.floor(shiftedY / TerminalLayout.SLOT_PITCH);
        double withinRow = shiftedY - visualRow * (double) TerminalLayout.SLOT_PITCH;
        if (visualRow < 0 || withinRow < 0.0D || withinRow >= TerminalLayout.SLOT_SIZE) return null;

        int bufferBase = this.menu instanceof StorageTerminalView terminal
                ? terminal.bufferedBaseRow() : baseRow();
        int bufferedRow = baseRow() + visualRow - bufferBase;
        if (!visibleBufferedRows().contains(bufferedRow)) return null;
        int viewIndex = bufferedRow * TerminalLayout.COLUMNS + column;
        if (viewIndex < 0 || viewIndex >= storageSlotCount()) return null;
        int menuIndex = storageSlotStart() + viewIndex;
        if (menuIndex < 0 || menuIndex >= this.menu.slots.size()) return null;
        Slot slot = this.menu.slots.get(menuIndex);
        if (!slot.isActive() || !shouldRenderMenuSlot(menuIndex)) return null;

        Rect2i bounds = storageCellBounds(viewIndex);
        return bounds.getWidth() > 0 && bounds.getHeight() > 0
                && TerminalLayout.containsHalfOpen(bounds, mouseX, mouseY)
                ? new StorageViewCell(viewIndex, bounds) : null;
    }

    protected final Rect2i storageCellBounds(int viewIndex) {
        int menuIndex = storageSlotStart() + viewIndex;
        if (viewIndex < 0 || viewIndex >= storageSlotCount()
                || menuIndex < 0 || menuIndex >= this.menu.slots.size()) {
            return new Rect2i(0, 0, 0, 0);
        }
        Slot slot = this.menu.slots.get(menuIndex);
        return TerminalLayout.clippedSlotBounds(storageBounds(),
                this.leftPos + visualSlotX(menuIndex, slot),
                this.topPos + visualSlotY(menuIndex, slot));
    }

    protected final int menuIndexOf(Slot slot) {
        Integer cached = this.slotMenuIndices.get(slot);
        if (cached != null) return cached;
        int index = slot == null ? -1 : slot.index;
        if (index >= 0 && index < this.menu.slots.size() && this.menu.slots.get(index) == slot) {
            this.slotMenuIndices.put(slot, index);
            return index;
        }
        return -1;
    }

    private void rebuildSlotIndexCaches() {
        this.slotMenuIndices.clear();
        this.slotIndicesByLogicalPosition.clear();
        Map<Long, List<Integer>> grouped = new HashMap<>();
        for (int menuIndex = 0; menuIndex < this.menu.slots.size(); menuIndex++) {
            Slot slot = this.menu.slots.get(menuIndex);
            this.slotMenuIndices.put(slot, menuIndex);
            grouped.computeIfAbsent(slotPositionKey(slot.x, slot.y), ignored -> new ArrayList<>())
                    .add(menuIndex);
        }
        grouped.forEach((position, indices) -> {
            int[] packed = new int[indices.size()];
            for (int index = 0; index < indices.size(); index++) packed[index] = indices.get(index);
            this.slotIndicesByLogicalPosition.put(position, packed);
        });
    }

    private int activeMenuIndexAtLogicalPosition(int x, int y) {
        int[] candidates = this.slotIndicesByLogicalPosition.get(slotPositionKey(x, y));
        if (candidates == null || candidates.length == 0) return -1;
        for (int menuIndex : candidates) {
            Slot slot = this.menu.slots.get(menuIndex);
            if (slot.isActive() && shouldRenderMenuSlot(menuIndex)) return menuIndex;
        }
        return candidates[0];
    }

    protected final boolean intersectsStorageViewport(int slotX, int slotY) {
        Rect2i viewport = storageBounds();
        return slotX < viewport.getX() + viewport.getWidth()
                && slotX + TerminalLayout.SLOT_SIZE > viewport.getX()
                && slotY < viewport.getY() + viewport.getHeight()
                && slotY + TerminalLayout.SLOT_SIZE > viewport.getY();
    }

    protected final boolean storageCellRequiresScissor(int slotX, int slotY) {
        Rect2i viewport = storageBounds();
        return slotX < viewport.getX()
                || slotX + TerminalLayout.SLOT_SIZE > viewport.getX() + viewport.getWidth()
                || slotY < viewport.getY()
                || slotY + TerminalLayout.SLOT_SIZE > viewport.getY() + viewport.getHeight();
    }

    private static long slotPositionKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    protected record StorageViewCell(int viewIndex, Rect2i bounds) {}

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        int menuIndex = activeMenuIndexAtLogicalPosition(x, y);
        if (menuIndex >= 0) {
            Slot slot = this.menu.slots.get(menuIndex);
            if (!slot.isActive() || !shouldRenderMenuSlot(menuIndex)) return false;
            int visualX = visualSlotX(menuIndex, slot);
            int visualY = visualSlotY(menuIndex, slot);
            if (isStorageMenuIndex(menuIndex)
                    && !intersectsStorageViewport(this.leftPos + visualX, this.topPos + visualY)) {
                return false;
            }
            return super.isHovering(visualX, visualY, width, height, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean cultivation$isCraftingVisible() {
        return this.craftingVisible;
    }

    private void changeRows(int delta) {
        int maximum = maximumEffectiveRows();
        int desired = Mth.clamp(this.visibleRows + delta, TerminalLayout.MIN_ROWS, maximum);
        if (desired == this.visibleRows) return;
        TerminalLayout.setConfiguredRows(desired);
        if (this.searchBox != null) {
            this.retainedSearch = this.searchBox.getValue();
        }
        onViewportChanged(desired, baseRow());
        this.rebuildWidgets();
    }

    private void onSearchChanged(String text) {
        this.retainedSearch = text;
        if (!this.applyingExternalSearch) {
            sendQuery(new TerminalQuery(text, this.sortOrder, this.sortDirection), true);
        }
    }

    private void cycleSort() {
        TerminalQuery.SortOrder[] values = TerminalQuery.SortOrder.values();
        this.sortOrder = values[(this.sortOrder.ordinal() + 1) % values.length];
        this.sortButton.setMessage(sortLabel());
        this.sortButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(sortTooltip()));
        sendQuery(new TerminalQuery(searchText(), this.sortOrder, this.sortDirection), true);
    }

    private void toggleDirection() {
        this.sortDirection = this.sortDirection == TerminalQuery.SortDirection.ASCENDING
                ? TerminalQuery.SortDirection.DESCENDING : TerminalQuery.SortDirection.ASCENDING;
        this.sortDirectionButton.setMessage(directionLabel());
        sendQuery(new TerminalQuery(searchText(), this.sortOrder, this.sortDirection), true);
    }

    private void sendQuery(TerminalQuery query, boolean pushToRecipeViewers) {
        setLocalQuery(query);
        PacketDistributor.sendToServer(new ModPayloads.SetTerminalQuery(
                query.text(), query.sortOrder().ordinal(), query.sortDirection().ordinal()));
        this.scrollPx = this.scrollStartPx = this.targetScrollPx = 0.0D;
        onViewportChanged(this.visibleRows, 0);
        if (pushToRecipeViewers) {
            RecipeViewerSearchSync.push(query.text());
        }
    }

    private Component sortLabel() {
        return Component.literal(switch (this.sortOrder) {
            case AMOUNT -> "#";
            case NAME -> "A";
            case MOD_ID -> "@";
        });
    }

    private Component sortTooltip() {
        String key = switch (this.sortOrder) {
            case AMOUNT -> "container.cultivation.terminal.sort_amount";
            case NAME -> "container.cultivation.terminal.sort_name";
            case MOD_ID -> "container.cultivation.terminal.sort_mod";
        };
        return Component.translatable(key);
    }

    private Component directionLabel() {
        return Component.literal(this.sortDirection == TerminalQuery.SortDirection.ASCENDING ? "^" : "v");
    }

    private void updateRowButtons() {
        int maximum = maximumEffectiveRows();
        this.fewerRowsButton.active = this.visibleRows > TerminalLayout.MIN_ROWS;
        this.moreRowsButton.active = this.visibleRows < maximum;
    }

    private int maximumEffectiveRows() {
        return TerminalLayout.effectiveRows(TerminalLayout.MAX_ROWS,
                TerminalLayout.currentScreenHeight(), this.workspaceExpanded, maximumContentRows());
    }

    private void synchronizeViewport() {
        if (!(this.menu instanceof StorageTerminalView terminal)) {
            return;
        }
        int clampedBase = TerminalViewport.clampBaseRow(baseRow(), this.visibleRows, totalStorageRows());
        if (clampedBase != baseRow()) {
            setServerBaseRow(clampedBase);
        }
        if (terminal.viewport().visibleRows() != this.visibleRows
                || terminal.viewport().baseRow() != clampedBase) {
            onViewportChanged(this.visibleRows, clampedBase);
        }
    }

    private void animateTo(double requestedPx) {
        tickScrollAnimation();
        this.scrollStartPx = this.scrollPx;
        this.targetScrollPx = Mth.clamp(requestedPx, 0.0D, maxScrollPx());
        this.scrollStartTime = System.currentTimeMillis();
    }

    private void tickScrollAnimation() {
        int oldBaseRow = baseRow();
        if (this.draggingScrollbar) {
            this.scrollPx = this.targetScrollPx;
        } else {
            double progress = Mth.clamp((System.currentTimeMillis() - this.scrollStartTime)
                    / (double) SCROLL_ANIMATION_MS, 0.0D, 1.0D);
            double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
            this.scrollPx = Mth.lerp(eased, this.scrollStartPx, this.targetScrollPx);
        }
        this.scrollPx = Mth.clamp(this.scrollPx, 0.0D, maxScrollPx());
        if (oldBaseRow != baseRow()) {
            onBaseRowChanged(baseRow());
        }
    }

    private double maxScrollPx() {
        return Math.max(0, totalStorageRows() - this.visibleRows) * (double) TerminalLayout.SLOT_PITCH;
    }

    private float scrollFraction() {
        double max = maxScrollPx();
        return max <= 0.0D ? 0.0F : (float) (this.scrollPx / max);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + TerminalLayout.SCROLLBAR_X
                && mouseX < this.leftPos + TerminalLayout.SCROLLBAR_X + TerminalLayout.SCROLLBAR_WIDTH
                && mouseY >= this.topPos + TerminalLayout.STORAGE_Y
                && mouseY < this.topPos + TerminalLayout.STORAGE_Y + this.visibleRows * TerminalLayout.SLOT_PITCH;
    }

    private void beginScrollbarDrag(double mouseY) {
        int trackTop = this.topPos + TerminalLayout.STORAGE_Y + 1;
        int trackHeight = this.visibleRows * TerminalLayout.SLOT_PITCH;
        int thumbHeight = TerminalLayout.scrollbarThumbHeight(trackHeight, this.visibleRows, totalStorageRows());
        int thumbTop = trackTop + TerminalLayout.scrollbarThumbOffset(
                trackHeight, this.visibleRows, totalStorageRows(), scrollFraction());
        if (mouseY >= thumbTop && mouseY < thumbTop + thumbHeight) {
            this.scrollbarGrabOffset = mouseY - thumbTop;
        } else {
            this.scrollbarGrabOffset = thumbHeight / 2.0D;
        }
        this.draggingScrollbar = true;
        updateScrollbarDrag(mouseY);
    }

    private void updateScrollbarDrag(double mouseY) {
        int trackTop = this.topPos + TerminalLayout.STORAGE_Y + 1;
        int trackHeight = this.visibleRows * TerminalLayout.SLOT_PITCH;
        int travel = TerminalLayout.scrollbarTravel(trackHeight, this.visibleRows, totalStorageRows());
        double fraction = Mth.clamp((mouseY - trackTop - this.scrollbarGrabOffset)
                / Math.max(1.0D, travel), 0.0D, 1.0D);
        int oldBaseRow = baseRow();
        this.scrollStartPx = this.targetScrollPx = this.scrollPx = fraction * maxScrollPx();
        this.scrollStartTime = System.currentTimeMillis();
        if (oldBaseRow != baseRow()) {
            onBaseRowChanged(baseRow());
        }
    }

    private boolean isStorageMenuIndex(int menuIndex) {
        return menuIndex >= storageSlotStart() && menuIndex < storageSlotStart() + storageSlotCount();
    }
}
