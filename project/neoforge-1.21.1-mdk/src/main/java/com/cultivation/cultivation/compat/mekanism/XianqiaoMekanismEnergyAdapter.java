package com.cultivation.cultivation.compat.mekanism;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;

import java.util.Objects;
import java.util.function.Supplier;

/** Official Mekanism long-energy facade over the independent Xianqiao FE ledger. */
public final class XianqiaoMekanismEnergyAdapter implements IStrictEnergyHandler {
    public enum Mode { PULL, PUSH, DISABLED }

    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;
    private final Supplier<Mode> mode;

    public XianqiaoMekanismEnergyAdapter(
            Supplier<AtomicEnergyRefill.ResourceStore> storage,
            Supplier<Mode> mode) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public int getEnergyContainerCount() {
        return 1;
    }

    @Override
    public long getEnergy(int container) {
        AtomicEnergyRefill.ResourceStore current = valid(container) ? storage.get() : null;
        return current == null ? 0L : Math.max(0L, current.amount());
    }

    @Override
    public void setEnergy(int container, long energy) {
        throw new UnsupportedOperationException(
                "Xianqiao energy is transactional and cannot be assigned directly");
    }

    @Override
    public long getMaxEnergy(int container) {
        return valid(container) ? Long.MAX_VALUE : 0L;
    }

    @Override
    public long getNeededEnergy(int container) {
        return valid(container) ? Long.MAX_VALUE - getEnergy(container) : 0L;
    }

    @Override
    public long insertEnergy(int container, long amount, Action action) {
        if (amount <= 0L) return 0L;
        AtomicEnergyRefill.ResourceStore current = valid(container) ? storage.get() : null;
        if (current == null || mode.get() != Mode.PULL) return amount;
        long accepted = current.insert(amount, transferAction(action));
        requireBounded("insert", accepted, amount);
        return amount - accepted;
    }

    @Override
    public long extractEnergy(int container, long amount, Action action) {
        if (amount <= 0L) return 0L;
        AtomicEnergyRefill.ResourceStore current = valid(container) ? storage.get() : null;
        if (current == null || mode.get() != Mode.PUSH) return 0L;
        long extracted = current.extract(amount, transferAction(action));
        requireBounded("extract", extracted, amount);
        return extracted;
    }

    private static boolean valid(int container) {
        return container == 0;
    }

    private static ResourceTransferAction transferAction(Action action) {
        return action.execute() ? ResourceTransferAction.EXECUTE : ResourceTransferAction.SIMULATE;
    }

    private static void requireBounded(String operation, long result, long requested) {
        if (result < 0L || result > requested) {
            throw new IllegalStateException(
                    "Mekanism " + operation + " returned " + result + " for " + requested);
        }
    }
}
