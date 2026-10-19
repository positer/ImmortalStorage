package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/** Durability-free ascended form of the Spirit Staff. */
public final class ImmortalArtifactItem extends SpiritStaffItem {
    public static final int MAX_TELEPORT_DISTANCE = 50;

    public ImmortalArtifactItem(Item.Properties properties) {
        super(properties, true);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    /**
     * The artifact has no durability, so the 1.21.1 table gate (max stack one AND a MAX_DAMAGE component) rejects
     * it although it is enchantable equipment. Declaring it here is what makes the anvil, other mods and the mod's
     * own enchantment machine treat it as an enchantable item; 26.1.2 reads DataComponents.ENCHANTABLE instead.
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, LivingEntity miner) {
        return true;
    }
}
