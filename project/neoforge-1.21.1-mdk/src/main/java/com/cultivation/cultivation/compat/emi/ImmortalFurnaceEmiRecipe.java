package com.cultivation.cultivation.compat.emi;

import com.cultivation.cultivation.CultivationMod;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;

import java.util.List;

public final class ImmortalFurnaceEmiRecipe implements EmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, "immortal_furnace"),
            EmiStack.of(com.cultivation.cultivation.block.ModBlocks.IMMORTAL_FURNACE.get()));
    private final RecipeHolder<AbstractCookingRecipe> holder;
    private final EmiIngredient input;
    private final EmiStack output;

    public ImmortalFurnaceEmiRecipe(RecipeHolder<AbstractCookingRecipe> holder) {
        this.holder = holder;
        this.input = EmiIngredient.of(holder.value().getIngredients().getFirst());
        this.output = EmiStack.of(holder.value().getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() { return holder.id(); }
    @Override public List<EmiIngredient> getInputs() { return List.of(input); }
    @Override public List<EmiStack> getOutputs() { return List.of(output); }
    @Override public int getDisplayWidth() { return 82; }
    @Override public int getDisplayHeight() { return 28; }
    @Override public RecipeHolder<?> getBackingRecipe() { return holder; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(input, 0, 5);
        widgets.addFillingArrow(26, 5, Math.max(1, holder.value().getCookingTime()));
        widgets.addSlot(output, 60, 5).recipeContext(this);
    }
}
