package com.immortalstorage.immortalstorage.network;

import com.immortalstorage.immortalstorage.compressor.SpiritCompressorRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.List;

/** Sends the resolved server catalog, including config-overlaid source ratios, to remote viewers. */
public record SpiritCompressorSync(List<SpiritCompressorRecipes.Entry> entries) implements CustomPacketPayload {
    public static final Type<SpiritCompressorSync> TYPE = new Type<>(Identifier.fromNamespaceAndPath("immortalstorage", "spirit_compressor_catalog"));
    @Override public Type<SpiritCompressorSync> type() { return TYPE; }
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiritCompressorSync> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entries.size());
                for (var entry : packet.entries) {
                    buf.writeIdentifier(entry.id()); buf.writeVarLong(entry.count()); buf.writeBoolean(entry.reversed()); buf.writeVarInt(entry.outputs().size()); for (var output : entry.outputs()) ItemStack.STREAM_CODEC.encode(buf, output);
                    var candidates = BuiltInRegistries.ITEM.stream().filter(item -> entry.input().test(new ItemStack(item))).toList();
                    buf.writeVarInt(candidates.size()); for (var item : candidates) buf.writeIdentifier(BuiltInRegistries.ITEM.getKey(item));
                    buf.writeVarInt(entry.additionalInputs().size()); for (var input : entry.additionalInputs()) input.write(buf);
                }
            }, buf -> {
                int count = bounded(buf.readVarInt(), 0, 8192); var entries = new ArrayList<SpiritCompressorRecipes.Entry>();
                for (int i = 0; i < count; i++) {
                    var id = buf.readIdentifier(); long input = buf.readVarLong(); if (input < 1 || input > 768) throw new IllegalArgumentException("input count");
                    boolean reversed = buf.readBoolean(); int outputs = bounded(buf.readVarInt(), 1, 12);
                    var results = new ArrayList<ItemStack>(); for (int k = 0; k < outputs; k++) results.add(ItemStack.STREAM_CODEC.decode(buf));
                    int size = bounded(buf.readVarInt(), 1, 65536);
                    var items = new net.minecraft.world.item.Item[size]; for (int j = 0; j < size; j++) items[j] = BuiltInRegistries.ITEM.getValue(buf.readIdentifier());
                    int extra = bounded(buf.readVarInt(), 0, 11);
                    var inputs = new ArrayList<com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient>();
                    for (int k = 0; k < extra; k++) inputs.add(com.immortalstorage.immortalstorage.recipe.SpiritCompressionIngredient.read(buf));
                    if (reversed && extra != 0) throw new IllegalArgumentException("decompression inputs");
                    entries.add(new SpiritCompressorRecipes.Entry(id, Ingredient.of(items), input, results.getFirst(), List.copyOf(results.subList(1, results.size())), reversed, inputs));
                }
                return new SpiritCompressorSync(List.copyOf(entries));
            });
    private static int bounded(int value, int min, int max) { if (value < min || value > max) throw new IllegalArgumentException("catalog size"); return value; }
}
