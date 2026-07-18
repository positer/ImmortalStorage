package com.cultivation.cultivation.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Vanilla shapeless recipe whose immortal-yuan substitutions are returned to their input slots. */
public final class YuanSubstitutionShapelessRecipe implements CraftingRecipe {
    private final ShapelessRecipe delegate;

    YuanSubstitutionShapelessRecipe(ShapelessRecipe delegate) {
        this.delegate = delegate;
    }

    @Override public boolean matches(CraftingInput input, Level level) { return delegate.matches(input, level); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return delegate.assemble(input, registries);
    }
    @Override public boolean canCraftInDimensions(int width, int height) {
        return delegate.canCraftInDimensions(width, height);
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {
        return delegate.getResultItem(registries);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return YuanSubstitutionRecipeSupport.remainingItems(input);
    }
    @Override public NonNullList<Ingredient> getIngredients() { return delegate.getIngredients(); }
    @Override public boolean isSpecial() { return delegate.isSpecial(); }
    @Override public boolean showNotification() { return delegate.showNotification(); }
    @Override public String getGroup() { return delegate.getGroup(); }
    @Override public ItemStack getToastSymbol() { return delegate.getToastSymbol(); }
    @Override public boolean isIncomplete() { return delegate.isIncomplete(); }
    @Override public CraftingBookCategory category() { return delegate.category(); }
    @Override public RecipeSerializer<?> getSerializer() {
        return ModRecipes.YUAN_SUBSTITUTION_SHAPELESS_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<YuanSubstitutionShapelessRecipe> {
        private static final MapCodec<YuanSubstitutionShapelessRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(
                        YuanSubstitutionShapelessRecipe::new, recipe -> recipe.delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, YuanSubstitutionShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
                                .encode(buffer, recipe.delegate),
                        buffer -> new YuanSubstitutionShapelessRecipe(
                                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().decode(buffer)));

        @Override public MapCodec<YuanSubstitutionShapelessRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, YuanSubstitutionShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
