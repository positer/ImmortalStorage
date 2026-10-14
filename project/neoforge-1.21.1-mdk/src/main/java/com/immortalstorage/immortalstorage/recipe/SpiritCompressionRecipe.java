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
    public SpiritCompressionRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                                   ItemStack result, float experience, int cookingTime) {
        super(ModRecipes.SPIRIT_COMPRESSION_TYPE.get(), group, category, ingredient, result, experience, cookingTime);
    }
    public int inputCount() { return inputCount; }
    public Ingredient inputIngredient() { return ingredient; }
    public ItemStack outputStack(HolderLookup.Provider registries) { return getResultItem(registries).copy(); }
    private SpiritCompressionRecipe withCount(int count) { inputCount = count; return this; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.SPIRIT_COMPRESSION_SERIALIZER.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<SpiritCompressionRecipe> {
        private static final RecipeSerializer<SpiritCompressionRecipe> BASE = new SimpleCookingSerializer<>(SpiritCompressionRecipe::new, 1);
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
        @Override public MapCodec<SpiritCompressionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpiritCompressionRecipe> streamCodec() { return STREAM; }
    }
}
