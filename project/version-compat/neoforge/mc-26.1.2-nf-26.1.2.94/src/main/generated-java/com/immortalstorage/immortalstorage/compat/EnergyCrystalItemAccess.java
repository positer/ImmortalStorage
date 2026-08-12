package com.immortalstorage.immortalstorage.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Loader seam for rechargeable item lookup. NeoForge 26.1 changes the item
 * capability context to {@code ItemAccess}; the compatibility generator maps
 * this one method without leaking that API change into machine logic.
 */
public final class EnergyCrystalItemAccess {
    public static @Nullable IEnergyStorage energy(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? null : com.immortalstorage.immortalstorage.compat.mc2612.CompatItemCapabilities.energy(stack);
    }

    private EnergyCrystalItemAccess() {}
}
