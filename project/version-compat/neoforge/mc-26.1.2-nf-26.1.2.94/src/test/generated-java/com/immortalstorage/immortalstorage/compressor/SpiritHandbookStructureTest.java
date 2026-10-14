package com.immortalstorage.immortalstorage.compressor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

final class SpiritHandbookStructureTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private JsonObject entry(String locale) throws Exception {
        String path = "/assets/immortalstorage/patchouli_books/jade_guide/" + locale + "/entries/automation/spirit_compressor.json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void handbookProjectionMatchesTheRealTwentySevenCellStructureInBothLanguages() throws Exception {
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            var pages = entry(locale).getAsJsonArray("pages");
            assertEquals(12, pages.size());
            for (int mode = 0; mode < 2; mode++) {
                var page = pages.get(mode + 1).getAsJsonObject();
                assertEquals("patchouli:multiblock", page.get("type").getAsString());
                assertTrue(page.get("enable_visualize").getAsBoolean());
                var shape = page.getAsJsonObject("multiblock"); var pattern = shape.getAsJsonArray("pattern");
                var mapping = shape.getAsJsonObject("mapping"); int filled = 0;
                for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    // Patchouli DenseMultiblock.build stores serialized layer i at height - i - 1.
                    String row = pattern.get(1 - y).getAsJsonArray().get(x + 1).getAsString();
                    String actual = mapping.get(String.valueOf(row.charAt(z + 1))).getAsString().split("\\[")[0];
                    if (actual.equals("minecraft:iron_chain")) actual = "minecraft:chain";
                    assertEquals(SpiritCompressorStructure.blockId(x, y, z), actual, "Projection must not invert top/bottom or move the core");
                    if (actual.equals("minecraft:chain")) {
                        String axis = y == 0 ? "y" : x == 0 ? "x" : "z";
                        assertTrue(mapping.get(String.valueOf(row.charAt(z + 1))).getAsString().contains("axis=" + axis),
                                "Chain orientation must match Buildings fixture at " + x + "," + y + "," + z);
                    }
                    if (!actual.equals("minecraft:air")) filled++;
                }
                assertEquals(23, filled);
                assertEquals("immortalstorage:world_shard_miner", mapping.get("0").getAsString());
                assertEquals('0', pattern.get(2).getAsJsonArray().get(1).getAsString().charAt(1));
                assertTrue(mapping.get("R").getAsString().endsWith("[reversed=" + (mode == 1) + "]"));
            }
        }
    }
    @Test void layersAreCompleteAndCannotProjectAStandalonePartialStructure() throws Exception {
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            var pages = entry(locale).getAsJsonArray("pages");
            String[] centre = {"immortalstorage:world_shard_miner", "immortalstorage:miniature_immortal_ruin_block", "immortalstorage:treasure_basin"};
            for (int i = 0; i < 3; i++) {
                var page = pages.get(i + 3).getAsJsonObject(); assertFalse(page.get("enable_visualize").getAsBoolean());
                var shape = page.getAsJsonObject("multiblock"); var pattern = shape.getAsJsonArray("pattern");
                assertEquals(1, pattern.size()); assertEquals(3, pattern.get(0).getAsJsonArray().size());
                assertEquals(centre[i], shape.getAsJsonObject("mapping").get("0").getAsString());
            }
        }
    }
}
