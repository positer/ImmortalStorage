package com.immortalstorage.immortalstorage.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnergyCrystalTextureAssetTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void runtimeCrystalUsesAFullFaceMaterialAtEightyPercentOpacity() throws IOException {
        BufferedImage image = ImageIO.read(resources().resolve(Path.of(
                "assets", "immortalstorage", "textures", "block", "energy_crystal_crystal.png")).toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertCrystalPaletteAndAlpha(image);
    }

    @Test
    void runtimeCrystalMaterialCoversEveryCuboidFaceWithoutChangingTheBlockbenchGeometry() throws IOException {
        Path modelPath = resources().resolve(Path.of(
                "assets", "immortalstorage", "models", "block", "energy_crystal.json"));
        JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
        JsonArray elements = model.getAsJsonArray("elements");
        assertEquals(6, elements.size());
        assertEquals("minecraft:translucent", model.get("render_type").getAsString());
        assertElementTexture(elements.get(0).getAsJsonObject(), new int[]{5, 0, 5}, new int[]{11, 2, 11}, "#base");

        int[][] crystalFrom = {
                {6, 1, 6}, {6, 2, 7}, {7, 2, 6}, {8, 2, 7}, {7, 2, 8}
        };
        int[][] crystalTo = {
                {10, 12, 10}, {8, 10, 9}, {9, 10, 8}, {10, 10, 9}, {9, 10, 10}
        };
        for (int index = 0; index < crystalFrom.length; index++) {
            assertElementTexture(elements.get(index + 1).getAsJsonObject(),
                    crystalFrom[index], crystalTo[index], "#crystal");
        }
    }

    @Test
    void redrawScriptAdaptsTheCrystalTopFaceToItsPartitionedUvIslands() throws IOException {
        String script = Files.readString(workspace().resolve(Path.of(
                "tools", "redraw_requested_crystal_and_frames.py")));
        assertTrue(script.contains("CRYSTAL_TOP_UV_ISLANDS"));
        assertTrue(script.contains("(4.0, 7.5, 2.0, 5.5)"), "main crystal top UV orientation must stay mapped");
        assertTrue(script.contains("(2.0, 10.0, 0.0, 8.0)"), "crossed crystal top UV orientation must stay mapped");
        assertTrue(script.contains("paste_uv_island"));
        assertTrue(script.contains("reference_width * 0.18"), "the cap uses a fitted reference region");
    }

    private static void assertElementTexture(JsonObject element, int[] from, int[] to, String texture) {
        assertArrayEquals(from, element);
        assertArrayEquals(to, element, "to");
        JsonObject faces = element.getAsJsonObject("faces");
        for (String faceName : new String[]{"down", "up", "north", "south", "west", "east"}) {
            JsonObject face = faces.getAsJsonObject(faceName);
            assertEquals(texture, face.get("texture").getAsString(), faceName);
            assertTrue(!face.has("uv"), "the existing Blockbench face UV mapping must stay unchanged");
        }
    }

    private static void assertCrystalPaletteAndAlpha(BufferedImage image) {
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                assertEquals(204, alpha, "every Cristal pixel must remain 80% opaque");
                colours.add(argb & 0x00FFFFFF);
            }
        }
        assertTrue(colours.size() >= 5, "crystal facets must use a layered cyan/blue palette");
    }

    private static void assertArrayEquals(int[] expected, JsonObject object) {
        assertArrayEquals(expected, object, "from");
    }

    private static void assertArrayEquals(int[] expected, JsonObject object, String key) {
        JsonArray actual = object.getAsJsonArray(key);
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i).getAsInt());
        }
    }

    private static Path resources() {
        return workspace().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main", "resources"));
    }

    private static Path workspace() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("project"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage workspace");
    }
}
