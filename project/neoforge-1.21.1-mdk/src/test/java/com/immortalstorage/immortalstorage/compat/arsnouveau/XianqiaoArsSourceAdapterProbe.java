package com.immortalstorage.immortalstorage.compat.arsnouveau;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceTransferAction;

/** Standalone official-API probe that does not launch Ars Nouveau as a mod. */
public final class XianqiaoArsSourceAdapterProbe {
    public static void main(String[] args) {
        simulationDoesNotMutate();
        directInteractionIgnoresSidedModes();
        longLedgerUsesAnIntWindow();
        System.out.println("Verified Xianqiao Ars Source adapter against the published API JAR.");
    }

    private static void simulationDoesNotMutate() {
        Store store = new Store(1_000L);
        XianqiaoArsSourceAdapter adapter = new XianqiaoArsSourceAdapter(() -> store);
        require(adapter.addSource(250, true) == 250, "input simulation amount");
        require(store.amount() == 1_000L, "input simulation mutated storage");
        require(adapter.removeSource(250, true) == 250, "output simulation amount");
        require(store.amount() == 1_000L, "output simulation mutated storage");
        require(adapter.addSource(250, false) == 250, "input execution amount");
        require(store.amount() == 1_250L, "input execution");
        require(adapter.removeSource(200, false) == 200, "output execution amount");
        require(store.amount() == 1_050L, "output execution");
    }

    private static void directInteractionIgnoresSidedModes() {
        Store store = new Store(640L);
        XianqiaoArsSourceAdapter adapter = new XianqiaoArsSourceAdapter(() -> store);
        require(adapter.canAcceptSource(), "direct source input");
        require(adapter.canProvideSource(), "direct source output");
        require(adapter.getSource() == 640, "direct source visibility");
        require(adapter.addSource(64, false) == 64, "direct source insertion");
        require(adapter.removeSource(64, false) == 64, "direct source extraction");
    }

    private static void longLedgerUsesAnIntWindow() {
        Store store = new Store(Long.MAX_VALUE - 1L);
        XianqiaoArsSourceAdapter adapter = new XianqiaoArsSourceAdapter(() -> store);
        require(adapter.getSource() == Integer.MAX_VALUE, "int display saturation");
        require(adapter.addSource(10, false) == 1, "long-capacity insertion");
        require(store.amount() == Long.MAX_VALUE, "long capacity");
        require(!adapter.canAcceptSource(), "full ledger acceptance");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
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
            long extracted = Math.min(Math.max(0L, requested), amount);
            if (action.executes()) amount -= extracted;
            return extracted;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            long accepted = Math.min(Math.max(0L, offered), Long.MAX_VALUE - amount);
            if (action.executes()) amount += accepted;
            return accepted;
        }
    }

    private XianqiaoArsSourceAdapterProbe() {}
}
