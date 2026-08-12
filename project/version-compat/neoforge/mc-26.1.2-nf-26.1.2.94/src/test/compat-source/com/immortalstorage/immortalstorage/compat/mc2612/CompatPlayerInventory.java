package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Explicit 26.1 inventory/equipment accessors. */
public final class CompatPlayerInventory {
    private CompatPlayerInventory() {
    }

    public static List<ItemStack> items(Player player) {
        return player.getInventory().getNonEquipmentItems();
    }

    public static List<ItemStack> armor(Player player) {
        return List.of(player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET));
    }

    public static List<ItemStack> slot(Player player, EquipmentSlot slot) {
        return List.of(player.getItemBySlot(slot));
    }
}
