package com.immortalstorage.immortalstorage.block;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedstoneWorkModeTest {
    @Test void threeStatesHaveExpectedTruthTable() {
        assertTrue(RedstoneWorkMode.SIGNAL_WORK.allows(true));
        assertFalse(RedstoneWorkMode.SIGNAL_WORK.allows(false));
        assertFalse(RedstoneWorkMode.NO_SIGNAL_WORK.allows(true));
        assertTrue(RedstoneWorkMode.NO_SIGNAL_WORK.allows(false));
        assertTrue(RedstoneWorkMode.IGNORE.allows(true));
        assertTrue(RedstoneWorkMode.IGNORE.allows(false));
    }

    @Test void cycleOrderIsStable() {
        assertEquals(RedstoneWorkMode.NO_SIGNAL_WORK, RedstoneWorkMode.SIGNAL_WORK.next());
        assertEquals(RedstoneWorkMode.IGNORE, RedstoneWorkMode.NO_SIGNAL_WORK.next());
        assertEquals(RedstoneWorkMode.SIGNAL_WORK, RedstoneWorkMode.IGNORE.next());
        assertEquals(RedstoneWorkMode.IGNORE, RedstoneWorkMode.byId(99));
    }
}
