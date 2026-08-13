package com.immortalstorage.immortalstorage.compat.jei;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JeiScreenVisibilityContractTest {
    @Test
    void everyImmortalStorageScreenLeavesJeiOverlayAtItsDefaultLayout() throws Exception {
        Path main = locateMain();
        Path project = main;
        while (project != null && !Files.isRegularFile(project.resolve("build.gradle"))) {
            project = project.getParent();
        }
        assertTrue(project != null);

        assertFalse(Files.exists(project.resolve(Path.of("src", "main", "resources",
                "immortalstorage.jei.mixins.json"))));
        String descriptor = Files.readString(project.resolve(Path.of("src", "main", "resources", "META-INF",
                "neoforge.mods.toml")));
        assertFalse(descriptor.contains("immortalstorage.jei.mixins.json"));

        Path mixin = main.resolve(Path.of("mixin", "jei"));
        assertFalse(Files.exists(mixin.resolve("IngredientListOverlayControllerMixin.java")));
        assertFalse(Files.exists(mixin.resolve("IngredientListOverlayMixin.java")));
        assertFalse(Files.exists(main.resolve(Path.of("compat", "jei", "JeiOverlayScreenPolicy.java"))));

        String plugin = Files.readString(main.resolve(Path.of("compat", "jei", "ImmortalStorageJeiPlugin.java")));
        assertFalse(plugin.contains("SuppressedGuiHandler"));
        assertFalse(plugin.contains("getGuiExtraAreas"));
        assertTrue(plugin.contains("addGuiContainerHandler(XianqiaoStorageScreen.class"));
        assertTrue(plugin.contains("addGhostIngredientHandler"));

        String normal = Files.readString(main.resolve(Path.of("compat", "jei",
                "XianqiaoInterfaceJeiGuiHandler.java")));
        String advanced = Files.readString(main.resolve(Path.of("compat", "jei",
                "AdvancedXianqiaoInterfaceJeiGuiHandler.java")));
        assertFalse(normal.contains("getGuiExtraAreas"));
        assertFalse(advanced.contains("getGuiExtraAreas"));
        assertFalse(Files.exists(main.resolve(Path.of("compat", "jei", "XianqiaoInterfaceJeiLayout.java"))));
    }

    private static Path locateMain() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "java", "com",
                    "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate main source root");
    }
}
