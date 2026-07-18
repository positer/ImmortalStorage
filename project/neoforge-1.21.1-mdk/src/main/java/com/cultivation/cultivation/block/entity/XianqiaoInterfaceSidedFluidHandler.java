package com.cultivation.cultivation.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Stable live-policy fluid wrapper for one physical Xianqiao Interface face. */
final class XianqiaoInterfaceSidedFluidHandler implements IFluidHandler {
    private final IFluidHandler delegate;
    private final Supplier<XianqiaoInterfaceBlockEntity.SideMode> modeSupplier;

    XianqiaoInterfaceSidedFluidHandler(
            IFluidHandler delegate,
            Supplier<XianqiaoInterfaceBlockEntity.SideMode> modeSupplier) {
        this.delegate = delegate;
        this.modeSupplier = modeSupplier;
    }

    @Override
    public int getTanks() {
        return enabled() && delegate != null ? delegate.getTanks() : 0;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return pushing() && delegate != null ? delegate.getFluidInTank(tank) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return enabled() && delegate != null ? delegate.getTankCapacity(tank) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return pulling() && delegate != null && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        return pulling() && delegate != null ? delegate.fill(resource, action) : 0;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        return pushing() && delegate != null ? delegate.drain(resource, action) : FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return pushing() && delegate != null ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
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
