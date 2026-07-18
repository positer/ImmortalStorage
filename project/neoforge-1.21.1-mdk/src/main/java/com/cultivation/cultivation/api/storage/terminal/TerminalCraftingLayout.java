package com.cultivation.cultivation.api.storage.terminal;

/**
 * Loader-neutral logical coordinates for the embedded 3x3 crafting workspace.
 *
 * <p>The menu uses the five-row baseline while client screens translate the
 * same anchors for their effective 2..12-row image height. GUI scale is
 * intentionally absent: all values are logical GUI pixels.</p>
 */
public final class TerminalCraftingLayout {
    public static final int SLOT_SIZE = 16;
    public static final int SLOT_PITCH = 18;
    public static final int GRID_X = 26;
    public static final int GRID_BOTTOM_OFFSET = 158;
    public static final int RESULT_X = 134;
    public static final int RESULT_BOTTOM_OFFSET = 140;
    public static final int RESULT_FRAME_MARGIN = 5;
    public static final int RESULT_FRAME_SIZE = SLOT_SIZE + RESULT_FRAME_MARGIN * 2;
    public static final int ARROW_X = 92;
    public static final int ARROW_RESULT_Y_OFFSET = 2;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 15;

    /** 197 fixed workspace pixels plus the default five 18px storage rows. */
    public static final int MENU_BASELINE_IMAGE_HEIGHT = 287;

    private TerminalCraftingLayout() {}

    public static int gridY(int imageHeight) {
        return imageHeight - GRID_BOTTOM_OFFSET;
    }

    public static int inputX(int column) {
        return GRID_X + Math.max(0, Math.min(2, column)) * SLOT_PITCH;
    }

    public static int inputY(int imageHeight, int row) {
        return gridY(imageHeight) + Math.max(0, Math.min(2, row)) * SLOT_PITCH;
    }

    public static int resultY(int imageHeight) {
        return imageHeight - RESULT_BOTTOM_OFFSET;
    }

    public static int resultFrameX() {
        return RESULT_X - RESULT_FRAME_MARGIN;
    }

    public static int resultFrameY(int imageHeight) {
        return resultY(imageHeight) - RESULT_FRAME_MARGIN;
    }

    public static int arrowY(int imageHeight) {
        return resultY(imageHeight) + ARROW_RESULT_Y_OFFSET;
    }
}
