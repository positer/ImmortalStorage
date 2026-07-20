package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XianqiaoInterfaceEnergyTransferTest {
    @Test
    void activePushMovesEnergyAndRestoresExecuteShortfall() {
        Store ledger = new Store(1_000L, Long.MAX_VALUE);
        Energy target = new Energy(100, 1_000, true, false);
        target.executeReceiveLimit = 60;

        assertEquals(60L, XianqiaoInterfaceEnergyTransfer.push(ledger, target));
        assertEquals(940L, ledger.amount());
        assertEquals(160, target.energy);
    }

    @Test
    void activePullMovesEnergyAndRestoresLedgerCommitShortfall() {
        Store ledger = new Store(90L, 150L);
        ledger.executeInsertLimit = 40L;
        Energy source = new Energy(500, 1_000, true, true);

        assertEquals(40L, XianqiaoInterfaceEnergyTransfer.pull(source, ledger));
        assertEquals(130L, ledger.amount());
        assertEquals(460, source.energy);
    }

    @Test
    void simulationAndCapabilityFlagsPreventMutation() {
        Store ledger = new Store(100L, Long.MAX_VALUE);
        Energy closed = new Energy(50, 100, false, false);
        assertEquals(0L, XianqiaoInterfaceEnergyTransfer.push(ledger, closed));
        assertEquals(0L, XianqiaoInterfaceEnergyTransfer.pull(closed, ledger));
        assertEquals(100L, ledger.amount());
        assertEquals(50, closed.energy);
    }

    private static final class Store implements AtomicEnergyRefill.ResourceStore {
        private long amount;
        private final long capacity;
        private long executeInsertLimit = Long.MAX_VALUE;

        private Store(long amount, long capacity) {
            this.amount = amount;
            this.capacity = capacity;
        }

        @Override public long amount() { return amount; }

        @Override
        public long extract(long requested, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, requested), amount);
            if (action.executes()) amount -= moved;
            return moved;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            long moved = Math.min(Math.max(0L, offered), Math.max(0L, capacity - amount));
            if (action.executes()) moved = Math.min(moved, executeInsertLimit);
            if (action.executes()) amount += moved;
            return moved;
        }
    }

    private static final class Energy implements IEnergyStorage {
        private int energy;
        private final int capacity;
        private final boolean receive;
        private final boolean extract;
        private int executeReceiveLimit = Integer.MAX_VALUE;

        private Energy(int energy, int capacity, boolean receive, boolean extract) {
            this.energy = energy;
            this.capacity = capacity;
            this.receive = receive;
            this.extract = extract;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (!receive || amount <= 0) return 0;
            int moved = Math.min(amount, capacity - energy);
            if (!simulate) moved = Math.min(moved, executeReceiveLimit);
            if (!simulate) energy += moved;
            return moved;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (!extract || amount <= 0) return 0;
            int moved = Math.min(amount, energy);
            if (!simulate) energy -= moved;
            return moved;
        }

        @Override public int getEnergyStored() { return energy; }
        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract() { return extract; }
        @Override public boolean canReceive() { return receive; }
    }
}
