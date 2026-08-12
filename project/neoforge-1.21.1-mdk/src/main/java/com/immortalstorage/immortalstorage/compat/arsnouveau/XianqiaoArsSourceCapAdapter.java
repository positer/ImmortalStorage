package com.immortalstorage.immortalstorage.compat.arsnouveau;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.hollingsworth.arsnouveau.api.source.ISourceCap;

import java.util.Objects;
import java.util.function.Supplier;

/** Official Ars Nouveau sided Source capability over a long-valued crystal cache. */
public final class XianqiaoArsSourceCapAdapter implements ISourceCap {
    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;

    public XianqiaoArsSourceCapAdapter(
            Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override public boolean canAcceptSource(int amount) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current != null && amount > 0 && current.amount() < Long.MAX_VALUE;
    }

    @Override public boolean canProvideSource(int amount) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current != null && amount > 0 && current.amount() > 0L;
    }

    @Override public int getMaxExtract() { return Integer.MAX_VALUE; }
    @Override public int getMaxReceive() { return Integer.MAX_VALUE; }

    @Override public int getSource() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current == null ? 0 : LongAmountBridge.saturatingInt(current.amount());
    }

    @Override public int getSourceCapacity() { return Integer.MAX_VALUE; }

    @Override public void setSource(int source) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return;
        long target = Math.max(0L, source);
        long before = current.amount();
        if (target > before) {
            current.insert(target - before, ResourceTransferAction.EXECUTE);
        } else if (target < before) {
            current.extract(before - target, ResourceTransferAction.EXECUTE);
        }
    }

    @Override public void setMaxSource(int source) {
        // Crystal capacity is configuration-backed and is not mutable through
        // an adjacent Ars container.
    }

    @Override public int receiveSource(int amount, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || amount <= 0) return 0;
        long accepted = current.insert(amount, simulate
                ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE);
        return LongAmountBridge.saturatingInt(accepted);
    }

    @Override public int extractSource(int amount, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || amount <= 0) return 0;
        long extracted = current.extract(amount, simulate
                ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE);
        return LongAmountBridge.saturatingInt(extracted);
    }
}
