package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/** Official 26.1 item-capability context bridge for rechargeable stacks. */
public final class CompatItemCapabilities {
    private CompatItemCapabilities() {
    }

    public static IEnergyStorage energy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        EnergyHandler handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
        return CompatTransfer.energyHandler(handler);
    }
}
