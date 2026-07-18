package com.cultivation.cultivation.item;

import com.cultivation.cultivation.item.custom.ImmortalYuanItem;
import com.cultivation.cultivation.item.custom.TrueYuanItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class YuanVanillaFuelContractTest {
    @Test
    void yuanBurnTimesMatchTenAndFiveHundredVanillaSmelts() {
        assertEquals(2_000, TrueYuanItem.VANILLA_BURN_TICKS);
        assertEquals(100_000, ImmortalYuanItem.VANILLA_BURN_TICKS);
    }
}
