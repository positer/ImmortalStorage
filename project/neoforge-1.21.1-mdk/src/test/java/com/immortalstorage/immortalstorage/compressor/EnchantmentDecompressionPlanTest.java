package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class EnchantmentDecompressionPlanTest {
    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    @Test void splittingABookLeavesNoCarrierBehind() {
        // v1.2 ruling: the spent book is consumed, so N <= 11 produces exactly N books and nothing else.
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), protection, 2);
        EnchantmentTestSupport.enchanted(input, sharpness, 3);
        EnchantmentTestSupport.enchanted(input, unbreaking, 1);

        var plan = EnchantmentDecompressionPlan.plan(input);
        assertNotNull(plan);
        assertEquals(12, plan.outputs().size());
        assertEquals("minecraft:protection", EnchantmentTestSupport.singleName(plan.outputs().get(0)));
        assertEquals("minecraft:sharpness", EnchantmentTestSupport.singleName(plan.outputs().get(1)));
        assertEquals("minecraft:unbreaking", EnchantmentTestSupport.singleName(plan.outputs().get(2)));
        assertEquals(2, EnchantmentStackRules.enchantments(plan.outputs().get(0)).getLevel(protection));
        for (int index = 3; index < 12; index++)
            assertTrue(plan.outputs().get(index).isEmpty(), "a spent book is never returned: slot " + index + " must stay empty");
        int[] expectedConsume = new int[13];
        expectedConsume[0] = 1;
        assertArrayEquals(expectedConsume, plan.consume());
        assertArrayEquals(new boolean[13], plan.strip());
    }

    @Test void splitBooksCarryExactlyOneEnchantmentInTheStoredComponent() {
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), protection, 2);
        EnchantmentTestSupport.enchanted(input, sharpness, 3);

        var plan = EnchantmentDecompressionPlan.plan(input);
        for (int index = 0; index < 2; index++) {
            ItemStack book = plan.outputs().get(index);
            assertTrue(book.is(Items.ENCHANTED_BOOK));
            var stored = book.get(DataComponents.STORED_ENCHANTMENTS);
            assertNotNull(stored);
            assertEquals(1, stored.size());
            assertFalse(stored.isEmpty());
            assertTrue(book.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                    "the single enchantment lives in the stored component, never in the applied one");
            assertEquals(1, EnchantmentStackRules.enchantments(book).size());
        }
        assertTrue(plan.outputs().get(2).isEmpty(), "no plain book is returned for a split book");
        for (int index = 3; index < 12; index++) assertTrue(plan.outputs().get(index).isEmpty());
    }

    @Test void elevenEnchantmentsOnABookFillEverySlotWithBooks() {
        var input = new ItemStack(Items.ENCHANTED_BOOK);
        for (int index = 0; index < 11; index++)
            EnchantmentTestSupport.enchanted(input, EnchantmentTestSupport.enchantment(String.format("test:e%02d", index)), index + 1);

        var plan = EnchantmentDecompressionPlan.plan(input);
        for (int index = 0; index < 11; index++) assertEquals(String.format("test:e%02d", index), EnchantmentTestSupport.singleName(plan.outputs().get(index)));
        assertTrue(plan.outputs().get(11).isEmpty(), "a book carrier is spent, so there is no carrier slot to fill");
    }

    @Test void thirteenEnchantmentsOnABookKeepTheRemainderOnAnEnchantedBook() {
        var input = new ItemStack(Items.ENCHANTED_BOOK);
        for (int index = 0; index < 13; index++)
            EnchantmentTestSupport.enchanted(input, EnchantmentTestSupport.enchantment(String.format("test:e%02d", index)), index + 1);

        var plan = EnchantmentDecompressionPlan.plan(input);
        for (int index = 0; index < 11; index++) assertEquals(String.format("test:e%02d", index), EnchantmentTestSupport.singleName(plan.outputs().get(index)));
        ItemStack carrier = plan.outputs().get(11);
        assertTrue(carrier.is(Items.ENCHANTED_BOOK), "the only reason a book carrier comes back is the remainder");
        var remaining = EnchantmentStackRules.enchantments(carrier);
        assertEquals(2, remaining.size());
        assertEquals(Set.of("test:e11", "test:e12"),
                remaining.keySet().stream().map(EnchantmentStackRules::registryName).collect(Collectors.toSet()));
    }

    @Test void anItemCarrierKeepsItsIdentityAndItsUnsplitEnchantments() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 4), sharpness, 5);
        EnchantmentTestSupport.enchanted(input, unbreaking, 3);

        var plan = EnchantmentDecompressionPlan.plan(input);
        assertEquals(1, plan.outputs().get(0).getCount());
        assertTrue(plan.outputs().get(2).is(Items.DIAMOND_SWORD));
        assertTrue(EnchantmentStackRules.enchantments(plan.outputs().get(2)).isEmpty(), "every enchantment moved onto a book");
    }

    @Test void thirteenEnchantmentsLeaveElevenBooksAndTheRemainderOnTheItem() {
        var input = new ItemStack(Items.DIAMOND_SWORD);
        for (int index = 0; index < 13; index++)
            EnchantmentTestSupport.enchanted(input, EnchantmentTestSupport.enchantment(String.format("test:e%02d", index)), index + 1);

        var plan = EnchantmentDecompressionPlan.plan(input);
        for (int index = 0; index < 11; index++) assertEquals(String.format("test:e%02d", index), EnchantmentTestSupport.singleName(plan.outputs().get(index)));
        ItemStack carrier = plan.outputs().get(11);
        assertTrue(carrier.is(Items.DIAMOND_SWORD));
        var remaining = EnchantmentStackRules.enchantments(carrier);
        assertEquals(2, remaining.size());
        assertEquals(Set.of("test:e11", "test:e12"),
                remaining.keySet().stream().map(EnchantmentStackRules::registryName).collect(Collectors.toSet()));
    }

    @Test void anUnenchantedOrEmptyInputIsRejected() {
        assertNull(EnchantmentDecompressionPlan.plan(new ItemStack(Items.DIAMOND_SWORD)));
        assertNull(EnchantmentDecompressionPlan.plan(new ItemStack(Items.ENCHANTED_BOOK)));
        assertNull(EnchantmentDecompressionPlan.plan(ItemStack.EMPTY));
    }
}
