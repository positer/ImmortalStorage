package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Level-free enchantment primitives shared by both enchantment-machine planners. */
public final class EnchantmentStackRules {
    /** Registry-name order, so splitting one item always produces the same books in the same slots. */
    public static List<Holder<Enchantment>> ordered(ItemEnchantments enchantments) {
        ArrayList<Holder<Enchantment>> holders = new ArrayList<>(enchantments.keySet());
        // Keyed entries first, ascending by registry name; a holder without a key sorts after all of
        // them, and the stable sort keeps the encounter order of those ties reproducible.
        holders.sort(Comparator.comparingInt((Holder<Enchantment> holder) -> holder.unwrapKey().isPresent() ? 0 : 1)
                .thenComparing(EnchantmentStackRules::registryName));
        return holders;
    }

    /** The registry name of one enchantment, or an empty string when the holder is not registered. */
    public static String registryName(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(ResourceKey::identifier).map(Object::toString).orElse("");
    }

    /** The enchantments an item actually offers: stored ones for books, applied ones otherwise. */
    public static ItemEnchantments enchantments(ItemStack stack) {
        return stack.isEmpty() ? ItemEnchantments.EMPTY : EnchantmentHelper.getEnchantmentsForCrafting(stack);
    }

    /** Highest level wins per enchantment; vanilla conflicts are deliberately ignored. */
    public static ItemEnchantments highest(ItemEnchantments... sources) {
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (ItemEnchantments source : sources)
            for (Holder<Enchantment> enchantment : source.keySet()) merged.upgrade(enchantment, source.getLevel(enchantment));
        return merged.toImmutable();
    }

    /** Writes the whole set, leaving no empty enchantment component behind when nothing remains. */
    public static void write(ItemStack stack, ItemEnchantments enchantments) {
        if (stack.isEmpty()) return;
        if (enchantments.isEmpty()) clear(stack); else EnchantmentHelper.setEnchantments(stack, enchantments);
    }

    public static void clear(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.remove(DataComponents.ENCHANTMENTS);
        stack.remove(DataComponents.STORED_ENCHANTMENTS);
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /** Books, and anything that can carry enchantments whether it has any yet or not. */
    public static boolean acceptsCarrier(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isBook(stack) || stack.has(net.minecraft.core.component.DataComponents.ENCHANTABLE) || !enchantments(stack).isEmpty();
    }

    private EnchantmentStackRules() {}
}
