package com.cultivation.cultivation.compat.botania;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ResourceTransferAction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotaniaManaWindowTest {
    @Test
    void intViewSaturatesWithoutTreatingAnOrdinaryLargeLongBalanceAsFull() {
        MutableStore store = new MutableStore((long) Integer.MAX_VALUE + 100L);

        assertEquals(Integer.MAX_VALUE, BotaniaManaWindow.currentMana(store));
        assertEquals(Integer.MAX_VALUE, BotaniaManaWindow.availableSpace(store));
        assertFalse(BotaniaManaWindow.isFull(store));
    }

    @Test
    void longMaxIsTheOnlyFullFiniteLedgerState() {
        MutableStore store = new MutableStore(Long.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, BotaniaManaWindow.currentMana(store));
        assertEquals(0, BotaniaManaWindow.availableSpace(store));
        assertTrue(BotaniaManaWindow.isFull(store));
    }

    @Test
    void integerMinValueWithdrawalWidensBeforeNegation() {
        MutableStore store = new MutableStore((long) Integer.MAX_VALUE + 10L);

        BotaniaManaWindow.receiveMana(store, Integer.MIN_VALUE);

        assertEquals(9L, store.amount());
    }

    @Test
    void positiveInputNearLongMaxClampsWithoutSignedOverflow() {
        MutableStore store = new MutableStore(Long.MAX_VALUE - 5L);

        BotaniaManaWindow.receiveMana(store, 64);

        assertEquals(Long.MAX_VALUE, store.amount());
    }

    private static final class MutableStore implements AtomicEnergyRefill.ResourceStore {
        private final AtomicLong amount;

        private MutableStore(long amount) {
            this.amount = new AtomicLong(amount);
        }

        @Override
        public long amount() {
            return amount.get();
        }

        @Override
        public long extract(long requested, ResourceTransferAction action) {
            long accepted = Math.min(amount.get(), requested);
            if (action.executes()) amount.addAndGet(-accepted);
            return accepted;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            long accepted = Math.min(offered, Long.MAX_VALUE - amount.get());
            if (action.executes()) amount.addAndGet(accepted);
            return accepted;
        }
    }
}
