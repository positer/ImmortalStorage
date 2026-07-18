package com.cultivation.cultivation.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Stable NeoForge capability object that resolves the owner's current
 * Xianqiao fluid endpoint for every operation. This avoids caching a null
 * capability when a placed interface crosses the stage-seven fluid boundary,
 * while still failing closed when the owner is offline or no longer eligible.
 */
final class XianqiaoInterfaceFluidHandler implements IFluidHandler {
    private final Supplier<IFluidHandler> delegateSupplier;

    XianqiaoInterfaceFluidHandler(Supplier<IFluidHandler> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public int getTanks() {
        IFluidHandler delegate = delegate();
        return delegate == null ? 0 : delegate.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        IFluidHandler delegate = delegate();
        return delegate == null ? FluidStack.EMPTY : delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        IFluidHandler delegate = delegate();
        return delegate == null ? 0 : delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        IFluidHandler delegate = delegate();
        return delegate != null && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        IFluidHandler delegate = delegate();
        return delegate == null ? 0 : delegate.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        IFluidHandler delegate = delegate();
        return delegate == null ? FluidStack.EMPTY : delegate.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        IFluidHandler delegate = delegate();
        return delegate == null ? FluidStack.EMPTY : delegate.drain(maxDrain, action);
    }

    private IFluidHandler delegate() {
        return delegateSupplier == null ? null : delegateSupplier.get();
    }
}
