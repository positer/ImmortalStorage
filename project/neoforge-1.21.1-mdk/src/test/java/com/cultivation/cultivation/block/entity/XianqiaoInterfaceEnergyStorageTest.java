package com.cultivation.cultivation.block.entity;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ResourceTransferAction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceEnergyStorageTest {
    @Test
    void pullReceivesPushExtractsAndSimulationDoesNotMutate() {
        Store store = new Store(10L);
        AtomicReference<XianqiaoInterfaceBlockEntity.SideMode> mode =
                new AtomicReference<>(XianqiaoInterfaceBlockEntity.SideMode.PULL);
        var energy = new XianqiaoInterfaceEnergyStorage(() -> store, mode::get);

        assertTrue(energy.canReceive());
        assertFalse(energy.canExtract());
        assertEquals(5, energy.receiveEnergy(5, true));
        assertEquals(10, energy.getEnergyStored());
        assertEquals(5, energy.receiveEnergy(5, false));
        assertEquals(15, energy.getEnergyStored());

        mode.set(XianqiaoInterfaceBlockEntity.SideMode.PUSH);
        assertFalse(energy.canReceive());
        assertTrue(energy.canExtract());
        assertEquals(7, energy.extractEnergy(7, true));
        assertEquals(15, energy.getEnergyStored());
        assertEquals(7, energy.extractEnergy(7, false));
        assertEquals(8, energy.getEnergyStored());
    }

    @Test
    void disabledOrMissingOwnerFailsClosedAndLongAmountsSaturate() {
        Store store = new Store(Long.MAX_VALUE);
        AtomicReference<AtomicEnergyRefill.ResourceStore> live = new AtomicReference<>(store);
        AtomicReference<XianqiaoInterfaceBlockEntity.SideMode> mode =
                new AtomicReference<>(XianqiaoInterfaceBlockEntity.SideMode.DISABLED);
        var energy = new XianqiaoInterfaceEnergyStorage(live::get, mode::get);

        assertEquals(Integer.MAX_VALUE, energy.getEnergyStored());
        assertEquals(Integer.MAX_VALUE, energy.getMaxEnergyStored());
        assertEquals(0, energy.receiveEnergy(1, false));
        assertEquals(0, energy.extractEnergy(1, false));

        live.set(null);
        mode.set(XianqiaoInterfaceBlockEntity.SideMode.PULL);
        assertFalse(energy.canReceive());
        assertEquals(0, energy.getEnergyStored());
    }

    private static final class Store implements AtomicEnergyRefill.ResourceStore {
        private long amount;

        private Store(long amount) {
            this.amount = amount;
        }

        @Override
        public long amount() {
            return amount;
        }

        @Override
        public long extract(long requested, ResourceTransferAction action) {
            long accepted = Math.min(Math.max(0L, requested), amount);
            if (action.executes()) amount -= accepted;
            return accepted;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            long accepted = Math.min(Math.max(0L, offered), Long.MAX_VALUE - amount);
            if (action.executes()) amount += accepted;
            return accepted;
        }
    }
}
