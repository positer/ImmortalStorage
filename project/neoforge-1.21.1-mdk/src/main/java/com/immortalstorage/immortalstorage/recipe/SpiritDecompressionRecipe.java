package com.immortalstorage.immortalstorage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Native, network-synchronized datapack recipe; an explicit recipe overrides source inversion. */
public final class SpiritDecompressionRecipe extends AbstractCookingRecipe {
    private int inputCount = 1;
    private java.util.List<ItemStack> additionalResults = java.util.List.of();
    public java.util.List<ItemStack> additionalResults() { return additionalResults; }
    private SpiritDecompressionRecipe withResults(java.util.List<ItemStack> results) {
        if (results.size() > 11 || results.stream().anyMatch(ItemStack::isEmpty)) throw new IllegalArgumentException("additional_results");
        additionalResults = results.stream().map(ItemStack::copy).toList(); return this;
    }
    public SpiritDecompressionRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                                   ItemStack result, float experience, int cookingTime) {
        super(ModRecipes.SPIRIT_DECOMPRESSION_TYPE.get(), group, category, ingredient, result, experience, cookingTime);
    }
    public int inputCount() { return inputCount; }
    public Ingredient inputIngredient() { return ingredient; }
    public ItemStack outputStack(HolderLookup.Provider registries) { return getResultItem(registries).copy(); }
    private SpiritDecompressionRecipe withCount(int count) { inputCount = count; return this; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.SPIRIT_DECOMPRESSION_SERIALIZER.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<SpiritDecompressionRecipe> {
        private static final RecipeSerializer<SpiritDecompressionRecipe> BASE = new SimpleCookingSerializer<>(SpiritDecompressionRecipe::new, 1);
        private static final MapCodec<SpiritDecompressionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(1, 64).fieldOf("input_count").forGetter(SpiritDecompressionRecipe::inputCount),
                BASE.codec().codec().fieldOf("recipe").forGetter(recipe -> recipe),
                ItemStack.CODEC.listOf().optionalFieldOf("additional_results", java.util.List.of()).forGetter(SpiritDecompressionRecipe::additionalResults)
        ).apply(instance, (count, recipe, results) -> recipe.withCount(count).withResults(results)));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpiritDecompressionRecipe> STREAM = StreamCodec.of(
                (buf, recipe) -> { buf.writeVarInt(recipe.inputCount()); BASE.streamCodec().encode(buf, recipe); buf.writeVarInt(recipe.additionalResults.size()); for (var result : recipe.additionalResults) ItemStack.STREAM_CODEC.encode(buf, result); },
                buf -> { int count = buf.readVarInt(); if (count < 1 || count > 64) throw new IllegalArgumentException("input_count");
                    var recipe = BASE.streamCodec().decode(buf).withCount(count);
                    int size = buf.readVarInt(); if (size < 0 || size > 11) throw new IllegalArgumentException("additional_results");
                    var results = new java.util.ArrayList<ItemStack>(); for (int i = 0; i < size; i++) results.add(ItemStack.STREAM_CODEC.decode(buf));
                    return recipe.withResults(results); });
        @Override public MapCodec<SpiritDecompressionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpiritDecompressionRecipe> streamCodec() { return STREAM; }
    }
}
