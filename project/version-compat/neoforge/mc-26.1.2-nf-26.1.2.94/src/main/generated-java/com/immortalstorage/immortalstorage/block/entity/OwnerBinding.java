package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** First-claim-wins UUID binding used by owner-scoped block capabilities. */
final class OwnerBinding {
    private UUID owner;

    public @Nullable UUID owner() {
        return owner;
    }

    /** Claims an unowned binding or validates the existing owner without replacing it. */
    public boolean claim(@Nullable UUID candidate) {
        if (candidate == null) return false;
        if (owner == null) owner = candidate;
        return owner.equals(candidate);
    }

    /** Claims with the stable id and upgrades this player's legacy session id. */
    public boolean claim(Player player) {
        if (player == null) return false;
        UUID stable = PersistentPlayerIdentity.id(player);
        if (owner == null || owner.equals(player.getUUID())) owner = stable;
        return owner.equals(stable);
    }

    public boolean isOwner(Player player) {
        return player != null && PersistentPlayerIdentity.matches(player, owner);
    }

    public boolean isOwner(@Nullable UUID candidate) {
        return candidate != null && candidate.equals(owner);
    }

    public void save(CompoundTag tag, String key) {
        if (owner != null) com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(tag, key, owner);
    }

    public void load(CompoundTag tag, String key) {
        owner = com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, key) ? com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, key) : null;
    }
}
