package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.List;
import java.util.function.Predicate;

/** Drain both modes using legal item stack sizes; a rejected spawn preserves its inventory. */
public final class SpiritCompressorCacheEjection {
    public static boolean eject(List<ItemStackHandler> inventories, Predicate<ItemStack> spawn) {
        for (ItemStackHandler inventory : inventories) for (int slot = 0; slot < inventory.getSlots(); slot++) {
            while (!inventory.getStackInSlot(slot).isEmpty()) {
                ItemStack current = inventory.getStackInSlot(slot);
                ItemStack offered = inventory.extractItem(slot, Math.max(1, current.getMaxStackSize()), true);
                if (offered.isEmpty() || !spawn.test(offered)) return false;
                inventory.extractItem(slot, offered.getCount(), false);
            }
        }
        return true;
    }
    private SpiritCompressorCacheEjection() {}
}
