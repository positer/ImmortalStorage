package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LongResourceLedgerTest {
    private static final ResourceChannelKey ENERGY =
            new ResourceChannelKey("energy", "neoforge:fe");
    private static final ResourceChannelKey HYDROGEN =
            new ResourceChannelKey("mekanism_chemical", "mekanism:hydrogen");

    @Test
    void simulationReportsTheExactTransferWithoutChangingBalanceOrRevision() {
        LongResourceLedger ledger = new LongResourceLedger();
        assertEquals(120L, ledger.insert(ENERGY, 120L, ResourceTransferAction.EXECUTE));
        long revision = ledger.revision();

        assertEquals(80L, ledger.extract(ENERGY, 80L, ResourceTransferAction.SIMULATE));
        assertEquals(120L, ledger.amount(ENERGY));
        assertEquals(revision, ledger.revision());

        assertEquals(80L, ledger.extract(ENERGY, 80L, ResourceTransferAction.EXECUTE));
        assertEquals(40L, ledger.amount(ENERGY));
        assertEquals(revision + 1L, ledger.revision());
    }

    @Test
    void independentResourceKeysDoNotAliasAndSnapshotsAreStable() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(HYDROGEN, 90L, ResourceTransferAction.EXECUTE);
        ledger.insert(ENERGY, 20L, ResourceTransferAction.EXECUTE);

        assertEquals(90L, ledger.amount(HYDROGEN));
        assertEquals(20L, ledger.amount(ENERGY));
        assertEquals(HYDROGEN, ledger.snapshot().get(0).key());
        assertEquals(ENERGY, ledger.snapshot().get(1).key());
        assertThrows(UnsupportedOperationException.class,
                () -> ledger.snapshot().add(new ResourceChannelEntry(ENERGY, 1L)));
    }

    @Test
    void longOverflowSaturatesAndNoOpMutationsDoNotAdvanceRevision() {
        LongResourceLedger ledger = new LongResourceLedger();
        assertEquals(Long.MAX_VALUE - 2L,
                ledger.insert(ENERGY, Long.MAX_VALUE - 2L, ResourceTransferAction.EXECUTE));
        long revision = ledger.revision();

        assertEquals(2L, ledger.insert(ENERGY, 10L, ResourceTransferAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, ledger.amount(ENERGY));
        assertEquals(revision + 1L, ledger.revision());
        assertEquals(0L, ledger.insert(ENERGY, 1L, ResourceTransferAction.EXECUTE));
        assertEquals(revision + 1L, ledger.revision());
        assertEquals(0L, ledger.extract(ENERGY, 0L, ResourceTransferAction.EXECUTE));
        assertEquals(revision + 1L, ledger.revision());
    }

    @Test
    void channelKeysRejectUnstableOrAmbiguousIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceChannelKey("", "mekanism:oxygen"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceChannelKey("MEK", "mekanism:oxygen"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceChannelKey("mek", "oxygen"));
        assertTrue(new ResourceChannelKey("mekanism_chemical", "mekanism:oxygen")
                .toString().contains("mekanism:oxygen"));
        assertFalse(ENERGY.equals(HYDROGEN));
    }
}
