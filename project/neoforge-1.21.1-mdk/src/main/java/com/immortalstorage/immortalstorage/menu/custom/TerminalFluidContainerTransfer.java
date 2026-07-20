package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Safe official-capability transaction used by the fluid terminal. The first
 * pass simulates the container, storage and return inventory with no player
 * fallback, so a full inventory rejects instead of dropping or deleting a
 * returned container. Exactly one execute pass follows a successful plan.
 */
public final class TerminalFluidContainerTransfer {
    public static Result deposit(ItemStack carried, IFluidHandler destination, IItemHandler returnInventory) {
        return transfer(carried, destination, returnInventory, true);
    }

    public static Result withdraw(ItemStack carried, IFluidHandler source, IItemHandler returnInventory) {
        return transfer(carried, source, returnInventory, false);
    }

    public static IFluidHandler exactSource(TerminalFluidStorage storage, TerminalFluidKey key) {
        if (storage == null || key == null) throw new IllegalArgumentException("storage and key are required");
        return new ExactFluidSource(storage, key);
    }

    /** Transactional path used by the terminal's own long-valued storage. */
    public static Result depositToStorage(ItemStack carried, TerminalFluidStorage destination,
                                          IItemHandler returnInventory) {
        if (!valid(carried, destination, returnInventory)) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        var itemHandler = FluidUtil.getFluidHandler(carried.copyWithCount(1));
        if (itemHandler.isEmpty()) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        FluidStack available = itemHandler.get().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        TerminalFluidKey key = TerminalFluidKey.of(available);
        long planned = destination.insert(key, available.getAmount(), TerminalStorageAction.SIMULATE);
        if (planned <= 0L) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);

        FluidStack drained = itemHandler.get().drain(
                available.copyWithAmount((int) Math.min(planned, Integer.MAX_VALUE)),
                IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return Result.failure(Failure.EXECUTE_FAILED);
        ItemStack outputContainer = itemHandler.get().getContainer().copy();
        if (!canReturn(carried, outputContainer, returnInventory)) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        long committed = destination.insert(TerminalFluidKey.of(drained), drained.getAmount(),
                TerminalStorageAction.EXECUTE);
        if (committed != drained.getAmount()) {
            if (committed > 0L) destination.extract(TerminalFluidKey.of(drained), committed,
                    TerminalStorageAction.EXECUTE);
            return Result.failure(Failure.EXECUTE_FAILED);
        }
        ItemStack nextCarried = commitReturn(carried, outputContainer, returnInventory);
        if (nextCarried == null) {
            destination.extract(TerminalFluidKey.of(drained), committed, TerminalStorageAction.EXECUTE);
            return Result.failure(Failure.EXECUTE_FAILED);
        }
        return new Result(true, nextCarried, null);
    }

    /** Transactional exact-key withdrawal used by the terminal's own long storage. */
    public static Result withdrawFromStorage(ItemStack carried, TerminalFluidStorage source,
                                             TerminalFluidKey key, IItemHandler returnInventory) {
        if (!valid(carried, source, returnInventory) || key == null) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        var itemHandler = FluidUtil.getFluidHandler(carried.copyWithCount(1));
        if (itemHandler.isEmpty()) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        long available = source.snapshot().getOrDefault(key, 0L);
        if (available <= 0L) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        FluidStack offered = key.prototype().copyWithAmount((int) Math.min(Integer.MAX_VALUE, available));
        int planned = itemHandler.get().fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (planned <= 0 || source.extract(key, planned, TerminalStorageAction.SIMULATE) != planned) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        int filled = itemHandler.get().fill(offered.copyWithAmount(planned), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) return Result.failure(Failure.EXECUTE_FAILED);
        ItemStack outputContainer = itemHandler.get().getContainer().copy();
        if (!canReturn(carried, outputContainer, returnInventory)) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        long committed = source.extract(key, filled, TerminalStorageAction.EXECUTE);
        if (committed != filled) {
            if (committed > 0L) source.insert(key, committed, TerminalStorageAction.EXECUTE);
            return Result.failure(Failure.EXECUTE_FAILED);
        }
        ItemStack nextCarried = commitReturn(carried, outputContainer, returnInventory);
        if (nextCarried == null) {
            source.insert(key, committed, TerminalStorageAction.EXECUTE);
            return Result.failure(Failure.EXECUTE_FAILED);
        }
        return new Result(true, nextCarried, null);
    }

    private static boolean valid(ItemStack carried, TerminalFluidStorage storage, IItemHandler inventory) {
        return carried != null && !carried.isEmpty() && storage != null && inventory != null;
    }

    private static boolean canReturn(ItemStack carried, ItemStack output, IItemHandler inventory) {
        return carried.getCount() == 1
                || ItemHandlerHelper.insertItemStacked(inventory, output.copy(), true).isEmpty();
    }

    /** Null means an execution-time inventory divergence. */
    private static ItemStack commitReturn(ItemStack carried, ItemStack output, IItemHandler inventory) {
        if (carried.getCount() == 1) return output.copy();
        if (!ItemHandlerHelper.insertItemStacked(inventory, output.copy(), false).isEmpty()) return null;
        return carried.copyWithCount(carried.getCount() - 1);
    }

    private static Result transfer(ItemStack carried, IFluidHandler endpoint,
                                   IItemHandler returnInventory, boolean deposit) {
        if (carried == null || carried.isEmpty() || endpoint == null || returnInventory == null) {
            return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        FluidActionResult simulated = deposit
                ? FluidUtil.tryEmptyContainerAndStow(carried.copy(), endpoint, returnInventory,
                        Integer.MAX_VALUE, null, false)
                : FluidUtil.tryFillContainerAndStow(carried.copy(), endpoint, returnInventory,
                        Integer.MAX_VALUE, null, false);
        if (!simulated.isSuccess()) return Result.failure(Failure.INCOMPATIBLE_OR_NO_SPACE);

        FluidActionResult executed = deposit
                ? FluidUtil.tryEmptyContainerAndStow(carried.copy(), endpoint, returnInventory,
                        Integer.MAX_VALUE, null, true)
                : FluidUtil.tryFillContainerAndStow(carried.copy(), endpoint, returnInventory,
                        Integer.MAX_VALUE, null, true);
        return executed.isSuccess()
                ? new Result(true, executed.getResult(), null)
                : Result.failure(Failure.EXECUTE_FAILED);
    }

    public enum Failure {
        INCOMPATIBLE_OR_NO_SPACE,
        EXECUTE_FAILED
    }

    public record Result(boolean success, ItemStack carried, Failure failure) {
        public Result {
            carried = carried == null ? ItemStack.EMPTY : carried.copy();
            if ((success && failure != null) || (!success && failure == null)) {
                throw new IllegalArgumentException("inconsistent transfer result");
            }
        }
        @Override public ItemStack carried() { return carried.copy(); }
        private static Result failure(Failure failure) { return new Result(false, ItemStack.EMPTY, failure); }
    }

    private static final class ExactFluidSource implements IFluidHandler {
        private final TerminalFluidStorage storage;
        private final TerminalFluidKey key;

        private ExactFluidSource(TerminalFluidStorage storage, TerminalFluidKey key) {
            this.storage = storage;
            this.key = key;
        }

        @Override public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) return FluidStack.EMPTY;
            long amount = storage.snapshot().getOrDefault(key, 0L);
            return amount <= 0L ? FluidStack.EMPTY
                    : key.prototype().copyWithAmount((int) Math.min(Integer.MAX_VALUE, amount));
        }

        @Override public int getTankCapacity(int tank) { return tank == 0 ? Integer.MAX_VALUE : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty() || !key.matches(resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || action == null) return FluidStack.EMPTY;
            long extracted = storage.extract(key, maxDrain,
                    action.execute() ? TerminalStorageAction.EXECUTE : TerminalStorageAction.SIMULATE);
            return extracted <= 0L ? FluidStack.EMPTY
                    : key.prototype().copyWithAmount((int) Math.min(Integer.MAX_VALUE, extracted));
        }
    }

    private TerminalFluidContainerTransfer() {}
}
