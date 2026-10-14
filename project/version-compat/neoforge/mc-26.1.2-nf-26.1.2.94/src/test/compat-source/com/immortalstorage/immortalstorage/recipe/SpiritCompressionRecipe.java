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
public final class SpiritCompressionRecipe extends AbstractCookingRecipe {
    private int inputCount = 1;
    private java.util.List<SpiritCompressionIngredient> additionalInputs = java.util.List.of();
    public java.util.List<SpiritCompressionIngredient> additionalInputs() { return additionalInputs; }
    private SpiritCompressionRecipe withInputs(java.util.List<SpiritCompressionIngredient> inputs) {
        if (inputs.size() > 11) throw new IllegalArgumentException("additional_inputs");
        additionalInputs = java.util.List.copyOf(inputs); return this;
    }
    public SpiritCompressionRecipe(Recipe.CommonInfo common, AbstractCookingRecipe.CookingBookInfo book,
                                   Ingredient ingredient, net.minecraft.world.item.ItemStackTemplate result, float experience, int cookingTime) {
        super(common, book, ingredient, result, experience, cookingTime);
    }
    @Override public RecipeType<SpiritCompressionRecipe> getType() { return ModRecipes.SPIRIT_COMPRESSION_TYPE.get(); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.FURNACE_MISC; }
    @Override protected net.minecraft.world.item.Item furnaceIcon() { return com.immortalstorage.immortalstorage.item.ModItems.MINIATURE_IMMORTAL_RUIN.get(); }
    public int inputCount() { return inputCount; }
    public Ingredient inputIngredient() { return input(); }
    public ItemStack outputStack(HolderLookup.Provider registries) { return result().create(); }
    private SpiritCompressionRecipe withCount(int count) { inputCount = count; return this; }
    @Override public RecipeSerializer<SpiritCompressionRecipe> getSerializer() { return ModRecipes.SPIRIT_COMPRESSION_SERIALIZER.get(); }

    public static final class Serializer {
        private static final RecipeSerializer<SpiritCompressionRecipe> BASE = new RecipeSerializer<>(AbstractCookingRecipe.cookingMapCodec(SpiritCompressionRecipe::new, 1), AbstractCookingRecipe.cookingStreamCodec(SpiritCompressionRecipe::new));
        private static final MapCodec<SpiritCompressionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(1, 768).fieldOf("input_count").forGetter(SpiritCompressionRecipe::inputCount),
                BASE.codec().codec().fieldOf("recipe").forGetter(recipe -> recipe),
                SpiritCompressionIngredient.CODEC.listOf().optionalFieldOf("additional_inputs", java.util.List.of()).forGetter(SpiritCompressionRecipe::additionalInputs)
        ).apply(instance, (count, recipe, inputs) -> recipe.withCount(count).withInputs(inputs)));
        private static final StreamCodec<RegistryFriendlyByteBuf, SpiritCompressionRecipe> STREAM = StreamCodec.of(
                (buf, recipe) -> { buf.writeVarInt(recipe.inputCount()); BASE.streamCodec().encode(buf, recipe); buf.writeVarInt(recipe.additionalInputs.size()); for (var input : recipe.additionalInputs) input.write(buf); },
                buf -> { int count = buf.readVarInt(); if (count < 1 || count > 768) throw new IllegalArgumentException("input_count");
                    var recipe = BASE.streamCodec().decode(buf).withCount(count);
                    int size = buf.readVarInt(); if (size < 0 || size > 11) throw new IllegalArgumentException("additional_inputs");
                    var inputs = new java.util.ArrayList<SpiritCompressionIngredient>();
                    for (int i = 0; i < size; i++) inputs.add(SpiritCompressionIngredient.read(buf));
                    return recipe.withInputs(inputs); });
        public static RecipeSerializer<SpiritCompressionRecipe> create() { return new RecipeSerializer<>(CODEC, STREAM); }
    }
}
