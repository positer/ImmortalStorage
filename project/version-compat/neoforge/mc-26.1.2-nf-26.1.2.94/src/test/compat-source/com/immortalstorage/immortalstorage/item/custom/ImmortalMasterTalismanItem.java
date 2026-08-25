package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/** Slot-flexible, unbreakable talisman that powers Spiritual Aura Guard. */
public final class ImmortalMasterTalismanItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public ImmortalMasterTalismanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.BODY;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof net.minecraft.world.entity.Mob mob)
                || !target.canUseSlot(EquipmentSlot.BODY)
                || !mob.getBodyArmorItem().isEmpty() || target.isBaby()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            mob.setBodyArmorItem(stack.copyWithCount(1));
            if (!player.isCreative()) stack.shrink(1);
        }
        return (player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
    }
}
