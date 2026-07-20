package com.immortalstorage.immortalstorage.compat.ifsouls;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.immortalstorage.core.resource.AtomicEnergyRefill;

import java.util.Objects;
import java.util.function.Supplier;

/** Official int soul capability facade over the owner-bound long resource ledger. */
public final class XianqiaoSoulHandler implements ISoulHandler {
    private final SoulTransferPort port;

    public XianqiaoSoulHandler(Supplier<AtomicEnergyRefill.ResourceStore> storage) {
        Objects.requireNonNull(storage, "storage");
        this.port = new SoulTransferPort(storage);
    }

    @Override
    public int getSoulTanks() {
        return port.tankCount();
    }

    @Override
    public int getSoulInTank(int tank) {
        return port.stored(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return port.capacity(tank);
    }

    @Override
    public int fill(int amount, Action action) {
        return port.fill(amount, action.execute());
    }

    @Override
    public int drain(int amount, Action action) {
        return port.drain(amount, action.execute());
    }

}
