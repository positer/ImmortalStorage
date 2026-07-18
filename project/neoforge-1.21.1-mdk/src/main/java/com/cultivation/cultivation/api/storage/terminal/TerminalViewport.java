package com.cultivation.cultivation.api.storage.terminal;

/** Authoritative row window for a nine-column terminal. */
public record TerminalViewport(int desiredRows, int visibleRows, int baseRow, int totalRows, long revision) {
    public static final int COLUMNS = 9;
    public static final int MIN_ROWS = 2;
    public static final int DEFAULT_ROWS = 5;
    public static final int MAX_ROWS = 12;
    public static final int MAX_BUFFERED_ROWS = MAX_ROWS * 2;

    public TerminalViewport {
        desiredRows = clampRows(desiredRows);
        visibleRows = Math.max(MIN_ROWS, Math.min(desiredRows, visibleRows));
        totalRows = Math.max(0, totalRows);
        baseRow = clampBaseRow(baseRow, visibleRows, totalRows);
        revision = Math.max(0L, revision);
    }

    public static int clampRows(int rows) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
    }

    public static int clampBaseRow(int baseRow, int visibleRows, int totalRows) {
        return Math.max(0, Math.min(Math.max(0, totalRows - visibleRows), baseRow));
    }

    public static int bufferedRows(int visibleRows) {
        return Math.min(MAX_BUFFERED_ROWS, clampRows(visibleRows) * 2);
    }

    /** Maximum number of rows that can intersect a smooth-scrolling viewport. */
    public static int maxIntersectingRows(int visibleRows) {
        return clampRows(visibleRows) + 1;
    }

    /**
     * Maps the current visual viewport into its already-synchronized row buffer.
     * An exact 18-pixel boundary touches {@code R} rows; any fractional offset
     * touches at most one additional clipped row. The 2R network buffer remains
     * intact, but render and hit-test passes do not walk rows that cannot appear.
     */
    public static BufferedRowWindow intersectingBufferedRows(int bufferBaseRow, int bufferedRows,
                                                              int viewBaseRow, int visibleRows,
                                                              int fractionalScrollPx) {
        int safeBufferBase = Math.max(0, bufferBaseRow);
        int safeBufferedRows = Math.max(0, bufferedRows);
        int safeViewBase = Math.max(0, viewBaseRow);
        int rowsTouchingViewport = clampRows(visibleRows) + (fractionalScrollPx > 0 ? 1 : 0);

        long bufferStart = safeBufferBase;
        long bufferEnd = bufferStart + safeBufferedRows;
        long viewStart = safeViewBase;
        long viewEnd = viewStart + rowsTouchingViewport;
        long overlapStart = Math.max(bufferStart, viewStart);
        long overlapEnd = Math.min(bufferEnd, viewEnd);
        if (overlapEnd <= overlapStart) {
            int edge = viewStart >= bufferEnd ? safeBufferedRows : 0;
            return new BufferedRowWindow(edge, edge);
        }
        return new BufferedRowWindow((int) (overlapStart - bufferStart),
                (int) (overlapEnd - bufferStart));
    }

    public static int recenterBufferBase(int viewBaseRow, int visibleRows, int totalRows) {
        int rows = bufferedRows(visibleRows);
        int maxBase = Math.max(0, totalRows - rows);
        return Math.max(0, Math.min(maxBase, viewBaseRow - clampRows(visibleRows) / 2));
    }

    public static int ensureBufferBase(int currentBufferBase, int viewBaseRow, int visibleRows, int totalRows) {
        int rows = bufferedRows(visibleRows);
        int maxBase = Math.max(0, totalRows - rows);
        int clampedCurrent = Math.max(0, Math.min(maxBase, currentBufferBase));
        int guardRows = Math.max(1, (clampRows(visibleRows) + 3) / 4);
        boolean leftProtected = clampedCurrent == 0 || viewBaseRow >= clampedCurrent + guardRows;
        boolean rightProtected = clampedCurrent == maxBase
                || viewBaseRow + visibleRows <= clampedCurrent + rows - guardRows;
        return leftProtected && rightProtected
                ? clampedCurrent : recenterBufferBase(viewBaseRow, visibleRows, totalRows);
    }

    public int visibleSlotCount() {
        return visibleRows * COLUMNS;
    }

    public record BufferedRowWindow(int fromInclusive, int toExclusive) {
        public BufferedRowWindow {
            if (fromInclusive < 0 || toExclusive < fromInclusive) {
                throw new IllegalArgumentException("invalid buffered row window");
            }
        }

        public int rowCount() {
            return toExclusive - fromInclusive;
        }

        public boolean contains(int bufferedRow) {
            return bufferedRow >= fromInclusive && bufferedRow < toExclusive;
        }
    }
}
