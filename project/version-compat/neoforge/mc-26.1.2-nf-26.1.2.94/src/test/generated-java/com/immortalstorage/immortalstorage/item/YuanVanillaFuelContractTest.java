package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class YuanVanillaFuelContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void yuanBurnTimesMatchTenAndFiveHundredVanillaSmelts() {
        assertEquals(2_000, TrueYuanItem.VANILLA_BURN_TICKS);
        assertEquals(100_000, ImmortalYuanItem.VANILLA_BURN_TICKS);
    }
}
