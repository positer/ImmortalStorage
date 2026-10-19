package com.immortalstorage.immortalstorage.compressor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Design section 4: a material cost that is only paid when the product is really taken. A teardown voids
 * the uncollected product instead of paying for it, so neither half can leave the machine alone.
 */
public final class DeferredEnchantmentTransaction {
    private int[] consume;
    private boolean[] strip;

    public boolean pending() { return consume != null; }

    /** Registers the cost of one produced batch; both vectors are indexed by input slot. */
    public void arm(int[] consume, boolean[] strip) {
        this.consume = consume.clone();
        this.strip = strip.clone();
    }

    /**
     * Pays the registered cost against one inventory exactly once, and only on a real take: a product
     * and the material it was made from can never both leave the machine. Teardown paths discard instead.
     */
    public boolean settle(ItemStackHandler inventory) {
        int[] cost = consume;
        boolean[] cleared = strip;
        consume = null;
        strip = null;
        if (cost == null || inventory == null) return false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            int amount = slot < cost.length ? cost[slot] : 0;
            // A handler may cap one extraction at the item's max stack size; the registered count is still owed.
            for (int owed = amount; owed > 0; ) {
                ItemStack taken = inventory.extractItem(slot, owed, false);
                if (taken.isEmpty()) break;
                owed -= taken.getCount();
            }
            if (slot < cleared.length && cleared[slot]) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) { EnchantmentStackRules.clear(stack); inventory.setStackInSlot(slot, stack); }
            }
        }
        return true;
    }

    /** The machine routes every product-slot extraction here; true when this take paid the cost. */
    public boolean afterExtract(ItemStackHandler inventory, int slot, int firstOutputSlot) {
        if (consume == null || slot < firstOutputSlot) return false;
        return settle(inventory);
    }

    /**
     * Teardown rule: an uncollected product is voided (deleted, never dropped) and the material is left
     * in its input slots. False when no product was pending, which leaves player-owned leftovers alone.
     */
    public boolean discard(ItemStackHandler inventory, int firstOutputSlot) {
        boolean pending = consume != null;
        consume = null;
        strip = null;
        if (!pending || inventory == null) return false;
        for (int slot = Math.max(0, firstOutputSlot); slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return true;
    }

    public void restore(int[] consume, boolean[] strip) {
        if (consume == null || consume.length == 0 || strip == null) clear(); else arm(consume, strip);
    }

    public int[] consume() { return consume == null ? null : consume.clone(); }
    public boolean[] strip() { return strip == null ? null : strip.clone(); }
    public void clear() { consume = null; strip = null; }
}
