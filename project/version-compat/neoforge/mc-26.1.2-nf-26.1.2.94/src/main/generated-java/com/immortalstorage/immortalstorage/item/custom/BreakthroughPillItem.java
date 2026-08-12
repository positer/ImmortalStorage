package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BreakthroughPillItem extends com.immortalstorage.immortalstorage.compat.mc2612.CompatItem {
    public BreakthroughPillItem(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        ItemStack r = super.finishUsingItem(stack, level, ent);
        if (!level.isClientSide() && ent instanceof Player p && p.level() instanceof ServerLevel sl) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers
                        .BREAKTHROUGH_PILL_USED.trigger(sp);
            }
            int st = d.getStage();
            int layers = d.getLingqiSaturatedLayers();
            if (layers > 0) {
                p.hurt(p.damageSources().magic(), 20.0f * layers);
            }
            if (st >= 1 && st <= 4
                    && com.immortalstorage.immortalstorage.progression.ImmortalStorageProgressionRules
                    .allowsNormalAdvance(st, st + 1)) {
                d.addLingqiSaturated(5, 120 * 20);
                d.setAdvancedWeak(120 * 20);
                d.setStage(st + 1);
                if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.fireForStage(st + 1, sp);
                }
            } else if (st == 5) {
                int cap = d.getLingqiCap();
                if (cap > 0) d.setLingqiProgress(cap);
                d.addLingqiSaturated(1, 60 * 20);
            } else if (st >= 6 && st <= 10) {
                d.depositImmortalYuan(d.getImmortalYuanCap() > 0 ? d.getImmortalYuanCap() / 2 : 0);
                d.addLingqiSaturated(1, 60 * 20);
            }
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PillTooltips.add(tooltip, "breakthrough");
    }
}
