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
    private java.util.List<net.minecraft.world.item.ItemStackTemplate> additionalResults = java.util.List.of();
    public java.util.List<ItemStack> additionalResults() { return additionalResults.stream().map(net.minecraft.world.item.ItemStackTemplate::create).toList(); }
    private java.util.List<net.minecraft.world.item.ItemStackTemplate> resultTemplates() { return additionalResults; }
    private SpiritDecompressionRecipe withResults(java.util.List<net.minecraft.world.item.ItemStackTemplate> results) {
        if (results.size() > 11 || results.stream().anyMatch(result -> result.count() <= 0)) throw new IllegalArgumentException("additional_results");
        additionalResults = java.util.List.copyOf(results); return this;
    }
    public SpiritDecompressionRecipe(Recipe.CommonInfo common, AbstractCookingRecipe.CookingBookInfo book,
                                   Ingredient ingredient, net.minecraft.world.item.ItemStackTemplate result, float experience, int cookingTime) {
        super(common, book, ingredient, result, experience, cookingTime);
    }
    @Override public RecipeType<SpiritDecompressionRecipe> getType() { return ModRecipes.SPIRIT_DECOMPRESSION_TYPE.get(); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.FURNACE_MISC; }
    @Override protected net.minecraft.world.item.Item furnaceIcon() { return com.immortalstorage.immortalstorage.item.ModItems.MINIATURE_IMMORTAL_RUIN.get(); }
    public int inputCount() { return inputCount; }
    public Ingredient inputIngredient() { return input(); }
    public ItemStack outputStack(HolderLookup.Provider registries) { return result().create(); }
    private SpiritDecompressionRecipe withCount(int count) { inputCount = count; return this; }
    @Override public RecipeSerializer<SpiritDecompressionRecipe> getSerializer() { return ModRecipes.SPIRIT_DECOMPRESSION_SERIALIZER.get(); }

    public static final class Serializer {
        private static final RecipeSerializer<SpiritDecompressionRecipe> BASE = new RecipeSerializer<>(AbstractCookingRecipe.cookingMapCodec(SpiritDecompressionRecipe::new, 1), AbstractCookingRecipe.cookingStreamCodec(SpiritDecompressionRecipe::new));
        private static final MapCodec<SpiritDecompressionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(1, 64).fieldOf("input_count").forGetter(SpiritDecompressionRecipe::inputCount),
                BASE.codec().codec().fieldOf("recipe").forGetter(recipe -> recipe),
                net.minecraft.world.item.ItemStackTemplate.CODEC.listOf().optionalFieldOf("additional_results", java.util.List.of()).forGetter(SpiritDecompressionRecipe::resultTemplates)
        ).apply(instance, (count, recipe, results) -> recipe.withCount(count).withResults(results)));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpiritDecompressionRecipe> STREAM = StreamCodec.of(
                (buf, recipe) -> { buf.writeVarInt(recipe.inputCount()); BASE.streamCodec().encode(buf, recipe); buf.writeVarInt(recipe.additionalResults.size()); for (var result : recipe.additionalResults) net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC.encode(buf, result); },
                buf -> { int count = buf.readVarInt(); if (count < 1 || count > 64) throw new IllegalArgumentException("input_count");
                    var recipe = BASE.streamCodec().decode(buf).withCount(count);
                    int size = buf.readVarInt(); if (size < 0 || size > 11) throw new IllegalArgumentException("additional_results");
                    var results = new java.util.ArrayList<net.minecraft.world.item.ItemStackTemplate>(); for (int i = 0; i < size; i++) results.add(net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC.decode(buf));
                    return recipe.withResults(results); });
        public static RecipeSerializer<SpiritDecompressionRecipe> create() { return new RecipeSerializer<>(CODEC, STREAM); }
    }
}
