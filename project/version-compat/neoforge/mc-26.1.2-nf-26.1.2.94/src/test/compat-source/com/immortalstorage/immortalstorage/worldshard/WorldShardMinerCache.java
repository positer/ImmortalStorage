package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public final class WorldShardMinerCache extends ItemStackHandler {
    public static final int SLOT_COUNT = 27;
    private final Runnable onChanged;

    public WorldShardMinerCache(Runnable onChanged) {
        super(SLOT_COUNT);
        this.onChanged = onChanged == null ? () -> { } : onChanged;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    public ItemStack removeStackNoUpdate(int slot) {
        validateSlotIndex(slot);
        return stacks.set(slot, ItemStack.EMPTY);
    }

    /**
     * Plans every insertion against a detached 27-slot snapshot and publishes
     * it only when the entire batch fits.
     */
    public boolean tryInsertAll(List<ItemStack> offered) {
        ItemStackHandler planned = new ItemStackHandler(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            planned.setStackInSlot(slot, getStackInSlot(slot).copy());
        }
        boolean hasContent = false;
        for (ItemStack stack : offered) {
            if (stack == null || stack.isEmpty()) continue;
            hasContent = true;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(planned, stack.copy(), false);
            if (!remainder.isEmpty()) return false;
        }
        if (!hasContent) return true;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            stacks.set(slot, planned.getStackInSlot(slot).copy());
        }
        onChanged.run();
        return true;
    }

    /**
     * Inserts every stack as far as the live 27-slot cache permits and returns
     * component-preserving remainders in source order.  This is intentionally
     * separate from {@link #tryInsertAll(List)}: normal transaction probes stay
     * atomic, while a completed reinforced generation may commit its fitting
     * portion before the caller persists the remainder and pauses.
     */
    public List<ItemStack> insertAsMuchAsPossible(List<ItemStack> offered) {
        if (offered == null || offered.isEmpty()) return List.of();
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack stack : offered) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(this, stack.copy(), false);
            if (!remainder.isEmpty()) overflow.add(remainder.copy());
        }
        return List.copyOf(overflow);
    }
}
