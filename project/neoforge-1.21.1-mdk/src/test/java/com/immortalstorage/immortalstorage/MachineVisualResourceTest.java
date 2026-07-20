package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineVisualResourceTest {
    @Test
    void interfaceAndSourceManagerUseIndependentNativePixelTextures() throws Exception {
        for (String face : new String[]{"front", "side", "top"}) {
            BufferedImage xianqiao = png("assets/immortalstorage/textures/block/xianqiao_interface_" + face + ".png");
            BufferedImage sourceManager = png("assets/immortalstorage/textures/block/source_vein_manager_" + face + ".png");
            assertEquals(16, xianqiao.getWidth());
            assertEquals(16, xianqiao.getHeight());
            assertEquals(16, sourceManager.getWidth());
            assertEquals(16, sourceManager.getHeight());
            assertNotEquals(pixelHash(xianqiao), pixelHash(sourceManager), face);
        }

        String model = text("assets/immortalstorage/models/block/source_vein_manager.json");
        assertTrue(model.contains("immortalstorage:block/source_vein_manager_front"));
        assertFalse(model.contains("immortalstorage:block/xianqiao_interface_"));
    }

    @Test
    void sourceManagerIsAnOpaqueNeutralBookshelfCubeWithoutBakedMembersOrPerspective() throws Exception {
        for (String face : new String[]{"front", "side", "top"}) {
            BufferedImage image = png("assets/immortalstorage/textures/block/source_vein_manager_" + face + ".png");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int red = argb >>> 16 & 0xFF;
                    int green = argb >>> 8 & 0xFF;
                    int blue = argb & 0xFF;
                    assertEquals(255, argb >>> 24, face + " must remain an opaque full-cube face");
                    assertEquals(red, green, face + " must not bake a colored source member");
                    assertEquals(red, blue, face + " must use only the neutral black/gray frame palette");
                }
            }
        }

        BufferedImage front = png("assets/immortalstorage/textures/block/source_vein_manager_front.png");
        int emptyBay = front.getRGB(3, 4);
        for (int y : new int[]{4, 11}) {
            for (int x : new int[]{3, 7, 12}) {
                assertEquals(emptyBay, front.getRGB(x, y),
                        "all six bookshelf bays must stay empty in the baked texture");
            }
        }
        assertNotEquals(emptyBay, front.getRGB(7, 7), "the central shelf must remain structural");

        String blockModel = text("assets/immortalstorage/models/block/source_vein_manager.json");
        assertTrue(blockModel.contains("\"parent\": \"minecraft:block/cube\""));
        assertFalse(blockModel.contains("\"elements\""), "no pre-baked perspective machine geometry");
        assertFalse(blockModel.contains("\"display\""), "block model must not carry an item-only preview transform");

        String itemModel = text("assets/immortalstorage/models/item/source_vein_manager.json");
        assertTrue(itemModel.contains("\"parent\": \"immortalstorage:block/source_vein_manager\""));
        assertFalse(itemModel.contains("\"overrides\""), "the inventory model only shows the manager body");
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
