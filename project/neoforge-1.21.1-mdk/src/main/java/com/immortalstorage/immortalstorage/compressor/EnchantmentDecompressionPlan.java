package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Design section 6: one input item is split, in registry-name order, into as many single-enchantment
 * books as the output row can hold. An item always comes back, holding the remainder when the split
 * cannot fit it; a book is spent, and only comes back as the enchanted remainder holder.
 */
public final class EnchantmentDecompressionPlan {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_SLOTS = 12;
    /** A split fills at most eleven book slots and leaves the last output slot to a remainder carrier. */
    private static final int SEPARATE_BOOKS = OUTPUT_SLOTS - 1;
    private static final int SLOTS = 13;

    /** Null when the input carries no enchantment at all. */
    public static Result plan(ItemStack input) {
        if (input.isEmpty()) return null;
        ItemEnchantments enchantments = EnchantmentStackRules.enchantments(input);
        if (enchantments.isEmpty()) return null;
        List<Holder<Enchantment>> ordered = EnchantmentStackRules.ordered(enchantments);
        int books = Math.min(ordered.size(), SEPARATE_BOOKS);
        // A spent book carrier only comes back when a split of eleven books cannot hold the remainder.
        boolean carried = !EnchantmentStackRules.isBook(input) || ordered.size() > SEPARATE_BOOKS;
        ItemEnchantments.Mutable remaining = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (int index = books; index < ordered.size(); index++)
            remaining.set(ordered.get(index), enchantments.getLevel(ordered.get(index)));
        ArrayList<ItemStack> outputs = new ArrayList<>(OUTPUT_SLOTS);
        for (int index = 0; index < books; index++) {
            ItemEnchantments.Mutable single = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            single.set(ordered.get(index), enchantments.getLevel(ordered.get(index)));
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentStackRules.write(book, single.toImmutable());
            outputs.add(book);
        }
        while (outputs.size() < OUTPUT_SLOTS) outputs.add(ItemStack.EMPTY);
        if (carried) outputs.set(books, carrier(input, remaining.toImmutable()));
        int[] consume = new int[SLOTS];
        consume[INPUT_SLOT] = 1;
        return new Result(List.copyOf(outputs), consume, new boolean[SLOTS]);
    }

    /** An item keeps its identity and whatever did not fit; a book comes back only as the remainder holder. */
    private static ItemStack carrier(ItemStack input, ItemEnchantments remaining) {
        if (!EnchantmentStackRules.isBook(input)) {
            ItemStack carrier = input.copyWithCount(1);
            EnchantmentStackRules.write(carrier, remaining);
            return carrier;
        }
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentStackRules.write(book, remaining);
        return book;
    }

    /** One stack per output slot, index 0 being the first output slot. */
    public record Result(List<ItemStack> outputs, int[] consume, boolean[] strip) {}

    private EnchantmentDecompressionPlan() {}
}
