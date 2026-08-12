package com.immortalstorage.immortalstorage.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Single authority for every durable player binding written by the mod.
 * Launcher/session UUIDs remain useful for vanilla networking, but must never
 * become a new owner id after a stable ImmortalStorage identity exists.
 */
public final class PersistentPlayerIdentity {
    public static UUID id(Player player) {
        if (player == null) throw new IllegalArgumentException("player");
        return ImmortalStoragePlayerData.get(player).bindPersonalRealmOnce(player.getUUID());
    }

    /** Accept the stable id and the pre-0.0.9 session UUID during migration. */
    public static boolean matches(Player player, @Nullable UUID storedOwner) {
        return player != null && storedOwner != null
                && (storedOwner.equals(id(player)) || storedOwner.equals(player.getUUID()));
    }

    /** Returns the canonical id only when the old binding belongs to this player. */
    public static @Nullable UUID migrate(Player player, @Nullable UUID storedOwner) {
        return matches(player, storedOwner) ? id(player) : null;
    }

    /** Resolve either a 0.0.9 stable owner id or a legacy live session UUID. */
    public static @Nullable ServerPlayer onlinePlayer(MinecraftServer server, @Nullable UUID owner) {
        if (server == null || owner == null) return null;
        ServerPlayer direct = server.getPlayerList().getPlayer(owner);
        if (direct != null) return direct;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            UUID stable = ImmortalStoragePlayerData.get(candidate).getPersonalRealmId();
            if (owner.equals(stable)) return candidate;
        }
        return null;
    }

    private PersistentPlayerIdentity() {}
}
