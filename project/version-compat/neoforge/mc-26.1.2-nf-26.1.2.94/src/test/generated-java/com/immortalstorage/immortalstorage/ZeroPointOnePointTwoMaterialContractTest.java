package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZeroPointOnePointTwoMaterialContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path ROOT = locateMainSourceRoot();

    private static Path locateMainSourceRoot() {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve("src/main");
            if (Files.isDirectory(candidate.resolve("java"))
                    && Files.isDirectory(candidate.resolve("resources"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate the NeoForge src/main directory");
    }

    @Test void remainsRegistrationAndEndActivationUseTheNewBlock() throws Exception {
        String blocks = Files.readString(ROOT.resolve("java/com/immortalstorage/immortalstorage/block/ModBlocks.java"));
        String modes = Files.readString(ROOT.resolve("java/com/immortalstorage/immortalstorage/worldshard/WorldShardMinerModes.java"));
        assertTrue(blocks.contains("IMMORTAL_ART_REMAINS"));
        assertTrue(blocks.contains("Blocks.ANCIENT_DEBRIS"));
        assertTrue(modes.contains("ModBlocks.IMMORTAL_ART_REMAINS.get()"));
        assertFalse(modes.contains("WorldShardMinerActivation.forBlock(Blocks.PURPUR_BLOCK)"));
    }

    @Test void remainsRequiresNetheriteAndIsInOreTags() throws Exception {
        String diamond = Files.readString(ROOT.resolve("resources/data/minecraft/tags/block/incorrect_for_diamond_tool.json"));
        String pickaxe = Files.readString(ROOT.resolve("resources/data/minecraft/tags/block/mineable/pickaxe.json"));
        String ore = Files.readString(ROOT.resolve("resources/data/c/tags/block/ores/immortal_art_remains.json"));
        assertTrue(diamond.contains("immortalstorage:immortal_art_remains"));
        assertTrue(pickaxe.contains("immortalstorage:immortal_art_remains"));
        assertTrue(ore.contains("immortalstorage:immortal_art_remains"));
        assertFalse(Files.exists(ROOT.resolve("resources/data/minecraft/tags/block/incorrect_for_netherite_tool.json")));
    }

    @Test void allThreeFurnacesAndBothShapelessRecipesExist() throws Exception {
        for (String name : new String[]{"immortal_art_remains_smelting.json",
                "immortal_art_remains_blasting.json", "immortal_art_remains_immortal_furnace.json"}) {
            String recipe = Files.readString(ROOT.resolve("resources/data/immortalstorage/recipe/" + name));
            assertTrue(recipe.contains("immortalstorage:immortal_forged_alloy_scrap"));
        }
        String alloy = Files.readString(ROOT.resolve("resources/data/immortalstorage/recipe/immortal_forged_alloy.json"));
        assertEquals(4, alloy.split("immortal_forged_alloy_scrap", -1).length - 1);
        assertEquals(4, alloy.split("immortalstorage:spirit_iron", -1).length - 1);
        assertTrue(alloy.contains("immortalstorage:nurturing_crystal"));
        String crystal = Files.readString(ROOT.resolve("resources/data/immortalstorage/recipe/nurturing_crystal_from_alloy_scrap.json"));
        assertTrue(crystal.contains("immortalstorage:spirit_crystal"));
    }

    @Test void endGenerationUsesVanillaOreVeinsOnOuterIslands() throws Exception {
        String feature = Files.readString(ROOT.resolve(
                "resources/data/immortalstorage/worldgen/configured_feature/immortal_art_remains.json"));
        String placement = Files.readString(ROOT.resolve(
                "resources/data/immortalstorage/worldgen/placed_feature/immortal_art_remains.json"));
        String modifier = Files.readString(ROOT.resolve(
                "resources/data/immortalstorage/neoforge/biome_modifier/add_immortal_art_remains.json"));
        String biomes = Files.readString(ROOT.resolve(
                "resources/data/immortalstorage/tags/worldgen/biome/is_end_outer_island.json"));
        assertTrue(feature.contains("\"type\": \"minecraft:ore\""));
        assertTrue(feature.contains("\"predicate_type\": \"minecraft:block_match\""));
        assertTrue(feature.contains("\"block\": \"minecraft:end_stone\""));
        assertTrue(feature.contains("\"size\": 3"));
        assertTrue(placement.contains("\"type\": \"minecraft:rarity_filter\""));
        assertTrue(placement.contains("\"type\": \"minecraft:in_square\""));
        assertTrue(placement.contains("\"type\": \"minecraft:height_range\""));
        assertTrue(placement.contains("\"type\": \"minecraft:biome\""));
        assertTrue(modifier.contains("#immortalstorage:is_end_outer_island"));
        assertFalse(biomes.contains("minecraft:the_end"));
        assertTrue(biomes.contains("minecraft:end_highlands"));
        assertTrue(biomes.contains("minecraft:small_end_islands"));
    }
}
