package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Immutable item-and-components identity used by storage-terminal views.
 * Counts are intentionally excluded so equal stacks aggregate into one entry.
 */
public final class TerminalEntryKey {
    private final ItemStack prototype;
    private final int hash;

    private TerminalEntryKey(ItemStack stack) {
        this.prototype = stack.copyWithCount(1);
        this.hash = ItemStack.hashItemAndComponents(this.prototype);
    }

    public static TerminalEntryKey of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("A terminal entry key requires a non-empty stack");
        }
        return new TerminalEntryKey(stack);
    }

    public ItemStack prototype() {
        return prototype.copy();
    }

    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TerminalEntryKey key
                && ItemStack.isSameItemSameComponents(prototype, key.prototype);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return Objects.toString(prototype.getItem()) + "@" + Integer.toUnsignedString(hash, 16);
    }
}
