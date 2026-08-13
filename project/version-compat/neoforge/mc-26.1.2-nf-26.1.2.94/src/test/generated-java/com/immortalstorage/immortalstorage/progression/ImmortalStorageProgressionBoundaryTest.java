package com.immortalstorage.immortalstorage.progression;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmortalStorageProgressionBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void carryingJadeForExactlyOneFullDayInitiatesStageOne() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();

        for (int tick = 1; tick < ImmortalStorageProgressionRules.JADE_INITIATION_TICKS; tick++) {
            assertFalse(data.tickJadeInitiation(true));
        }
        assertEquals(0, data.getStage());

        assertTrue(data.tickJadeInitiation(true));
        assertEquals(1, data.getStage());
        assertFalse(data.tickJadeInitiation(true), "stage one must not initiate twice");
    }

    @Test
    void removingJadeResetsTheContinuousCarryWindow() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        for (int tick = 0; tick < 12_000; tick++) {
            assertFalse(data.tickJadeInitiation(true));
        }

        assertFalse(data.tickJadeInitiation(false));
        for (int tick = 0; tick < 23_999; tick++) {
            assertFalse(data.tickJadeInitiation(true));
        }
        assertEquals(0, data.getStage());
        assertTrue(data.tickJadeInitiation(true));
        assertEquals(1, data.getStage());
    }

    @Test
    void sleepingWithJadeInitiatesStageOneWithoutAscensionBlasts() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();

        assertTrue(data.tryJadeSleepInitiation(true));
        assertEquals(1, data.getStage());
        assertEquals(0, ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                0, 1, ImmortalStorageProgressionRules.AdvancementSource.JADE_SLEEP));
    }

    @Test
    void onlyNormalStageFiveImmortalPillAscensionRequestsFiveBlasts() {
        assertEquals(5, ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                5, 6, ImmortalStorageProgressionRules.AdvancementSource.IMMORTAL_PILL));
        assertEquals(0, ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                4, 6, ImmortalStorageProgressionRules.AdvancementSource.IMMORTAL_PILL));
        assertEquals(0, ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                5, 6, ImmortalStorageProgressionRules.AdvancementSource.ASCENSION_DAN));
        assertEquals(0, ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                5, 6, ImmortalStorageProgressionRules.AdvancementSource.OTHER));
    }

    @Test
    void cosmeticHelperEmitsFiveIndexedVisualAndSoundEventsOnly() {
        List<Integer> emitted = new ArrayList<>();

        AscensionCosmeticEffects.emitImmortalPillBlasts(emitted::add);

        assertEquals(List.of(0, 1, 2, 3, 4), emitted);
    }
}
