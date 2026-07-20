package com.cultivation.cultivation.block.entity;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.IntActiveResourceTransfer;
import com.cultivation.core.resource.ResourceTransferAction;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Transactional active FE transfer between one adjacent machine and the shared long ledger. */
final class XianqiaoInterfaceEnergyTransfer {
    static long push(AtomicEnergyRefill.ResourceStore source, IEnergyStorage target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.push(source, endpoint(target));
    }

    static long pull(IEnergyStorage source, AtomicEnergyRefill.ResourceStore target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.pull(endpoint(source), target);
    }

    private static IntActiveResourceTransfer.Endpoint endpoint(IEnergyStorage energy) {
        return new IntActiveResourceTransfer.Endpoint() {
            @Override public boolean canInsert() { return energy.canReceive(); }
            @Override public boolean canExtract() { return energy.canExtract(); }
            @Override public int insert(int amount, ResourceTransferAction action) {
                return energy.receiveEnergy(amount, !action.executes());
            }
            @Override public int extract(int amount, ResourceTransferAction action) {
                return energy.extractEnergy(amount, !action.executes());
            }
        };
    }

    private XianqiaoInterfaceEnergyTransfer() {}
}
