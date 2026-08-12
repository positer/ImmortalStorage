package com.immortalstorage.immortalstorage.network.storage.backend;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Dependency-free request passed to an optional personal-storage provider.
 * Optional-mod types are deliberately excluded from this public boundary.
 */
public record PersonalStorageBackendContext(
        ServerPlayer player,
        boolean includeFluids,
        Runnable onChanged) {

    public PersonalStorageBackendContext {
        Objects.requireNonNull(player, "player");
        onChanged = onChanged == null ? () -> {} : onChanged;
    }
}
