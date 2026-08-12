package com.immortalstorage.immortalstorage.menu.custom;

import net.minecraft.world.item.ItemStack;

/** Side-aware filter surface shared by the entangled ruin menus. */
public interface SideFilterMenu {
    void setFilter(int side, int slot, ItemStack stack);
    void toggleFilterMode(int side, int mode);
}
