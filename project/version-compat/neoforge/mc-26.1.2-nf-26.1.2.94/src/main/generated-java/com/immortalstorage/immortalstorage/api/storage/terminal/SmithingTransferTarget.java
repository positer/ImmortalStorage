package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.List;

/** Recipe-viewer contract for an embedded three-input smithing table. */
public interface SmithingTransferTarget {
    boolean isSmithingUnlocked();
    boolean isSmithingVisible();
    List<Slot> smithingInputSlots();
    Slot smithingResultSlotView();
    List<Slot> smithingSourceSlots();
    List<CraftingTransferTarget.TransferIngredient> smithingStorageIngredients();
    boolean transferSmithingRecipe(RecipeHolder<SmithingRecipe> recipe, long expectedRevision);
}
