package com.cultivation.cultivation.api.storage;

import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Public API for owner-scoped personal storage lookup.
 *
 * This intentionally returns standard NeoForge handlers so adjacent mods can
 * integrate without linking against internal player-data classes.
 */
public final class PersonalStorageApi {
    public static @Nullable PersonalStorageEndpoint resolve(MinecraftServer server, @Nullable UUID owner) {
        return PersonalStorageNetwork.resolve(server, owner);
    }

    /** Stage-seven+ owner-scoped endpoint with standard NeoForge fluid capability support. */
    public static @Nullable PersonalStorageEndpoint resolveWithFluids(
            MinecraftServer server, @Nullable UUID owner) {
        return PersonalStorageNetwork.resolveWithFluids(server, owner, null);
    }

    /**
     * Resolves only stage-six-or-higher Xianqiao storage. This is the correct
     * boundary for interfaces and optional storage-network cells; it never
     * falls back to Kongqiao. Stage seven adds the fluid namespace.
     */
    public static @Nullable PersonalStorageEndpoint resolveXianqiao(
            MinecraftServer server, @Nullable UUID owner) {
        return PersonalStorageNetwork.resolveXianqiao(server, owner, null);
    }

    /**
     * Resolves the endpoint after validating that {@code realm} is the exact
     * personal-realm dimension bound to {@code owner}. Stage-six endpoints stay
     * item-only; the standard fluid capability appears at stage seven.
     */
    public static @Nullable PersonalStorageEndpoint resolveInOwnerRealm(
            ServerLevel realm, @Nullable UUID owner) {
        return PersonalStorageNetwork.resolveInOwnerRealm(realm, owner, null);
    }

    private PersonalStorageApi() {}
}
