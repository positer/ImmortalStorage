package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locks the dimension assignment used by native structure-chest discovery. */
final class WorldShardStructureLootScannerTest {

    @Test
    void endCityTreasureIsEndScoped() {
        assertEquals(WorldShardMinerModes.END,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/end_city_treasure")));
    }

    @Test
    void netherStructuresAreNetherScoped() {
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/nether_bridge")));
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/bastion_treasure")));
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/bastion_other")));
    }

    @Test
    void everyOtherChestTableBelongsToTheOverworld() {
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/ancient_city")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/ancient_city_ice_box")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/simple_dungeon")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("minecraft:chests/village/village_plains_house")));
        // Modded structure chests also default to the overworld instead of
        // diluting End/Nether windows.
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(ResourceLocation.parse("somemod:chests/ruined_temple")));
    }

    @Test
    void discoveredTablesUseAStableDefaultWeight() {
        assertEquals(8L, WorldShardStructureLootScanner.defaultWeight(
                ResourceLocation.parse("minecraft:chests/ancient_city")));
    }
}
