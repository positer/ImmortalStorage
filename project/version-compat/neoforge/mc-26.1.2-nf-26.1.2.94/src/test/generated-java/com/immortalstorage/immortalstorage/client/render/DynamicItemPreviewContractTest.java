package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DynamicItemPreviewContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void previewsUseItemModelRenderingForInventoryCreativeJeiAndEmi() throws IOException {
        String setup = read("client", "ClientSetup.java");
        String source = read("client", "render", "SourceVeinItemRenderer.java");
        String sourceDecorator = read("client", "render", "SourceVeinOutputDecorator.java");
        String ruinDecorator = read("client", "render", "RuinCoreItemDecorator.java");
        String managerDecorator = read("client", "render", "XianqiaoManagerItemDecorator.java");

        assertTrue(setup.contains("RegisterItemDecorationsEvent"));
        assertTrue(setup.contains("SourceVeinOutputDecorator.INSTANCE"));
        assertTrue(setup.contains("RuinCoreItemDecorator.INSTANCE"));
        assertTrue(setup.contains("XianqiaoManagerItemDecorator.INSTANCE"));
        assertTrue(source.contains("implements SpecialModelRenderer<ItemStack>"));
        assertTrue(source.contains("SourceVeinDisplayRenderer.renderForItem")
                || source.contains("SourceVeinDisplayRenderer.submit"));
        assertTrue(!sourceDecorator.contains("enableScissor"));
        assertTrue(!sourceDecorator.contains("disableScissor"));
        assertTrue(!ruinDecorator.contains("enableScissor"));
        assertTrue(!ruinDecorator.contains("disableScissor"));
        assertTrue(!managerDecorator.contains("enableScissor"));
        assertTrue(!managerDecorator.contains("disableScissor"));
        assertTrue(readResource("immortalstorage.core.mixins.json")
                .contains("\"client\": [\"GuiGraphicsItemPreviewMixin\"]") == false);
    }

    private static String read(String... relative) throws IOException {
        Path path = MAIN;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
    }

    private static String readResource(String relative) throws IOException {
        Path resources = MAIN;
        for (int i = 0; i < 6; i++) resources = resources.getParent();
        resources = resources.resolve(Path.of("src", "main", "resources"));
        return Files.readString(resources.resolve(relative));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate main sources");
    }
}
