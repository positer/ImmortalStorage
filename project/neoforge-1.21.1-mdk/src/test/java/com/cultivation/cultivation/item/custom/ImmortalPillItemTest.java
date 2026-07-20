package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImmortalPillItemTest {
    @Test
    void finiteStagesRestoreHalfCapAndUnboundedStagesGrantTwoThousandTicksOfGeneration() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(8);
        assertEquals(512L, ImmortalPillItem.immortalYuanReward(data));

        data.setStage(9);
        assertEquals(3_200L, ImmortalPillItem.immortalYuanReward(data));

        data.setStage(10);
        assertEquals(25_600L, ImmortalPillItem.immortalYuanReward(data));
    }
}
