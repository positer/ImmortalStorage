package com.immortalstorage.immortalstorage.compat.ifsouls;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;

import java.util.Objects;
import java.util.function.Supplier;

/** Loader-neutral transaction core for the optional int-valued soul capability. */
public final class SoulTransferPort {
    private final Supplier<AtomicEnergyRefill.ResourceStore> storage;

    public SoulTransferPort(Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public int tankCount() {
        return storage.get() == null ? 0 : 1;
    }

    public int stored(int tank) {
        AtomicEnergyRefill.ResourceStore current = valid(tank) ? storage.get() : null;
        return current == null ? 0 : LongAmountBridge.saturatingInt(current.amount());
    }

    public int capacity(int tank) {
        return valid(tank) && storage.get() != null ? Integer.MAX_VALUE : 0;
    }

    public int fill(int amount, boolean execute) {
        if (amount <= 0) return 0;
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return 0;
        long accepted = current.insert(amount, action(execute));
        requireBounded("fill", accepted, amount);
        return LongAmountBridge.saturatingInt(accepted);
    }

    public int drain(int amount, boolean execute) {
        if (amount <= 0) return 0;
        AtomicEnergyRefill.ResourceStore current = storage.get();
        if (current == null) return 0;
        long extracted = current.extract(amount, action(execute));
        requireBounded("drain", extracted, amount);
        return LongAmountBridge.saturatingInt(extracted);
    }

    private boolean valid(int tank) {
        return tank == 0;
    }

    private static ResourceTransferAction action(boolean execute) {
        return execute ? ResourceTransferAction.EXECUTE : ResourceTransferAction.SIMULATE;
    }

    private static void requireBounded(String operation, long result, long requested) {
        if (result < 0L || result > requested) {
            throw new IllegalStateException(
                    "Industrial Foregoing Souls " + operation + " returned " + result
                            + " for " + requested);
        }
    }
}
