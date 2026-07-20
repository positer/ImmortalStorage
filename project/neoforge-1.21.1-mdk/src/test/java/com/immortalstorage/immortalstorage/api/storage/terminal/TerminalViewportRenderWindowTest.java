package com.immortalstorage.immortalstorage.api.storage.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalViewportRenderWindowTest {
    @Test
    void exactRowBoundaryTouchesOnlyTheVisibleRows() {
        TerminalViewport.BufferedRowWindow window = TerminalViewport.intersectingBufferedRows(
                40, 10, 43, 5, 0);

        assertEquals(3, window.fromInclusive());
        assertEquals(8, window.toExclusive());
        assertEquals(5, window.rowCount());
    }

    @Test
    void fractionalScrollAddsOnlyOneClippedBufferRow() {
        TerminalViewport.BufferedRowWindow window = TerminalViewport.intersectingBufferedRows(
                40, 10, 43, 5, 9);

        assertEquals(3, window.fromInclusive());
        assertEquals(9, window.toExclusive());
        assertEquals(6, window.rowCount());
        assertTrue(window.contains(8));
        assertFalse(window.contains(9));
    }

    @Test
    void renderWindowIsClippedToTheAvailableDoubleBuffer() {
        TerminalViewport.BufferedRowWindow window = TerminalViewport.intersectingBufferedRows(
                0, 10, 6, 5, 17);

        assertEquals(6, window.fromInclusive());
        assertEquals(10, window.toExclusive());
        assertEquals(4, window.rowCount());
    }

    @Test
    void maximumInteractiveProxyWindowIsOneRowLargerThanTheViewport() {
        assertEquals(6, TerminalViewport.maxIntersectingRows(5));
        assertEquals(13, TerminalViewport.maxIntersectingRows(12));
    }
}
