package com.cultivation.cultivation.menu.custom;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Vanilla-equivalent armor slot used by both portable terminals. */
final class TerminalArmorSlot extends Slot {
    private final LivingEntity owner;
    private final EquipmentSlot equipmentSlot;
    private final ResourceLocation emptyIcon;

    TerminalArmorSlot(Container container, LivingEntity owner, EquipmentSlot equipmentSlot,
                      int inventoryIndex, int x, int y, ResourceLocation emptyIcon) {
        super(container, inventoryIndex, x, y);
        this.owner = owner;
        this.equipmentSlot = equipmentSlot;
        this.emptyIcon = emptyIcon;
    }

    @Override public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        owner.onEquipItem(equipmentSlot, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(equipmentSlot, owner); }

    @Override public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        return stack.isEmpty() || player.isCreative()
                || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    @Override public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
    }
}
