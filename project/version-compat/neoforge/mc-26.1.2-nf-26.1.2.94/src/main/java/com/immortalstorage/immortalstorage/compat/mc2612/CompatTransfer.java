package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Official 26.1 transfer-capability bridge.
 *
 * <p>NeoForge 26.1 exposes {@code ResourceHandler}/{@code EnergyHandler} at
 * capabilities while the shared 1.21.1 implementation deliberately keeps
 * the legacy handler contracts internally.  The adapters are explicit at the
 * capability boundary; no class-presence or reflective probe is involved.</p>
 */
public final class CompatTransfer {
    private CompatTransfer() {
    }

    public static IItemHandler itemHandler(ResourceHandler<ItemResource> handler) {
        return handler == null ? null : IItemHandler.of(handler);
    }

    public static IFluidHandler fluidHandler(ResourceHandler<FluidResource> handler) {
        return handler == null ? null : IFluidHandler.of(handler);
    }

    public static IEnergyStorage energyHandler(EnergyHandler handler) {
        return handler == null ? null : IEnergyStorage.of(handler);
    }

    public static ResourceHandler<ItemResource> item(IItemHandler legacy) {
        if (legacy == null) return null;
        return new ResourceHandler<>() {
            @Override public int size() {
                return legacy.getSlots();
            }

            @Override public ItemResource getResource(int index) {
                ItemStack stack = legacy.getStackInSlot(index);
                return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack.copyWithCount(1));
            }

            @Override public long getAmountAsLong(int index) {
                return legacy.getStackInSlot(index).getCount();
            }

            @Override public long getCapacityAsLong(int index, ItemResource resource) {
                return legacy.getSlotLimit(index);
            }

            @Override public boolean isValid(int index, ItemResource resource) {
                return resource != null && legacy.isItemValid(index, resource.toStack(1));
            }

            @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (resource == null || resource.isEmpty() || amount <= 0) return 0;
                ItemStack remainder = legacy.insertItem(index, resource.toStack(amount), false);
                return Math.max(0, amount - remainder.getCount());
            }

            @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (resource == null || resource.isEmpty() || amount <= 0) return 0;
                return legacy.extractItem(index, amount, false).getCount();
            }
        };
    }

    public static ResourceHandler<FluidResource> fluid(IFluidHandler legacy) {
        if (legacy == null) return null;
        return new ResourceHandler<>() {
            @Override public int size() {
                return legacy.getTanks();
            }

            @Override public FluidResource getResource(int index) {
                FluidStack stack = legacy.getFluidInTank(index);
                return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
            }

            @Override public long getAmountAsLong(int index) {
                return legacy.getFluidInTank(index).getAmount();
            }

            @Override public long getCapacityAsLong(int index, FluidResource resource) {
                return legacy.getTankCapacity(index);
            }

            @Override public boolean isValid(int index, FluidResource resource) {
                return resource != null && legacy.isFluidValid(index, resource.toStack(1));
            }

            @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (resource == null || resource.isEmpty() || amount <= 0) return 0;
                return legacy.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
            }

            @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (resource == null || resource.isEmpty() || amount <= 0) return 0;
                return legacy.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        };
    }

    public static EnergyHandler energy(IEnergyStorage legacy) {
        if (legacy == null) return null;
        return new EnergyHandler() {
            @Override public long getAmountAsLong() {
                return legacy.getEnergyStored();
            }

            @Override public long getCapacityAsLong() {
                return legacy.getMaxEnergyStored();
            }

            @Override public int insert(int amount, TransactionContext transaction) {
                return legacy.receiveEnergy(Math.max(0, amount), false);
            }

            @Override public int extract(int amount, TransactionContext transaction) {
                return legacy.extractEnergy(Math.max(0, amount), false);
            }
        };
    }
}
