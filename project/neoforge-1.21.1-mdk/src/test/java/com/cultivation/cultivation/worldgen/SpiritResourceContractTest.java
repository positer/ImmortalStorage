package com.cultivation.cultivation.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritResourceContractTest {
    private static final List<String> SIMPLE_BLOCKS = List.of(
            "spirit_iron_ore",
            "spirit_crystal_ore",
            "deepslate_spirit_iron_ore",
            "deepslate_spirit_crystal_ore",
            "spirit_iron_block",
            "spirit_crystal_block",
            "crude_spirit_iron_block");
    private static final Set<String> ORES = Set.of(
            "cultivation:spirit_iron_ore",
            "cultivation:spirit_crystal_ore",
            "cultivation:deepslate_spirit_iron_ore",
            "cultivation:deepslate_spirit_crystal_ore");
    private static final Map<String, String> ORE_DROPS = Map.of(
            "spirit_iron_ore", "cultivation:crude_spirit_iron",
            "deepslate_spirit_iron_ore", "cultivation:crude_spirit_iron",
            "spirit_crystal_ore", "cultivation:spirit_crystal",
            "deepslate_spirit_crystal_ore", "cultivation:spirit_crystal");

    @Test
    void blockstateModelsItemModelsTexturesAndNamesAreClosed() throws IOException {
        for (String id : SIMPLE_BLOCKS) {
            JsonObject blockstate = resource("assets/cultivation/blockstates/" + id + ".json");
            assertEquals("cultivation:block/" + id,
                    blockstate.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());

            JsonObject blockModel = resource("assets/cultivation/models/block/" + id + ".json");
            assertEquals("minecraft:block/cube_all", blockModel.get("parent").getAsString());
            assertEquals("cultivation:block/" + id,
                    blockModel.getAsJsonObject("textures").get("all").getAsString());

            JsonObject itemModel = resource("assets/cultivation/models/item/" + id + ".json");
            assertEquals("cultivation:block/" + id, itemModel.get("parent").getAsString());
            assertTexture16("assets/cultivation/textures/block/" + id + ".png");
        }

        JsonObject minerState = resource("assets/cultivation/blockstates/world_shard_miner.json");
        assertEquals("cultivation:block/world_shard_miner",
                minerState.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        JsonObject minerItem = resource("assets/cultivation/models/item/world_shard_miner.json");
        assertEquals("minecraft:block/block", minerItem.get("parent").getAsString());
        assertEquals("minecraft:translucent", minerItem.get("render_type").getAsString());
        assertEquals("minecraft:block/glass", minerItem.getAsJsonObject("textures").get("glass").getAsString());
        assertEquals("minecraft:block/white_concrete", minerItem.getAsJsonObject("textures").get("core").getAsString());
        assertEquals(3, minerItem.getAsJsonArray("elements").size(),
                "item preview keeps a transparent glass shell around the inactive white core");
        JsonObject minerModel = resource("assets/cultivation/models/block/world_shard_miner.json");
        for (String face : List.of("bottom", "side", "top")) {
            assertEquals("cultivation:block/world_shard_miner_" + face,
                    minerModel.getAsJsonObject("textures").get(face).getAsString());
            assertTexture16("assets/cultivation/textures/block/world_shard_miner_" + face + ".png");
        }
        assertEquals(1, minerModel.getAsJsonArray("elements").size(),
                "runtime renderer owns the transparent glass and mode-colored internal core");
        minerModel.getAsJsonArray("elements").get(0).getAsJsonObject()
                .getAsJsonObject("faces").entrySet()
                .forEach(entry -> assertFalse(entry.getValue().getAsJsonObject().has("tintindex")));

        JsonObject en = resource("assets/cultivation/lang/en_us.json");
        JsonObject zh = resource("assets/cultivation/lang/zh_cn.json");
        Map<String, String> chineseNames = Map.of(
                "spirit_iron_ore", "灵铁矿",
                "spirit_crystal_ore", "灵晶矿",
                "deepslate_spirit_iron_ore", "深层灵铁矿",
                "deepslate_spirit_crystal_ore", "深层灵晶矿",
                "spirit_iron_block", "灵铁块",
                "spirit_crystal_block", "灵晶块",
                "crude_spirit_iron_block", "粗灵铁块",
                "world_shard_miner", "世界碎片开采器");
        for (Map.Entry<String, String> entry : chineseNames.entrySet()) {
            String key = "block.cultivation." + entry.getKey();
            assertTrue(en.has(key), "missing English name for " + key);
            assertEquals(entry.getValue(), zh.get(key).getAsString());
        }
    }

    @Test
    void compressionAndMinerRecipesMatchTheSpecifiedMatrices() {
        assertCompression("spirit_iron", "spirit_iron_block", 'I');
        assertCompression("spirit_crystal", "spirit_crystal_block", 'C');
        assertCompression("crude_spirit_iron", "crude_spirit_iron_block", 'R');

        JsonObject miner = resource("data/cultivation/recipe/world_shard_miner.json");
        assertEquals("minecraft:crafting_shaped", miner.get("type").getAsString());
        assertEquals(List.of("NDN", "BEB", "ICI"), strings(miner.getAsJsonArray("pattern")));
        JsonObject key = miner.getAsJsonObject("key");
        assertEquals("minecraft:netherite_ingot", key.getAsJsonObject("N").get("item").getAsString());
        assertEquals("minecraft:dragon_egg", key.getAsJsonObject("D").get("item").getAsString());
        assertEquals("minecraft:diamond_block", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals("minecraft:beacon", key.getAsJsonObject("E").get("item").getAsString());
        assertEquals("cultivation:spirit_iron_block", key.getAsJsonObject("I").get("item").getAsString());
        assertEquals("cultivation:spirit_crystal_block", key.getAsJsonObject("C").get("item").getAsString());
        assertResult(miner, "cultivation:world_shard_miner", 1);

        JsonObject blast = resource("data/cultivation/recipe/spirit_iron_from_crude_blasting.json");
        assertEquals("minecraft:blasting", blast.get("type").getAsString());
        assertEquals("c:raw_materials/spirit_iron",
                blast.getAsJsonObject("ingredient").get("tag").getAsString());
        assertResult(blast, "cultivation:spirit_iron", 1);
        JsonObject immortalFurnace = resource(
                "data/cultivation/recipe/crude_spirit_iron_immortal_furnace.json");
        assertEquals("cultivation:immortal_furnace", immortalFurnace.get("type").getAsString());
        assertEquals("cultivation:crude_spirit_iron",
                immortalFurnace.getAsJsonObject("ingredient").get("item").getAsString());
        assertResult(immortalFurnace, "cultivation:spirit_iron", 1);
        assertMissing("data/cultivation/recipe/crude_spirit_iron_smelting.json");
        assertMissing("data/cultivation/recipe/spirit_iron_ore_smelting.json");
        assertMissing("data/cultivation/recipe/deepslate_spirit_iron_ore_smelting.json");
        assertOreCooking("spirit_iron_ore_blasting", "minecraft:blasting", 100);
        assertOreCooking("deepslate_spirit_iron_ore_blasting", "minecraft:blasting", 100);
        assertOreCooking("spirit_iron_ore_immortal_furnace", "cultivation:immortal_furnace", 50);
        assertOreCooking("deepslate_spirit_iron_ore_immortal_furnace", "cultivation:immortal_furnace", 50);
    }

    private void assertOreCooking(String recipe, String type, int cookingTime) {
        JsonObject json = resource("data/cultivation/recipe/" + recipe + ".json");
        assertEquals(type, json.get("type").getAsString());
        assertEquals("cultivation:spirit_iron_ores", json.get("group").getAsString());
        assertEquals(cookingTime, json.get("cookingtime").getAsInt());
        assertEquals(0.7D, json.get("experience").getAsDouble());
        assertResult(json, "cultivation:spirit_iron", 1);
    }

    @Test
    void oreLootSupportsSilkTouchFortuneAndExpectedRawDrops() {
        for (Map.Entry<String, String> entry : ORE_DROPS.entrySet()) {
            JsonObject table = resource("data/cultivation/loot_table/blocks/" + entry.getKey() + ".json");
            JsonObject alternatives = table.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("minecraft:alternatives", alternatives.get("type").getAsString());
            JsonArray children = alternatives.getAsJsonArray("children");
            assertEquals(2, children.size());

            JsonObject silk = children.get(0).getAsJsonObject();
            assertEquals("cultivation:" + entry.getKey(), silk.get("name").getAsString());
            assertTrue(silk.toString().contains("minecraft:silk_touch"));

            JsonObject normal = children.get(1).getAsJsonObject();
            assertEquals(entry.getValue(), normal.get("name").getAsString());
            assertTrue(normal.toString().contains("minecraft:fortune"));
            assertTrue(normal.toString().contains("minecraft:ore_drops"));
            assertTrue(normal.toString().contains("minecraft:explosion_decay"));
        }

        for (String id : List.of("spirit_iron_block", "spirit_crystal_block",
                "crude_spirit_iron_block", "world_shard_miner")) {
            JsonObject table = resource("data/cultivation/loot_table/blocks/" + id + ".json");
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            assertTrue(pool.toString().contains("minecraft:survives_explosion"));
            assertEquals("cultivation:" + id,
                    pool.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        }
    }

    @Test
    void miningAndCommonTagsExposeTheFullCompatibilitySurface() {
        Set<String> pickaxe = values("data/minecraft/tags/block/mineable/pickaxe.json");
        assertTrue(pickaxe.containsAll(ORES));
        assertTrue(pickaxe.containsAll(Set.of(
                "cultivation:spirit_iron_block",
                "cultivation:spirit_crystal_block",
                "cultivation:crude_spirit_iron_block",
                "cultivation:world_shard_miner")));

        Set<String> diamond = values("data/minecraft/tags/block/needs_diamond_tool.json");
        Set<String> iron = values("data/minecraft/tags/block/needs_iron_tool.json");
        Set<String> stone = values("data/minecraft/tags/block/needs_stone_tool.json");
        assertTrue(diamond.containsAll(ORES));
        assertFalse(iron.stream().anyMatch(ORES::contains));
        assertTrue(stone.containsAll(Set.of(
                "cultivation:spirit_iron_block", "cultivation:crude_spirit_iron_block")));
        assertFalse(stone.contains("cultivation:spirit_crystal_block"));
        assertTrue(iron.contains("cultivation:world_shard_miner"));

        assertTrue(values("data/c/tags/block/ores.json")
                .containsAll(Set.of("#c:ores/spirit_iron", "#c:ores/spirit_crystal")));
        assertTrue(values("data/c/tags/item/ores.json")
                .containsAll(Set.of("#c:ores/spirit_iron", "#c:ores/spirit_crystal")));
        Set<String> spiritIronOres = Set.of(
                "cultivation:spirit_iron_ore", "cultivation:deepslate_spirit_iron_ore");
        Set<String> spiritCrystalOres = Set.of(
                "cultivation:spirit_crystal_ore", "cultivation:deepslate_spirit_crystal_ore");
        assertTrue(values("data/c/tags/block/ores/spirit_iron.json").containsAll(spiritIronOres));
        assertTrue(values("data/c/tags/item/ores/spirit_iron.json").containsAll(spiritIronOres));
        assertTrue(values("data/c/tags/block/ores/spirit_crystal.json").containsAll(spiritCrystalOres));
        assertTrue(values("data/c/tags/item/ores/spirit_crystal.json").containsAll(spiritCrystalOres));
        assertTrue(values("data/c/tags/block/ores_in_ground/stone.json")
                .containsAll(Set.of("cultivation:spirit_iron_ore", "cultivation:spirit_crystal_ore")));
        assertTrue(values("data/c/tags/block/ores_in_ground/deepslate.json")
                .containsAll(Set.of("cultivation:deepslate_spirit_iron_ore", "cultivation:deepslate_spirit_crystal_ore")));
        assertTrue(values("data/c/tags/item/ores_in_ground/stone.json")
                .containsAll(Set.of("cultivation:spirit_iron_ore", "cultivation:spirit_crystal_ore")));
        assertTrue(values("data/c/tags/item/ores_in_ground/deepslate.json")
                .containsAll(Set.of("cultivation:deepslate_spirit_iron_ore", "cultivation:deepslate_spirit_crystal_ore")));

        Set<String> expectedStorage = Set.of(
                "#c:storage_blocks/spirit_iron",
                "#c:storage_blocks/spirit_crystal",
                "#c:storage_blocks/crude_spirit_iron");
        assertTrue(values("data/c/tags/block/storage_blocks.json").containsAll(expectedStorage));
        assertTrue(values("data/c/tags/item/storage_blocks.json").containsAll(expectedStorage));
        assertEquals(Set.of("cultivation:spirit_iron_block"),
                values("data/c/tags/block/storage_blocks/spirit_iron.json"));
        assertEquals(Set.of("cultivation:spirit_iron_block"),
                values("data/c/tags/item/storage_blocks/spirit_iron.json"));
        assertEquals(Set.of("cultivation:spirit_crystal_block"),
                values("data/c/tags/block/storage_blocks/spirit_crystal.json"));
        assertEquals(Set.of("cultivation:spirit_crystal_block"),
                values("data/c/tags/item/storage_blocks/spirit_crystal.json"));
        assertEquals(Set.of("cultivation:crude_spirit_iron_block"),
                values("data/c/tags/block/storage_blocks/crude_spirit_iron.json"));
        assertEquals(Set.of("cultivation:crude_spirit_iron_block"),
                values("data/c/tags/item/storage_blocks/crude_spirit_iron.json"));
        assertEquals(Set.of("cultivation:spirit_iron"),
                values("data/c/tags/item/ingots/spirit_iron.json"));
        assertEquals(Set.of("cultivation:spirit_crystal"),
                values("data/c/tags/item/gems/spirit_crystal.json"));
        assertEquals(Set.of("cultivation:crude_spirit_iron"),
                values("data/c/tags/item/raw_materials/spirit_iron.json"));
        assertTrue(values("data/c/tags/item/ingots.json").contains("#c:ingots/spirit_iron"));
        assertTrue(values("data/c/tags/item/gems.json").contains("#c:gems/spirit_crystal"));
        assertTrue(values("data/c/tags/item/raw_materials.json").contains("#c:raw_materials/spirit_iron"));
    }

    @Test
    void biomeModifierPlacedFeaturesAndConfiguredFeaturesFormAClosedGraph() {
        JsonObject modifier = resource("data/cultivation/neoforge/biome_modifier/add_spirit_ores.json");
        assertEquals("neoforge:add_features", modifier.get("type").getAsString());
        assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
        assertEquals("underground_ores", modifier.get("step").getAsString());

        for (String namespacedPlaced : strings(modifier.getAsJsonArray("features"))) {
            String placedId = namespacedPlaced.substring("cultivation:".length());
            JsonObject placed = resource("data/cultivation/worldgen/placed_feature/" + placedId + ".json");
            String namespacedConfigured = placed.get("feature").getAsString();
            String configuredId = namespacedConfigured.substring("cultivation:".length());
            JsonObject configured = resource(
                    "data/cultivation/worldgen/configured_feature/" + configuredId + ".json");
            assertEquals("minecraft:ore", configured.get("type").getAsString());
            assertTrue(placed.toString().contains("minecraft:in_square"));
            assertTrue(placed.toString().contains("minecraft:height_range"));
            assertTrue(placed.toString().contains("minecraft:biome"));
            for (JsonElement target : configured.getAsJsonObject("config").getAsJsonArray("targets")) {
                String state = target.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString();
                assertTrue(ORES.contains(state), "configured feature points at unregistered ore " + state);
            }
        }
    }

    private static void assertCompression(String item, String block, char symbol) {
        JsonObject compact = resource("data/cultivation/recipe/" + block + ".json");
        assertEquals("minecraft:crafting_shaped", compact.get("type").getAsString());
        String row = String.valueOf(symbol).repeat(3);
        assertEquals(List.of(row, row, row), strings(compact.getAsJsonArray("pattern")));
        assertEquals("cultivation:" + item,
                compact.getAsJsonObject("key").getAsJsonObject(String.valueOf(symbol)).get("item").getAsString());
        assertResult(compact, "cultivation:" + block, 1);

        JsonObject unpack = resource("data/cultivation/recipe/" + item + "_from_block.json");
        assertEquals("minecraft:crafting_shapeless", unpack.get("type").getAsString());
        assertEquals("cultivation:" + block,
                unpack.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
        assertResult(unpack, "cultivation:" + item, 9);
    }

    private static void assertResult(JsonObject recipe, String id, int count) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(id, result.get("id").getAsString());
        assertEquals(count, result.has("count") ? result.get("count").getAsInt() : 1);
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static Set<String> values(String path) {
        Set<String> result = new HashSet<>();
        try {
            for (URL url : Collections.list(SpiritResourceContractTest.class.getClassLoader().getResources(path))) {
                try (InputStream stream = url.openStream();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    for (JsonElement value : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values")) {
                        result.add(value.isJsonPrimitive()
                                ? value.getAsString()
                                : value.getAsJsonObject().get("id").getAsString());
                    }
                }
            }
        } catch (IOException error) {
            throw new AssertionError("could not read tag resources " + path, error);
        }
        assertFalse(result.isEmpty(), "missing tag resources " + path);
        return result;
    }

    private static JsonObject resource(String path) {
        InputStream stream = SpiritResourceContractTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException error) {
            throw new AssertionError("could not read " + path, error);
        }
    }

    private static void assertMissing(String path) {
        try (InputStream stream = SpiritResourceContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertEquals(null, stream, "obsolete recipe must stay absent: " + path);
        } catch (IOException error) {
            throw new AssertionError("could not close resource " + path, error);
        }
    }

    private static void assertTexture16(String path) throws IOException {
        try (InputStream stream = SpiritResourceContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing texture " + path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, "invalid PNG " + path);
            assertEquals(16, image.getWidth(), path + " width");
            assertEquals(16, image.getHeight(), path + " height");
        }
    }
}
