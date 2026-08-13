package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TreasureBasinMultiplierTest {
    @Test
    void oneLootRollIsMultipliedAsOutputRatherThanRepeated() {
        List<ItemStack> output = TreasureBasinBlockEntity.multiplyOutputs(
                List.of(new ItemStack(Items.DIAMOND, 3)), 16);

        assertEquals(1, output.size());
        assertEquals(48, output.getFirst().getCount());
    }

    @Test
    void oversizedProductsAreSplitWithoutLosingTheMultiplier() {
        List<ItemStack> output = TreasureBasinBlockEntity.multiplyOutputs(
                List.of(new ItemStack(Items.DIAMOND, 2)), 256);

        assertEquals(8, output.size());
        assertEquals(512, output.stream().mapToInt(ItemStack::getCount).sum());
    }
}
