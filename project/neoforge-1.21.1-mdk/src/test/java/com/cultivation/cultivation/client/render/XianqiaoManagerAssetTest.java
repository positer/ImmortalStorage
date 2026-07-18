package com.cultivation.cultivation.client.render;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoManagerAssetTest {
    private static final Path RESOURCES = locateResources();

    @Test
    void managerFrameUsesExactlyOneOpaquePureGrayMaterial() throws IOException {
        BufferedImage image = ImageIO.read(RESOURCES.resolve(Path.of(
                "assets", "cultivation", "textures", "block", "xianqiao_manager_frame.png")).toFile());

        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                assertEquals(0xFF808080, argb,
                        "the manager border must be uniform pure gray, without a custom metal ramp");
            }
        }
    }

    @Test
    void itemModelAloneRestoresTheNormalBlockGuiScale() throws IOException {
        String itemModel = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "cultivation", "models", "item", "xianqiao_manager.json")));
        String blockModel = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "cultivation", "models", "block", "xianqiao_manager.json")));

        assertTrue(itemModel.contains("\"scale\":[0.625,0.625,0.625]"),
                "only the inventory transform should use Minecraft's normal block-item scale");
        assertTrue(blockModel.contains("\"parent\":\"cultivation:block/arcane_machine_frame\""),
                "the placed block must keep the full-size world frame model");
    }

    private static Path locateResources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "resources"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation resources");
    }
}
