package com.cultivation.cultivation.compat.ifsouls;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.LongResourceLedger;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.core.resource.ResourceTransferAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SoulTransferPortTest {
    private static final ResourceChannelKey SOUL =
            new ResourceChannelKey("industrial_foregoing_soul", "industrialforegoingsouls:soul");

    @Test
    void pullAcceptsAndPushExtractsWithSimulationPreservingTheLongLedger() {
        LongResourceLedger ledger = new LongResourceLedger();
        SoulTransferPort port = new SoulTransferPort(() -> store(ledger));

        assertEquals(64, port.fill(64, false));
        assertEquals(0L, ledger.amount(SOUL));
        assertEquals(64, port.fill(64, true));
        assertEquals(64L, ledger.amount(SOUL));
        assertEquals(10, port.drain(10, false));
        assertEquals(64L, ledger.amount(SOUL));
        assertEquals(10, port.drain(10, true));
        assertEquals(54L, ledger.amount(SOUL));
        assertEquals(1, port.fill(1, false));
    }

    @Test
    void disabledAndMissingStoresFailClosedWhileLongAmountsSaturateForIntCallers() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(SOUL, (long) Integer.MAX_VALUE + 99L, ResourceTransferAction.EXECUTE);
        java.util.concurrent.atomic.AtomicReference<AtomicEnergyRefill.ResourceStore> storage =
                new java.util.concurrent.atomic.AtomicReference<>(store(ledger));
        SoulTransferPort port = new SoulTransferPort(storage::get);

        assertEquals(Integer.MAX_VALUE, port.stored(0));
        assertEquals(Integer.MAX_VALUE, port.capacity(0));
        assertEquals(Integer.MAX_VALUE, port.drain(Integer.MAX_VALUE, false));
        assertEquals((long) Integer.MAX_VALUE + 99L, ledger.amount(SOUL));

        storage.set(null);
        assertEquals(0, port.tankCount());
        assertEquals(0, port.stored(0));
    }

    private static AtomicEnergyRefill.ResourceStore store(LongResourceLedger ledger) {
        return new AtomicEnergyRefill.ResourceStore() {
            @Override
            public long amount() {
                return ledger.amount(SOUL);
            }

            @Override
            public long extract(long requested, ResourceTransferAction action) {
                return ledger.extract(SOUL, requested, action);
            }

            @Override
            public long insert(long offered, ResourceTransferAction action) {
                return ledger.insert(SOUL, offered, action);
            }
        };
    }
}
