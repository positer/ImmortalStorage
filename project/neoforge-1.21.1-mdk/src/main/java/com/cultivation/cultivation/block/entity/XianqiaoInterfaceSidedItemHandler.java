package com.cultivation.cultivation.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/** Direction-independent pipe view over the interface's configured item caches. */
final class XianqiaoInterfaceSidedItemHandler implements BulkItemInsertTarget {
    private final XianqiaoInterfaceInventory delegate;
    private final Direction side;

    XianqiaoInterfaceSidedItemHandler(XianqiaoInterfaceInventory delegate, Direction side) {
        this.delegate = delegate;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return extractAllowed(slot) ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        long accepted = delegate.insertItemIntoCache(slot, stack, simulate);
        if (accepted <= 0L) return stack;
        return accepted == stack.getCount() ? ItemStack.EMPTY
                : stack.copyWithCount((int) (stack.getCount() - accepted));
    }

    @Override
    public long insertBulk(ItemStack prototype, long amount, boolean simulate) {
        return delegate.insertItemIntoCaches(prototype, amount, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractAllowed(slot) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return delegate.isItemValidForCache(slot, stack);
    }

    private boolean extractAllowed(int slot) {
        return side == null || delegate.isOutputFaceEnabled(slot, side);
    }
}
