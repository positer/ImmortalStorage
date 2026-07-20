package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import com.cultivation.cultivation.progression.AscensionCosmeticEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ImmortalPillItem extends Item {
    private static final int UNBOUNDED_REWARD_TICKS = 2_000;
    public ImmortalPillItem(Properties props) {
        super(props.food(new FoodProperties.Builder().nutrition(20).saturationModifier(20f)
                .alwaysEdible().fast().build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        ItemStack r = super.finishUsingItem(stack, level, ent);
        if (!level.isClientSide && ent instanceof Player p && p.level() instanceof ServerLevel sl) {
            CultivationPlayerData d = CultivationPlayerData.get(p);
            int st = d.getStage();
            if (st >= 1 && st <= 4) {
                p.hurt(p.damageSources().magic(), 1000f);
            } else if (st == 5 && com.cultivation.cultivation.progression.CultivationProgressionRules
                    .allowsNormalAdvance(st, 6)) {
                int cap = d.getLingqiCap();
                if (cap == 0 || d.getLingqiProgress() < cap) {
                    p.hurt(p.damageSources().magic(), 1000f);
                } else {
                    p.setHealth(1.0f);
                    d.setAdvancedWeak(180 * 20);
                    d.setStage(6);
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                        com.cultivation.cultivation.advancement.CultivationCriteriaTriggers.STAGE_6.trigger(sp);
                        AscensionCosmeticEffects.playImmortalPillAscension(sp, st, d.getStage());
                    }
                }
            } else if (st >= 6) {
                long reward = immortalYuanReward(d);
                if (reward > 0L) d.depositImmortalYuan(reward);
                d.addLingqiSaturated(2, 60 * 20);
            }
        }
        return r;
    }

    static long immortalYuanReward(CultivationPlayerData data) {
        if (data == null || data.isInfiniteImmortalYuan()) return 0L;
        long cap = data.getImmortalYuanCapLong();
        if (cap > 0L) return Math.max(1L, cap / 2L);
        int interval = data.getImmortalYuanGenInterval();
        long amount = data.getImmortalYuanGenAmount();
        if (interval <= 0 || amount <= 0L) return 0L;
        long periods = UNBOUNDED_REWARD_TICKS / interval;
        return periods > Long.MAX_VALUE / amount ? Long.MAX_VALUE : periods * amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PillTooltips.add(tooltip, "immortal");
    }
}
