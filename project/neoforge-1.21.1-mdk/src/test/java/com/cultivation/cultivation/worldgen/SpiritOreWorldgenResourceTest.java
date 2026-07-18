package com.cultivation.cultivation.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritOreWorldgenResourceTest {
    @Test
    void configuredFeaturesMirrorVanillaIronAndDiamondFamilies() {
        assertOreConfig("spirit_iron_ore", 9, 0.0D);
        assertOreConfig("spirit_iron_ore_small", 4, 0.0D);
        assertOreConfig("spirit_crystal_ore", 4, 0.5D);
        assertOreConfig("spirit_crystal_ore_medium", 8, 0.5D);
        assertOreConfig("spirit_crystal_ore_large", 12, 0.7D);
        assertOreConfig("spirit_crystal_ore_buried", 8, 1.0D);
    }

    @Test
    void biomeModifierInstallsAllVanillaShapedDistributions() {
        JsonArray features = resource("data/cultivation/neoforge/biome_modifier/add_spirit_ores.json")
                .getAsJsonArray("features");
        Set<String> installed = new HashSet<>();
        features.forEach(value -> installed.add(value.getAsString()));

        assertEquals(Set.of(
                "cultivation:spirit_iron_ore_upper",
                "cultivation:spirit_iron_ore_middle",
                "cultivation:spirit_iron_ore_small",
                "cultivation:spirit_crystal_ore",
                "cultivation:spirit_crystal_ore_medium",
                "cultivation:spirit_crystal_ore_large",
                "cultivation:spirit_crystal_ore_buried"), installed);
    }

    @Test
    void allFourOreBlocksRequireDiamondTier() {
        Set<String> diamond = values("data/minecraft/tags/block/needs_diamond_tool.json");
        Set<String> iron = values("data/minecraft/tags/block/needs_iron_tool.json");
        for (String id : Set.of(
                "cultivation:spirit_iron_ore",
                "cultivation:spirit_crystal_ore",
                "cultivation:deepslate_spirit_iron_ore",
                "cultivation:deepslate_spirit_crystal_ore")) {
            assertTrue(diamond.contains(id));
            assertFalse(iron.contains(id));
        }
    }

    private static void assertOreConfig(String id, int size, double discardChance) {
        JsonObject config = resource("data/cultivation/worldgen/configured_feature/" + id + ".json")
                .getAsJsonObject("config");
        assertEquals(size, config.get("size").getAsInt());
        assertEquals(discardChance, config.get("discard_chance_on_air_exposure").getAsDouble(), 0.0001D);
        JsonArray targets = config.getAsJsonArray("targets");
        assertEquals(2, targets.size(), "stone and deepslate must share one configured feature");
        assertEquals("minecraft:stone_ore_replaceables",
                targets.get(0).getAsJsonObject().getAsJsonObject("target").get("tag").getAsString());
        assertEquals("minecraft:deepslate_ore_replaceables",
                targets.get(1).getAsJsonObject().getAsJsonObject("target").get("tag").getAsString());
    }

    private static Set<String> values(String path) {
        Set<String> values = new HashSet<>();
        try {
            for (URL url : Collections.list(SpiritOreWorldgenResourceTest.class.getClassLoader().getResources(path))) {
                try (InputStream stream = url.openStream();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values")
                            .forEach(value -> values.add(value.isJsonPrimitive()
                                    ? value.getAsString()
                                    : value.getAsJsonObject().get("id").getAsString()));
                }
            }
        } catch (java.io.IOException error) {
            throw new AssertionError("could not read tag resources " + path, error);
        }
        assertFalse(values.isEmpty(), "missing tag resources " + path);
        return values;
    }

    private static JsonObject resource(String path) {
        InputStream stream = SpiritOreWorldgenResourceTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing test resource " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException error) {
            throw new AssertionError("could not read " + path, error);
        }
    }
}
