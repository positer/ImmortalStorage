package com.cultivation.cultivation.recipe;

import com.cultivation.cultivation.item.custom.ImmortalYuanItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Remaining-item policy used only by recipes where immortal yuan substitutes for true yuan. */
final class YuanSubstitutionRecipeSupport {
    private YuanSubstitutionRecipeSupport() {}

    static NonNullList<ItemStack> remainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof ImmortalYuanItem) {
                remaining.set(slot, stack.copyWithCount(1));
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(slot, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }
}
