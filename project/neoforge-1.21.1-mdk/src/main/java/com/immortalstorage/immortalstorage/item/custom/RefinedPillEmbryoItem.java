package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RefinedPillEmbryoItem extends Item {
    public RefinedPillEmbryoItem(Properties props) { super(props); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        ItemStack r = super.finishUsingItem(stack, level, ent);
        if (!level.isClientSide && ent instanceof Player p && p.level() instanceof ServerLevel sl) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            int cap = d.getLingqiCap();
            if (cap > 0) d.addLingqiProgress(80);
            d.addLingqiSaturated(2, 15 * 20);
            if (d.getLingqiSaturatedLayers() > 0) {
                p.hurt(p.damageSources().magic(), 5.0f);
            }
        }
        return r;
    }
}
