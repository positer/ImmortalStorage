package com.cultivation.cultivation.enchantment;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** Resource keys for 1.21's data-driven enchantment registry. */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> SPIRIT_REPAIR = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, "spirit_repair"));

    public static void applySpiritRepair(ItemStack stack, RegistryAccess registries) {
        if (stack == null || stack.isEmpty() || registries == null) return;
        registries.lookupOrThrow(Registries.ENCHANTMENT).get(SPIRIT_REPAIR)
                .ifPresent(holder -> ensureLevel(stack, holder));
    }

    private static void ensureLevel(ItemStack stack, Holder<Enchantment> holder) {
        if (stack.getEnchantmentLevel(holder) < 1) stack.enchant(holder, 1);
    }

    private ModEnchantments() {}
}
