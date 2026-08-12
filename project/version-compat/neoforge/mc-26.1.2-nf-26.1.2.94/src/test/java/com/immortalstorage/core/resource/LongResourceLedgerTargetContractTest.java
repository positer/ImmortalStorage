package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(com.immortalstorage.immortalstorage.compat.CompatTestBootstrapExtension.class)
final class LongResourceLedgerTargetContractTest {
    private static final ResourceChannelKey FE = ExternalResourceChannels.FE;
    private static final ResourceChannelKey CHEMICAL =
            ExternalResourceChannels.mekanismChemical("mekanism:hydrogen");

    @Test
    void feUsesTheSharedLongLedgerWithoutAliasingChemicalChannels() {
        LongResourceLedger ledger = new LongResourceLedger();

        assertEquals(Long.MAX_VALUE - 2L,
                ledger.insert(FE, Long.MAX_VALUE - 2L, ResourceTransferAction.EXECUTE));
        assertEquals(2L,
                ledger.insert(FE, 10L, ResourceTransferAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, ledger.amount(FE));
        assertEquals(0L, ledger.amount(CHEMICAL));
        assertFalse(ledger.snapshot().isEmpty());
    }

    @Test
    void simulationsAreReadOnlyAndExecutionIsBoundedByTheSharedBalance() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(FE, 120L, ResourceTransferAction.EXECUTE);
        long revision = ledger.revision();

        assertEquals(90L, ledger.extract(FE, 90L, ResourceTransferAction.SIMULATE));
        assertEquals(120L, ledger.amount(FE));
        assertEquals(revision, ledger.revision());

        assertEquals(120L, ledger.extract(FE, 200L, ResourceTransferAction.EXECUTE));
        assertEquals(0L, ledger.amount(FE));
        assertEquals(revision + 1L, ledger.revision());
        assertTrue(ledger.snapshot().isEmpty());
    }
}
