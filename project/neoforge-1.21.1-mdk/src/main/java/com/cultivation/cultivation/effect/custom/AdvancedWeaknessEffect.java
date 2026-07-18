package com.cultivation.cultivation.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Visible marker for the period in which Cultivation's stage buffs are suppressed.
 * The player attachment owns the persistent timer; this effect is its normal
 * vanilla HUD/inventory projection and must not decrement that timer itself.
 */
public class AdvancedWeaknessEffect extends MobEffect {
    public AdvancedWeaknessEffect(MobEffectCategory cat, int color) {
        super(cat, color);
    }

}
