package com.immortalstorage.immortalstorage.api.worldshard;

import com.immortalstorage.immortalstorage.worldshard.WorldShardLootDefinition;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerActivation;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerMode;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts for the public world-shard addon registry: programmatic
 * registration surfaces as stable snapshots for the reload listeners and is
 * closed after startup. The NeoForge {@code unitTest} harness loads the mod
 * (freezing the registry) before any test runs, so each test resets to a
 * clean slate and exercises one contract independently.
 */
final class WorldShardApiTest {
    @BeforeAll
    static void bootstrap() {
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void reset() {
        WorldShardAddonRegistry.resetForTests();
    }

    private static WorldShardMinerMode mode(String id) {
        return new WorldShardMinerMode(ResourceLocation.parse(id),
                WorldShardMinerActivation.forBlock(Blocks.GOLD_BLOCK),
                Optional.of(Level.OVERWORLD.location()),
                Optional.of(TagKey.create(Registries.BIOME, ResourceLocation.parse("minecraft:is_overworld"))),
                0xFF00FF00, 1.0D, Map.of());
    }

    private static WorldShardLootDefinition loot(String id, String table, long weight) {
        return new WorldShardLootDefinition(ResourceLocation.parse(id),
                WorldShardMinerModes.OVERWORLD, ResourceLocation.parse(table), weight);
    }

    @Test
    void registrationSurfacesInSnapshot() {
        WorldShardAddonRegistry.registerLootDefinition(
                loot("test:api_loot_a", "minecraft:chests/simple_dungeon", 5L));
        WorldShardAddonRegistry.registerMinerMode(mode("test:api_mode_a"));

        assertTrue(WorldShardAddonRegistry.lootOverrides().stream()
                .anyMatch(d -> d.id().toString().equals("test:api_loot_a")));
        assertTrue(WorldShardAddonRegistry.minerModeOverrides().stream()
                .anyMatch(m -> m.id().toString().equals("test:api_mode_a")));
    }

    @Test
    void duplicateRegistrationRejectsADifferentValue() {
        WorldShardAddonRegistry.registerLootDefinition(
                loot("test:api_loot_a", "minecraft:chests/simple_dungeon", 5L));
        assertThrows(IllegalStateException.class,
                () -> WorldShardAddonRegistry.registerLootDefinition(
                        loot("test:api_loot_a", "minecraft:chests/nether_bridge", 9L)));
        assertEquals(5L, WorldShardAddonRegistry.lootOverrides().stream()
                .filter(d -> d.id().toString().equals("test:api_loot_a"))
                .findFirst().orElseThrow().weight());
    }

    @Test
    void duplicateRegistrationWithTheSameValueIsIdempotent() {
        WorldShardLootDefinition def = loot("test:api_loot_same", "minecraft:chests/simple_dungeon", 5L);
        WorldShardAddonRegistry.registerLootDefinition(def);
        WorldShardAddonRegistry.registerLootDefinition(def);
        assertEquals(1, WorldShardAddonRegistry.lootOverrides().stream()
                .filter(d -> d.id().toString().equals("test:api_loot_same")).count());
    }

    @Test
    void registrationClosesAfterFreeze() {
        WorldShardAddonRegistry.freeze();
        assertTrue(WorldShardAddonRegistry.isFrozen());
        assertThrows(IllegalStateException.class,
                () -> WorldShardAddonRegistry.registerLootDefinition(
                        loot("test:api_loot_frozen", "minecraft:chests/simple_dungeon", 7L)));
        assertThrows(IllegalStateException.class,
                () -> WorldShardAddonRegistry.registerMinerMode(mode("test:api_mode_frozen")));
    }
}
