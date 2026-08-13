package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImmortalPillItemTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void finiteStagesRestoreHalfCapAndUnboundedStagesGrantTwoThousandTicksOfGeneration() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(8);
        assertEquals(512L, ImmortalPillItem.immortalYuanReward(data));

        data.setStage(9);
        assertEquals(3_200L, ImmortalPillItem.immortalYuanReward(data));

        data.setStage(10);
        assertEquals(25_600L, ImmortalPillItem.immortalYuanReward(data));
    }
}
