package com.immortalstorage.immortalstorage.compat.mekanism;

import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.compat.TerminalExternalResourceCompatHooks;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Atomic terminal transaction for Mekanism chemical item capabilities. */
final class MekanismChemicalContainerTransfer {
    static TerminalExternalResourceCompatHooks.TransferResult depositToStorage(
            ItemStack carried, ExternalResourceStorage storage, IItemHandler returnInventory) {
        if (!valid(carried, storage, returnInventory)) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        ItemStack simulatedStack = carried.copyWithCount(1);
        IChemicalHandler simulatedHandler = MekanismCompat.chemicalItemHandler(simulatedStack);
        Content available = firstContent(simulatedHandler);
        if (available == null) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        ChemicalStack simulatedExtract = simulatedHandler.extractChemical(
                available.tank(), available.amount(), Action.SIMULATE);
        if (!matches(simulatedExtract, available.chemical(), available.amount())) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        ResourceChannelKey key = XianqiaoMekanismChemicalAdapter.key(available.chemical());
        long planned = storage.insert(key, simulatedExtract.getAmount(), ResourceTransferAction.SIMULATE);
        if (planned <= 0L || planned > simulatedExtract.getAmount()) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        ItemStack working = carried.copyWithCount(1);
        IChemicalHandler executeHandler = MekanismCompat.chemicalItemHandler(working);
        ChemicalStack executedExtract = executeHandler == null
                ? ChemicalStack.EMPTY
                : executeHandler.extractChemical(available.tank(), planned, Action.EXECUTE);
        if (!matches(executedExtract, available.chemical(), planned)) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        ItemStack outputContainer = working.copyWithCount(1);
        if (!canReturn(carried, outputContainer, returnInventory)) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        long committed = storage.insert(key, planned, ResourceTransferAction.EXECUTE);
        if (committed != planned) {
            rollbackInsert(storage, key, committed);
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        ItemStack nextCarried = commitReturn(carried, outputContainer, returnInventory);
        if (nextCarried == null) {
            rollbackInsert(storage, key, committed);
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        return TerminalExternalResourceCompatHooks.TransferResult.success(nextCarried);
    }

    static TerminalExternalResourceCompatHooks.TransferResult withdrawFromStorage(
            ItemStack carried, ResourceChannelKey key, ExternalResourceStorage storage,
            IItemHandler returnInventory) {
        if (!valid(carried, storage, returnInventory)) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        Chemical chemical = chemicalFor(key);
        if (chemical == null) {
            return TerminalExternalResourceCompatHooks.TransferResult.notHandled(carried);
        }

        ItemStack simulatedStack = carried.copyWithCount(1);
        IChemicalHandler simulatedHandler = MekanismCompat.chemicalItemHandler(simulatedStack);
        if (simulatedHandler == null || firstContent(simulatedHandler) != null) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
        long available = storage.extract(key, Long.MAX_VALUE, ResourceTransferAction.SIMULATE);
        if (available <= 0L) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        TankPlan plan = findTank(simulatedHandler, chemical, available);
        if (plan == null || storage.extract(key, plan.accepted(), ResourceTransferAction.SIMULATE)
                != plan.accepted()) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        ItemStack working = carried.copyWithCount(1);
        IChemicalHandler executeHandler = MekanismCompat.chemicalItemHandler(working);
        ChemicalStack offered = stack(chemical, plan.accepted());
        ChemicalStack remainder = executeHandler == null
                ? offered
                : executeHandler.insertChemical(plan.tank(), offered, Action.EXECUTE);
        if (amount(remainder) != 0L) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        ItemStack outputContainer = working.copyWithCount(1);
        if (!canReturn(carried, outputContainer, returnInventory)) {
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.INCOMPATIBLE_OR_NO_SPACE);
        }

        long committed = storage.extract(key, plan.accepted(), ResourceTransferAction.EXECUTE);
        if (committed != plan.accepted()) {
            rollbackExtract(storage, key, committed);
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        ItemStack nextCarried = commitReturn(carried, outputContainer, returnInventory);
        if (nextCarried == null) {
            rollbackExtract(storage, key, committed);
            return TerminalExternalResourceCompatHooks.TransferResult.failure(
                    TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED);
        }
        return TerminalExternalResourceCompatHooks.TransferResult.success(nextCarried);
    }

    private static boolean valid(ItemStack carried, ExternalResourceStorage storage,
                                 IItemHandler returnInventory) {
        return carried != null && !carried.isEmpty() && storage != null && returnInventory != null;
    }

    private static Content firstContent(IChemicalHandler handler) {
        if (handler == null) return null;
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            ChemicalStack stack = handler.getChemicalInTank(tank);
            if (stack != null && !stack.isEmpty() && stack.getAmount() > 0L
                    && stack.getChemical() != null) {
                return new Content(tank, stack.getChemical(), stack.getAmount());
            }
        }
        return null;
    }

    private static TankPlan findTank(IChemicalHandler handler, Chemical chemical, long available) {
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            long capacity = handler.getChemicalTankCapacity(tank);
            if (capacity <= 0L) continue;
            long offeredAmount = Math.min(available, capacity);
            if (offeredAmount <= 0L) continue;
            ChemicalStack offered = stack(chemical, offeredAmount);
            ChemicalStack remainder = handler.insertChemical(tank, offered, Action.SIMULATE);
            long remaining = amount(remainder);
            if (remaining < 0L || remaining > offeredAmount) {
                throw new IllegalStateException("invalid Mekanism chemical container remainder");
            }
            long accepted = offeredAmount - remaining;
            if (accepted > 0L) return new TankPlan(tank, accepted);
        }
        return null;
    }

    private static Chemical chemicalFor(ResourceChannelKey key) {
        if (key == null || !ExternalResourceChannels.MEKANISM_CHEMICAL_CHANNEL
                .equals(key.channel())) return null;
        ResourceLocation id = ResourceLocation.tryParse(key.resourceId());
        if (id == null) return null;
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
        if (chemical == null || MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical)
                .is(MekanismAPI.EMPTY_CHEMICAL_KEY)) return null;
        return chemical;
    }

    private static boolean matches(ChemicalStack stack, Chemical chemical, long amount) {
        return stack != null && !stack.isEmpty() && stack.getAmount() == amount
                && stack.is(chemical);
    }

    private static long amount(ChemicalStack stack) {
        return stack == null || stack.isEmpty() ? 0L : stack.getAmount();
    }

    private static ChemicalStack stack(Chemical chemical, long amount) {
        return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), amount);
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

    private static void rollbackInsert(ExternalResourceStorage storage, ResourceChannelKey key,
                                       long amount) {
        if (amount > 0L) storage.extract(key, amount, ResourceTransferAction.EXECUTE);
    }

    private static void rollbackExtract(ExternalResourceStorage storage, ResourceChannelKey key,
                                        long amount) {
        if (amount > 0L) storage.insert(key, amount, ResourceTransferAction.EXECUTE);
    }

    private record Content(int tank, Chemical chemical, long amount) {}

    private record TankPlan(int tank, long accepted) {}

    private MekanismChemicalContainerTransfer() {}
}
