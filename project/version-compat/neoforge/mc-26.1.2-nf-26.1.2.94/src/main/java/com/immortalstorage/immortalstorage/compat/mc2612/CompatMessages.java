package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Keeps the canonical action-bar message intent on the 26.1 send API. */
public final class CompatMessages {
    private CompatMessages() {
    }

    public static void sendSystemMessage(Player player, Component message, boolean overlay) {
        if (player == null || message == null) return;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, overlay);
        } else {
            player.sendSystemMessage(message);
        }
    }
}
