package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.item.custom.ReinforcementPluginItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Shared one-slot reinforcement contract for every supported machine. */
public interface ReinforcementPluginHost {
    ItemStack reinforcementPlugin();
    void setReinforcementPlugin(ItemStack stack);

    default int reinforcementMultiplier() {
        return multiplier(reinforcementPlugin());
    }

    default boolean tryInstallReinforcement(ServerPlayer player, ItemStack held) {
        int offered = multiplier(held);
        if (offered <= 1) return false;
        ItemStack current = reinforcementPlugin();
        if (!current.isEmpty() && !isPlugin(current)) return false;
        if (!current.isEmpty() && offered <= multiplier(current)) return false;
        ItemStack replacement = held.copyWithCount(1);
        if (!player.getAbilities().instabuild) held.shrink(1);
        setReinforcementPlugin(replacement);
        if (!current.isEmpty() && !player.getInventory().add(current.copy())) player.drop(current.copy(), false);
        return true;
    }

    static boolean isPlugin(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ReinforcementPluginItem;
    }

    static int multiplier(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ReinforcementPluginItem plugin
                ? plugin.multiplier() : 1;
    }

    static long multiplySaturated(long value, int multiplier) {
        if (value <= 0L) return 0L;
        return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
    }

    /** Expands one logical result without repeating the producer's work. */
    static List<ItemStack> multiplyOutputs(List<ItemStack> source, int multiplier) {
        ArrayList<ItemStack> result = new ArrayList<>();
        if (source == null) return result;
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty()) continue;
            long remaining = Math.min(Integer.MAX_VALUE,
                    multiplySaturated(stack.getCount(), Math.max(1, multiplier)));
            while (remaining > 0L) {
                int count = (int) Math.min(stack.getMaxStackSize(), remaining);
                result.add(stack.copyWithCount(count));
                remaining -= count;
            }
        }
        return result;
    }
}
