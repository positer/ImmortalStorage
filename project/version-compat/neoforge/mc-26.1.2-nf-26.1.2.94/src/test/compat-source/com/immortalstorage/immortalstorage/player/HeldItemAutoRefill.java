package com.immortalstorage.immortalstorage.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/** Detects selected-stack exhaustion and restores one exact-component stack from personal storage. */
public final class HeldItemAutoRefill {
    private static final Map<UUID, Snapshot> LAST = new HashMap<>();

    public static void tick(ServerPlayer player, ImmortalStoragePlayerData data) {
        if (player == null || data == null) return;
        ItemStack current = player.getMainHandItem();
        UUID id = player.getUUID();
        Snapshot previous = LAST.get(id);
        if (data.isHandAutoRefill() && current.isEmpty() && previous != null && !previous.template().isEmpty()
                && previous.selectedSlot() == player.getInventory().getSelectedSlot() && previous.count() > 0) {
            int amount = Math.max(1, previous.template().getMaxStackSize());
            ItemStack extracted = data.extractStack(previous.template(), amount);
            if (!extracted.isEmpty()) {
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), extracted);
                current = extracted;
            }
        }
        LAST.put(id, current.isEmpty()
                ? new Snapshot(ItemStack.EMPTY, 0, player.getInventory().getSelectedSlot())
                : new Snapshot(current.copyWithCount(1), current.getCount(), player.getInventory().getSelectedSlot()));
    }

    public static void clear(ServerPlayer player) {
        if (player != null) LAST.remove(player.getUUID());
    }

    private record Snapshot(ItemStack template, int count, int selectedSlot) {}

    private HeldItemAutoRefill() {}
}
