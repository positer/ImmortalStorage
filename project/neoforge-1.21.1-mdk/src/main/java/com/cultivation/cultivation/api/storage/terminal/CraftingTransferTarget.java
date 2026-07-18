package com.cultivation.cultivation.api.storage.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** Optional recipe-viewer contract for a real 3x3 terminal crafting grid. */
public interface CraftingTransferTarget {
    List<Slot> craftingInputSlots();
    Slot craftingResultSlotView();

    /** Player inventory and the existing 3x3 grid; never includes read-only terminal proxies. */
    List<Slot> craftingSourceSlots();

    /** Full logical storage amounts, including entries outside the current viewport. */
    List<TransferIngredient> craftingStorageIngredients();

    /** Server-authoritative recipe placement with menu/revision validation. */
    boolean transferCraftingRecipe(RecipeHolder<CraftingRecipe> recipe, int requestedSets, long expectedRevision);

    /** Accept one validated read-only recipe-source chunk on the client. */
    default void applyRecipeSourceChunk(long revision, int chunkIndex, int chunkCount,
                                        List<TransferIngredient> entries) {}

    record TransferIngredient(ItemStack stack, long amount) {
        public TransferIngredient {
            if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("stack must be non-empty");
            if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
            stack = stack.copyWithCount(1);
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
