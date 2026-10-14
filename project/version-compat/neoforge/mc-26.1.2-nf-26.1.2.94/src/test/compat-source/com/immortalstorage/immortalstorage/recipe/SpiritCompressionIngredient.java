package com.immortalstorage.immortalstorage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record SpiritCompressionIngredient(Ingredient ingredient, int count) {
    public static final Codec<SpiritCompressionIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SpiritCompressionIngredient::ingredient),
            Codec.intRange(1, 768).fieldOf("count").forGetter(SpiritCompressionIngredient::count)
    ).apply(instance, SpiritCompressionIngredient::new));
    public SpiritCompressionIngredient { if (count < 1 || count > 768) throw new IllegalArgumentException("ingredient count"); }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(count);
        var candidates = BuiltInRegistries.ITEM.stream().filter(item -> ingredient.test(new ItemStack(item))).toList();
        buf.writeVarInt(candidates.size());
        for (var item : candidates) buf.writeIdentifier(BuiltInRegistries.ITEM.getKey(item));
    }
    public static SpiritCompressionIngredient read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt(), size = buf.readVarInt();
        if (size < 1 || size > 65536) throw new IllegalArgumentException("ingredient candidates");
        var items = new net.minecraft.world.item.Item[size];
        for (int i = 0; i < size; i++) items[i] = BuiltInRegistries.ITEM.getValue(buf.readIdentifier());
        return new SpiritCompressionIngredient(Ingredient.of(items), count);
    }
}
