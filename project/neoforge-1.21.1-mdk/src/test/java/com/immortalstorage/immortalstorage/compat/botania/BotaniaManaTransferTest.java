package com.immortalstorage.immortalstorage.compat.botania;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.LongResourceLedger;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotaniaManaTransferTest {
    private static final ResourceChannelKey MANA =
            new ResourceChannelKey("botania_mana", "botania:mana");

    @Test
    void simulationDoesNotMutatePoolManaOrInternalBalances() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(MANA, 8_000L, ResourceTransferAction.EXECUTE);
        AtomicLong immortalYuan = new AtomicLong(4L);
        FakePool pool = new FakePool(500, 10_000);

        AtomicEnergyRefill.Result result = BotaniaManaTransfer.fillPool(
                2_000, 2_000L, 5_000L,
                BotaniaManaTransfer.ledgerStore(ledger, MANA),
                chargeSource(immortalYuan), pool,
                ResourceTransferAction.SIMULATE);

        assertEquals(2_000L, result.delivered());
        assertEquals(8_000L, ledger.amount(MANA));
        assertEquals(4L, immortalYuan.get());
        assertEquals(500, pool.currentMana());
    }

    @Test
    void executionFillsOnlyTheConfiguredPerTickLimitAndUsesStoredManaFirst() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(MANA, 8_000L, ResourceTransferAction.EXECUTE);
        AtomicLong immortalYuan = new AtomicLong(4L);
        FakePool pool = new FakePool(500, 10_000);

        AtomicEnergyRefill.Result result = BotaniaManaTransfer.fillPool(
                9_500, 2_000L, 5_000L,
                BotaniaManaTransfer.ledgerStore(ledger, MANA),
                chargeSource(immortalYuan), pool,
                ResourceTransferAction.EXECUTE);

        assertEquals(2_000L, result.delivered());
        assertEquals(2_500, pool.currentMana());
        assertEquals(6_000L, ledger.amount(MANA));
        assertEquals(4L, immortalYuan.get());
        assertEquals(0L, result.chargeUnitsConsumed());
    }

    @Test
    void insufficientStoredManaConsumesWholeImmortalYuanAndReturnsConversionRemainder() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(MANA, 600L, ResourceTransferAction.EXECUTE);
        AtomicLong immortalYuan = new AtomicLong(2L);
        FakePool pool = new FakePool(0, 2_000);

        AtomicEnergyRefill.Result result = BotaniaManaTransfer.fillPool(
                2_000, 2_000L, 1_000L,
                BotaniaManaTransfer.ledgerStore(ledger, MANA),
                chargeSource(immortalYuan), pool,
                ResourceTransferAction.EXECUTE);

        assertEquals(2_000L, result.delivered());
        assertEquals(600L, result.storedResourceUsed());
        assertEquals(2L, result.chargeUnitsConsumed());
        assertEquals(600L, result.conversionRemainderStored());
        assertEquals(600L, ledger.amount(MANA));
        assertEquals(0L, immortalYuan.get());
        assertEquals(2_000, pool.currentMana());
    }

    @Test
    void executionReductionCommitsOnlyTheManaActuallyObservedInThePool() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(MANA, 2_000L, ResourceTransferAction.EXECUTE);
        AtomicLong immortalYuan = new AtomicLong(0L);
        FakePool pool = new FakePool(0, 2_000);
        pool.executionLimit = 640;

        AtomicEnergyRefill.Result result = BotaniaManaTransfer.fillPool(
                2_000, 2_000L, 1_000L,
                BotaniaManaTransfer.ledgerStore(ledger, MANA),
                chargeSource(immortalYuan), pool,
                ResourceTransferAction.EXECUTE);

        assertEquals(640L, result.delivered());
        assertTrue(result.targetExecutionReduced());
        assertEquals(1_360L, ledger.amount(MANA));
        assertEquals(640, pool.currentMana());
    }

    @Test
    void invalidPoolStateAndStageGateFailClosedWithoutTouchingBalances() {
        LongResourceLedger ledger = new LongResourceLedger();
        ledger.insert(MANA, 2_000L, ResourceTransferAction.EXECUTE);
        AtomicLong immortalYuan = new AtomicLong(2L);
        FakePool invalid = new FakePool(3_000, 2_000);

        AtomicEnergyRefill.Result result = BotaniaManaTransfer.fillPoolIfAllowed(
                false, 2_000, 2_000L, 1_000L,
                BotaniaManaTransfer.ledgerStore(ledger, MANA),
                chargeSource(immortalYuan), invalid,
                ResourceTransferAction.EXECUTE);

        assertEquals(0L, result.delivered());
        assertFalse(result.requestSatisfied());
        assertEquals(2_000L, ledger.amount(MANA));
        assertEquals(2L, immortalYuan.get());
        assertEquals(3_000, invalid.currentMana());
    }

    private static AtomicEnergyRefill.ChargeSource chargeSource(AtomicLong balance) {
        return new AtomicEnergyRefill.ChargeSource() {
            @Override
            public long availableUnits() {
                return balance.get();
            }

            @Override
            public long consume(long requestedUnits, ResourceTransferAction action) {
                long accepted = Math.min(balance.get(), requestedUnits);
                if (action.executes()) balance.addAndGet(-accepted);
                return accepted;
            }
        };
    }

    private static final class FakePool implements BotaniaManaTransfer.IntManaPool {
        private int mana;
        private final int maxMana;
        private int executionLimit = Integer.MAX_VALUE;

        private FakePool(int mana, int maxMana) {
            this.mana = mana;
            this.maxMana = maxMana;
        }

        @Override
        public int currentMana() {
            return mana;
        }

        @Override
        public int maxMana() {
            return maxMana;
        }

        @Override
        public void receiveMana(int amount) {
            int accepted = Math.min(Math.max(0, amount), executionLimit);
            mana = Math.min(maxMana, mana + accepted);
        }
    }
}
