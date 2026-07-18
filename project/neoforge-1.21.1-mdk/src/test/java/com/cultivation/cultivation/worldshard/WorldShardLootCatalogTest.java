package com.cultivation.cultivation.worldshard;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardLootCatalogTest {
    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    @Test
    void selectsOnlyTablesRegisteredForTheActiveMode() {
        WorldShardLootDefinition overworld = new WorldShardLootDefinition(
                id("test:overworld_a"), WorldShardMinerModes.OVERWORLD,
                id("minecraft:chests/simple_dungeon"), 2L);
        WorldShardLootDefinition nether = new WorldShardLootDefinition(
                id("test:nether_a"), WorldShardMinerModes.NETHER,
                id("minecraft:chests/nether_bridge"), 100L);

        WorldShardLootCatalog catalog = WorldShardLootCatalog.of(List.of(overworld, nether));

        assertEquals(overworld, catalog.select(WorldShardMinerModes.OVERWORLD, 99L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertEquals(nether, catalog.select(WorldShardMinerModes.NETHER, 0L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertTrue(catalog.select(id("test:missing"), 0L,
                WorldShardLootWeightProvider.configured()).isEmpty());
    }

    @Test
    void weightedSelectionUsesTheConfiguredDatapackWeights() {
        WorldShardLootDefinition first = new WorldShardLootDefinition(
                id("test:first"), WorldShardMinerModes.OVERWORLD, id("test:first_table"), 1L);
        WorldShardLootDefinition second = new WorldShardLootDefinition(
                id("test:second"), WorldShardMinerModes.OVERWORLD, id("test:second_table"), 3L);
        WorldShardLootCatalog catalog = WorldShardLootCatalog.of(List.of(first, second));

        assertEquals(first, catalog.select(WorldShardMinerModes.OVERWORLD, 0L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertEquals(second, catalog.select(WorldShardMinerModes.OVERWORLD, 1L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertEquals(second, catalog.select(WorldShardMinerModes.OVERWORLD, 3L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertEquals(first, catalog.select(WorldShardMinerModes.OVERWORLD, 4L,
                WorldShardLootWeightProvider.configured()).orElseThrow());
        assertTrue(catalog.hasSelectable(WorldShardMinerModes.OVERWORLD,
                WorldShardLootWeightProvider.configured()));
        assertFalse(catalog.hasSelectable(WorldShardMinerModes.NETHER,
                WorldShardLootWeightProvider.configured()));
        assertFalse(catalog.hasSelectable(WorldShardMinerModes.OVERWORLD, ignored -> 0L));
    }

    @Test
    void externalResourceWithTheSameIdOverridesWhileNewIdsInject() {
        WorldShardLootDefinition builtin = new WorldShardLootDefinition(
                id("cultivation:overworld_dungeon"), WorldShardMinerModes.OVERWORLD,
                id("minecraft:chests/simple_dungeon"), 10L);
        WorldShardLootDefinition override = new WorldShardLootDefinition(
                builtin.id(), WorldShardMinerModes.OVERWORLD, id("pack:chests/rebalanced"), 7L);
        WorldShardLootDefinition injected = new WorldShardLootDefinition(
                id("pack:extra"), WorldShardMinerModes.OVERWORLD, id("pack:chests/extra"), 2L);

        Map<ResourceLocation, WorldShardLootDefinition> merged =
                WorldShardLootCatalog.mergeDefinitions(List.of(builtin), List.of(override, injected));

        assertEquals(2, merged.size());
        assertEquals(override, merged.get(builtin.id()));
        assertEquals(injected, merged.get(injected.id()));
    }
    @Test
    void zeroWeightOverrideDeletesTheMatchingBuiltInEntry() {
        WorldShardLootDefinition builtin = new WorldShardLootDefinition(
                id("cultivation:overworld_dungeon"), WorldShardMinerModes.OVERWORLD,
                id("minecraft:chests/simple_dungeon"), 10L);
        WorldShardLootDefinition removal = new WorldShardLootDefinition(
                builtin.id(), WorldShardMinerModes.OVERWORLD,
                id("minecraft:chests/simple_dungeon"), 0L);

        Map<ResourceLocation, WorldShardLootDefinition> merged =
                WorldShardLootCatalog.mergeDefinitions(List.of(builtin), List.of(removal));

        assertTrue(merged.isEmpty());
    }

}
