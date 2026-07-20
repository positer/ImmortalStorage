package com.immortalstorage.immortalstorage.compat.botania;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;

import java.util.Objects;

/** Safe int window used by Botania over a long-valued Xianqiao mana store. */
public final class BotaniaManaWindow {
    private BotaniaManaWindow() {
    }

    public static int currentMana(AtomicEnergyRefill.ResourceStore storage) {
        return LongAmountBridge.saturatingInt(amount(storage));
    }

    public static boolean isFull(AtomicEnergyRefill.ResourceStore storage) {
        return amount(storage) == Long.MAX_VALUE;
    }

    public static int availableSpace(AtomicEnergyRefill.ResourceStore storage) {
        return LongAmountBridge.saturatingInt(Long.MAX_VALUE - amount(storage));
    }

    public static void receiveMana(AtomicEnergyRefill.ResourceStore storage, int delta) {
        Objects.requireNonNull(storage, "storage");
        if (delta > 0) {
            long offered = Math.min((long) delta, Long.MAX_VALUE - amount(storage));
            if (offered > 0L) storage.insert(offered, ResourceTransferAction.EXECUTE);
        } else if (delta < 0) {
            // Widen before negation: -(int) Integer.MIN_VALUE would overflow.
            long requested = -(long) delta;
            storage.extract(requested, ResourceTransferAction.EXECUTE);
        }
    }

    private static long amount(AtomicEnergyRefill.ResourceStore storage) {
        Objects.requireNonNull(storage, "storage");
        long amount = storage.amount();
        if (amount < 0L) throw new IllegalStateException("Xianqiao mana amount must be non-negative");
        return amount;
    }
}
