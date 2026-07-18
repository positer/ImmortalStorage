package com.cultivation.cultivation.api.storage.terminal;

import net.neoforged.neoforge.fluids.FluidStack;

/** A read-only aggregated fluid entry; amounts are millibuckets stored as long. */
public record TerminalFluidEntry(long entryId, FluidStack displayStack, long amountMb) {
    public TerminalFluidEntry {
        if (entryId == 0L) throw new IllegalArgumentException("entryId must be non-zero");
        if (displayStack == null || displayStack.isEmpty()) {
            throw new IllegalArgumentException("displayStack must be non-empty");
        }
        if (amountMb <= 0L) throw new IllegalArgumentException("amountMb must be positive");
        displayStack = displayStack.copyWithAmount(1);
    }

    @Override
    public FluidStack displayStack() {
        return displayStack.copyWithAmount(1);
    }

    public TerminalFluidKey key() {
        return TerminalFluidKey.of(displayStack);
    }
}
