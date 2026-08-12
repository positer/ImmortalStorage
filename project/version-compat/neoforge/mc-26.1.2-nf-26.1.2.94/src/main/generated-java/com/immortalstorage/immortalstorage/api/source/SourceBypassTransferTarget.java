package com.immortalstorage.immortalstorage.api.source;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Optional-mod transfer endpoint used only by an explicitly configured source
 * {@code BYPASS_PUSH} face.
 *
 * <p>The amount is deliberately a {@code long}; adapters translate to their
 * official storage API without routing a high-volume transfer through an
 * ordinary one-stack NeoForge capability. Implementations must return the
 * exact accepted amount for both simulation and execution.</p>
 */
public interface SourceBypassTransferTarget {
    default boolean supportsItems() {
        return false;
    }

    default boolean supportsFluids() {
        return false;
    }

    default long insertItem(ItemStack prototype, long amount, boolean simulate) {
        return 0L;
    }

    default long insertFluid(FluidStack prototype, long amount, boolean simulate) {
        return 0L;
    }
}
