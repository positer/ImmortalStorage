package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImmortalPillItemTest {
    @Test
    void unboundedStageRewardIsFiniteAndInfiniteStageNeedsNoMaterialization() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(9);
        assertEquals(4_096L, ImmortalPillItem.immortalYuanReward(data));

        data.setStage(10);
        assertEquals(4_096L, ImmortalPillItem.immortalYuanReward(data));
    }
}
