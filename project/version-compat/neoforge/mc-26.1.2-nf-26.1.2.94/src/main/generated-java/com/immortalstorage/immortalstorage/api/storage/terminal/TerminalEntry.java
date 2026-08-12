package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.world.item.ItemStack;

/** A read-only aggregated entry exposed by a storage terminal. */
public record TerminalEntry(long entryId, ItemStack displayStack, long amount) {
    public TerminalEntry {
        if (entryId == 0L) throw new IllegalArgumentException("entryId must be non-zero");
        if (displayStack == null || displayStack.isEmpty()) throw new IllegalArgumentException("displayStack must be non-empty");
        if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
        displayStack = displayStack.copyWithCount(1);
    }

    @Override
    public ItemStack displayStack() {
        return displayStack.copy();
    }
}
