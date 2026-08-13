package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalCraftingLayout;
import com.immortalstorage.immortalstorage.config.ImmortalStorageClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

/** Shared vanilla-pixel geometry for both portable storage terminals. */
public final class TerminalLayout {
    public static final int WIDTH = 195;
    public static final int COLUMNS = 9;
    public static final int SLOT_SIZE = 16;
    public static final int SLOT_PITCH = 18;
    public static final int SLOT_FRAME = 1;
    /** Above vanilla slot contents (item 250, decorations 300), below carried items (382) and tooltips (400). */
    public static final float STORAGE_AMOUNT_Z = 320.0F;
    public static final double SCROLL_STEP_PX = 9.0D;
    public static final int MIN_ROWS = 2;
    public static final int MAX_ROWS = 12;
    public static final int DEFAULT_ROWS = 5;
    public static final int STORAGE_X = 8;
    public static final int STORAGE_Y = 18;
    public static final int SEARCH_X = 80;
    public static final int SEARCH_Y = 4;
    public static final int SEARCH_WIDTH = 89;
    public static final int SEARCH_HEIGHT = 12;
    public static final int SCROLLBAR_X = 175;
    public static final int SCROLLBAR_WIDTH = 14;
    public static final int SCROLLBAR_THUMB_HEIGHT = 15;
    public static final int TAB_WIDTH = TerminalTabStyle.WIDTH;
    public static final int TAB_HEIGHT = TerminalTabStyle.HEIGHT;
    /** Advancement LEFT tabs overlap the owning panel by four pixels. */
    public static final int MODULE_RAIL_X = -TAB_WIDTH + TerminalTabStyle.PANEL_OVERLAP;
    public static final int RAIL_GROUP_GAP = 4;
    public static final int CONTROL_SIZE = 16;
    public static final int CONTROL_COUNT = 4;
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 6;
    public static final int CRAFT_GRID_X = TerminalCraftingLayout.GRID_X;
    public static final int CRAFT_RESULT_X = TerminalCraftingLayout.RESULT_X;
    public static final int CRAFT_RESULT_FRAME_MARGIN = TerminalCraftingLayout.RESULT_FRAME_MARGIN;
    public static final int CRAFT_RESULT_FRAME_SIZE = TerminalCraftingLayout.RESULT_FRAME_SIZE;
    public static final int FURNACE_LANE_COUNT = 3;
    public static final int FURNACE_INPUT_X = 48;
    public static final int FURNACE_FUEL_X = 8;
    public static final int FURNACE_PLUGIN_X = FURNACE_FUEL_X;
    public static final int FURNACE_RESULT_X = 134;

    private static final int STORAGE_ONLY_FIXED_HEIGHT = 116;
    private static final int WORKSPACE_FIXED_HEIGHT = 197;

    private TerminalLayout() {}

    public static int clampDesiredRows(int rows) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
    }

    public static int effectiveRows(int desiredRows, int screenHeight, boolean craftingVisible) {
        return effectiveRows(desiredRows, screenHeight, craftingVisible, MAX_ROWS);
    }

    /**
     * Resolves the visible viewport against all three independent limits: the
     * user's preference, the current logical screen height and the number of
     * rows the backing storage can actually contain. The shared two-row
     * minimum is retained for the smallest first-stage Kongqiao.
     */
    public static int effectiveRows(int desiredRows, int screenHeight, boolean craftingVisible,
                                    int maximumContentRows) {
        int fixedHeight = craftingVisible ? WORKSPACE_FIXED_HEIGHT : STORAGE_ONLY_FIXED_HEIGHT;
        int availableRows = Math.max(MIN_ROWS, (screenHeight - 20 - fixedHeight) / SLOT_PITCH);
        int contentRows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, maximumContentRows));
        return Math.min(clampDesiredRows(desiredRows), Math.min(contentRows, availableRows));
    }

    public static int imageHeight(int rows, boolean craftingVisible) {
        return (craftingVisible ? WORKSPACE_FIXED_HEIGHT : STORAGE_ONLY_FIXED_HEIGHT) + SLOT_PITCH * rows;
    }

    public static int inventoryY(int imageHeight) {
        return imageHeight - 84;
    }

    public static int hotbarY(int imageHeight) {
        return imageHeight - 26;
    }

    public static int craftGridY(int imageHeight) {
        return TerminalCraftingLayout.gridY(imageHeight);
    }

    public static int craftInputSlotX(int column) {
        return TerminalCraftingLayout.inputX(column);
    }

    public static int craftInputSlotY(int imageHeight, int row) {
        return TerminalCraftingLayout.inputY(imageHeight, row);
    }

    public static int craftResultY(int imageHeight) {
        return TerminalCraftingLayout.resultY(imageHeight);
    }

    public static Rect2i craftInputSlotBounds(int imageHeight, int row, int column) {
        return new Rect2i(craftInputSlotX(column), craftInputSlotY(imageHeight, row),
                SLOT_SIZE, SLOT_SIZE);
    }

    public static int craftArrowX() {
        return TerminalCraftingLayout.ARROW_X;
    }

    public static int craftArrowY(int imageHeight) {
        return TerminalCraftingLayout.arrowY(imageHeight);
    }

    public static Rect2i craftArrowBounds(int imageHeight) {
        return new Rect2i(craftArrowX(), craftArrowY(imageHeight),
                TerminalCraftingLayout.ARROW_WIDTH, TerminalCraftingLayout.ARROW_HEIGHT);
    }

    public static Rect2i craftResultSlotBounds(int imageHeight) {
        return new Rect2i(CRAFT_RESULT_X, craftResultY(imageHeight), SLOT_SIZE, SLOT_SIZE);
    }

    public static Rect2i craftResultFrameBounds(int imageHeight) {
        return new Rect2i(TerminalCraftingLayout.resultFrameX(),
                TerminalCraftingLayout.resultFrameY(imageHeight),
                CRAFT_RESULT_FRAME_SIZE, CRAFT_RESULT_FRAME_SIZE);
    }

    public static int furnaceInputY(int imageHeight) {
        return furnaceLaneY(imageHeight, 0);
    }

    public static int furnaceFuelY(int imageHeight) {
        return furnaceLaneY(imageHeight, 2);
    }

    public static int furnacePluginY(int imageHeight) {
        return furnaceLaneY(imageHeight, 0);
    }

    public static int furnaceFlameY(int imageHeight) {
        return furnaceLaneY(imageHeight, 1) + 3;
    }

    public static int furnaceResultY(int imageHeight) {
        return furnaceLaneY(imageHeight, 0);
    }

    public static int furnaceInputY(int imageHeight, int channel) {
        return furnaceLaneY(imageHeight, channel);
    }

    public static int furnaceResultY(int imageHeight, int channel) {
        return furnaceLaneY(imageHeight, channel);
    }

    public static int furnaceLaneY(int imageHeight, int channel) {
        int clamped = Math.max(0, Math.min(FURNACE_LANE_COUNT - 1, channel));
        return imageHeight - 158 + clamped * SLOT_PITCH;
    }

    public static Rect2i storageBounds(AbstractContainerScreen<?> screen, int rows) {
        return storageBounds(screen.getGuiLeft(), screen.getGuiTop(), rows);
    }

    public static Rect2i storageBounds(int guiLeft, int guiTop, int rows) {
        return new Rect2i(guiLeft + STORAGE_X - SLOT_FRAME, guiTop + STORAGE_Y - SLOT_FRAME,
                COLUMNS * SLOT_PITCH, rows * SLOT_PITCH);
    }

    public static Rect2i scrollRegionBounds(AbstractContainerScreen<?> screen, int rows) {
        return new Rect2i(screen.getGuiLeft() + STORAGE_X - SLOT_FRAME,
                screen.getGuiTop() + STORAGE_Y - SLOT_FRAME,
                SCROLLBAR_X + SCROLLBAR_WIDTH - STORAGE_X + SLOT_FRAME,
                rows * SLOT_PITCH);
    }

    public static Rect2i slotTileBounds(int slotX, int slotY) {
        return new Rect2i(slotX - SLOT_FRAME, slotY - SLOT_FRAME, SLOT_PITCH, SLOT_PITCH);
    }

    public static boolean containsHalfOpen(Rect2i bounds, double x, double y) {
        return x >= bounds.getX() && x < bounds.getX() + bounds.getWidth()
                && y >= bounds.getY() && y < bounds.getY() + bounds.getHeight();
    }

    public static Rect2i clippedSlotBounds(Rect2i viewport, int slotX, int slotY) {
        int left = Math.max(viewport.getX(), slotX);
        int top = Math.max(viewport.getY(), slotY);
        int right = Math.min(viewport.getX() + viewport.getWidth(), slotX + SLOT_SIZE);
        int bottom = Math.min(viewport.getY() + viewport.getHeight(), slotY + SLOT_SIZE);
        return new Rect2i(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public static boolean terminalContains(int guiLeft, int guiTop, int imageHeight, double mouseX, double mouseY) {
        return mouseX >= guiLeft && mouseX < guiLeft + WIDTH
                && mouseY >= guiTop && mouseY < guiTop + imageHeight;
    }

    public static int compositeLeft(int screenWidth, int mainWidth, int railX, int gap, int extraWidth) {
        int leftRailWidth = Math.max(0, -railX);
        int rightExtent = mainWidth + gap + extraWidth;
        int desired = (screenWidth - leftRailWidth - rightExtent) / 2 + leftRailWidth;
        int minimum = leftRailWidth;
        int maximum = screenWidth - rightExtent;
        return maximum < minimum ? minimum : Math.max(minimum, Math.min(maximum, desired));
    }

    public static int moduleTabY(int index) {
        return index * TAB_HEIGHT;
    }

    public static int railControlOffset(int moduleCount) {
        return Math.max(0, moduleCount) * TAB_HEIGHT + RAIL_GROUP_GAP;
    }

    public static int railControlX() {
        return MODULE_RAIL_X + (TAB_WIDTH - CONTROL_SIZE) / 2;
    }

    public static int railHeight(int moduleCount) {
        return railControlOffset(moduleCount) + CONTROL_COUNT * CONTROL_SIZE;
    }

    public static double wheelScrollDelta(double verticalWheelDelta) {
        return -verticalWheelDelta * SCROLL_STEP_PX;
    }

    public static int scrollbarThumbHeight(int trackHeight, int visibleRows, int totalRows) {
        int interior = Math.max(1, trackHeight - 2);
        return Math.min(interior, SCROLLBAR_THUMB_HEIGHT);
    }

    public static int scrollbarTravel(int trackHeight, int visibleRows, int totalRows) {
        if (totalRows <= visibleRows) return 0;
        return Math.max(0, trackHeight - 2 - scrollbarThumbHeight(trackHeight, visibleRows, totalRows));
    }

    public static int scrollbarThumbOffset(int trackHeight, int visibleRows, int totalRows, float scrollFraction) {
        float clamped = Math.max(0.0F, Math.min(1.0F, scrollFraction));
        return Math.round(scrollbarTravel(trackHeight, visibleRows, totalRows) * clamped);
    }

    public static int baseRow(double scrollPx) {
        return (int) Math.floor(Math.max(0.0D, scrollPx) / SLOT_PITCH);
    }

    public static int fractionalScrollOffset(double scrollPx) {
        return (int) Math.floor(Math.max(0.0D, scrollPx)) % SLOT_PITCH;
    }

    public static int visualStorageRow(int slotIndex, int snapshotBaseRow, int visualBaseRow) {
        return slotIndex / COLUMNS + snapshotBaseRow - visualBaseRow;
    }

    public static int configuredRows() {
        return clampDesiredRows(ImmortalStorageClientConfig.TERMINAL_ROWS.get());
    }

    public static void setConfiguredRows(int rows) {
        ImmortalStorageClientConfig.TERMINAL_ROWS.set(clampDesiredRows(rows));
        ImmortalStorageClientConfig.TERMINAL_ROWS.save();
    }

    public static int currentScreenHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getWindow().getGuiScaledHeight();
    }
}
