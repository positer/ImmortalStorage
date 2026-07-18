package com.cultivation.cultivation.recipe;

import com.cultivation.cultivation.block.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Data-driven recipes that are intentionally exclusive to the immortal furnace. */
public final class ImmortalFurnaceRecipe extends AbstractCookingRecipe {
    public ImmortalFurnaceRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                                 ItemStack result, float experience, int cookingTime) {
        super(ModRecipes.IMMORTAL_FURNACE_TYPE.get(), group, category, ingredient,
                result, experience, cookingTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModBlocks.IMMORTAL_FURNACE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.IMMORTAL_FURNACE_SERIALIZER.get();
    }
}
