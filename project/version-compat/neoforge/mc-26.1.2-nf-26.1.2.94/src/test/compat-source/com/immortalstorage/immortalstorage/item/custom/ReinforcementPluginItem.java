package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.item.Item;

/** A machine reinforcement whose multiplier never changes fuel duration. */
public final class ReinforcementPluginItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    private final int multiplier;

    public ReinforcementPluginItem(Properties properties, int multiplier) {
        super(properties);
        if (multiplier <= 1) throw new IllegalArgumentException("multiplier must be greater than one");
        this.multiplier = multiplier;
    }

    public int multiplier() {
        return multiplier;
    }
}
