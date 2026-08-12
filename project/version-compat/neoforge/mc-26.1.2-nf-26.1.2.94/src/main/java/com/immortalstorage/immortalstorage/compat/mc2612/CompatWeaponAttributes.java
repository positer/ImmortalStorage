package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Official 26.1 ItemAttributeModifiers builder used by mutable weapons. */
public final class CompatWeaponAttributes {
    private CompatWeaponAttributes() {
    }

    public static ItemAttributeModifiers swordAttributes(ToolMaterial material,
                                                          float itemBonus,
                                                          float attackSpeed) {
        return toolAttributes(material, itemBonus, attackSpeed);
    }

    public static ItemAttributeModifiers toolAttributes(ToolMaterial material,
                                                         float itemBonus,
                                                         float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                                material.attackDamageBonus() + itemBonus,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                                attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
