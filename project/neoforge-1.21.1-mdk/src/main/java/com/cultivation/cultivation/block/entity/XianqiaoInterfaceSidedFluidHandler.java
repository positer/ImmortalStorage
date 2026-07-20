package com.cultivation.cultivation.block.entity;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/** Direction-independent pipe view over the interface's configured fluid caches. */
final class XianqiaoInterfaceSidedFluidHandler implements IFluidHandler {
    private final IFluidHandler delegate;
    private final XianqiaoInterfaceInventory resources;
    private final Direction side;

    XianqiaoInterfaceSidedFluidHandler(
            IFluidHandler delegate, XianqiaoInterfaceInventory resources, Direction side) {
        this.delegate = delegate;
        this.resources = resources;
        this.side = side;
    }

    @Override
    public int getTanks() {
        return delegate == null ? 0 : delegate.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return delegate == null || !extractAllowed(tank)
                ? FluidStack.EMPTY : delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate == null ? 0 : delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return delegate != null && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        return delegate == null ? 0 : delegate.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        if (delegate == null || resource.isEmpty()) return FluidStack.EMPTY;
        return resources.drainFluid(resource, resource.getAmount(),
                action == FluidAction.SIMULATE, side);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (delegate == null || maxDrain <= 0) return FluidStack.EMPTY;
        for (int tank = 0; tank < resources.getSlots(); tank++) {
            if (!extractAllowed(tank)) continue;
            FluidStack buffered = resources.getBufferedFluid(tank);
            if (!buffered.isEmpty()) {
                return resources.drainFluid(buffered, maxDrain,
                        action == FluidAction.SIMULATE, side);
            }
        }
        return FluidStack.EMPTY;
    }

    private boolean extractAllowed(int tank) {
        return side == null || resources.isOutputFaceEnabled(tank, side);
    }
}
