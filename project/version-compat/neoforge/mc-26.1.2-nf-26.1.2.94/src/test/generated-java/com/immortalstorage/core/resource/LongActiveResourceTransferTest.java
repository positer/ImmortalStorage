package com.immortalstorage.core.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LongActiveResourceTransferTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void movesAmountsBeyondIntRangeInBothDirections() {
        Store ledger = new Store(5_000_000_000L, Long.MAX_VALUE);
        Endpoint external = new Endpoint(0L, Long.MAX_VALUE);
        assertEquals(5_000_000_000L, LongActiveResourceTransfer.push(ledger, external));
        assertEquals(0L, ledger.amount());
        assertEquals(5_000_000_000L, external.amount);

        assertEquals(5_000_000_000L, LongActiveResourceTransfer.pull(external, ledger));
        assertEquals(5_000_000_000L, ledger.amount());
        assertEquals(0L, external.amount);
    }

    @Test
    void restoresExecuteShortfallsOnPushAndPull() {
        Store source = new Store(1_000L, Long.MAX_VALUE);
        Endpoint pushTarget = new Endpoint(0L, 1_000L);
        pushTarget.executeInsertLimit = 600L;
        assertEquals(600L, LongActiveResourceTransfer.push(source, pushTarget));
        assertEquals(400L, source.amount());

        Endpoint pullSource = new Endpoint(1_000L, 1_000L);
        Store pullTarget = new Store(90L, 150L);
        pullTarget.executeInsertLimit = 40L;
        assertEquals(40L, LongActiveResourceTransfer.pull(pullSource, pullTarget));
        assertEquals(130L, pullTarget.amount());
        assertEquals(960L, pullSource.amount);
    }

    private static final class Store implements AtomicEnergyRefill.ResourceStore {
        private long amount;
        private final long capacity;
        private long executeInsertLimit = Long.MAX_VALUE;
        private Store(long amount, long capacity) { this.amount = amount; this.capacity = capacity; }
        @Override public long amount() { return amount; }
        @Override public long extract(long requested, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, requested), amount);
            if (action.executes()) amount -= moved;
            return moved;
        }
        @Override public long insert(long offered, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, offered), capacity - amount);
            if (action.executes()) moved = Math.min(moved, executeInsertLimit);
            if (action.executes()) amount += moved;
            return moved;
        }
    }

    private static final class Endpoint implements LongActiveResourceTransfer.Endpoint {
        private long amount;
        private final long capacity;
        private long executeInsertLimit = Long.MAX_VALUE;
        private Endpoint(long amount, long capacity) { this.amount = amount; this.capacity = capacity; }
        @Override public boolean canInsert() { return true; }
        @Override public boolean canExtract() { return true; }
        @Override public long insert(long offered, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, offered), capacity - amount);
            if (action.executes()) moved = Math.min(moved, executeInsertLimit);
            if (action.executes()) amount += moved;
            return moved;
        }
        @Override public long extract(long requested, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, requested), amount);
            if (action.executes()) amount -= moved;
            return moved;
        }
    }
}
