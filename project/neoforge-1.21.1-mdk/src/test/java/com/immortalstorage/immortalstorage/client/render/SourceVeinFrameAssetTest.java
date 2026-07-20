package com.immortalstorage.immortalstorage.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinFrameAssetTest {
    private static final Path PROJECT = locateProject();
    private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));

    @Test
    void everySourceVeinUsesTheSinglePureBlackFrameAndTheRendererSuppliesIdentity() throws IOException {
        Path models = RESOURCES.resolve(Path.of("assets", "immortalstorage", "models", "block"));
        List<Path> sourceModels;
        try (Stream<Path> paths = Files.list(models)) {
            sourceModels = paths
                    .filter(path -> path.getFileName().toString().endsWith("_vein.json"))
                    .sorted()
                    .toList();
        }
        assertEquals(39, sourceModels.size(), "all registered and generic source models must be audited");

        for (Path modelPath : sourceModels) {
            JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
            assertEquals("immortalstorage:block/arcane_machine_frame", model.get("parent").getAsString(),
                    modelPath.toString());
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals("immortalstorage:block/source_vein_frame", textures.get("frame").getAsString(),
                    modelPath.toString());
            assertEquals("immortalstorage:block/source_vein_frame", textures.get("particle").getAsString(),
                    modelPath.toString());
        }

        Path texture = RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "textures", "block", "source_vein_frame.png"));
        BufferedImage image = ImageIO.read(texture.toFile());
        assertNotNull(image, texture.toString());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0xFF000000, image.getRGB(x, y),
                        "the source cage texture must be flat opaque black at " + x + "," + y);
            }
        }

        String renderer = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage", "client", "render",
                "SourceVeinRenderer.java")));
        assertTrue(renderer.contains("FloatingCubeRenderer.render("),
                "resource identity must remain a real dynamic center core");
        assertTrue(renderer.contains("sampleOutput()"),
                "the dynamic core color must derive from the actual configured output");
        assertTrue(renderer.contains("themeColor(blockEntity)"),
                "every rendered source must supply an identity color");
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
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
