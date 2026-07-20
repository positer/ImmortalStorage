package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.world.item.ItemStack;

/**
 * Immutable logical item identity and its long-valued stored amount.
 *
 * <p>The prototype is always count one. This lets terminals and capability
 * adapters share one revision-gated directory without exposing the physical
 * overstack representation used by Xianqiao persistence.</p>
 */
public record StorageItemSummary(ItemStack prototype, long amount) {
    public StorageItemSummary {
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
