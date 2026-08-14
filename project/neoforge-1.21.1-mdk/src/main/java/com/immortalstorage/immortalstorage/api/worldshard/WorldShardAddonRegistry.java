package com.immortalstorage.immortalstorage.api.worldshard;

import com.immortalstorage.immortalstorage.worldshard.WorldShardLootDefinition;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerMode;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Startup-only programmatic contributions to the world-shard miner and
 * treasure-basin catalogs.
 *
 * <p>Addons register miner modes and loot definitions here during their mod
 * constructor or {@code FMLCommonSetupEvent}. Registration is frozen right
 * after common setup; from then on the reload listeners merge these
 * contributions beneath datapack overrides on every server start and
 * {@code /reload}, so programmatic entries survive a reload instead of being
 * discarded with the JSON snapshot.</p>
 *
 * <p>Prefer the facade in {@link WorldShardApi}; this class exists so the
 * {@code worldshard} reload listeners can read the frozen snapshot without
 * depending on the facade's public surface.</p>
 */
public final class WorldShardAddonRegistry {
    private static final Map<ResourceLocation, WorldShardMinerMode> MINER_MODES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, WorldShardLootDefinition> LOOT_DEFINITIONS = new ConcurrentHashMap<>();
    private static final AtomicBoolean FROZEN = new AtomicBoolean(false);

    private WorldShardAddonRegistry() {
    }

    public static void registerMinerMode(WorldShardMinerMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (FROZEN.get()) {
            throw new IllegalStateException("World-shard addon registration is closed: " + mode.id());
        }
        WorldShardMinerMode previous = MINER_MODES.putIfAbsent(mode.id(), mode);
        if (previous != null && !previous.equals(mode)) {
            throw new IllegalStateException("World-shard miner mode already registered: " + mode.id());
        }
    }

    public static void registerLootDefinition(WorldShardLootDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (FROZEN.get()) {
            throw new IllegalStateException("World-shard addon registration is closed: " + definition.id());
        }
        WorldShardLootDefinition previous = LOOT_DEFINITIONS.putIfAbsent(definition.id(), definition);
        if (previous != null && !previous.equals(definition)) {
            throw new IllegalStateException("World-shard loot definition already registered: " + definition.id());
        }
    }

    public static void freeze() {
        FROZEN.set(true);
    }

    public static boolean isFrozen() {
        return FROZEN.get();
    }

    /** Server-internal snapshot merged by {@code WorldShardMinerReloadListener}. */
    public static List<WorldShardMinerMode> minerModeOverrides() {
        return List.copyOf(new ArrayList<>(MINER_MODES.values()));
    }

    /** Server-internal snapshot merged by {@code WorldShardLootReloadListener}. */
    public static List<WorldShardLootDefinition> lootOverrides() {
        return List.copyOf(new ArrayList<>(LOOT_DEFINITIONS.values()));
    }

    /**
     * Test-only reset: clears contributions and re-opens registration. The
     * NeoForge {@code unitTest} harness loads the mod (and therefore freezes
     * this registry) before any test runs, so tests that exercise the
     * register/duplicate/freeze contracts reset to a clean slate first.
     */
    static void resetForTests() {
        MINER_MODES.clear();
        LOOT_DEFINITIONS.clear();
        FROZEN.set(false);
    }
}
