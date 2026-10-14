package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class SpiritCompressionOutputsTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }
    private static ItemStackHandler bulk(int slots) {
        return new ItemStackHandler(slots) { @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; } };
    }
    @Test void oneWholeInputStackGeneratesBothBulkResultsTogether() {
        var inventory = bulk(2);
        var plan = SpiritCompressionOutputs.plan(inventory, 0, 2,
                List.of(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.RAW_COPPER, 8)), 64);
        assertNotNull(plan);
        assertEquals(64, plan.get(0).getCount());
        assertEquals(512, plan.get(1).getCount());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertTrue(inventory.getStackInSlot(1).isEmpty());
    }
    @Test void blockedSecondResultDoesNotChangeTheFirstResultSlot() {
        var inventory = bulk(2);
        inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        inventory.setStackInSlot(1, new ItemStack(Items.DIRT, 64));
        assertNull(SpiritCompressionOutputs.plan(inventory, 0, 2,
                List.of(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.RAW_IRON, 4)), 64));
        assertEquals(64, inventory.getStackInSlot(0).getCount());
        assertEquals(64, inventory.getStackInSlot(1).getCount());
    }
    @Test void duplicateResultsShareCapacityWithoutDuplicatingItems() {
        var inventory = bulk(1);
        var plan = SpiritCompressionOutputs.plan(inventory, 0, 1,
                List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND, 4)), 64);
        assertNotNull(plan);
        assertEquals(320, plan.getFirst().getCount());
    }
    @Test void finiteSlotCapacityCanSplitResultsOrRejectTheWholeTransaction() {
        var inventory = new ItemStackHandler(2) { @Override public int getSlotLimit(int slot) { return 64; } };
        assertNotNull(SpiritCompressionOutputs.plan(inventory, 0, 2, List.of(new ItemStack(Items.RAW_IRON, 4)), 32));
        assertNull(SpiritCompressionOutputs.plan(inventory, 0, 2, List.of(new ItemStack(Items.RAW_IRON, 4)), 33));
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertTrue(inventory.getStackInSlot(1).isEmpty());
    }
}
