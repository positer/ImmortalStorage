package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DynamicItemPreviewContractTest {
    private static final Path MAIN = locateMainSources();

    @Test
    void previewsUseItemModelRenderingForInventoryCreativeJeiAndEmi() throws IOException {
        String setup = read("client", "ClientSetup.java");
        String source = read("client", "render", "SourceVeinItemRenderer.java");
        String sourceDecorator = read("client", "render", "SourceVeinOutputDecorator.java");
        String nativeRenderer = read("client", "render", "WorldBlockItemRenderer.java");
        String snapshot = read("client", "render", "WorldBlockItemVisual.java");
        assertTrue(setup.contains("SourceVeinOutputDecorator.INSTANCE"));
        assertTrue(!setup.contains("RuinCoreItemDecorator"));
        assertTrue(!setup.contains("EntangledRuinCoreItemDecorator"));
        assertTrue(!setup.contains("SimulatedSpiritFieldItemDecorator"));
        assertTrue(!setup.contains("XianqiaoManagerItemDecorator"));
        assertTrue(source.contains("SourceVeinDisplayRenderer.renderForItem")
                || source.contains("SourceVeinDisplayRenderer.submit"));
        assertTrue(nativeRenderer.contains("getBlockEntityRenderDispatcher().getRenderer(visual.entity())"));
        assertTrue(nativeRenderer.contains("renderer.render(visual.entity()") || nativeRenderer.contains("renderer.submit(state"));
        assertTrue(nativeRenderer.contains("renderSingleBlock(visual.state()") || nativeRenderer.contains("model.update(base, visual.state()"));
        assertTrue(!nativeRenderer.contains("FloatingCubeRenderer.render"));
        assertTrue(snapshot.contains("block instanceof SourceVeinBlock"));
        assertTrue(snapshot.contains("newBlockEntity(BlockPos.ZERO, state)"));
        assertTrue(snapshot.contains("loadWithComponents"));
        assertTrue(!snapshot.contains("setBlock("));
        assertTrue(!snapshot.contains("serverTick("));
        assertTrue(!sourceDecorator.contains("enableScissor"));
        assertTrue(readResource("immortalstorage.core.mixins.json")
                .contains("\"client\": [\"GuiGraphicsItemPreviewMixin\"]") == false);
    }

    private static String read(String... relative) throws IOException {
        Path path = MAIN;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
    }

    private static String readResource(String relative) throws IOException {
        Path mainJava = MAIN;
        Path main = mainJava;
        for (int i = 0; i < 4; i++) main = main.getParent();
        return Files.readString(main.resolve("resources").resolve(relative));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate main sources");
    }
}
