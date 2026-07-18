package com.cultivation.cultivation.item;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritStaffVisualResourceTest {
    private static final Path PROJECT = locateProject();
    private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));

    @Test
    void fourModesUseTheApprovedPixelMasksAndNoScreenshotBackground() throws Exception {
        assertMask("spirit_staff_explore", 173, 0, 2, 15, 15);
        assertMask("spirit_staff_wrench", 45, 1, 1, 14, 14);
        assertMask("spirit_staff_pick", 68, 2, 2, 14, 14);
        assertMask("spirit_staff_build", 37, 2, 2, 14, 14);

        assertArrayEquals(
                digest(png("spirit_staff")),
                digest(png("spirit_staff_explore")),
                "the legacy texture alias must remain the Explore palette conversion");

        BufferedImage wrench = png("spirit_staff_wrench");
        assertEquals(0, wrench.getRGB(0, 0) >>> 24);
        assertEquals(0, wrench.getRGB(15, 15) >>> 24);
        for (int y = 0; y < wrench.getHeight(); y++) {
            for (int x = 0; x < wrench.getWidth(); x++) {
                int alpha = wrench.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 255,
                        "the restored wrench must not retain screenshot antialias alpha at " + x + "," + y);
            }
        }
    }

    @Test
    void modelPredicateAndGeneratorUseTheSamePersistentMode() throws IOException {
        String model = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "cultivation", "models", "item", "spirit_staff.json")));
        assertTrue(model.contains("\"cultivation:staff_mode\": 1.0"));
        assertTrue(model.contains("\"cultivation:staff_mode\": 2.0"));
        assertTrue(model.contains("\"cultivation:staff_mode\": 3.0"));
        for (String mode : new String[]{"explore", "wrench", "pick", "build"}) {
            Path modeModel = RESOURCES.resolve(Path.of(
                    "assets", "cultivation", "models", "item", "spirit_staff_" + mode + ".json"));
            assertTrue(Files.isRegularFile(modeModel), modeModel.toString());
            assertTrue(Files.readString(modeModel).contains("cultivation:item/spirit_staff_" + mode));
        }

        String clientSetup = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "java", "com", "cultivation", "cultivation", "client",
                "ClientSetup.java")));
        assertTrue(clientSetup.contains("ItemProperties.register("));
        assertTrue(clientSetup.contains("\"staff_mode\""));
        assertTrue(clientSetup.contains("SpiritStaffItem.getMode(stack)"));

        Path generatorPath = PROJECT.getParent().getParent().resolve(Path.of(
                "tools", "generate_spirit_staff_mode_textures.py"));
        String generator = Files.readString(generatorPath);
        assertTrue(generator.contains("assets/minecraft/textures/item/bundle.png"));
        assertTrue(generator.contains("assets/minecraft/textures/item/netherite_pickaxe.png"));
        assertTrue(generator.contains("assets/minecraft/textures/item/stick.png"));
        assertTrue(generator.contains("recover_wrench_sprite"));
        assertTrue(generator.contains("7ad320575145c4716bafca114dc70d2d1f4546849f441d58926ddd92c7274bd9"));
    }

    private static void assertMask(
            String name, int expectedVisible, int minX, int minY, int maxX, int maxY
    ) throws Exception {
        BufferedImage image = png(name);
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        int visible = 0;
        int actualMinX = 16;
        int actualMinY = 16;
        int actualMaxX = -1;
        int actualMaxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                visible++;
                actualMinX = Math.min(actualMinX, x);
                actualMinY = Math.min(actualMinY, y);
                actualMaxX = Math.max(actualMaxX, x);
                actualMaxY = Math.max(actualMaxY, y);
            }
        }
        assertEquals(expectedVisible, visible, name);
        assertEquals(minX, actualMinX, name);
        assertEquals(minY, actualMinY, name);
        assertEquals(maxX, actualMaxX, name);
        assertEquals(maxY, actualMaxY, name);
    }

    private static BufferedImage png(String name) throws Exception {
        Path path = RESOURCES.resolve(Path.of(
                "assets", "cultivation", "textures", "item", name + ".png"));
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, path.toString());
        return image;
    }

    private static byte[] digest(BufferedImage image) throws Exception {
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
        return digest.digest();
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources")))
                    && Files.isDirectory(current.resolve(Path.of("src", "main", "java")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation project");
    }
}
