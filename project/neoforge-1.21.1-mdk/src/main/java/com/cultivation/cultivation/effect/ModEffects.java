package com.cultivation.cultivation.effect;

import java.util.function.Supplier;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CultivationMod.MODID);

    public static final Supplier<MobEffect> ADVANCED_WEAKNESS =
            EFFECTS.register("advanced_weakness", () -> new com.cultivation.cultivation.effect.custom.AdvancedWeaknessEffect(
                    net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x55ccff));

    public static final Supplier<MobEffect> LINGQI_SATURATION =
            EFFECTS.register("lingqi_saturation", () -> new com.cultivation.cultivation.effect.custom.LingqiSaturationEffect(
                    net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x88ffcc));

    private ModEffects() {}
}
