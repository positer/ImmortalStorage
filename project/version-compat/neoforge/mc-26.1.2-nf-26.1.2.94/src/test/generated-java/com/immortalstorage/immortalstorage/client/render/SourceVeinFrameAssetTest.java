package com.immortalstorage.immortalstorage.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinFrameAssetTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));

    @Test
    void everySourceVeinUsesTheLayeredFrameAndTheRuleRendererSuppliesIdentity() throws IOException {
        Path models = RESOURCES.resolve(Path.of("assets", "immortalstorage", "models", "block"));
        List<Path> sourceModels;
        try (Stream<Path> paths = Files.list(models)) {
            sourceModels = paths
                    .filter(path -> path.getFileName().toString().endsWith("_vein.json"))
                    .sorted()
                    .toList();
        }
        assertEquals(40, sourceModels.size(), "all registered and generic source models must be audited");

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
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                assertEquals(255, argb >>> 24,
                        "the source cage frame must stay opaque at " + x + "," + y);
                colours.add(argb);
            }
        }
        assertTrue(colours.size() >= 3, "the source cage frame must use layered native pixel colours");
        assertTrue(colours.stream().anyMatch(argb -> argb != 0xFF000000),
                "the source cage frame must no longer be a flat black placeholder");
        assertRotationallySymmetric(image, "source vein frame");

        String renderer = Files.readString(PROJECT.resolve(Path.of(
                "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "client", "render",
                "SourceVeinRenderer.java")));
        assertTrue(renderer.contains("SourceVeinDisplayRenderer.render(")
                        || renderer.contains("SourceVeinDisplayRenderer.submit("),
                "world source rendering must delegate to the shared rule renderer");
        String ruleRenderer = Files.readString(PROJECT.resolve(Path.of(
                "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "client", "render",
                "SourceVeinDisplayRenderer.java")));
        assertTrue(ruleRenderer.contains("definition.fluid()"),
                "fluid source definitions must select the dynamic fluid rule");
        assertTrue(ruleRenderer.contains("IClientFluidTypeExtensions")
                        || ruleRenderer.contains("FluidModel"),
                "fluid source rendering must use the fluid's client material and tint");
        assertTrue(ruleRenderer.contains("BlockItem"),
                "block source definitions must render the associated block model");
        assertTrue(ruleRenderer.contains("renderItem(")
                        || ruleRenderer.contains("renderOutputItem(")
                        || ruleRenderer.contains("submitItem(")
                        || ruleRenderer.contains("submitNestedItem"),
                "item source definitions must render the associated item itself");
        assertTrue(ruleRenderer.contains("FLOATING_ALPHA"),
                "unknown externally injected sources must use the same translucent fallback rule");

        String redrawScript = Files.readString(PROJECT.getParent().getParent().resolve(Path.of(
                "tools", "redraw_requested_crystal_and_frames.py")));
        assertTrue(redrawScript.contains("symmetric_frame_image"));
        assertTrue(redrawScript.contains("manager_edge_image"));
        assertTrue(redrawScript.contains("source_frame_path"));
        assertTrue(redrawScript.contains("pixel-identical"));
    }

    @Test
    void sourceVeinManagerInheritsTheExactSourceFrameAndRendersOnlyItsCore() throws IOException {
        Path managerModel = RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "block", "source_vein_manager.json"));
        JsonObject model = JsonParser.parseString(Files.readString(managerModel)).getAsJsonObject();
        assertEquals("immortalstorage:block/custom_source_vein", model.get("parent").getAsString(),
                "the manager must inherit the source block frame model verbatim");
        assertFalse(model.has("elements"),
                "the manager must not carry a second hand-authored edge cage");

        Path itemModel = RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "item", "source_vein_manager.json"));
        JsonObject item = JsonParser.parseString(Files.readString(itemModel)).getAsJsonObject();
        assertEquals("immortalstorage:block/source_vein_manager", item.get("parent").getAsString());

        String worldRenderer = Files.readString(PROJECT.resolve(Path.of(
                "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "client", "render",
                "SourceVeinManagerRenderer.java")));
        assertTrue(worldRenderer.contains("drawCore("));
        assertFalse(worldRenderer.contains("manager_edge"),
                "manager world rendering must not maintain a separate frame texture");

        String itemRenderer = Files.readString(PROJECT.resolve(Path.of(
                "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage", "client", "render",
                "SourceVeinManagerItemRenderer.java")));
        assertFalse(itemRenderer.contains("ITEM_PREVIEW_SCALE"),
                "the manager GUI preview must use the same transform scale as the source item");
        assertFalse(itemRenderer.contains("poseStack.scale(")
                        || itemRenderer.contains("poses.scale("),
                "the manager must not introduce a second GUI-only scale");
        assertTrue(itemRenderer.contains("renderModelLists(")
                        || itemRenderer.contains("SpecialModelGeometry.submitBlockBase"),
                "the item path must render the inherited source frame model");
        assertTrue(itemRenderer.contains("SourceVeinManagerRenderer.drawCore("),
                "the manager item must retain its animated eight-segment core");
        assertTrue(itemRenderer.contains("itemDisplayState(stack)"),
                "the manager item must preserve the persisted occupancy projection");
        assertTrue(itemRenderer.contains("SourceVeinAnimation.rotationDegrees")
                        || itemRenderer.contains("ticks * SourceVeinManagerRenderer.DEGREES_PER_TICK"),
                "the manager item core must continue to rotate continuously");
    }

    @Test
    void sourceVeinManagerGuiDisplayTransformMatchesSourceItemExactly() throws IOException {
        JsonObject sourceItem = JsonParser.parseString(Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "item", "source_vein.json")))).getAsJsonObject();
        JsonObject managerItem = JsonParser.parseString(Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "models", "item", "source_vein_manager.json")))).getAsJsonObject();
        JsonObject sourceGui = sourceItem.getAsJsonObject("display").getAsJsonObject("gui");
        JsonObject managerGui = managerItem.getAsJsonObject("display").getAsJsonObject("gui");
        assertEquals(sourceGui.toString(), managerGui.toString(),
                "source manager GUI preview must use the exact source item display transform");
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources")))
                    && Files.isDirectory(current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }

    private static void assertRotationallySymmetric(BufferedImage image, String label) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(image.getRGB(x, y), image.getRGB(image.getHeight() - 1 - y, x),
                        label + " must be 90-degree centre-symmetric at " + x + "," + y);
            }
        }
    }
}
