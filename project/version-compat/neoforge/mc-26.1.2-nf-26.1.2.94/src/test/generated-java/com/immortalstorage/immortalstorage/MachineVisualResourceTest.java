package com.immortalstorage.immortalstorage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineVisualResourceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void managerUsesTheSourceVeinFrameAndIndependentCoreTextures() throws Exception {
        for (String xianqiaoFace : new String[]{"panel", "side", "top"}) {
            BufferedImage xianqiao = png("assets/immortalstorage/textures/block/xianqiao_interface_" + xianqiaoFace + ".png");
            assertEquals(16, xianqiao.getWidth());
            assertEquals(16, xianqiao.getHeight());
            for (String managerTexture : new String[]{"source_vein_manager_edge",
                    "source_vein_manager_core_empty", "source_vein_manager_core_used",
                    "source_vein_manager_core_full"}) {
                BufferedImage managerImage = png("assets/immortalstorage/textures/block/" + managerTexture + ".png");
                assertEquals(16, managerImage.getWidth());
                assertEquals(16, managerImage.getHeight());
                assertNotEquals(pixelHash(xianqiao), pixelHash(managerImage), managerTexture);
            }
        }

        String model = text("assets/immortalstorage/models/block/source_vein_manager.json");
        assertTrue(model.contains("immortalstorage:block/custom_source_vein"),
                "the manager must inherit the complete source-vein frame model");
        assertFalse(model.contains("\"elements\""),
                "the manager must not carry a second hand-authored frame");
        assertFalse(model.contains("immortalstorage:block/source_vein_manager_core_used"),
                "the manager core is rendered by its block entity renderer");
        assertFalse(model.contains("immortalstorage:block/source_vein_manager_front"));
        assertFalse(model.contains("immortalstorage:block/xianqiao_interface_"));
    }

    @Test
    void managerCageUsesLayeredFrameAndTranslucentCoreTextures() throws Exception {
        BufferedImage edge = png("assets/immortalstorage/textures/block/source_vein_manager_edge.png");
        BufferedImage sourceFrame = png("assets/immortalstorage/textures/block/source_vein_frame.png");
        assertEquals(16, edge.getWidth());
        assertEquals(16, edge.getHeight());
        assertEquals(pixelHash(sourceFrame), pixelHash(edge),
                "the legacy manager edge texture must stay pixel-identical to the source frame");
        Set<Integer> edgeColours = new HashSet<>();
        for (int y = 0; y < edge.getHeight(); y++) {
            for (int x = 0; x < edge.getWidth(); x++) {
                int argb = edge.getRGB(x, y);
                if ((argb >>> 24) != 0) edgeColours.add(argb);
            }
        }
        assertTrue(edgeColours.size() >= 3, "the manager edge must contain layered pixel colours");
        assertTrue((edge.getRGB(0, 0) >>> 24) > 0, "the outer frame corner must remain visible");
        for (int y = 0; y < edge.getHeight(); y++) {
            for (int x = 0; x < edge.getWidth(); x++) {
                assertEquals(edge.getRGB(x, y), edge.getRGB(edge.getHeight() - 1 - y, x),
                        "the manager atlas must be 90-degree centre-symmetric");
            }
        }
        for (String name : new String[]{"source_vein_manager_core_empty", "source_vein_manager_core_used",
                "source_vein_manager_core_full"}) {
            BufferedImage image = png("assets/immortalstorage/textures/block/" + name + ".png");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            Set<Integer> coreColours = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    assertTrue((argb >>> 24) > 0 && (argb >>> 24) < 255,
                            name + " must stay translucent");
                    coreColours.add(argb);
                }
            }
            assertTrue(coreColours.size() > 1, name + " must contain crystal-like shading");
        }

        String blockModel = text("assets/immortalstorage/models/block/source_vein_manager.json");
        assertTrue(blockModel.contains("\"parent\": \"immortalstorage:block/custom_source_vein\""));
        assertFalse(blockModel.contains("\"elements\""),
                "the source block owns the complete manager frame geometry");
        assertFalse(blockModel.contains("source_vein_manager_front"));

        String itemModel = text("assets/immortalstorage/models/item/source_vein_manager.json");
        assertTrue(itemModel.contains("\"parent\": \"immortalstorage:block/source_vein_manager\""));
        assertFalse(itemModel.contains("\"overrides\""),
                "the rotating core comes from the item BEWLR, not model overrides");
    }

    @Test
    void managerFrameUvComesFromTheSourceModelWithoutASecondUvIslandSet() throws Exception {
        JsonObject model = JsonParser.parseString(
                text("assets/immortalstorage/models/block/source_vein_manager.json")).getAsJsonObject();
        assertEquals("immortalstorage:block/custom_source_vein", model.get("parent").getAsString());
        assertFalse(model.has("elements"),
                "the manager must inherit the source model's point-to-point UV mapping unchanged");
    }

    @Test
    void immortalFurnaceIsTheLockedCyanWhiteRuntimeSmokerPaletteConversion() throws Exception {
        String blockstate = text("assets/immortalstorage/blockstates/immortal_furnace.json");
        assertTrue(blockstate.contains("facing=north,lit=false"));
        assertTrue(blockstate.contains("facing=west,lit=true"));
        assertTrue(blockstate.contains("immortalstorage:block/immortal_furnace_on"));

        for (String suffix : new String[]{"", "_on"}) {
            String model = text("assets/immortalstorage/models/block/immortal_furnace" + suffix + ".json");
            assertTrue(model.contains("\"parent\": \"minecraft:block/orientable_with_bottom\""));
            assertTrue(model.contains("immortalstorage:block/immortal_furnace_bottom"));
            assertTrue(model.contains("immortalstorage:block/immortal_furnace_side"));
            assertTrue(model.contains("immortalstorage:block/immortal_furnace_top"));
            assertTrue(model.contains("immortalstorage:block/immortal_furnace_front"));
            assertFalse(model.contains("\"particle\""), "the model must be the runtime smoker structure without custom geometry");
        }

        Map<String, String> lockedPixelDigests = Map.of(
                "front", "71a97cf4a53e50e5c38c6a6cd07898e6a2c67a34285ca4c228c5f6ff0298583f",
                "front_on", "568b8321f0a394745e04326760414ccdfa09756b876eae70eab0565233492c6a",
                "top", "9ff22a4a00387a6f6a350a7ed53e39ddef8cc4263cff572684974c681baa4f69",
                "side", "f6be9d9c10f39b894260d02deb69652e7abcab08571eef41c104a3092d8c2e1a",
                "bottom", "bf5ea10531b174003e7484b41db560779b74225bade6f4dc6c29cbf2dc62c7e3"
        );
        for (Map.Entry<String, String> entry : lockedPixelDigests.entrySet()) {
            String name = entry.getKey();
            BufferedImage image = png("assets/immortalstorage/textures/block/immortal_furnace_" + name + ".png");
            assertEquals(16, image.getWidth());
            assertEquals(name.equals("front_on") ? 48 : 16, image.getHeight(), name);
            assertEquals(entry.getValue(), rgbaDigest(image), name + " must only change via the runtime-smoker palette tool");
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = argb >>> 24;
                    int red = argb >>> 16 & 0xFF;
                    int green = argb >>> 8 & 0xFF;
                    int blue = argb & 0xFF;
                    assertEquals(255, alpha, name + " must preserve the smoker's opaque geometry");
                    assertTrue(green >= red && blue >= red,
                            name + " must stay inside the cyan-white palette");
                    assertTrue(Math.abs(green - blue) <= 12,
                            name + " must not retain the smoker's brown/orange palette");
                }
            }
        }
        assertNotEquals(pixelHash(png("assets/immortalstorage/textures/block/immortal_furnace_front.png")),
                pixelHash(png("assets/immortalstorage/textures/block/immortal_furnace_front_on.png")));

        String animation = text("assets/immortalstorage/textures/block/immortal_furnace_front_on.png.mcmeta");
        assertTrue(animation.contains("\"frametime\": 4"), "the three runtime smoker flame frames must animate");
        assertTrue(animation.contains("\"interpolate\": false"), "the smoker flame remains hard pixel art");
    }

    @Test
    void immortalSageUsesTransparentIndependentWhiteRobeLayers() throws Exception {
        for (String kind : new String[]{"villager", "zombie_villager"}) {
            BufferedImage image = png("assets/immortalstorage/textures/entity/" + kind
                    + "/profession/immortal_sage.png");
            assertEquals(64, image.getWidth());
            assertEquals(64, image.getHeight());
            assertEquals(0, image.getRGB(63, 63) >>> 24, "unused skin pixels remain transparent");
            assertTrue((image.getRGB(10, 45) >>> 24) > 0, "robe UV must be populated");
            String metadata = text("assets/immortalstorage/textures/entity/" + kind
                    + "/profession/immortal_sage.png.mcmeta");
            assertTrue(metadata.contains("\"hat\": \"none\""));
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static BufferedImage png(String path) throws Exception {
        try (InputStream stream = MachineVisualResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path);
            return image;
        }
    }

    private static String text(String path) throws Exception {
        try (InputStream stream = MachineVisualResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int pixelHash(BufferedImage image) {
        int result = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                result = 31 * result + image.getRGB(x, y);
            }
        }
        return result;
    }

    private static String rgbaDigest(BufferedImage image) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                digest.update((byte) (argb >>> 16));
                digest.update((byte) (argb >>> 8));
                digest.update((byte) argb);
                digest.update((byte) (argb >>> 24));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
