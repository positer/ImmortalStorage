package com.immortalstorage.immortalstorage.api.worldshard;

import com.immortalstorage.immortalstorage.worldshard.WorldShardLootCatalog;
import com.immortalstorage.immortalstorage.worldshard.WorldShardLootDefinition;
import com.immortalstorage.immortalstorage.worldshard.WorldShardLootWeightProvider;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerMode;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Public read + registration boundary for the world-shard miner and the
 * treasure basin. Addons integrate through this facade instead of linking
 * against internal catalog classes, so catalog reshapes stay addon-safe.
 *
 * <ul>
 *   <li>Read access exposes the current world-gen result: which miner modes are
 *       resolved, which ore pool each mode sampled, and which structure-chest
 *       loot tables the basin may roll.</li>
 *   <li>Registration lets an addon contribute a new mining mode or loot table
 *       programmatically, mirroring the {@code world_shard_miner} /
 *       {@code world_shard_loot} datapack JSON without shipping a datapack.</li>
 * </ul>
 */
public final class WorldShardApi {
    private WorldShardApi() {
    }

    // ------------------------------------------------------------------
    // World shard miner (collector) read access
    // ------------------------------------------------------------------

    /** Immutable map of the currently loaded miner modes, keyed by mode id. */
    public static Map<ResourceLocation, WorldShardMinerMode> minerModes() {
        return WorldShardMinerModes.definitions();
    }

    /** The resolved mode, including its sampled ore pool, for {@code id}. */
    public static Optional<WorldShardMinerModes.ResolvedMode> resolvedMinerMode(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return WorldShardMinerModes.resolved(id);
    }

    /** Monotonic stamp that changes whenever the miner catalog is rebuilt. */
    public static long minerGeneration() {
        return WorldShardMinerModes.generation();
    }

    // ------------------------------------------------------------------
    // Treasure basin loot read access
    // ------------------------------------------------------------------

    /** Selectable loot definitions for one basin mode, in stable id order. */
    public static List<WorldShardLootDefinition> lootDefinitions(ResourceLocation mode) {
        Objects.requireNonNull(mode, "mode");
        return WorldShardLootCatalog.active().definitions(mode);
    }

    /** Whether a basin running {@code mode} can currently roll any loot table. */
    public static boolean hasSelectableLoot(ResourceLocation mode) {
        Objects.requireNonNull(mode, "mode");
        return WorldShardLootCatalog.active()
                .hasSelectable(mode, WorldShardLootWeightProvider.configured());
    }

    /** Monotonic stamp that changes whenever the loot catalog is reinstalled. */
    public static long lootGeneration() {
        return WorldShardLootCatalog.active().generation();
    }

    // ------------------------------------------------------------------
    // Addon registration
    // ------------------------------------------------------------------

    /**
     * Registers an addon mining mode. The mode is merged beneath datapack
     * {@code world_shard_miner} overrides on the next reload. Must be called
     * before registration closes (see {@link #freezeRegistration()}).
     */
    public static void registerMinerMode(WorldShardMinerMode mode) {
        WorldShardAddonRegistry.registerMinerMode(mode);
    }

    /**
     * Registers an addon loot definition. The definition is merged beneath
     * datapack {@code world_shard_loot} overrides on the next reload. Must be
     * called before registration closes (see {@link #freezeRegistration()}).
     */
    public static void registerLootDefinition(WorldShardLootDefinition definition) {
        WorldShardAddonRegistry.registerLootDefinition(definition);
    }

    /**
     * Closes addon registration. Called once after common setup; later calls
     * are idempotent and registration attempts throw {@link IllegalStateException}.
     */
    public static void freezeRegistration() {
        WorldShardAddonRegistry.freeze();
    }

    public static boolean isRegistrationFrozen() {
        return WorldShardAddonRegistry.isFrozen();
    }
}
