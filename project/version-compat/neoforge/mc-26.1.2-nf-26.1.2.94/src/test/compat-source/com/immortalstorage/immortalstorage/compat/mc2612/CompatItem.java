package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Adapts the 26.1 official item callbacks to the canonical callback shape.
 * The overloads are deliberate compatibility entry points, not API probes:
 * the target callbacks call them through normal virtual dispatch.
 */
public class CompatItem extends Item {
    public CompatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        int legacySlot = slot == null ? -1 : slot.getIndex();
        boolean selected = entity instanceof Player player
                && slot == EquipmentSlot.MAINHAND
                && player.getMainHandItem() == stack;
        inventoryTick(stack, level, entity, legacySlot, selected);
    }

    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level,
                               Entity entity, int slot, boolean selected) {
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        onCraftedBy(stack, player.level(), player);
    }

    public void onCraftedBy(ItemStack stack, net.minecraft.world.level.Level level, Player player) {
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> adder,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        List<Component> legacyLines = new java.util.ArrayList<>();
        appendHoverText(stack, context, legacyLines, flag);
        legacyLines.forEach(adder);
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return false;
    }

    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
