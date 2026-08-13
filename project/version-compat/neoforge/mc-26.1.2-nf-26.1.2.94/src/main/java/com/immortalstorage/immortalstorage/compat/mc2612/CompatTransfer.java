package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

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
        return new TransactionalItemAdapter(legacy);
    }

    public static ResourceHandler<FluidResource> fluid(IFluidHandler legacy) {
        if (legacy == null) return null;
        return new TransactionalFluidAdapter(legacy);
    }

    public static EnergyHandler energy(IEnergyStorage legacy) {
        if (legacy == null) return null;
        return new TransactionalEnergyAdapter(legacy);
    }

    /**
     * The 26.1 capability contract is transactional. Legacy handlers have no
     * rollback hook, so irreversible mutations are queued and executed only
     * from {@link SnapshotJournal#onRootCommit(Object)}. Nested transaction
     * aborts restore the queued operation list without touching game state.
     */
    private abstract static class DeferredJournal<O> extends SnapshotJournal<List<O>> {
        final List<O> pending = new ArrayList<>();

        final void stage(O operation, TransactionContext transaction) {
            if (transaction == null) throw new NullPointerException("transaction");
            updateSnapshots(transaction);
            pending.add(operation);
        }

        @Override protected List<O> createSnapshot() {
            return List.copyOf(pending);
        }

        @Override protected void revertToSnapshot(List<O> snapshot) {
            pending.clear();
            pending.addAll(snapshot);
        }

        @Override protected final void onRootCommit(List<O> originalState) {
            List<O> committed = List.copyOf(pending.subList(originalState.size(), pending.size()));
            pending.clear();
            pending.addAll(originalState);
            for (O operation : committed) apply(operation);
        }

        abstract void apply(O operation);
    }

    private record ItemOperation(boolean insert, int index, ItemResource resource, int amount) {}

    private static final class TransactionalItemAdapter extends DeferredJournal<ItemOperation>
            implements ResourceHandler<ItemResource> {
        private final IItemHandler legacy;

        private TransactionalItemAdapter(IItemHandler legacy) {
            this.legacy = legacy;
        }

        @Override public int size() {
            return legacy.getSlots();
        }

        @Override public ItemResource getResource(int index) {
            ItemStack stack = stack(index);
            return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack.copyWithCount(1));
        }

        @Override public long getAmountAsLong(int index) {
            return stack(index).getCount();
        }

        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            return validIndex(index) ? Math.max(0, legacy.getSlotLimit(index)) : 0L;
        }

        @Override public boolean isValid(int index, ItemResource resource) {
            return validIndex(index) && resource != null && !resource.isEmpty()
                    && legacy.isItemValid(index, resource.toStack(1));
        }

        @Override public int insert(
                int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (amount == 0 || !validIndex(index)) return 0;
            int alreadyStaged = stagedItemInsertion(index, resource);
            int simulatedOffer = saturatedAdd(amount, alreadyStaged);
            ItemStack remainder = legacy.insertItem(index, resource.toStack(simulatedOffer), true);
            int totalAccepted = Math.max(0, simulatedOffer - remainder.getCount());
            int accepted = Math.max(0, Math.min(amount, totalAccepted - alreadyStaged));
            if (accepted > 0) stage(new ItemOperation(true, index, resource, accepted), transaction);
            return accepted;
        }

        @Override public int extract(
                int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (amount == 0 || !validIndex(index)) return 0;
            ItemStack current = legacy.getStackInSlot(index);
            if (!matches(current, resource)) return 0;
            int alreadyStaged = stagedItemExtraction(index, resource);
            int available = Math.max(0, current.getCount() - alreadyStaged);
            if (available == 0) return 0;
            ItemStack simulated = legacy.extractItem(index, Math.min(amount, available), true);
            if (!matches(simulated, resource)) return 0;
            int accepted = Math.min(Math.min(amount, available), simulated.getCount());
            if (accepted > 0) stage(new ItemOperation(false, index, resource, accepted), transaction);
            return accepted;
        }

        private ItemStack stack(int index) {
            return validIndex(index) ? legacy.getStackInSlot(index) : ItemStack.EMPTY;
        }

        private boolean validIndex(int index) {
            return index >= 0 && index < legacy.getSlots();
        }

        private int stagedItemExtraction(int index, ItemResource resource) {
            long staged = 0L;
            for (ItemOperation operation : pending) {
                if (!operation.insert && operation.index == index && operation.resource.equals(resource)) {
                    staged += operation.amount;
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, staged);
        }

        private int stagedItemInsertion(int index, ItemResource resource) {
            long staged = 0L;
            for (ItemOperation operation : pending) {
                if (operation.insert && operation.index == index && operation.resource.equals(resource)) {
                    staged += operation.amount;
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, staged);
        }

        @Override void apply(ItemOperation operation) {
            if (!validIndex(operation.index)) return;
            if (operation.insert) {
                legacy.insertItem(operation.index, operation.resource.toStack(operation.amount), false);
                return;
            }
            // Dynamic handlers may remap an index between discovery and root
            // commit. Never consume a different item under the requested key.
            if (!matches(legacy.getStackInSlot(operation.index), operation.resource)) return;
            legacy.extractItem(operation.index, operation.amount, false);
        }

        private static boolean matches(ItemStack stack, ItemResource resource) {
            return stack != null && !stack.isEmpty() && resource != null && !resource.isEmpty()
                    && resource.equals(ItemResource.of(stack.copyWithCount(1)));
        }
    }

    private record FluidOperation(boolean insert, int index, FluidResource resource, int amount) {}

    private static final class TransactionalFluidAdapter extends DeferredJournal<FluidOperation>
            implements ResourceHandler<FluidResource> {
        private final IFluidHandler legacy;

        private TransactionalFluidAdapter(IFluidHandler legacy) {
            this.legacy = legacy;
        }

        @Override public int size() { return legacy.getTanks(); }

        @Override public FluidResource getResource(int index) {
            FluidStack stack = validIndex(index) ? legacy.getFluidInTank(index) : FluidStack.EMPTY;
            return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
        }

        @Override public long getAmountAsLong(int index) {
            return validIndex(index) ? legacy.getFluidInTank(index).getAmount() : 0L;
        }

        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return validIndex(index) ? Math.max(0, legacy.getTankCapacity(index)) : 0L;
        }

        @Override public boolean isValid(int index, FluidResource resource) {
            return validIndex(index) && resource != null && !resource.isEmpty()
                    && legacy.isFluidValid(index, resource.toStack(1));
        }

        @Override public int insert(
                int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (amount == 0 || !validIndex(index)) return 0;
            int accepted = legacy.fill(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE);
            accepted = Math.max(0, Math.min(amount, accepted));
            if (accepted > 0) stage(new FluidOperation(true, index, resource, accepted), transaction);
            return accepted;
        }

        @Override public int extract(
                int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (amount == 0 || !validIndex(index)) return 0;
            FluidStack current = legacy.getFluidInTank(index);
            if (!matches(current, resource)) return 0;
            int alreadyStaged = stagedFluidExtraction(index, resource);
            int available = Math.max(0, current.getAmount() - alreadyStaged);
            FluidStack simulated = legacy.drain(resource.toStack(Math.min(amount, available)),
                    IFluidHandler.FluidAction.SIMULATE);
            if (!matches(simulated, resource)) return 0;
            int accepted = Math.min(Math.min(amount, available), simulated.getAmount());
            if (accepted > 0) stage(new FluidOperation(false, index, resource, accepted), transaction);
            return accepted;
        }

        private boolean validIndex(int index) { return index >= 0 && index < legacy.getTanks(); }

        private int stagedFluidExtraction(int index, FluidResource resource) {
            long staged = 0L;
            for (FluidOperation operation : pending) {
                if (!operation.insert && operation.index == index && operation.resource.equals(resource)) {
                    staged += operation.amount;
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, staged);
        }

        @Override void apply(FluidOperation operation) {
            if (!validIndex(operation.index)) return;
            if (operation.insert) {
                legacy.fill(operation.resource.toStack(operation.amount), IFluidHandler.FluidAction.EXECUTE);
            } else if (matches(legacy.getFluidInTank(operation.index), operation.resource)) {
                legacy.drain(operation.resource.toStack(operation.amount), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        private static boolean matches(FluidStack stack, FluidResource resource) {
            return stack != null && !stack.isEmpty() && resource != null && !resource.isEmpty()
                    && resource.equals(FluidResource.of(stack));
        }
    }

    private record EnergyOperation(boolean insert, int amount) {}

    private static final class TransactionalEnergyAdapter extends DeferredJournal<EnergyOperation>
            implements EnergyHandler {
        private final IEnergyStorage legacy;

        private TransactionalEnergyAdapter(IEnergyStorage legacy) {
            this.legacy = legacy;
        }

        @Override public long getAmountAsLong() { return legacy.getEnergyStored(); }

        @Override public long getCapacityAsLong() { return legacy.getMaxEnergyStored(); }

        @Override public int insert(int amount, TransactionContext transaction) {
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
            int accepted = legacy.receiveEnergy(amount, true);
            if (accepted > 0) stage(new EnergyOperation(true, accepted), transaction);
            return accepted;
        }

        @Override public int extract(int amount, TransactionContext transaction) {
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
            int staged = pending.stream().filter(operation -> !operation.insert)
                    .mapToInt(EnergyOperation::amount).sum();
            int accepted = legacy.extractEnergy(Math.min(amount,
                    Math.max(0, legacy.getEnergyStored() - staged)), true);
            if (accepted > 0) stage(new EnergyOperation(false, accepted), transaction);
            return accepted;
        }

        @Override void apply(EnergyOperation operation) {
            if (operation.insert) legacy.receiveEnergy(operation.amount, false);
            else legacy.extractEnergy(operation.amount, false);
        }
    }

    private static int saturatedAdd(int first, int second) {
        return first > Integer.MAX_VALUE - second ? Integer.MAX_VALUE : first + second;
    }
}
