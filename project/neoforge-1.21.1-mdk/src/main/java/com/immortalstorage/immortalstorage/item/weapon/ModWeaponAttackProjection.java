package com.immortalstorage.immortalstorage.item.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Shared projection of mod weapon growth into standard readable attack attributes. */
public final class ModWeaponAttackProjection {
    public static float calculate(float baseDamage, float paidDamage, boolean canPay,
                                  long growthPoints, double growthRate) {
        double ordinaryDamage = Math.max(0.0F, baseDamage) + (canPay ? Math.max(0.0F, paidDamage) : 0.0F);
        double growthDamage = ordinaryDamage * Math.min(Math.max(0L, growthPoints), 1_000_000_000L)
                * Math.max(0.0D, growthRate);
        return (float) Math.min(Float.MAX_VALUE, ordinaryDamage + growthDamage);
    }

    public static void applySword(ItemStack stack, Tier tier, float projectedDamage, float attackSpeed) {
        float itemBonus = projectedDamage - 1.0F - tier.getAttackDamageBonus();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                SwordItem.createAttributes(tier, itemBonus, attackSpeed));
    }

    private ModWeaponAttackProjection() {
    }
}
