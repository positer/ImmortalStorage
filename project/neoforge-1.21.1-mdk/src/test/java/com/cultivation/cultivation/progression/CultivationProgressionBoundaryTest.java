package com.cultivation.cultivation.progression;

import com.cultivation.cultivation.player.CultivationPlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationProgressionBoundaryTest {
    @Test
    void carryingJadeForExactlyOneFullDayInitiatesStageOne() {
        CultivationPlayerData data = new CultivationPlayerData();

        for (int tick = 1; tick < CultivationProgressionRules.JADE_INITIATION_TICKS; tick++) {
            assertFalse(data.tickJadeInitiation(true));
        }
        assertEquals(0, data.getStage());

        assertTrue(data.tickJadeInitiation(true));
        assertEquals(1, data.getStage());
        assertFalse(data.tickJadeInitiation(true), "stage one must not initiate twice");
    }

    @Test
    void removingJadeResetsTheContinuousCarryWindow() {
        CultivationPlayerData data = new CultivationPlayerData();
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
        CultivationPlayerData data = new CultivationPlayerData();

        assertTrue(data.tryJadeSleepInitiation(true));
        assertEquals(1, data.getStage());
        assertEquals(0, CultivationProgressionRules.cosmeticTntBlastCount(
                0, 1, CultivationProgressionRules.AdvancementSource.JADE_SLEEP));
    }

    @Test
    void onlyNormalStageFiveImmortalPillAscensionRequestsFiveBlasts() {
        assertEquals(5, CultivationProgressionRules.cosmeticTntBlastCount(
                5, 6, CultivationProgressionRules.AdvancementSource.IMMORTAL_PILL));
        assertEquals(0, CultivationProgressionRules.cosmeticTntBlastCount(
                4, 6, CultivationProgressionRules.AdvancementSource.IMMORTAL_PILL));
        assertEquals(0, CultivationProgressionRules.cosmeticTntBlastCount(
                5, 6, CultivationProgressionRules.AdvancementSource.ASCENSION_DAN));
        assertEquals(0, CultivationProgressionRules.cosmeticTntBlastCount(
                5, 6, CultivationProgressionRules.AdvancementSource.OTHER));
    }

    @Test
    void cosmeticHelperEmitsFiveIndexedVisualAndSoundEventsOnly() {
        List<Integer> emitted = new ArrayList<>();

        AscensionCosmeticEffects.emitImmortalPillBlasts(emitted::add);

        assertEquals(List.of(0, 1, 2, 3, 4), emitted);
    }
}
