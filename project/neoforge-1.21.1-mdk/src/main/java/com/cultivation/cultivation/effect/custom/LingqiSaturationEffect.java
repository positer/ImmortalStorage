package com.cultivation.cultivation.effect.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class LingqiSaturationEffect extends MobEffect {
    public LingqiSaturationEffect(MobEffectCategory cat, int color) {
        super(cat, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player p) {
            CultivationPlayerData d = CultivationPlayerData.get(p);
            d.tickLingqiSaturated();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tick, int amplifier) {
        return true;
    }
}
