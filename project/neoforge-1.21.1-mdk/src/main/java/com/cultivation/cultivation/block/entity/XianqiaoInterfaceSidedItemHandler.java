package com.cultivation.cultivation.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Stable live-policy wrapper for one physical Xianqiao Interface face. */
final class XianqiaoInterfaceSidedItemHandler implements BulkItemInsertTarget {
    private final XianqiaoInterfaceInventory delegate;
    private final Supplier<XianqiaoInterfaceBlockEntity.SideMode> modeSupplier;

    XianqiaoInterfaceSidedItemHandler(
            XianqiaoInterfaceInventory delegate,
            Supplier<XianqiaoInterfaceBlockEntity.SideMode> modeSupplier) {
        this.delegate = delegate;
        this.modeSupplier = modeSupplier;
    }

    @Override
    public int getSlots() {
        return enabled() ? delegate.getSlots() : 0;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return pushing() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return pulling() ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public long insertBulk(ItemStack prototype, long amount, boolean simulate) {
        return pulling() ? delegate.insertBulk(prototype, amount, simulate) : 0L;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return pushing() ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return enabled() ? delegate.getSlotLimit(slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return pulling() && delegate.isItemValid(slot, stack);
    }

    private boolean enabled() {
        return mode() != XianqiaoInterfaceBlockEntity.SideMode.DISABLED;
    }

    private boolean pulling() {
        return mode() == XianqiaoInterfaceBlockEntity.SideMode.PULL;
    }

    private boolean pushing() {
        return mode() == XianqiaoInterfaceBlockEntity.SideMode.PUSH;
    }

    private XianqiaoInterfaceBlockEntity.SideMode mode() {
        XianqiaoInterfaceBlockEntity.SideMode mode = modeSupplier == null ? null : modeSupplier.get();
        return mode == null ? XianqiaoInterfaceBlockEntity.SideMode.DISABLED : mode;
    }
}
