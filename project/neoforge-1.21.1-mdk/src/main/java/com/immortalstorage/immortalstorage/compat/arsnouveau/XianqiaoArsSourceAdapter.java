package com.immortalstorage.immortalstorage.compat.arsnouveau;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Official Ars Nouveau int Source facade over ImmortalStorage's shared long ledger.
 * Ars discovers providers by position rather than a block face, so all source
 * transactions intentionally bypass the interface's sided transfer modes.
 */
public final class XianqiaoArsSourceAdapter implements ISourceTile {
    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;

    public XianqiaoArsSourceAdapter(
            Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public int getTransferRate() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canAcceptSource() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current != null && current.amount() < Long.MAX_VALUE;
    }

    @Override
    public boolean canProvideSource() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current != null && current.amount() > 0L;
    }

    @Override
    public int getSource() {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        return current == null ? 0 : LongAmountBridge.saturatingInt(current.amount());
    }

    @Override
    public int getMaxSource() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int setSource(int source) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return 0;
        long target = Math.max(0L, source);
        long before = current.amount();
        if (target > before) {
            current.insert(target - before, ResourceTransferAction.EXECUTE);
        } else if (target < before) {
            current.extract(before - target, ResourceTransferAction.EXECUTE);
        }
        return visibleAmount(current);
    }

    @Override
    public int addSource(int source) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current != null && source > 0) {
            current.insert(source, ResourceTransferAction.EXECUTE);
        }
        return visibleAmount(current);
    }

    @Override
    public int addSource(int source, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || source <= 0) return 0;
        long accepted = current.insert(source, simulate
                ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE);
        return LongAmountBridge.saturatingInt(accepted);
    }

    @Override
    public int removeSource(int source) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current != null && source > 0) {
            current.extract(source, ResourceTransferAction.EXECUTE);
        }
        return visibleAmount(current);
    }

    @Override
    public int removeSource(int source, boolean simulate) {
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null || source <= 0) return 0;
        long extracted = current.extract(source, simulate
                ? ResourceTransferAction.SIMULATE : ResourceTransferAction.EXECUTE);
        return LongAmountBridge.saturatingInt(extracted);
    }

    private int visibleAmount(AtomicEnergyRefill.ResourceStore current) {
        return current == null ? 0 : LongAmountBridge.saturatingInt(current.amount());
    }
}
