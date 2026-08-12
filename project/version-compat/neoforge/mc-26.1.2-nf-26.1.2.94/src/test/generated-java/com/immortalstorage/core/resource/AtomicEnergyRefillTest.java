package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AtomicEnergyRefillTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void existingEnergyIsUsedBeforeTheMinimumWholeImmortalYuanConversion() {
        FakeEnergyStore storage = new FakeEnergyStore(40L);
        FakeChargeSource immortalYuan = new FakeChargeSource(3L);
        FakeTarget target = new FakeTarget(1_000L);

        AtomicEnergyRefill.Result result = AtomicEnergyRefill.transfer(
                130L, 1_000L, 100L, storage, immortalYuan, target,
                ResourceTransferAction.EXECUTE);

        assertEquals(130L, result.delivered());
        assertEquals(40L, result.storedResourceUsed());
        assertEquals(1L, result.chargeUnitsConsumed());
        assertEquals(10L, result.conversionRemainderStored());
        assertEquals(10L, storage.amount());
        assertEquals(2L, immortalYuan.availableUnits());
        assertEquals(130L, target.stored);
    }

    @Test
    void simulationPredictsTheSamePlanWithoutMutatingAnyParticipant() {
        FakeEnergyStore storage = new FakeEnergyStore(25L);
        FakeChargeSource immortalYuan = new FakeChargeSource(5L);
        FakeTarget target = new FakeTarget(90L);

        AtomicEnergyRefill.Result simulated = AtomicEnergyRefill.transfer(
                200L, 150L, 64L, storage, immortalYuan, target,
                ResourceTransferAction.SIMULATE);

        assertEquals(90L, simulated.delivered());
        assertEquals(25L, simulated.storedResourceUsed());
        assertEquals(2L, simulated.chargeUnitsConsumed());
        assertEquals(63L, simulated.conversionRemainderStored());
        assertEquals(25L, storage.amount());
        assertEquals(5L, immortalYuan.availableUnits());
        assertEquals(0L, target.stored);
    }

    @Test
    void perTickLimitAndAvailableChargeBoundDeliveryWithoutOvercharging() {
        FakeEnergyStore storage = new FakeEnergyStore(0L);
        FakeChargeSource immortalYuan = new FakeChargeSource(1L);
        FakeTarget target = new FakeTarget(Long.MAX_VALUE);

        AtomicEnergyRefill.Result result = AtomicEnergyRefill.transfer(
                Long.MAX_VALUE, 250L, 100L, storage, immortalYuan, target,
                ResourceTransferAction.EXECUTE);

        assertEquals(100L, result.delivered());
        assertEquals(1L, result.chargeUnitsConsumed());
        assertEquals(0L, storage.amount());
        assertEquals(0L, immortalYuan.availableUnits());
        assertFalse(result.requestSatisfied());
    }

    @Test
    void executionReconcilesADeviceThatAcceptsLessThanItSimulated() {
        FakeEnergyStore storage = new FakeEnergyStore(30L);
        FakeChargeSource immortalYuan = new FakeChargeSource(4L);
        FakeTarget target = new FakeTarget(500L);
        target.executeLimit = 55L;

        AtomicEnergyRefill.Result result = AtomicEnergyRefill.transfer(
                180L, 180L, 100L, storage, immortalYuan, target,
                ResourceTransferAction.EXECUTE);

        assertEquals(55L, result.delivered());
        assertEquals(30L, result.storedResourceUsed());
        assertEquals(1L, result.chargeUnitsConsumed());
        assertEquals(75L, result.conversionRemainderStored());
        assertEquals(75L, storage.amount());
        assertEquals(3L, immortalYuan.availableUnits());
        assertTrue(result.targetExecutionReduced());
    }

    @Test
    void zeroOrInvalidRequestsNeverTouchTheTargetOrChargeSource() {
        FakeEnergyStore storage = new FakeEnergyStore(20L);
        FakeChargeSource immortalYuan = new FakeChargeSource(2L);
        FakeTarget target = new FakeTarget(100L);

        AtomicEnergyRefill.Result result = AtomicEnergyRefill.transfer(
                0L, 100L, 50L, storage, immortalYuan, target,
                ResourceTransferAction.EXECUTE);

        assertEquals(0L, result.delivered());
        assertEquals(0, target.calls);
        assertEquals(20L, storage.amount());
        assertEquals(2L, immortalYuan.availableUnits());
    }

    private static final class FakeEnergyStore implements AtomicEnergyRefill.ResourceStore {
        private long stored;

        private FakeEnergyStore(long stored) {
            this.stored = stored;
        }

        @Override
        public long amount() {
            return stored;
        }

        @Override
        public long extract(long requested, ResourceTransferAction action) {
            long accepted = Math.min(stored, Math.max(0L, requested));
            if (action.executes()) stored -= accepted;
            return accepted;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            long accepted = Math.min(Math.max(0L, offered), Long.MAX_VALUE - stored);
            if (action.executes()) stored += accepted;
            return accepted;
        }
    }

    private static final class FakeChargeSource implements AtomicEnergyRefill.ChargeSource {
        private long available;

        private FakeChargeSource(long available) {
            this.available = available;
        }

        @Override
        public long availableUnits() {
            return available;
        }

        @Override
        public long consume(long requestedUnits, ResourceTransferAction action) {
            long accepted = Math.min(available, Math.max(0L, requestedUnits));
            if (action.executes()) available -= accepted;
            return accepted;
        }
    }

    private static final class FakeTarget implements AtomicEnergyRefill.EnergyTarget {
        private final long capacity;
        private long executeLimit = Long.MAX_VALUE;
        private long stored;
        private int calls;

        private FakeTarget(long capacity) {
            this.capacity = capacity;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            calls++;
            long accepted = Math.min(Math.max(0L, offered), capacity - stored);
            if (action.executes()) {
                accepted = Math.min(accepted, executeLimit);
                stored += accepted;
            }
            return accepted;
        }
    }
}
