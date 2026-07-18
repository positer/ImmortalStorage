package com.cultivation.cultivation.api.storage.terminal;

import net.minecraft.world.item.ItemStack;

/**
 * Authoritative logical entry that replaces the physical aggregate for the
 * same item-and-components key. It is used for non-materialized resources such
 * as stage-ten immortal yuan.
 */
public record TerminalVirtualEntry(ItemStack prototype, long amount) {
    public TerminalVirtualEntry {
        if (prototype == null || prototype.isEmpty()) {
            throw new IllegalArgumentException("prototype must be non-empty");
        }
        if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
        prototype = prototype.copyWithCount(1);
    }

    @Override
    public ItemStack prototype() {
        return prototype.copy();
    }
}
