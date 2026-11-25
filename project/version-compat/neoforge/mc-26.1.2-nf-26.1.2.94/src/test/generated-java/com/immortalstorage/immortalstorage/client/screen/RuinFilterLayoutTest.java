package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.menu.custom.RuinFilterLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuinFilterLayoutTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void reinforcementSlotClearsWhitelistAndOptionalFaceGrid() {
        int slotTop = RuinFilterLayout.REINFORCEMENT_Y - 1;
        int slotBottom = slotTop + 18;
        assertTrue(slotTop >= 130 + 5, "Keep whitespace after the whitelist button");
        assertTrue(slotBottom < RuinFilterLayout.FACE_GRID_Y, "The real slot cannot overlap the first face");
        assertTrue(slotBottom + 7 <= RuinFilterLayout.MINIMUM_PANEL_HEIGHT);
    }
}
