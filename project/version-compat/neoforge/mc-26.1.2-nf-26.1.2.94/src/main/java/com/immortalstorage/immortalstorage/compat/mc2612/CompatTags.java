package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.stream.Stream;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Official 26.1 holder-tag access for item and fluid stacks. */
public final class CompatTags {
    private CompatTags() {
    }

    public static Stream<TagKey<Item>> getTags(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().tags();
    }

    public static Stream<TagKey<Fluid>> getTags(FluidStack stack) {
        return stack.getFluid().builtInRegistryHolder().tags();
    }
}
