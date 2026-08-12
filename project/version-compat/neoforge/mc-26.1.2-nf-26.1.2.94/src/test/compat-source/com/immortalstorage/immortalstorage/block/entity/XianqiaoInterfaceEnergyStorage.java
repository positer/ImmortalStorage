package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Objects;
import java.util.function.Supplier;

/** Int-valued NeoForge FE facade over the live owner-bound long ledger. */
final class XianqiaoInterfaceEnergyStorage implements IEnergyStorage {
    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;

    XianqiaoInterfaceEnergyStorage(Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || !canReceive() || toReceive <= 0) return 0;
        long accepted = current.insert(toReceive, action(simulate));
        return saturatingInt(accepted);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || !canExtract() || toExtract <= 0) return 0;
        long extracted = current.extract(toExtract, action(simulate));
        return saturatingInt(extracted);
    }

    @Override
    public int getEnergyStored() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current == null ? 0 : saturatingInt(current.amount());
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return storage.get() != null;
    }

    @Override
    public boolean canReceive() {
        return storage.get() != null;
    }

    private static ResourceTransferAction action(boolean simulate) {
        return simulate ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE;
    }

    private static int saturatingInt(long amount) {
        if (amount <= 0L) return 0;
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }
}
