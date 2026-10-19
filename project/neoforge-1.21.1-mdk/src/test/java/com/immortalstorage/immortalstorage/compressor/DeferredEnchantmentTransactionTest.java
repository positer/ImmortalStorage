package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DeferredEnchantmentTransactionTest {
    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    private static int[] cost(int slot, int amount) {
        int[] consume = new int[13];
        consume[slot] = amount;
        return consume;
    }

    private static boolean[] strip(int slot) {
        boolean[] strip = new boolean[13];
        strip[slot] = true;
        return strip;
    }

    private static ItemStackHandler loadedInventory() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var inventory = new ItemStackHandler(13);
        inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));
        inventory.setStackInSlot(12, new ItemStack(Items.ENCHANTED_BOOK, 3));
        return inventory;
    }

    @Test void takingTheProductPaysTheCostExactlyOnce() {
        var inventory = loadedInventory();
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 1), strip(0));
        assertTrue(transaction.pending());
        assertEquals(2, inventory.getStackInSlot(0).getCount(), "nothing is paid before the product leaves");

        transaction.afterExtract(inventory, 12, 12);
        assertFalse(transaction.pending());
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertTrue(EnchantmentStackRules.enchantments(inventory.getStackInSlot(0)).isEmpty());

        assertFalse(transaction.afterExtract(inventory, 12, 12), "a second take must not charge again");
        assertEquals(1, inventory.getStackInSlot(0).getCount());
    }

    @Test void aPartialTakeOfTheProductStillPaysTheWholeRegisteredCost() {
        var inventory = loadedInventory();
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 1), strip(0));
        ItemStack taken = inventory.extractItem(12, 1, false);
        assertEquals(1, taken.getCount());
        transaction.afterExtract(inventory, 12, 12);
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertFalse(transaction.pending());
    }

    @Test void takingFromAnInputSlotNeverSettlesAnything() {
        var inventory = loadedInventory();
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 1), strip(0));
        transaction.afterExtract(inventory, 0, 12);
        assertTrue(transaction.pending());
        assertEquals(2, inventory.getStackInSlot(0).getCount());
    }

    @Test void settlingPaysTheRegisteredCostOnceAndNeverTwice() {
        var inventory = loadedInventory();
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 1), strip(0));
        assertTrue(transaction.settle(inventory));
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertTrue(EnchantmentStackRules.enchantments(inventory.getStackInSlot(0)).isEmpty());
        assertFalse(transaction.pending());
        assertFalse(transaction.settle(inventory), "settling a spent transaction changes nothing");
        assertEquals(1, inventory.getStackInSlot(0).getCount());
    }

    @Test void settleConsumesTheWholeRegisteredCountWhenOneExtractionCannotDeliverIt() {
        // A wrapper that never hands out more than one item per call, independent of the item's own stack size.
        var inventory = new ItemStackHandler(13) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return super.extractItem(slot, Math.min(1, amount), simulate);
            }
        };
        inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 3), strip(0));

        assertTrue(transaction.settle(inventory));
        assertTrue(inventory.getStackInSlot(0).isEmpty(),
                "the registered count is owed in full even when one extraction cannot deliver it");
        assertFalse(transaction.pending());
    }

    @Test void aTeardownDiscardsTheUncollectedProductAndPaysNothing() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var inventory = new ItemStackHandler(13);
        inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));
        inventory.setStackInSlot(12, new ItemStack(Items.BOOK));
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(0, 1), strip(0));

        assertTrue(transaction.discard(inventory, 12));
        assertTrue(inventory.getStackInSlot(12).isEmpty(), "the uncollected product is deleted, never dropped");
        assertEquals(2, inventory.getStackInSlot(0).getCount(), "it was never produced, so nothing is charged");
        assertEquals(5, EnchantmentStackRules.enchantments(inventory.getStackInSlot(0)).getLevel(sharpness),
                "and the material is not stripped either");
        assertFalse(transaction.pending());
        assertFalse(transaction.settle(inventory), "nothing is left to settle");
        assertEquals(2, inventory.getStackInSlot(0).getCount());
    }

    @Test void discardWithoutAPendingProductLeavesPlayerOwnedOutputsAlone() {
        var inventory = new ItemStackHandler(13);
        inventory.setStackInSlot(12, new ItemStack(Items.BOOK, 2));
        var transaction = new DeferredEnchantmentTransaction();

        assertFalse(transaction.discard(inventory, 12));
        assertEquals(2, inventory.getStackInSlot(12).getCount(),
                "after a partial take the leftovers belong to the player and a teardown drops them normally");
    }

    @Test void aReloadedTransactionStaysPendingUntilTheNextTake() {
        var inventory = loadedInventory();
        var transaction = new DeferredEnchantmentTransaction();
        transaction.restore(cost(0, 1), strip(0));
        assertTrue(transaction.pending());
        assertEquals(2, inventory.getStackInSlot(0).getCount());
        transaction.afterExtract(inventory, 12, 12);
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertFalse(transaction.pending());
    }

    @Test void restoringNothingLeavesTheMachineUnblocked() {
        var transaction = new DeferredEnchantmentTransaction();
        transaction.restore(null, null);
        assertFalse(transaction.pending());
        transaction.restore(new int[0], new boolean[0]);
        assertFalse(transaction.pending());
        assertNull(transaction.consume());
        assertNull(transaction.strip());
    }

    @Test void theRegisteredVectorsSurviveAFullRoundTripThroughTheirAccessors() {
        var transaction = new DeferredEnchantmentTransaction();
        transaction.arm(cost(3, 2), strip(7));
        assertArrayEquals(cost(3, 2), transaction.consume());
        assertArrayEquals(strip(7), transaction.strip());
        transaction.clear();
        assertFalse(transaction.pending());
    }
}
