package com.immortalstorage.core.resource;

import java.util.Objects;

/** Two-phase transfer between one long ledger and one long-valued mod endpoint. */
public final class LongActiveResourceTransfer {
    public interface Endpoint {
        boolean canInsert();
        boolean canExtract();
        long insert(long offered, ResourceTransferAction action);
        long extract(long requested, ResourceTransferAction action);
    }

    public static long push(AtomicEnergyRefill.ResourceStore source, Endpoint target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (!target.canInsert()) return 0L;
        long offered = source.extract(Long.MAX_VALUE, ResourceTransferAction.SIMULATE);
        if (offered <= 0L) return 0L;
        long accepted = bounded(
                target.insert(offered, ResourceTransferAction.SIMULATE), offered);
        if (accepted <= 0L) return 0L;
        long staged = bounded(
                source.extract(accepted, ResourceTransferAction.EXECUTE), accepted);
        if (staged <= 0L) return 0L;
        long committed = bounded(
                target.insert(staged, ResourceTransferAction.EXECUTE), staged);
        long remainder = staged - committed;
        if (remainder > 0L
                && source.insert(remainder, ResourceTransferAction.EXECUTE) != remainder) {
            throw new IllegalStateException("failed to restore long-capability push remainder");
        }
        return committed;
    }

    public static long pull(Endpoint source, AtomicEnergyRefill.ResourceStore target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (!source.canExtract()) return 0L;
        long offered = source.extract(Long.MAX_VALUE, ResourceTransferAction.SIMULATE);
        if (offered <= 0L) return 0L;
        long requested = bounded(
                target.insert(offered, ResourceTransferAction.SIMULATE), offered);
        if (requested <= 0L) return 0L;
        long staged = bounded(
                source.extract(requested, ResourceTransferAction.EXECUTE), requested);
        if (staged <= 0L) return 0L;
        long committed = bounded(
                target.insert(staged, ResourceTransferAction.EXECUTE), staged);
        long remainder = staged - committed;
        if (remainder > 0L
                && source.insert(remainder, ResourceTransferAction.EXECUTE) != remainder) {
            throw new IllegalStateException("failed to restore long-capability pull remainder");
        }
        return committed;
    }

    private static long bounded(long amount, long maximum) {
        if (amount < 0L || amount > maximum) {
            throw new IllegalStateException(
                    "long capability returned " + amount + " for maximum " + maximum);
        }
        return amount;
    }

    private LongActiveResourceTransfer() {}
}
