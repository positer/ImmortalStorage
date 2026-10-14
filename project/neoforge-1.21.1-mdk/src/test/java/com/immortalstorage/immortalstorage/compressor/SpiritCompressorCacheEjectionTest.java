package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class SpiritCompressorCacheEjectionTest {
    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }
    @Test void drainsBothModesAndSplitsBulkOutputsWithoutChangingQuantities() {
        var normal = new ItemStackHandler(13); var inverse = new ItemStackHandler(13);
        normal.setStackInSlot(0, new ItemStack(Items.DIAMOND, 32));
        normal.setStackInSlot(12, new ItemStack(Items.COAL, 129));
        inverse.setStackInSlot(0, new ItemStack(Items.COPPER_ORE, 64));
        inverse.setStackInSlot(2, new ItemStack(Items.RAW_COPPER, 512));
        List<ItemStack> dropped = new ArrayList<>();
        assertTrue(SpiritCompressorCacheEjection.eject(List.of(normal, inverse), s -> { dropped.add(s.copy()); return true; }));
        assertEquals(737, dropped.stream().mapToInt(ItemStack::getCount).sum());
        assertTrue(dropped.stream().allMatch(s -> s.getCount() <= s.getMaxStackSize()));
        assertEquals(129, dropped.stream().filter(s -> s.is(Items.COAL)).mapToInt(ItemStack::getCount).sum());
        for (var handler : List.of(normal, inverse)) for (int i = 0; i < 13; i++) assertTrue(handler.getStackInSlot(i).isEmpty());
        int previous = dropped.size();
        assertTrue(SpiritCompressorCacheEjection.eject(List.of(normal, inverse), s -> { dropped.add(s.copy()); return true; }));
        assertEquals(previous, dropped.size());
    }
    @Test void rejectedDropPreservesItsUnspawnedStack() {
        var inventory = new ItemStackHandler(13);
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 32));
        assertFalse(SpiritCompressorCacheEjection.eject(List.of(inventory), s -> false));
        assertEquals(32, inventory.getStackInSlot(0).getCount());
    }
}
