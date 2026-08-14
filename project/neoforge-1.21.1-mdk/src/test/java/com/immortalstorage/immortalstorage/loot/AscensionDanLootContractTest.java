package com.immortalstorage.immortalstorage.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the data-driven world-treasure loot injection so the Treasure Basin's
 * global-loot-modifier chain (and real chests) keep producing the documented
 * pill/Ascension Dan drops.
 */
final class AscensionDanLootContractTest {

    private static Path resources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "resources"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main resources from "
                + Path.of("").toAbsolutePath());
    }

    private static JsonObject read(String relative) throws IOException {
        Path file = resources().resolve(relative);
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    void ascensionDanIsTwentyFivePercentInEndCityTreasure() throws IOException {
        JsonObject modifier = read("data/immortalstorage/loot_modifiers/ascension_dan_in_end.json");
        assertEquals("immortalstorage:add_item", modifier.get("type").getAsString());
        assertEquals(0.25f, modifier.get("chance").getAsFloat());
        assertEquals("immortalstorage:ascension_dan", modifier.get("item").getAsString());
        String condition = modifier.getAsJsonArray("conditions").get(0).getAsJsonObject()
                .get("loot_table_id").getAsString();
        assertEquals("minecraft:chests/end_city_treasure", condition);
    }

    @Test
    void allPillInjectionTargetsAreRegisteredInTheGlobalList() throws IOException {
        JsonObject global = read("data/neoforge/loot_modifiers/global_loot_modifiers.json");
        String entries = global.getAsJsonArray("entries").toString();
        assertTrue(entries.contains("\"immortalstorage:ascension_dan_in_end\""),
                "ascension dan modifier must be registered");
        assertTrue(entries.contains("\"immortalstorage:breakthrough_pill_in_end\""),
                "breakthrough pill end modifier must be registered");
        assertTrue(entries.contains("\"immortalstorage:breakthrough_pill_in_nether\""),
                "breakthrough pill nether modifier must be registered");
        assertTrue(entries.contains("\"immortalstorage:immortal_pill_in_end\""),
                "immortal pill end modifier must be registered");
        assertTrue(entries.contains("\"immortalstorage:refined_pill_in_village\""),
                "refined pill village modifier must be registered");
    }
}
