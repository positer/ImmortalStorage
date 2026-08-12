package com.immortalstorage.immortalstorage.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Target 26.1 adapter for the vanilla shaped recipe with yuan substitution returns. */
public final class YuanSubstitutionShapedRecipe implements CraftingRecipe {
    private final ShapedRecipe delegate;

    YuanSubstitutionShapedRecipe(ShapedRecipe delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return delegate.assemble(input);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return YuanSubstitutionRecipeSupport.remainingItems(input);
    }

    @Override public boolean isSpecial() { return delegate.isSpecial(); }
    @Override public boolean showNotification() { return delegate.showNotification(); }
    @Override public String group() { return delegate.group(); }
    @Override public CraftingBookCategory category() { return delegate.category(); }
    @Override public PlacementInfo placementInfo() { return delegate.placementInfo(); }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipes.YUAN_SUBSTITUTION_SHAPED_SERIALIZER.get();
    }

    public static final class Serializer {
        private static final MapCodec<YuanSubstitutionShapedRecipe> CODEC =
                ShapedRecipe.SERIALIZER.codec().xmap(
                        YuanSubstitutionShapedRecipe::new,
                        recipe -> recipe.delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, YuanSubstitutionShapedRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> ShapedRecipe.SERIALIZER.streamCodec().encode(buffer, recipe.delegate),
                        buffer -> new YuanSubstitutionShapedRecipe(
                                ShapedRecipe.SERIALIZER.streamCodec().decode(buffer)));

        private Serializer() {}

        public static RecipeSerializer<YuanSubstitutionShapedRecipe> create() {
            return new RecipeSerializer<>(CODEC, STREAM_CODEC);
        }
    }
}
