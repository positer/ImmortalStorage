package com.cultivation.cultivation.compat.ifsouls;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.cultivation.core.resource.AtomicEnergyRefill;

import java.util.Objects;
import java.util.function.Supplier;

/** Official int soul capability facade over the owner-bound long resource ledger. */
public final class XianqiaoSoulHandler implements ISoulHandler {
    public enum Mode { PULL, PUSH, DISABLED }

    private final SoulTransferPort port;

    public XianqiaoSoulHandler(
            Supplier<AtomicEnergyRefill.ResourceStore> storage,
            Supplier<Mode> mode) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(mode, "mode");
        this.port = new SoulTransferPort(storage, () -> transferMode(mode.get()));
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

    private static SoulTransferPort.Mode transferMode(Mode mode) {
        return switch (mode) {
            case PULL -> SoulTransferPort.Mode.PULL;
            case PUSH -> SoulTransferPort.Mode.PUSH;
            case DISABLED -> SoulTransferPort.Mode.DISABLED;
        };
    }
}
