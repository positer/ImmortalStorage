package com.immortalstorage.immortalstorage.client.render;

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

final class XianqiaoManagerAssetTest {
    private static final Path RESOURCES = locateResources();

    @Test
    void managerFrameUsesLayeredOpaqueBorderTexture() throws IOException {
        BufferedImage image = ImageIO.read(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "textures", "block", "xianqiao_manager_frame.png")).toFile());

        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                assertEquals(255, argb >>> 24,
                        "the manager border must remain an opaque Minecraft-style frame");
                colours.add(argb);
            }
        }
        assertTrue(colours.size() >= 3, "the manager border must contain layered pixel colours");
        assertTrue(colours.stream().anyMatch(argb -> argb != 0xFF808080),
                "the manager border must no longer be a flat gray placeholder");
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(image.getRGB(x, y), image.getRGB(image.getHeight() - 1 - y, x),
                        "the manager border must be 90-degree centre-symmetric");
            }
        }
    }

    @Test
    void itemModelAloneRestoresTheNormalBlockGuiScale() throws IOException {
        String itemModel = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "item", "xianqiao_manager.json")));
        String blockModel = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "block", "xianqiao_manager.json")));

        assertTrue(itemModel.contains("\"scale\":[0.625,0.625,0.625]"),
                "only the inventory transform should use Minecraft's normal block-item scale");
        assertTrue(blockModel.contains("\"parent\":\"immortalstorage:block/arcane_machine_frame\""),
                "the placed block must keep the full-size world frame model");
    }

    @Test
    void coreUsesAnExistingVanillaTextureInsteadOfTheMissingMiscWhitePixel() throws IOException {
        String renderer = Files.readString(locateProject().resolve(Path.of(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "FloatingCubeRenderer.java")));
        assertTrue(renderer.contains("textures/block/white_concrete.png"));
        assertTrue(!renderer.contains("textures/misc/white.png"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }

    private static Path locateResources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "resources"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage resources");
    }
}
