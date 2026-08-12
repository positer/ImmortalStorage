package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CrudePillItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public CrudePillItem(Properties props) { super(props); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        ItemStack r = super.finishUsingItem(stack, level, ent);
        if (!level.isClientSide() && ent instanceof Player p && p.level() instanceof ServerLevel sl) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            d.markConsumedSpiritPill();
            int cap = d.getLingqiCap();
            if (cap > 0) d.addLingqiProgress(10);
            int layersBefore = d.getLingqiSaturatedLayers();
            d.addLingqiSaturated(1, 5 * 20);
            if (layersBefore > 0) {
                p.hurt(p.damageSources().magic(), 1.0f);
            }
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PillTooltips.add(tooltip, "crude");
    }
}
