package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ImmortalFurnaceJeiCategory implements IRecipeCategory<RecipeHolder<AbstractCookingRecipe>> {
    public static final IRecipeType<RecipeHolder<AbstractCookingRecipe>> TYPE =
            IRecipeHolderType.create(Identifier.fromNamespaceAndPath(
                    ImmortalStorageMod.MODID, "immortal_furnace"));
    private final IDrawable icon;

    public ImmortalFurnaceJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.IMMORTAL_FURNACE.get());
    }

    @Override public IRecipeType<RecipeHolder<AbstractCookingRecipe>> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.immortalstorage.immortal_furnace"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 82; }
    @Override public int getHeight() { return 28; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AbstractCookingRecipe> holder,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 6)
                .addIngredients(holder.value().input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 6)
                .addItemStack(holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY)));
    }
}
