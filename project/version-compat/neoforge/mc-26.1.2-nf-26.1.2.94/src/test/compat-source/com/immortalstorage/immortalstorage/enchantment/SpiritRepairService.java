package com.immortalstorage.immortalstorage.enchantment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Converts Yuan consumption into fractional durability repair for held enchanted tools. */
public final class SpiritRepairService {
    private static final String TENTHS_KEY = "immortalstorageSpiritRepairTenths";

    public static void onYuanConsumed(ServerPlayer player, long consumed) {
        if (player == null || consumed <= 0L) return;
        repairHeld(player, player.getMainHandItem(), consumed);
        if (player.getOffhandItem() != player.getMainHandItem()) {
            repairHeld(player, player.getOffhandItem(), consumed);
        }
    }

    private static void repairHeld(ServerPlayer player, ItemStack stack, long consumed) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) return;
        var enchantment = player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(ModEnchantments.SPIRIT_REPAIR).orElse(null);
        if (enchantment == null || stack.getEnchantmentLevel(enchantment) <= 0) return;

        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = existing.copyTag();
        long accumulated = Math.max(0L, tag.getLongOr(TENTHS_KEY, 0L));
        long total = saturatingAdd(accumulated, consumed);
        long repairPoints = Math.min(stack.getDamageValue(), total / 10L);
        long remainder = repairPoints >= stack.getDamageValue() ? 0L : total - repairPoints * 10L;
        if (repairPoints > 0L) {
            stack.setDamageValue(stack.getDamageValue() - (int) repairPoints);
        }
        if (remainder == 0L) tag.remove(TENTHS_KEY);
        else tag.putLong(TENTHS_KEY, remainder);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) return Long.MAX_VALUE;
        return left + right;
    }

    private SpiritRepairService() {}
}
