package com.immortalstorage.immortalstorage.recipe;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Target 26.1 cooking recipe using the official common/book-info constructor. */
public final class ImmortalFurnaceRecipe extends AbstractCookingRecipe {
    public ImmortalFurnaceRecipe(Recipe.CommonInfo commonInfo,
                                 AbstractCookingRecipe.CookingBookInfo bookInfo,
                                 Ingredient ingredient,
                                 ItemStackTemplate result,
                                 float experience,
                                 int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }

    @Override
    public RecipeType<? extends AbstractCookingRecipe> getType() {
        return ModRecipes.IMMORTAL_FURNACE_TYPE.get();
    }

    @Override
    public RecipeSerializer<? extends AbstractCookingRecipe> getSerializer() {
        return ModRecipes.IMMORTAL_FURNACE_SERIALIZER.get();
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC;
    }

    @Override
    protected net.minecraft.world.item.Item furnaceIcon() {
        return ModBlocks.IMMORTAL_FURNACE.get().asItem();
    }
}
