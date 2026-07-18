package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** "    ? loot-only item: directly advances to stage 6 (   ) without side effects. */
public class AscensionDanItem extends Item {
    public AscensionDanItem(Properties props) {
        super(props);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity ent) {
        ItemStack r = super.finishUsingItem(stack, level, ent);
        if (!level.isClientSide && ent instanceof Player p) {
            CultivationPlayerData d = CultivationPlayerData.get(p);
            if (d.getStage() < 6
                    && com.cultivation.cultivation.progression.CultivationProgressionRules
                    .allowsNormalTargetStage(6)) {
                d.setStage(6);
                if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.cultivation.cultivation.advancement.CultivationCriteriaTriggers.STAGE_6.trigger(sp);
                }
            }
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PillTooltips.add(tooltip, "ascension");
    }
}
