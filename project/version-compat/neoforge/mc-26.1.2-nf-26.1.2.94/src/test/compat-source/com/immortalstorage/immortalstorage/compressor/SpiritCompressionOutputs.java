package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.ArrayList;
import java.util.List;

/** Plans every result on copies; a blocked result never partially changes real output slots. */
public final class SpiritCompressionOutputs {
    public static List<ItemStack> plan(IItemHandler inventory, int start, int end,
                                       List<ItemStack> results, long multiplier) {
        if (multiplier <= 0) return null;
        ArrayList<ItemStack> plan = new ArrayList<>();
        for (int slot = start; slot < end; slot++) plan.add(inventory.getStackInSlot(slot).copy());
        for (ItemStack result : results) {
            if (result.isEmpty() || multiplier > Long.MAX_VALUE / result.getCount()) return null;
            long remaining = result.getCount() * multiplier;
            // Fill compatible stacks before assigning an empty slot to a new result type.
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int i = 0; i < plan.size() && remaining > 0; i++) {
                    ItemStack stored = plan.get(i);
                    if (pass == 0 ? stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, result) : !stored.isEmpty()) continue;
                    long capacity = (long) inventory.getSlotLimit(start + i) - stored.getCount();
                    int accepted = (int) Math.min(Math.max(0, capacity), remaining);
                    if (accepted == 0) continue;
                    if (stored.isEmpty()) plan.set(i, result.copyWithCount(accepted)); else stored.grow(accepted);
                    remaining -= accepted;
                }
            }
            if (remaining != 0) return null;
        }
        return List.copyOf(plan);
    }
    private SpiritCompressionOutputs() {}
}
