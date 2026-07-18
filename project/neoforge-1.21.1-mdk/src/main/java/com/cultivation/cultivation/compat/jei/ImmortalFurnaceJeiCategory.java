package com.cultivation.cultivation.compat.jei;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ImmortalFurnaceJeiCategory implements IRecipeCategory<RecipeHolder<AbstractCookingRecipe>> {
    public static final RecipeType<RecipeHolder<AbstractCookingRecipe>> TYPE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(
                    CultivationMod.MODID, "immortal_furnace"));
    private final IDrawable icon;

    public ImmortalFurnaceJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.IMMORTAL_FURNACE.get());
    }

    @Override public RecipeType<RecipeHolder<AbstractCookingRecipe>> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.cultivation.immortal_furnace"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 82; }
    @Override public int getHeight() { return 28; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AbstractCookingRecipe> holder,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 6)
                .addIngredients(holder.value().getIngredients().getFirst());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 6)
                .addItemStack(holder.value().getResultItem(
                        net.minecraft.client.Minecraft.getInstance().level.registryAccess()));
    }
}
