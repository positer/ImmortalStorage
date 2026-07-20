package com.immortalstorage.core.resource;

import java.util.Objects;

/**
 * Loader-neutral two-phase transfer between a long authoritative ledger and
 * an int-valued mod capability. Version adapters provide only the capability
 * calls; reservation, bounds checks and compensation stay shared.
 */
public final class IntActiveResourceTransfer {
    public interface Endpoint {
        boolean canInsert();
        boolean canExtract();
        int insert(int offered, ResourceTransferAction action);
        int extract(int requested, ResourceTransferAction action);
    }

    public static long push(AtomicEnergyRefill.ResourceStore source, Endpoint target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (!target.canInsert()) return 0L;
        int offered = saturatingInt(source.extract(
                Integer.MAX_VALUE, ResourceTransferAction.SIMULATE));
        if (offered <= 0) return 0L;
        int accepted = bounded(target.insert(offered, ResourceTransferAction.SIMULATE), offered);
        if (accepted <= 0) return 0L;

        int staged = saturatingInt(source.extract(accepted, ResourceTransferAction.EXECUTE));
        if (staged <= 0) return 0L;
        int committed = bounded(target.insert(staged, ResourceTransferAction.EXECUTE), staged);
        long remainder = staged - committed;
        if (remainder > 0L) {
            long restored = source.insert(remainder, ResourceTransferAction.EXECUTE);
            if (restored != remainder) {
                throw new IllegalStateException("failed to restore int-capability push remainder");
            }
        }
        return committed;
    }

    public static long pull(Endpoint source, AtomicEnergyRefill.ResourceStore target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (!source.canExtract()) return 0L;
        int offered = bounded(source.extract(
                Integer.MAX_VALUE, ResourceTransferAction.SIMULATE), Integer.MAX_VALUE);
        if (offered <= 0) return 0L;
        int requested = saturatingInt(Math.min(
                offered, target.insert(offered, ResourceTransferAction.SIMULATE)));
        if (requested <= 0) return 0L;

        int staged = bounded(source.extract(requested, ResourceTransferAction.EXECUTE), requested);
        if (staged <= 0) return 0L;
        long committed = target.insert(staged, ResourceTransferAction.EXECUTE);
        if (committed < 0L || committed > staged) {
            throw new IllegalStateException("invalid int-capability pull commit amount: " + committed);
        }
        int remainder = staged - (int) committed;
        if (remainder > 0) {
            int restored = bounded(source.insert(
                    remainder, ResourceTransferAction.EXECUTE), remainder);
            if (restored != remainder) {
                throw new IllegalStateException("failed to restore int-capability pull remainder");
            }
        }
        return committed;
    }

    private static int saturatingInt(long amount) {
        if (amount <= 0L) return 0;
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    private static int bounded(int amount, int maximum) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException(
                    "int capability returned " + amount + " for maximum " + maximum);
        }
        return amount;
    }

    private IntActiveResourceTransfer() {}
}
