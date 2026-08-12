package com.immortalstorage.immortalstorage.compat.mc2612;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Registry-aware codecs mandated by the 26.1 item/fluid data-component API. */
public final class CompatCodec {
    private CompatCodec() {
    }

    private static <T> DynamicOps<Tag> ops(HolderLookup.Provider registries) {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }

    public static Tag saveItemStack(HolderLookup.Provider registries, ItemStack stack) {
        return ItemStack.CODEC.encodeStart(ops(registries), stack).result().orElse(ItemStack.EMPTY.isEmpty()
                ? new net.minecraft.nbt.CompoundTag() : new net.minecraft.nbt.CompoundTag());
    }

    public static ItemStack parseItemStack(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(ops(registries), tag).result().orElse(ItemStack.EMPTY);
    }

    public static ItemStack parseItemStack(HolderLookup.Provider registries,
                                           net.minecraft.nbt.CompoundTag tag) {
        return parseItemStack(registries, (Tag) tag);
    }

    public static Tag saveFluidStack(HolderLookup.Provider registries, FluidStack stack) {
        return FluidStack.CODEC.encodeStart(ops(registries), stack).result()
                .orElse(new net.minecraft.nbt.CompoundTag());
    }

    public static FluidStack parseFluidStack(HolderLookup.Provider registries, Tag tag) {
        return FluidStack.OPTIONAL_CODEC.parse(ops(registries), tag).result().orElse(FluidStack.EMPTY);
    }

    public static FluidStack parseFluidStack(HolderLookup.Provider registries,
                                              net.minecraft.nbt.CompoundTag tag) {
        return parseFluidStack(registries, (Tag) tag);
    }
}
