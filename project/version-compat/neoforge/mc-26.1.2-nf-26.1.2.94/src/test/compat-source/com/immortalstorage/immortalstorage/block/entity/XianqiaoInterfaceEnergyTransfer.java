package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.IntActiveResourceTransfer;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Transactional active FE transfer between one adjacent machine and the shared long ledger. */
final class XianqiaoInterfaceEnergyTransfer {
    static long push(AtomicEnergyRefill.ResourceStore source, IEnergyStorage target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.push(source, endpoint(target));
    }

    /** Offers the complete int-sized cache to the contacted face this tick. */
    static long pushAll(AtomicEnergyRefill.ResourceStore source, IEnergyStorage target) {
        return push(source, target);
    }

    /**
     * Long-to-long form used when a machine hands its local cache to the
     * owner's Xianqiao ledger.  It keeps the same simulate, execute and
     * remainder-restore transaction as the capability form without routing a
     * long ledger through an int-sized FE facade.
     */
    static long pushAll(AtomicEnergyRefill.ResourceStore source,
                        AtomicEnergyRefill.ResourceStore target) {
        if (source == null || target == null) return 0L;
        long offered = source.extract(Long.MAX_VALUE, ResourceTransferAction.SIMULATE);
        if (offered <= 0L) return 0L;
        long accepted = bounded(target.insert(offered, ResourceTransferAction.SIMULATE), offered);
        if (accepted <= 0L) return 0L;
        long staged = bounded(source.extract(accepted, ResourceTransferAction.EXECUTE), accepted);
        if (staged <= 0L) return 0L;
        long committed = bounded(target.insert(staged, ResourceTransferAction.EXECUTE), staged);
        long remainder = staged - committed;
        if (remainder > 0L
                && source.insert(remainder, ResourceTransferAction.EXECUTE) != remainder) {
            throw new IllegalStateException("failed to restore long-cache push remainder");
        }
        return committed;
    }

    static long pull(IEnergyStorage source, AtomicEnergyRefill.ResourceStore target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.pull(endpoint(source), target);
    }

    /** Requests the complete int-sized face capacity in this tick. */
    static long pullAll(IEnergyStorage source, AtomicEnergyRefill.ResourceStore target) {
        return pull(source, target);
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

    private static long bounded(long amount, long maximum) {
        if (amount < 0L || amount > maximum) {
            throw new IllegalStateException(
                    "long resource endpoint returned " + amount + " for maximum " + maximum);
        }
        return amount;
    }

    private XianqiaoInterfaceEnergyTransfer() {}
}
