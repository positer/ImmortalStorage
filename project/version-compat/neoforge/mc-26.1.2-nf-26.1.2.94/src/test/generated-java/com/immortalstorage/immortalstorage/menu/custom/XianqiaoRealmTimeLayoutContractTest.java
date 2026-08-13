package com.immortalstorage.immortalstorage.menu.custom;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianqiaoRealmTimeLayoutContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void timeScaleValueIsCenteredBetweenSymmetricControls() throws Exception {
        Path root = locateMainSourceRoot().getParent().getParent().resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source")).normalize();
        String screen = Files.readString(root.resolve(
                "com/immortalstorage/immortalstorage/client/screen/XianqiaoStorageScreen.java"));

        assertTrue(screen.contains("REALM_TIME_SIDE_INSET"));
        assertTrue(screen.contains("REALM_WIDTH - REALM_TIME_SIDE_INSET - REALM_TIME_BUTTON_SIZE"));
        assertTrue(screen.contains("container.immortalstorage.terminal.time_scale_title"));
        assertTrue(screen.contains("String timeScale = String.format"));
        assertTrue(screen.contains("graphics.text(this.font, timeScaleValue"));
        assertTrue(screen.contains("0xFF404040, false"),
                "realm labels and values must opt out of vanilla text shadows");
        assertFalse(screen.contains("drawCenteredString(this.font"),
                "centered text must use the no-shadow drawString overload");
        assertFalse(screen.contains("Component.translatable(\"container.immortalstorage.terminal.time_scale\",\n"),
                "the numeric value should no longer be attached to the title label");
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (Files.isRegularFile(direct.resolve(
                    "java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return direct;
            Path nested = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (Files.isRegularFile(nested.resolve(
                    "java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"))) return nested;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate ImmortalStorage src/main");
    }
}
