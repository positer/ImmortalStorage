package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortableTerminalChromeTest {
    @Test void titleSearchAndMagnetHaveDisjointHeaderBounds() {
        assertTrue(TerminalLayout.SEARCH_X - TerminalLayout.TITLE_X >= 85);
        assertTrue(TerminalLayout.SEARCH_X + TerminalLayout.SEARCH_WIDTH + 4 <= TerminalLayout.MAGNET_X);
        assertTrue(TerminalLayout.MAGNET_X + TerminalLayout.MAGNET_WIDTH + 4 <= TerminalLayout.WIDTH);
        assertTrue(TerminalLayout.SEARCH_Y + TerminalLayout.SEARCH_HEIGHT <= TerminalLayout.STORAGE_Y - 2);
    }

    @Test void managementFitsNarrowAndNormalLogicalWindows() {
        for (int screenWidth : new int[] {320, 330, 340, 426, 480}) {
            int panel = TerminalLayout.fitSidePanelWidth(screenWidth, TerminalLayout.WIDTH,
                    TerminalLayout.MODULE_RAIL_X, 4, 92, 116);
            int left = TerminalLayout.compositeLeft(screenWidth, TerminalLayout.WIDTH,
                    TerminalLayout.MODULE_RAIL_X, 4, panel);
            assertTrue(left + TerminalLayout.MODULE_RAIL_X >= 0);
            assertTrue(left + TerminalLayout.WIDTH + 4 + panel <= screenWidth);
            assertTrue(panel >= 92);
            if (screenWidth >= 426) assertEquals(116, panel);
        }
    }
}
