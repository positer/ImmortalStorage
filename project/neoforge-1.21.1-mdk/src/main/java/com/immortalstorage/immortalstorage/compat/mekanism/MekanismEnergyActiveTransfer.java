package com.immortalstorage.immortalstorage.compat.mekanism;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.LongActiveResourceTransfer;
import com.immortalstorage.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;

/** Long-native active transfer fallback for machines that expose strict energy but no NeoForge FE. */
final class MekanismEnergyActiveTransfer {
    static long push(AtomicEnergyRefill.ResourceStore source, IStrictEnergyHandler target) {
        return source == null || target == null ? 0L
                : LongActiveResourceTransfer.push(source, endpoint(target));
    }

    static long pull(IStrictEnergyHandler source, AtomicEnergyRefill.ResourceStore target) {
        return source == null || target == null ? 0L
                : LongActiveResourceTransfer.pull(endpoint(source), target);
    }

    private static LongActiveResourceTransfer.Endpoint endpoint(IStrictEnergyHandler energy) {
        return new LongActiveResourceTransfer.Endpoint() {
            @Override public boolean canInsert() { return energy.getEnergyContainerCount() > 0; }
            @Override public boolean canExtract() { return energy.getEnergyContainerCount() > 0; }
            @Override public long insert(long offered, ResourceTransferAction action) {
                long remainder = energy.insertEnergy(offered, mekanismAction(action));
                if (remainder < 0L || remainder > offered) {
                    throw new IllegalStateException("invalid Mekanism energy insert remainder");
                }
                return offered - remainder;
            }
            @Override public long extract(long requested, ResourceTransferAction action) {
                long extracted = energy.extractEnergy(requested, mekanismAction(action));
                if (extracted < 0L || extracted > requested) {
                    throw new IllegalStateException("invalid Mekanism energy extraction");
                }
                return extracted;
            }
        };
    }

    private static Action mekanismAction(ResourceTransferAction action) {
        return action.executes() ? Action.EXECUTE : Action.SIMULATE;
    }

    private MekanismEnergyActiveTransfer() {}
}
