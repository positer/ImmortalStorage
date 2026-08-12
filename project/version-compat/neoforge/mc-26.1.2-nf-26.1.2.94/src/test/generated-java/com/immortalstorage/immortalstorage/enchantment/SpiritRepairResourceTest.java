package com.immortalstorage.immortalstorage.enchantment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritRepairResourceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void enchantmentIsDataDrivenTradeableAndCraftedOntoBothTools() throws Exception {
        JsonObject enchantment = json("data/immortalstorage/enchantment/spirit_repair.json");
        assertEquals(1, enchantment.get("max_level").getAsInt());
        assertEquals("#minecraft:enchantable/durability",
                enchantment.get("supported_items").getAsString());
        assertTagContains("tradeable");
        assertTagContains("treasure");
        assertCraftResult("spirit_sword");
        assertCraftResult("spirit_staff");
        assertMainSourceContains("item/ModCreativeTabs.java", "EnchantmentHelper.createBook");
        assertMainSourceContains("villager/ModTrades.java", "trade_set");
        assertMainSourceContains("enchantment/SpiritRepairService.java", "total / 10L");
    }

    @Test
    void lingqiSaturationHasARealEighteenPixelStatusIcon() throws Exception {
        try (InputStream in = resource("assets/immortalstorage/textures/mob_effect/lingqi_saturation.png")) {
            var image = ImageIO.read(in);
            assertNotNull(image);
            assertEquals(18, image.getWidth());
            assertEquals(18, image.getHeight());
        }
    }

    private static void assertCraftResult(String id) throws Exception {
        JsonObject recipe = json("data/immortalstorage/recipe/" + id + ".json");
        JsonObject levels = recipe.getAsJsonObject("result").getAsJsonObject("components")
                .getAsJsonObject("minecraft:enchantments");
        assertEquals(1, levels.get("immortalstorage:spirit_repair").getAsInt());
    }

    private static void assertTagContains(String tag) throws Exception {
        String path = "data/minecraft/tags/enchantment/" + tag + ".json";
        boolean found = false;
        for (var url : Collections.list(SpiritRepairResourceTest.class.getClassLoader().getResources(path))) {
            try (InputStream in = url.openStream();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                found |= json.getAsJsonArray("values").asList().stream()
                        .anyMatch(value -> "immortalstorage:spirit_repair".equals(value.getAsString()));
            }
        }
        assertTrue(found, path);
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = resource(path);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream in = SpiritRepairResourceTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(in, path);
        return in;
    }

    private static void assertMainSourceContains(String relative, String needle) throws Exception {
        java.nio.file.Path current = java.nio.file.Path.of("").toAbsolutePath();
        while (current != null) {
            java.nio.file.Path root = current.resolve(java.nio.file.Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (java.nio.file.Files.isDirectory(root)) {
                assertTrue(java.nio.file.Files.readString(root.resolve(relative)).contains(needle));
                return;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main sources");
    }
}
