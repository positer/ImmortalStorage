package com.immortalstorage.immortalstorage.item.weapon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.ToolMaterial;

/** Shared projection of mod weapon growth into standard readable attack attributes. */
public final class ModWeaponAttackProjection {
    public static float calculate(float baseDamage, float paidDamage, boolean canPay,
                                  long growthPoints, double growthRate) {
        double ordinaryDamage = Math.max(0.0F, baseDamage) + (canPay ? Math.max(0.0F, paidDamage) : 0.0F);
        double growthDamage = ordinaryDamage * Math.min(Math.max(0L, growthPoints), 1_000_000_000L)
                * Math.max(0.0D, growthRate);
        return (float) Math.min(Float.MAX_VALUE, ordinaryDamage + growthDamage);
    }

    public static void applySword(ItemStack stack, ToolMaterial material, float projectedDamage, float attackSpeed) {
        float itemBonus = projectedDamage - 1.0F - material.attackDamageBonus();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                com.immortalstorage.immortalstorage.compat.mc2612.CompatWeaponAttributes.swordAttributes(material, itemBonus, attackSpeed));
    }

    private ModWeaponAttackProjection() {
    }
}
