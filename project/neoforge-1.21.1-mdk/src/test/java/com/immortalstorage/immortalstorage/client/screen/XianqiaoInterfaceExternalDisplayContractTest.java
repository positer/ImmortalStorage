package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceExternalDisplayContractTest {
    @Test
    void externalSlotsOwnTheirTooltipAndEmptyCacheRendering() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("cannot locate project root");
        String screen = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/screen/XianqiaoInterfaceScreen.java"));

        assertTrue(screen.contains("Optional<ExternalResourceHover> externalHover"));
        assertTrue(screen.contains("ExternalResourceCatalog.displayName(hover.key())"));
        assertTrue(screen.contains("if (menu.isExternalTarget(resourceSlot))"));
        assertTrue(screen.contains("renderExternalResource(graphics, key, amount, slot.x, slot.y)"));
    }
}
