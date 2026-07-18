package com.cultivation.cultivation.api.storage.terminal;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Long-safe recipe availability check that never materializes aggregate counts as stacks. */
public final class TerminalRecipeAvailability {
    private TerminalRecipeAvailability() {}

    public static boolean hasIngredients(
            List<CraftingTransferTarget.TransferIngredient> logicalStorage,
            List<ItemStack> physicalSources,
            List<Ingredient> ingredients) {
        Objects.requireNonNull(logicalStorage, "logicalStorage");
        Objects.requireNonNull(physicalSources, "physicalSources");
        Objects.requireNonNull(ingredients, "ingredients");

        List<MutableSource> available = new ArrayList<>(logicalStorage.size() + physicalSources.size());
        for (CraftingTransferTarget.TransferIngredient source : logicalStorage) {
            available.add(new MutableSource(source.stack(), source.amount()));
        }
        for (ItemStack stack : physicalSources) {
            if (stack != null && !stack.isEmpty()) {
                available.add(new MutableSource(stack, stack.getCount()));
            }
        }

        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            boolean claimed = false;
            for (MutableSource source : available) {
                if (source.amount <= 0L || !ingredient.test(source.prototype)) continue;
                source.amount--;
                claimed = true;
                break;
            }
            if (!claimed) return false;
        }
        return true;
    }

    private static final class MutableSource {
        private final ItemStack prototype;
        private long amount;

        private MutableSource(ItemStack prototype, long amount) {
            this.prototype = prototype.copyWithCount(1);
            this.amount = amount;
        }
    }
}
