package com.immortalstorage.immortalstorage.compat.ifsouls;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.IntActiveResourceTransfer;
import com.immortalstorage.core.resource.ResourceTransferAction;

/** Industrial Foregoing Souls adapter for the shared active int transfer core. */
final class SoulActiveTransfer {
    static long push(AtomicEnergyRefill.ResourceStore source, ISoulHandler target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.push(source, endpoint(target));
    }

    static long pull(ISoulHandler source, AtomicEnergyRefill.ResourceStore target) {
        if (source == null || target == null) return 0L;
        return IntActiveResourceTransfer.pull(endpoint(source), target);
    }

    private static IntActiveResourceTransfer.Endpoint endpoint(ISoulHandler souls) {
        return new IntActiveResourceTransfer.Endpoint() {
            @Override public boolean canInsert() { return souls.getSoulTanks() > 0; }
            @Override public boolean canExtract() { return souls.getSoulTanks() > 0; }
            @Override public int insert(int amount, ResourceTransferAction action) {
                return souls.fill(amount, soulAction(action));
            }
            @Override public int extract(int amount, ResourceTransferAction action) {
                return souls.drain(amount, soulAction(action));
            }
        };
    }

    private static ISoulHandler.Action soulAction(ResourceTransferAction action) {
        return action.executes() ? ISoulHandler.Action.EXECUTE : ISoulHandler.Action.SIMULATE;
    }

    private SoulActiveTransfer() {}
}
