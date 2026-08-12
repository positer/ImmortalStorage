package com.immortalstorage.immortalstorage.effect.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class LingqiSaturationEffect extends MobEffect {
    public LingqiSaturationEffect(MobEffectCategory cat, int color) {
        super(cat, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player p) {
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);
            d.tickLingqiSaturated();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tick, int amplifier) {
        return true;
    }
}
