package com.immortalstorage.immortalstorage.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TargetTreasureBasinModelContractTest {
    @Test
    void bothVersionsUseTheExactVanillaCauldronModelWithGoldenTextures() throws Exception {
        JsonObject canonical = JsonParser.parseString(Files.readString(canonicalModel())).getAsJsonObject();
        JsonObject target = JsonParser.parseString(Files.readString(targetModel())).getAsJsonObject();

        assertEquals("minecraft:block/cauldron", canonical.get("parent").getAsString());
        assertTrue(canonical.get("ambientocclusion").getAsBoolean(),
                "both versions must retain vanilla cauldron depth lighting");
        assertEquals(canonical, target, "both generations must use the same cauldron model contract");
        assertEquals(canonical.getAsJsonObject("textures"), target.getAsJsonObject("textures"));
        assertEquals("immortalstorage:block/treasure_basin",
                canonical.getAsJsonObject("textures").get("side").getAsString());
        assertEquals("immortalstorage:block/treasure_basin_top",
                canonical.getAsJsonObject("textures").get("top").getAsString());
        assertEquals("immortalstorage:block/treasure_basin_inner",
                canonical.getAsJsonObject("textures").get("inside").getAsString());
        assertEquals("immortalstorage:block/treasure_basin_bottom",
                canonical.getAsJsonObject("textures").get("bottom").getAsString());
        assertTenPixelBottomCenteredBounds(canonical);
        assertCollisionMatchesModelBounds();
        assertBothVersionsEmitFifteenBlockLight();

        assertGoldenCauldronTexture("treasure_basin.png", 0);
        assertGoldenCauldronTexture("treasure_basin_top.png", 100);
        assertGoldenCauldronTexture("treasure_basin_inner.png", 0);
        assertGoldenCauldronTexture("treasure_basin_bottom.png", 172);
    }

    private static void assertCollisionMatchesModelBounds() throws Exception {
        Path canonicalBlock = root().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main",
                "java", "com", "immortalstorage", "immortalstorage", "block", "custom",
                "TreasureBasinBlock.java"));
        Path targetBlock = root().resolve(Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java", "com",
                "immortalstorage", "immortalstorage", "block", "custom", "TreasureBasinBlock.java"));
        for (Path source : java.util.List.of(canonicalBlock, targetBlock)) {
            String block = Files.readString(source);
            assertTrue(block.contains("box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D)"));
            assertTrue(block.contains("protected VoxelShape getShape("));
            assertTrue(block.contains("protected VoxelShape getCollisionShape("));
        }
    }

    private static void assertBothVersionsEmitFifteenBlockLight() throws Exception {
        Path canonicalRegistration = root().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main",
                "java", "com", "immortalstorage", "immortalstorage", "block", "ModBlocks.java"));
        Path targetRegistration = root().resolve(Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java", "com",
                "immortalstorage", "immortalstorage", "block", "ModBlocks.java"));
        for (Path source : java.util.List.of(canonicalRegistration, targetRegistration)) {
            String registrations = Files.readString(source);
            int start = registrations.indexOf("TREASURE_BASIN = reg");
            int end = registrations.indexOf("TRUE_YUAN_LIGHT =", start);
            assertTrue(start >= 0 && end > start);
            assertTrue(registrations.substring(start, end).contains("lightLevel(state -> 15)"),
                    "both supported versions must emit block light level fifteen");
        }
    }

    private static void assertTenPixelBottomCenteredBounds(JsonObject model) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (var element : model.getAsJsonArray("elements")) {
            JsonObject object = element.getAsJsonObject();
            JsonArray from = object.getAsJsonArray("from");
            JsonArray to = object.getAsJsonArray("to");
            minX = Math.min(minX, from.get(0).getAsDouble());
            minY = Math.min(minY, from.get(1).getAsDouble());
            minZ = Math.min(minZ, from.get(2).getAsDouble());
            maxX = Math.max(maxX, to.get(0).getAsDouble());
            maxY = Math.max(maxY, to.get(1).getAsDouble());
            maxZ = Math.max(maxZ, to.get(2).getAsDouble());
            assertTrue(!object.toString().contains("cullface"),
                    "inset basin faces must not disappear beside full blocks");
        }
        assertEquals(3.0, minX);
        assertEquals(0.0, minY);
        assertEquals(3.0, minZ);
        assertEquals(13.0, maxX);
        assertEquals(10.0, maxY);
        assertEquals(13.0, maxZ);
    }

    @Test
    void itemUsesThreeDimensionalBasinWithVanillaBlockDisplayTransforms() throws Exception {
        JsonObject item = JsonParser.parseString(Files.readString(canonicalItemModel())).getAsJsonObject();
        assertEquals("immortalstorage:block/treasure_basin", item.get("parent").getAsString());
        assertEquals("side", item.get("gui_light").getAsString());

        JsonObject display = item.getAsJsonObject("display");
        assertTransform(display, "gui", "[30,225,0]", "[0.625,0.625,0.625]");
        assertTransform(display, "thirdperson_righthand", "[75,45,0]", "[0.375,0.375,0.375]");
        assertTransform(display, "firstperson_righthand", "[0,45,0]", "[0.4,0.4,0.4]");
        assertTransform(display, "firstperson_lefthand", "[0,225,0]", "[0.4,0.4,0.4]");

        JsonObject definition = JsonParser.parseString(Files.readString(targetItemDefinition())).getAsJsonObject();
        assertEquals("immortalstorage:item/treasure_basin",
                definition.getAsJsonObject("model").get("model").getAsString());
    }

    private static void assertTransform(JsonObject display, String context, String rotation, String scale) {
        JsonObject transform = display.getAsJsonObject(context);
        assertEquals(rotation, transform.getAsJsonArray("rotation").toString());
        assertEquals(scale, transform.getAsJsonArray("scale").toString());
    }

    private static void assertGoldenCauldronTexture(String fileName, int transparentPixels) throws Exception {
        BufferedImage image = ImageIO.read(textureRoot().resolve(fileName).toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        int transparent = 0;
        int minimumRed = 255;
        int minimumGreen = 255;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    transparent++;
                    continue;
                }
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                minimumRed = Math.min(minimumRed, red);
                minimumGreen = Math.min(minimumGreen, green);
                assertTrue(red > green && green > blue,
                        fileName + " must be a gold-only recolor at " + x + "," + y);
            }
        }
        assertEquals(transparentPixels, transparent,
                fileName + " must preserve the vanilla cauldron alpha mask");
        assertTrue(minimumRed >= 112 && minimumGreen >= 64,
                fileName + " must remain visibly golden under world block shading");
    }

    private static Path targetModel() {
        return root().resolve(Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "resources", "assets",
                "immortalstorage", "models", "block", "treasure_basin.json"));
    }

    private static Path canonicalModel() {
        return root().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main",
                "resources", "assets", "immortalstorage", "models", "block",
                "treasure_basin.json"));
    }

    private static Path canonicalItemModel() {
        return root().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main",
                "resources", "assets", "immortalstorage", "models", "item",
                "treasure_basin.json"));
    }

    private static Path targetItemDefinition() {
        return root().resolve(Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "resources", "assets",
                "immortalstorage", "items", "treasure_basin.json"));
    }

    private static Path textureRoot() {
        return root().resolve(Path.of("project", "neoforge-1.21.1-mdk", "src", "main",
                "resources", "assets", "immortalstorage", "textures", "block"));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("project"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate workspace root");
    }
}
