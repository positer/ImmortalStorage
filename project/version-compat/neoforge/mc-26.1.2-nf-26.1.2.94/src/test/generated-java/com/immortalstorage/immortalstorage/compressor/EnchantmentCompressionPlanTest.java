package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class EnchantmentCompressionPlanTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    private static ItemStack[] emptyInputs() {
        ItemStack[] slots = new ItemStack[13];
        Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }

    @Test void booksOnlyProduceOneMergedBookAndConsumeEveryWholeStack() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var slots = emptyInputs();
        slots[0] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 4);
        slots[5] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), protection, 2);
        slots[7] = new ItemStack(Items.BOOK, 5);
        slots[11] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 1);

        var plan = EnchantmentCompressionPlan.plan(slots);
        assertNotNull(plan);
        assertTrue(plan.output().is(Items.ENCHANTED_BOOK));
        assertEquals(1, plan.output().getCount());
        var merged = EnchantmentStackRules.enchantments(plan.output());
        assertEquals(2, merged.size());
        assertEquals(4, merged.getLevel(sharpness), "the highest level wins instead of adding up");
        assertEquals(2, merged.getLevel(protection));
        var stored = plan.output().get(DataComponents.STORED_ENCHANTMENTS);
        assertNotNull(stored);
        assertEquals(2, stored.size(), "a produced book carries its enchantments in the stored component");
        assertTrue(plan.output().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                "the merged set lives in the stored component, never in the applied one");
        int[] expectedConsume = new int[13];
        expectedConsume[0] = 1;
        expectedConsume[5] = 1;
        expectedConsume[7] = 5;
        expectedConsume[11] = 1;
        assertArrayEquals(expectedConsume, plan.consume());
        assertArrayEquals(new boolean[13], plan.strip());
    }

    @Test void theLowestIndexNonBookItemIsTheCarrierEvenWhenABookComesFirst() {
        // v1.2 ruling: a book is material only, so the pickaxe is the received item, not the book above it.
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var efficiency = EnchantmentTestSupport.enchantment("minecraft:efficiency");
        var slots = emptyInputs();
        slots[0] = new ItemStack(Items.BOOK, 5);
        slots[1] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), unbreaking, 3);
        slots[2] = EnchantmentTestSupport.enchanted(new ItemStack(Items.IRON_PICKAXE, 2), efficiency, 4);

        var plan = EnchantmentCompressionPlan.plan(slots);
        assertNotNull(plan);
        assertTrue(plan.output().is(Items.IRON_PICKAXE), "the book can never be the received carrier");
        assertEquals(1, plan.output().getCount());
        var merged = EnchantmentStackRules.enchantments(plan.output());
        assertEquals(3, merged.getLevel(unbreaking));
        assertEquals(4, merged.getLevel(efficiency));
        int[] expectedConsume = new int[13];
        expectedConsume[0] = 5;
        expectedConsume[1] = 1;
        expectedConsume[2] = 1;
        assertArrayEquals(expectedConsume, plan.consume());
        assertArrayEquals(new boolean[13], plan.strip(), "the target is charged, never stripped in place");
    }

    @Test void otherNonBookItemsAreStrippedInPlaceWhileBooksAreConsumedWhole() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var slots = emptyInputs();
        slots[1] = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 4), sharpness, 3);
        slots[2] = new ItemStack(Items.DIAMOND_SWORD, 2);
        slots[3] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK, 2), protection, 2);

        var plan = EnchantmentCompressionPlan.plan(slots);
        assertNotNull(plan);
        assertTrue(plan.output().is(Items.DIAMOND_SWORD));
        assertEquals(1, plan.output().getCount());
        var merged = EnchantmentStackRules.enchantments(plan.output());
        assertEquals(3, merged.getLevel(sharpness));
        assertEquals(2, merged.getLevel(protection));
        assertNull(plan.output().get(DataComponents.STORED_ENCHANTMENTS));
        assertNotNull(plan.output().get(DataComponents.ENCHANTMENTS), "an item carries its enchantments in the applied component");
        int[] expectedConsume = new int[13];
        expectedConsume[1] = 1;
        expectedConsume[3] = 2;
        assertArrayEquals(expectedConsume, plan.consume());
        boolean[] expectedStrip = new boolean[13];
        expectedStrip[2] = true;
        assertArrayEquals(expectedStrip, plan.strip());
    }

    @Test void theLowestIndexNonBookSlotIsTheTargetAndOnlyItLosesASingleItem() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var slots = emptyInputs();
        slots[4] = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 3), sharpness, 2);
        slots[7] = new ItemStack(Items.GOLDEN_SWORD, 1);

        var plan = EnchantmentCompressionPlan.plan(slots);
        assertTrue(plan.output().is(Items.DIAMOND_SWORD));
        assertEquals(1, plan.consume()[4]);
        assertFalse(plan.strip()[4], "the target keeps the item it was charged for");
        assertTrue(plan.strip()[7]);
    }

    @Test void aPlainBookAloneIsMaterialAndProducesNothing() {
        var slots = emptyInputs();
        slots[0] = new ItemStack(Items.BOOK, 5);
        assertNull(EnchantmentCompressionPlan.plan(slots), "a book has no enchantments to merge and can never be the carrier");
    }

    @Test void nothingIsPlannedWithoutAnEnchantmentToMerge() {
        var slots = emptyInputs();
        assertNull(EnchantmentCompressionPlan.plan(slots));
        slots[0] = new ItemStack(Items.DIAMOND_SWORD, 2);
        assertNull(EnchantmentCompressionPlan.plan(slots));
        slots[0] = ItemStack.EMPTY;
        slots[12] = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 1);
        assertNull(EnchantmentCompressionPlan.plan(slots), "the product slot is never scanned as an input");
    }
}
