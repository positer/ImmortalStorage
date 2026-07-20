package com.immortalstorage.core.amount;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LongAmountBridgeTest {
    @Test
    void saturatesLongTotalsWithoutSignedOverflow() {
        assertEquals(Integer.MAX_VALUE, LongAmountBridge.saturatingInt(Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, LongAmountBridge.saturatingInt(Integer.MAX_VALUE + 1L));
        assertEquals(64, LongAmountBridge.saturatingInt(64L));
        assertEquals(0, LongAmountBridge.saturatingInt(-1L));
    }

    @Test
    void chunksAgainstTheTargetApisPerCallLimit() {
        assertEquals(64, LongAmountBridge.nextChunk(Long.MAX_VALUE, 64));
        assertEquals(7, LongAmountBridge.nextChunk(7L, 64));
        assertEquals(0, LongAmountBridge.nextChunk(7L, 0));
    }

    @Test
    void committedCountsSaturateAndRejectNegativeAdapterResults() {
        assertEquals(Long.MAX_VALUE, LongAmountBridge.committed(Long.MAX_VALUE - 2L, 4));
        assertThrows(IllegalArgumentException.class, () -> LongAmountBridge.committed(1L, -1));
    }

    @Test
    void boundedTransferChunksLongMaxWithoutNarrowingOverflow() {
        AtomicInteger simulatedCalls = new AtomicInteger();
        AtomicInteger executedCalls = new AtomicInteger();
        var progress = LongAmountBridge.transferBounded(
                Long.MAX_VALUE, Integer.MAX_VALUE, 2,
                offered -> { simulatedCalls.incrementAndGet(); return offered; },
                offered -> { executedCalls.incrementAndGet(); return offered; });

        assertEquals(2L * Integer.MAX_VALUE, progress.committed());
        assertEquals(2, progress.attemptedChunks());
        assertEquals(2, simulatedCalls.get());
        assertEquals(2, executedCalls.get());
        assertEquals(false, progress.complete());
        assertEquals(false, progress.blocked());
    }

    @Test
    void rejectedSecondChunkDoesNotAdvanceTheCommittedOffset() {
        AtomicInteger execution = new AtomicInteger();
        var progress = LongAmountBridge.transferBounded(
                (long) Integer.MAX_VALUE + 64L, Integer.MAX_VALUE, 3,
                offered -> offered,
                offered -> execution.incrementAndGet() == 1 ? offered : 0);

        assertEquals(Integer.MAX_VALUE, progress.committed());
        assertEquals(2, progress.attemptedChunks());
        assertEquals(true, progress.blocked());
        assertEquals(false, progress.complete());
    }

    @Test
    void partialOrInvalidAdapterResultsStopAtTheActualCommit() {
        var partial = LongAmountBridge.transferBounded(
                64L, 64, 2, offered -> 7, offered -> 7);
        assertEquals(7L, partial.committed());
        assertEquals(true, partial.blocked());

        assertThrows(IllegalStateException.class, () -> LongAmountBridge.transferBounded(
                64L, 64, 1, offered -> offered + 1, offered -> offered));
        assertThrows(IllegalStateException.class, () -> LongAmountBridge.transferBounded(
                64L, 64, 1, offered -> offered, offered -> -1));
    }
}
