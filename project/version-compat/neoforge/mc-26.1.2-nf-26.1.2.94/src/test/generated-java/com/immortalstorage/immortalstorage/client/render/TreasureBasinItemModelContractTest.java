package com.immortalstorage.immortalstorage.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinItemModelContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void basinKeepsItsThreeDimensionalModelAtVanillaBlockItemScale() throws Exception {
        Path modelPath = resourceRoot().resolve(Path.of(
                "immortalstorage", "models", "item", "treasure_basin.json"));
        JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
        assertEquals("immortalstorage:block/treasure_basin", model.get("parent").getAsString());
        assertEquals("side", model.get("gui_light").getAsString());
        JsonObject display = model.getAsJsonObject("display");
        assertTransform(display, "gui", "[30,225,0]", "[0.625,0.625,0.625]");
        assertTransform(display, "thirdperson_righthand", "[75,45,0]", "[0.375,0.375,0.375]");
        assertTransform(display, "firstperson_righthand", "[0,45,0]", "[0.4,0.4,0.4]");
        assertTransform(display, "firstperson_lefthand", "[0,225,0]", "[0.4,0.4,0.4]");
    }

    @Test
    void worldModelIsTenPixelsWideAndBottomCentered() throws Exception {
        Path modelPath = resourceRoot().resolve(Path.of(
                "immortalstorage", "models", "block", "treasure_basin.json"));
        JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
        assertTrue(model.get("ambientocclusion").getAsBoolean(),
                "the inset golden cauldron must retain vanilla layered lighting");
        double[] minimum = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] maximum = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (var element : model.getAsJsonArray("elements")) {
            JsonObject object = element.getAsJsonObject();
            JsonArray from = object.getAsJsonArray("from");
            JsonArray to = object.getAsJsonArray("to");
            for (int axis = 0; axis < 3; axis++) {
                minimum[axis] = Math.min(minimum[axis], from.get(axis).getAsDouble());
                maximum[axis] = Math.max(maximum[axis], to.get(axis).getAsDouble());
            }
            assertFalse(object.toString().contains("cullface"));
        }
        assertEquals(3.0, minimum[0]);
        assertEquals(0.0, minimum[1]);
        assertEquals(3.0, minimum[2]);
        assertEquals(13.0, maximum[0]);
        assertEquals(10.0, maximum[1]);
        assertEquals(13.0, maximum[2]);

        String block = Files.readString(locateMdk().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                "immortalstorage", "immortalstorage", "block", "custom", "TreasureBasinBlock.java")));
        assertTrue(block.contains("box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D)"));
        assertTrue(block.contains("protected VoxelShape getShape("));
        assertTrue(block.contains("protected VoxelShape getCollisionShape("));

        String registrations = Files.readString(locateMdk().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                "immortalstorage", "immortalstorage", "block", "ModBlocks.java")));
        String basinRegistration = registrations.substring(
                registrations.indexOf("TREASURE_BASIN = reg"),
                registrations.indexOf("TRUE_YUAN_LIGHT =", registrations.indexOf("TREASURE_BASIN = reg")));
        assertTrue(basinRegistration.contains("lightLevel(state -> 15)"),
                "treasure basin must emit a constant block light level of eight");
    }

    @Test
    void goldenSideTextureKeepsReadableDarkValues() throws Exception {
        var image = ImageIO.read(resourceRoot().resolve(Path.of(
                "immortalstorage", "textures", "block", "treasure_basin.png")).toFile());
        int minimumRed = 255;
        int minimumGreen = 255;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) == 0) continue;
                minimumRed = Math.min(minimumRed, (argb >>> 16) & 0xFF);
                minimumGreen = Math.min(minimumGreen, (argb >>> 8) & 0xFF);
            }
        }
        assertTrue(minimumRed >= 112 && minimumGreen >= 64,
                "world shading must not collapse the gold palette to near-black brown");
    }

    private static void assertTransform(JsonObject display, String context, String rotation, String scale) {
        JsonObject transform = display.getAsJsonObject(context);
        assertEquals(rotation, transform.getAsJsonArray("rotation").toString());
        assertEquals(scale, transform.getAsJsonArray("scale").toString());
    }

    private static Path locateMdk() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "neoforge-1.21.1-mdk");
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isDirectory(candidate)) return candidate;
            if (current.endsWith(marker)) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate NeoForge MDK");
    }

    private static Path resourceRoot() {
        return locateMdk().resolve(Path.of("src", "main", "resources", "assets"));
    }
}
