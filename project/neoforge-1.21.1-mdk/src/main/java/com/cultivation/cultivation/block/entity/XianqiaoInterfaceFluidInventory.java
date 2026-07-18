package com.cultivation.cultivation.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/** Int-sized NeoForge boundary over the interface's nine real fluid caches. */
final class XianqiaoInterfaceFluidInventory implements IFluidHandler {
    private final XianqiaoInterfaceInventory resources;

    XianqiaoInterfaceFluidInventory(XianqiaoInterfaceInventory resources) {
        this.resources = resources;
    }

    @Override public int getTanks() { return resources.getSlots(); }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < XianqiaoInterfaceInventory.SLOT_COUNT
                ? resources.getBufferedFluid(tank) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank >= 0 && tank < XianqiaoInterfaceInventory.SLOT_COUNT
                ? resources.getFluidTargetLimitMb() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return tank >= 0 && tank < XianqiaoInterfaceInventory.SLOT_COUNT && !stack.isEmpty();
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        long accepted = resources.insertFluidBulk(
                resource, resource.getAmount(), action == FluidAction.SIMULATE);
        return (int) accepted;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        return resources.drainFluid(resource, resource.getAmount(), action == FluidAction.SIMULATE);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        for (int tank = 0; tank < XianqiaoInterfaceInventory.SLOT_COUNT; tank++) {
            FluidStack buffered = resources.getBufferedFluid(tank);
            if (!buffered.isEmpty()) {
                return resources.drainFluid(buffered, maxDrain, action == FluidAction.SIMULATE);
            }
        }
        return FluidStack.EMPTY;
    }
}
