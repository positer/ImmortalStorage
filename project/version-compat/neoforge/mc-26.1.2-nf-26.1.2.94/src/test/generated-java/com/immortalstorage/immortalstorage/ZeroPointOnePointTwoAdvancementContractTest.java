package com.immortalstorage.immortalstorage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeroPointOnePointTwoAdvancementContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Path RESOURCES = PROJECT.resolve("src/main/resources");

    @Test
    void newAdvancementBranchUsesRequestedItemsAndCriteria() throws IOException {
        assertAdvancement("immortal_art_remains", "immortalstorage:immortal_art_remains",
                "minecraft:inventory_changed");
        assertAdvancement("immortal_forged_alloy", "immortalstorage:immortal_forged_alloy",
                "minecraft:recipe_crafted");
        assertAdvancement("immortal_master_talisman", "immortalstorage:immortal_master_talisman",
                "minecraft:recipe_crafted");
        assertAdvancement("supreme_stage", "immortalstorage:white_day_thunder",
                "minecraft:immortalstorage_stage_10");
        assertAdvancement("dragon_power", "minecraft:ender_dragon_spawn_egg",
                "minecraft:immortalstorage_primordial_qi_ender_dragon");
    }

    @Test
    void requiredItemsAreNetheriteStyleFireResistant() throws IOException {
        String source = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/ModItems.java"));
        assertTrue(source.matches("(?s).*IMMORTAL_FORGED_ALLOY.*?new Item\\.Properties\\(\\)\\.fireResistant\\(\\).*"));
        assertTrue(source.matches("(?s).*IMMORTAL_MASTER_TALISMAN.*?fireResistant\\(\\).*"));
        assertTrue(source.matches("(?s).*IMMORTAL_ARTIFACT.*?fireResistant\\(\\).*"));
    }

    private static void assertAdvancement(String name, String icon, String trigger) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(RESOURCES.resolve(
                "data/immortalstorage/advancement/" + name + ".json"))).getAsJsonObject();
        assertEquals(icon, root.getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString());
        JsonObject criteria = root.getAsJsonObject("criteria");
        assertTrue(criteria.entrySet().stream().anyMatch(entry ->
                trigger.equals(entry.getValue().getAsJsonObject().get("trigger").getAsString())));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
