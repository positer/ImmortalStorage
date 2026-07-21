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
        String ruinDecorator = read("client", "render", "RuinCoreItemDecorator.java");
        String managerDecorator = read("client", "render", "XianqiaoManagerItemDecorator.java");

        assertTrue(setup.contains("RegisterItemDecorationsEvent"));
        assertTrue(setup.contains("SourceVeinOutputDecorator.INSTANCE"));
        assertTrue(setup.contains("RuinCoreItemDecorator.INSTANCE"));
        assertTrue(setup.contains("XianqiaoManagerItemDecorator.INSTANCE"));
        assertTrue(source.contains("extends BlockEntityWithoutLevelRenderer"));
        assertTrue(source.contains("context == ItemDisplayContext.GUI"));
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
