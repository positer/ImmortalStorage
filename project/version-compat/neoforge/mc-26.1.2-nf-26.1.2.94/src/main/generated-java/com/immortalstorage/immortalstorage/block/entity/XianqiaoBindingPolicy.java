package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Binding rules for machines that can route results into a player's personal
 * Xianqiao. Placement in a personal realm binds to that realm's owner first;
 * only outside a personal realm may a bound Spirit Drive provide the owner.
 * PersistentPlayerIdentity accepts the stable owner id and the one legacy
 * session UUID during migration; placement actor, nearest player and stale
 * NBT owners are never binding authorities.
 */
public final class XianqiaoBindingPolicy {
    public enum BindingSource {
        NONE,
        PERSONAL_REALM,
        SPIRIT_DRIVE
    }

    public record Binding(@Nullable UUID owner, BindingSource source) {
        public boolean isBound() {
            return owner != null && source != BindingSource.NONE;
        }
    }

    private XianqiaoBindingPolicy() {
    }

    /**
     * Shared machine binding with realm priority. A realm owner is returned
     * even when the owner is temporarily offline; the endpoint gate remains
     * responsible for deciding whether output can be enabled at that moment.
     */
    public static Binding resolve(ServerLevel level, ItemStack fuel) {
        if (level == null) return new Binding(null, BindingSource.NONE);

        UUID realmOwner = ImmortalStorageDimensions.personalRealmOwner(level.dimension()).orElse(null);
        if (realmOwner != null) {
            return new Binding(realmOwner, BindingSource.PERSONAL_REALM);
        }

        if (fuel == null || fuel.isEmpty() || !(fuel.getItem() instanceof SpiritDriveItem)) {
            return new Binding(null, BindingSource.NONE);
        }
        UUID driveOwner = SpiritDriveItem.owner(fuel).orElse(null);
        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), driveOwner);
        return player == null
                ? new Binding(null, BindingSource.NONE)
                : new Binding(PersistentPlayerIdentity.id(player), BindingSource.SPIRIT_DRIVE);
    }

    /**
     * Named entry point retained for callers that describe the Energy Crystal
     * specifically. It intentionally uses the same shared priority rule as
     * the simulated machines.
     */
    public static Binding resolveEnergyCrystal(ServerLevel level, ItemStack fuel) {
        return resolve(level, fuel);
    }

    /** Compatibility helper for output paths that only need the owner id. */
    public static @Nullable UUID ownerFor(ServerLevel level, ItemStack fuel) {
        Binding binding = resolve(level, fuel);
        return binding.isBound() ? binding.owner() : null;
    }
}
