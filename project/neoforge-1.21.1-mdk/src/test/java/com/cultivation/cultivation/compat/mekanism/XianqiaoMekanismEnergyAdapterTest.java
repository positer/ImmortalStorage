package com.cultivation.cultivation.compat.mekanism;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class XianqiaoMekanismEnergyAdapterTest {
    @Test
    void strictEnergyUsesRemainderOnInsertAndAmountOnExtract() {
        Store store = new Store(10L);
        AtomicReference<XianqiaoMekanismEnergyAdapter.Mode> mode =
                new AtomicReference<>(XianqiaoMekanismEnergyAdapter.Mode.PULL);
        var handler = new XianqiaoMekanismEnergyAdapter(() -> store, mode::get);

        assertEquals(1, handler.getEnergyContainerCount());
        assertEquals(0L, handler.insertEnergy(0, 5L, Action.SIMULATE));
        assertEquals(10L, handler.getEnergy(0));
        assertEquals(0L, handler.insertEnergy(0, 5L, Action.EXECUTE));
        assertEquals(15L, handler.getEnergy(0));
        assertEquals(0L, handler.extractEnergy(0, 4L, Action.EXECUTE),
                "PULL must not expose extraction");

        mode.set(XianqiaoMekanismEnergyAdapter.Mode.PUSH);
        assertEquals(5L, handler.insertEnergy(0, 5L, Action.EXECUTE),
                "PUSH returns the entire unaccepted insertion remainder");
        assertEquals(7L, handler.extractEnergy(0, 7L, Action.SIMULATE));
        assertEquals(15L, handler.getEnergy(0));
        assertEquals(7L, handler.extractEnergy(0, 7L, Action.EXECUTE));
        assertEquals(8L, handler.getEnergy(0));
    }

    @Test
    void missingDisabledAndDirectSetFailClosed() {
        AtomicReference<AtomicEnergyRefill.ResourceStore> live = new AtomicReference<>(null);
        var handler = new XianqiaoMekanismEnergyAdapter(
                live::get, () -> XianqiaoMekanismEnergyAdapter.Mode.DISABLED);

        assertEquals(0L, handler.getEnergy(0));
        assertEquals(9L, handler.insertEnergy(0, 9L, Action.EXECUTE));
        assertEquals(0L, handler.extractEnergy(0, 9L, Action.EXECUTE));
        assertThrows(UnsupportedOperationException.class, () -> handler.setEnergy(0, 1L));
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
