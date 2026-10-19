package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class EnchantmentStackRulesTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    @Test void keyedEnchantmentsSortByNameAndAKeylessOneSortsAfterEveryKeyedOne() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var direct = Holder.direct(EnchantmentTestSupport.value("test:direct"));
        var book = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 3);
        EnchantmentTestSupport.enchanted(book, direct, 2);
        EnchantmentTestSupport.enchanted(book, protection, 1);

        var enchantments = EnchantmentStackRules.enchantments(book);
        var ordered = EnchantmentStackRules.ordered(enchantments);
        assertEquals(3, ordered.size());
        assertSame(protection, ordered.get(0));
        assertSame(sharpness, ordered.get(1));
        assertSame(direct, ordered.get(2));
        assertEquals(Optional.empty(), ordered.get(2).unwrapKey());
        // The same input must always produce the same sequence, so a split is reproducible.
        assertEquals(ordered, EnchantmentStackRules.ordered(enchantments));
    }

    @Test void mergingKeepsTheHighestLevelPerEnchantmentAndIgnoresVanillaConflicts() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var smite = EnchantmentTestSupport.enchantment("minecraft:smite");
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        var first = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 3);
        EnchantmentTestSupport.enchanted(first, protection, 2);
        var second = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 5);
        EnchantmentTestSupport.enchanted(second, smite, 4);

        var merged = EnchantmentStackRules.highest(EnchantmentStackRules.enchantments(first),
                EnchantmentStackRules.enchantments(second));
        assertEquals(3, merged.size(), "damage and undead-damage enchantments merge despite being exclusive");
        assertEquals(5, merged.getLevel(sharpness));
        assertEquals(2, merged.getLevel(protection));
        assertEquals(4, merged.getLevel(smite));
    }

    @Test void aStrippedItemKeepsNoEmptyEnchantmentComponent() {
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var sword = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD), sharpness, 5);
        EnchantmentStackRules.clear(sword);
        assertNull(sword.get(DataComponents.ENCHANTMENTS));
        assertNull(sword.get(DataComponents.STORED_ENCHANTMENTS));
        assertTrue(EnchantmentStackRules.enchantments(sword).isEmpty());
    }

    @Test void writingAnEmptySetClearsInsteadOfStoringAnEmptyComponent() {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentStackRules.write(sword, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        assertNull(sword.get(DataComponents.ENCHANTMENTS));
    }

    @Test void booksAndCarriersAreAcceptedButBulkBlocksAreNot() {
        assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.BOOK)));
        assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.ENCHANTED_BOOK)));
        assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.LEATHER_HELMET)));
        assertFalse(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.DIRT)));
        assertFalse(EnchantmentStackRules.acceptsCarrier(new ItemStack(Items.COBBLESTONE)));
        assertFalse(EnchantmentStackRules.acceptsCarrier(ItemStack.EMPTY));
        var enchantedStick = EnchantmentTestSupport.enchanted(new ItemStack(Items.STICK),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 1);
        assertTrue(EnchantmentStackRules.acceptsCarrier(enchantedStick),
                "anything already carrying enchantments can be emptied into the merge");
    }
}
