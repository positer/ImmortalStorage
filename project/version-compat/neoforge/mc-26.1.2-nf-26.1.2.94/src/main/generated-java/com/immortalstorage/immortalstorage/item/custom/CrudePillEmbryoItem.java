package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CrudePillEmbryoItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public CrudePillEmbryoItem(Properties props) { super(props); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        if (ent instanceof Player p && !level.isClientSide()) {
            // FoodProperties already restores hunger/sat on consume; no lingqi yet.
        }
        return super.finishUsingItem(stack, level, ent);
    }
}
