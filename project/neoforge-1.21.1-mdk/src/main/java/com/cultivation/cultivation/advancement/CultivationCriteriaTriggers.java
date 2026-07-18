package com.cultivation.cultivation.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Optional;

public final class CultivationCriteriaTriggers {
    public static SimpleStageTrigger STAGE_1;
    public static SimpleStageTrigger STAGE_2;
    public static SimpleStageTrigger STAGE_3;
    public static SimpleStageTrigger STAGE_4;
    public static SimpleStageTrigger STAGE_5;
    public static SimpleStageTrigger STAGE_6;
    public static SimpleStageTrigger STAGE_7;
    public static SimpleStageTrigger STAGE_8;
    public static SimpleStageTrigger STAGE_9;
    public static SimpleStageTrigger STAGE_10;
    public static SimpleStageTrigger TRIBULATION_WON;
    public static SimpleStageTrigger WHITE_DAY_THUNDER_USED;

    /**
     * Register the {@code minecraft:cultivation_*} triggers in the
     * {@link Registries#TRIGGER_TYPES} registry.  Must run from a {@link RegisterEvent}
     * listener (not a static initializer) because {@link CriteriaTriggers#register}
     * throws if the registry is already frozen  ?and the trigger registry is frozen
     * by the time Forge instantiates the {@code @Mod} class.
     */
    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.TRIGGER_TYPE, reg -> {
            STAGE_1 = registerTrigger("cultivation_stage_1");
            STAGE_2 = registerTrigger("cultivation_stage_2");
            STAGE_3 = registerTrigger("cultivation_stage_3");
            STAGE_4 = registerTrigger("cultivation_stage_4");
            STAGE_5 = registerTrigger("cultivation_stage_5");
            STAGE_6 = registerTrigger("cultivation_stage_6");
            STAGE_7 = registerTrigger("cultivation_stage_7");
            STAGE_8 = registerTrigger("cultivation_stage_8");
            STAGE_9 = registerTrigger("cultivation_stage_9");
            STAGE_10 = registerTrigger("cultivation_stage_10");
            TRIBULATION_WON = registerTrigger("cultivation_tribulation_won");
            WHITE_DAY_THUNDER_USED = registerTrigger("cultivation_white_day_thunder_used");
        });
    }

    @SuppressWarnings("unchecked")
    private static SimpleStageTrigger registerTrigger(String name) {
        return (SimpleStageTrigger) (SimpleCriterionTrigger<?>) CriteriaTriggers.register(name, new SimpleStageTrigger());
    }

    public static void fireForStage(int stage, ServerPlayer player) {
        SimpleStageTrigger trig = switch (stage) {
            case 1 -> STAGE_1;
            case 2 -> STAGE_2;
            case 3 -> STAGE_3;
            case 4 -> STAGE_4;
            case 5 -> STAGE_5;
            case 6 -> STAGE_6;
            case 7 -> STAGE_7;
            case 8 -> STAGE_8;
            case 9 -> STAGE_9;
            case 10 -> STAGE_10;
            default -> null;
        };
        if (trig != null) trig.trigger(player);
    }

    private CultivationCriteriaTriggers() {}

    public static class SimpleStageTrigger extends SimpleCriterionTrigger<SimpleStageTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() { return TriggerInstance.CODEC; }

        public void trigger(ServerPlayer player) {
            this.trigger(player, t -> true);
        }

        public static record TriggerInstance(Optional<ContextAwarePredicate> player)
                implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst ->
                    inst.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                    .forGetter(TriggerInstance::player))
                            .apply(inst, TriggerInstance::new));

            @Override
            public Optional<ContextAwarePredicate> player() { return this.player; }
        }
    }
}
