package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.item.weapon.ModWeaponAttackProjection;

/** Shared server-combat and tooltip projection for the Spirit Sword. */
public final class SpiritSwordCombatModel {
    public static final float BASE_DAMAGE = 5.0F;

    public static Profile forStage(int stage) {
        int boundedStage = Math.max(0, Math.min(10, stage));
        if (boundedStage == 0) return new Profile(0, YuanCost.NONE, 0L, 0.0F);
        float bonus = (float) (1 << boundedStage);
        if (boundedStage <= 5) {
            return new Profile(boundedStage, YuanCost.TRUE, boundedStage, bonus);
        }
        if (boundedStage <= 9) {
            return new Profile(boundedStage, YuanCost.IMMORTAL, boundedStage, bonus);
        }
        return new Profile(10, YuanCost.NONE, 0L, bonus);
    }

    public static float projectedAttackDamage(Profile profile, boolean canPay, long temperingPoints,
                                               double temperingRate) {
        return ModWeaponAttackProjection.calculate(
                BASE_DAMAGE, profile.bonusDamage(), canPay, temperingPoints, temperingRate);
    }

    public record Profile(int stage, YuanCost cost, long costAmount, float bonusDamage) {
        public float successfulHitDamage() {
            return BASE_DAMAGE + bonusDamage;
        }
    }

    public enum YuanCost {
        NONE,
        TRUE,
        IMMORTAL
    }

    private SpiritSwordCombatModel() {}
}
