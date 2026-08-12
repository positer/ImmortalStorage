package com.immortalstorage.immortalstorage.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Target 26.1 adapter for the vanilla shapeless recipe with yuan substitution returns. */
public final class YuanSubstitutionShapelessRecipe implements CraftingRecipe {
    private final ShapelessRecipe delegate;

    YuanSubstitutionShapelessRecipe(ShapelessRecipe delegate) {
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
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return YuanSubstitutionRecipeSupport.remainingItems(input);
    }

    @Override public boolean isSpecial() { return delegate.isSpecial(); }
    @Override public boolean showNotification() { return delegate.showNotification(); }
    @Override public String group() { return delegate.group(); }
    @Override public CraftingBookCategory category() { return delegate.category(); }
    @Override public PlacementInfo placementInfo() { return delegate.placementInfo(); }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipes.YUAN_SUBSTITUTION_SHAPELESS_SERIALIZER.get();
    }

    public static final class Serializer {
        private static final MapCodec<YuanSubstitutionShapelessRecipe> CODEC =
                ShapelessRecipe.SERIALIZER.codec().xmap(
                        YuanSubstitutionShapelessRecipe::new,
                        recipe -> recipe.delegate);
        private static final StreamCodec<RegistryFriendlyByteBuf, YuanSubstitutionShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> ShapelessRecipe.SERIALIZER.streamCodec().encode(buffer, recipe.delegate),
                        buffer -> new YuanSubstitutionShapelessRecipe(
                                ShapelessRecipe.SERIALIZER.streamCodec().decode(buffer)));

        private Serializer() {}

        public static RecipeSerializer<YuanSubstitutionShapelessRecipe> create() {
            return new RecipeSerializer<>(CODEC, STREAM_CODEC);
        }
    }
}
