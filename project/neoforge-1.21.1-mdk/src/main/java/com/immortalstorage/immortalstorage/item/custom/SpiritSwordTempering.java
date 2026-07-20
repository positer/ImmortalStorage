package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Shared component-preserving tempering state for every furnace implementation. */
public final class SpiritSwordTempering {
    private static final String POINTS_TAG = "temperingPoints";

    public static long points(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SpiritSwordItem)) return 0L;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0L, tag.getLong(POINTS_TAG));
    }

    public static ItemStack temper(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SpiritSwordItem)) return ItemStack.EMPTY;
        ItemStack result = stack.copyWithCount(1);
        setPoints(result, points(stack) == Long.MAX_VALUE ? Long.MAX_VALUE : points(stack) + 1L);
        return result;
    }

    public static long consumeHalf(ItemStack stack) {
        long before = points(stack);
        setPoints(stack, before / 2L);
        return before;
    }

    public static float bonusDamage(float ordinaryDamage, long points) {
        if (ordinaryDamage <= 0.0F || points <= 0L) return 0.0F;
        double bonus = ordinaryDamage * Math.min(points, 1_000_000_000L) * 0.01D;
        return (float) Math.min(Float.MAX_VALUE, bonus);
    }

    public static void setPoints(ItemStack stack, long points) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(POINTS_TAG, Math.max(0L, points));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private SpiritSwordTempering() {}
}
