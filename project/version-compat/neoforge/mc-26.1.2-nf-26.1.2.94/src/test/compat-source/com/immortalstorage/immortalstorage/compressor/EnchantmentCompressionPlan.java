package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Design section 5: every input contributes its enchantments to one carrier, and the material cost
 * is registered instead of paid so the product can never leave without it. Books are material only:
 * without a non-book item the whole merge becomes one book, otherwise the lowest-index non-book item
 * is copied and receives the merged set.
 */
public final class EnchantmentCompressionPlan {
    public static final int INPUT_SLOTS = 12;
    public static final int OUTPUT_SLOT = 12;
    private static final int SLOTS = 13;

    /** Null when no enchantment can be merged; the caller then leaves every slot untouched. */
    public static Result plan(ItemStack[] slots) {
        ItemEnchantments[] sources = new ItemEnchantments[INPUT_SLOTS];
        for (int slot = 0; slot < INPUT_SLOTS; slot++) sources[slot] = EnchantmentStackRules.enchantments(slots[slot]);
        ItemEnchantments merged = EnchantmentStackRules.highest(sources);
        if (merged.isEmpty()) return null;
        int[] consume = new int[SLOTS];
        boolean[] strip = new boolean[SLOTS];
        int target = firstNonBookItem(slots);
        if (target < 0) {
            // Books only: every stack is material, and the whole merge becomes one enchanted book.
            for (int slot = 0; slot < INPUT_SLOTS; slot++) if (!slots[slot].isEmpty()) consume[slot] = slots[slot].getCount();
            ItemStack output = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentStackRules.write(output, merged);
            return new Result(output, consume, strip);
        }
        consume[target] = 1;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            if (slots[slot].isEmpty() || slot == target) continue;
            if (EnchantmentStackRules.isBook(slots[slot])) consume[slot] = slots[slot].getCount();
            else strip[slot] = true;
        }
        ItemStack output = slots[target].copyWithCount(1);
        EnchantmentStackRules.write(output, merged);
        return new Result(output, consume, strip);
    }

    /** The carrier is the lowest-index non-book item; no book can ever be the received item. */
    private static int firstNonBookItem(ItemStack[] slots) {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            if (!slots[slot].isEmpty() && !EnchantmentStackRules.isBook(slots[slot])) return slot;
        }
        return -1;
    }

    /** Consume counts per input slot; strip marks the slots that keep their item but lose their enchantments. */
    public record Result(ItemStack output, int[] consume, boolean[] strip) {}

    private EnchantmentCompressionPlan() {}
}
