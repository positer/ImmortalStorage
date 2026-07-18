package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinBufferTest {
    @Test
    void simulationReadsTheRealCacheWithoutMutatingIt() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(100);
        assertTrue(buffer.addCycle(64, () -> true));

        assertEquals(32, buffer.extract(32, true));
        assertEquals(64, buffer.available());
        assertEquals(32, buffer.extract(32, false));
        assertEquals(32, buffer.available());
    }

    @Test
    void anUnavailableChargeCannotCreateCachedOutput() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(100);
        AtomicInteger attempts = new AtomicInteger();

        assertFalse(buffer.addCycle(64, () -> {
            attempts.incrementAndGet();
            return false;
        }));

        assertEquals(1, attempts.get());
        assertEquals(0, buffer.available());
        assertEquals(0, buffer.extract(64, false));
    }

    @Test
    void aFullCacheDoesNotAttemptOrConsumeAnotherCharge() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(64);
        assertTrue(buffer.addCycle(64, () -> true));
        AtomicInteger attempts = new AtomicInteger();

        assertFalse(buffer.addCycle(1, () -> {
            attempts.incrementAndGet();
            return true;
        }));

        assertEquals(0, attempts.get());
        assertEquals(64, buffer.available());
    }

    @Test
    void cacheRoundTripsThroughBlockEntityNbt() {
        SourceVeinBuffer original = new SourceVeinBuffer(100);
        assertTrue(original.addCycle(73, () -> true));
        CompoundTag tag = new CompoundTag();
        original.save(tag);

        SourceVeinBuffer restored = new SourceVeinBuffer(100);
        restored.load(tag);

        assertEquals(73, restored.available());
    }

    @Test
    void manualItemCycleUsesOneConfiguredBatchOrOneFreeMaximumStack() {
        assertEquals(64, SourceVeinBuffer.manualItemBatch(VeinKind.COBBLE, 64));
        assertEquals(64, SourceVeinBuffer.manualItemBatch(VeinKind.STONE, 64));
        assertEquals(32, SourceVeinBuffer.manualItemBatch(VeinKind.COAL, 64));
        assertEquals(16, SourceVeinBuffer.manualItemBatch(VeinKind.RAW_COPPER, 64));
        assertEquals(1, SourceVeinBuffer.manualItemBatch(VeinKind.ANCIENT_DEBRIS, 64));
        assertEquals(1, SourceVeinBuffer.manualItemBatch(VeinKind.NETHER_STAR, 64));
    }

    @Test
    void cacheTargetUsesTheOwnersWholeImmortalYuanCapAndSaturatesSafely() {
        long capacity = Long.MAX_VALUE;

        assertEquals(capacity,
                SourceVeinBuffer.targetForChargeCapacity(64, 0, 64, capacity));
        assertEquals(capacity,
                SourceVeinBuffer.targetForChargeCapacity(-1, 8, 1, capacity));
        assertEquals(128,
                SourceVeinBuffer.targetForChargeCapacity(1024, 8, 1, capacity));
        assertEquals(32_768,
                SourceVeinBuffer.targetForChargeCapacity(1024, 1, 32, capacity));
        assertEquals(0,
                SourceVeinBuffer.targetForChargeCapacity(7, 8, 1, capacity));
        assertEquals(capacity,
                SourceVeinBuffer.targetForChargeCapacity(Long.MAX_VALUE, 1, 64, capacity));
    }

    @Test
    void paidRefillAggregatesOnlyAffordableCompleteBatches() {
        assertEquals(96,
                SourceVeinBuffer.affordableRefill(320, 64, 3, 1, 32));
        assertEquals(10,
                SourceVeinBuffer.affordableRefill(128, 10, 80, 8, 1));
        assertEquals(1,
                SourceVeinBuffer.affordableRefill(64, 63, 100, 1, 32));
        assertEquals(0,
                SourceVeinBuffer.affordableRefill(64, 64, 100, 1, 32));
        assertEquals(0,
                SourceVeinBuffer.affordableRefill(64, 0, 0, 1, 32));
    }

    @Test
    void oneBulkCacheAdditionSecuresItsAggregateChargeExactlyOnce() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(100_000);
        AtomicInteger attempts = new AtomicInteger();

        assertTrue(buffer.addCycle(32_768, () -> {
            attempts.incrementAndGet();
            return true;
        }));

        assertEquals(1, attempts.get());
        assertEquals(32_768, buffer.available());
    }

    @Test
    void aKnownFailedDeliveryCanRestorePrepaidCachedUnitsWithoutChargingAgain() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(100);
        AtomicInteger charges = new AtomicInteger();
        assertTrue(buffer.addCycle(64, () -> {
            charges.incrementAndGet();
            return true;
        }));

        assertEquals(32, buffer.extract(32, false));
        assertEquals(32, buffer.restore(32));

        assertEquals(64, buffer.available());
        assertEquals(1, charges.get(), "rolling back a delivery must not charge the owner twice");
    }

    @Test
    void freeSourceMaterializesItsCompatibilityMaximumInTheRealCache() {
        SourceVeinBuffer buffer = new SourceVeinBuffer(Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, buffer.fillToCapacityWithoutCharge());
        assertEquals(Long.MAX_VALUE, buffer.available());
        assertEquals(0L, buffer.fillToCapacityWithoutCharge(),
                "repeated load/tick reconciliation must be idempotent");
        assertEquals(Long.MAX_VALUE, buffer.extract(Long.MAX_VALUE, true));
    }

    @Test
    void persistedCacheCannotBeReplayedIntoAnotherSourceKind() {
        CompoundTag legacy = new CompoundTag();
        CompoundTag matching = new CompoundTag();
        matching.putString("Kind", VeinKind.STONE.name());
        CompoundTag mismatched = new CompoundTag();
        mismatched.putString("Kind", VeinKind.DIAMOND.name());

        assertTrue(SourceVeinBlockEntity.acceptsPersistedKind(legacy, VeinKind.STONE),
                "old saves without a Kind discriminator remain compatible");
        assertTrue(SourceVeinBlockEntity.acceptsPersistedKind(matching, VeinKind.STONE));
        assertFalse(SourceVeinBlockEntity.acceptsPersistedKind(mismatched, VeinKind.STONE));
    }
}
