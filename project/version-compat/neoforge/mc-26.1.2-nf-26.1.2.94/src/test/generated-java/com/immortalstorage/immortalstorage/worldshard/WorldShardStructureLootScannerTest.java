package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locks the dimension assignment used by native structure-chest discovery. */
final class WorldShardStructureLootScannerTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }


    @Test
    void endCityTreasureIsEndScoped() {
        assertEquals(WorldShardMinerModes.END,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/end_city_treasure")));
    }

    @Test
    void netherStructuresAreNetherScoped() {
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/nether_bridge")));
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/bastion_treasure")));
        assertEquals(WorldShardMinerModes.NETHER,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/bastion_other")));
    }

    @Test
    void everyOtherChestTableBelongsToTheOverworld() {
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/ancient_city")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/ancient_city_ice_box")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/simple_dungeon")));
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("minecraft:chests/village/village_plains_house")));
        // Modded structure chests also default to the overworld instead of
        // diluting End/Nether windows.
        assertEquals(WorldShardMinerModes.OVERWORLD,
                WorldShardStructureLootScanner.modeFor(Identifier.parse("somemod:chests/ruined_temple")));
    }

    @Test
    void discoveredTablesUseAStableDefaultWeight() {
        assertEquals(8L, WorldShardStructureLootScanner.defaultWeight(
                Identifier.parse("minecraft:chests/ancient_city")));
    }
}
