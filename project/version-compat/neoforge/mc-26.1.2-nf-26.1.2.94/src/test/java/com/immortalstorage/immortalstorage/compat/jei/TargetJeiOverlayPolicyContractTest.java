package com.immortalstorage.immortalstorage.compat.jei;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TargetJeiOverlayPolicyContractTest {
    @Test
    void targetLeavesJeiOverlayAtItsDefaultLayoutOnEveryModScreen() throws Exception {
        Path mixin = targetRoot().resolve(Path.of("src", "main", "java", "com", "immortalstorage",
                "immortalstorage", "mixin", "jei"));
        Path javaRoot = targetRoot().resolve(Path.of("src", "main", "java", "com", "immortalstorage",
                "immortalstorage"));
        Path generatedJei = targetRoot().resolve(Path.of("src", "main", "generated-java", "com", "immortalstorage",
                "immortalstorage", "compat", "jei"));

        assertFalse(Files.exists(targetRoot().resolve(Path.of("src", "main", "resources",
                "immortalstorage.jei.mixins.json"))));
        String descriptor = Files.readString(targetRoot().resolve(Path.of("src", "main", "resources", "META-INF",
                "neoforge.mods.toml")));
        assertFalse(descriptor.contains("immortalstorage.jei.mixins.json"));
        assertFalse(Files.exists(mixin.resolve("IngredientListOverlayControllerMixin.java")));
        assertFalse(Files.exists(mixin.resolve("IngredientListOverlayMixin.java")));
        assertFalse(Files.exists(javaRoot.resolve(Path.of("compat", "jei", "JeiOverlayScreenPolicy.java"))));
        assertFalse(Files.exists(generatedJei.resolve("XianqiaoInterfaceJeiLayout.java")));

        String plugin = Files.readString(generatedJei.resolve("ImmortalStorageJeiPlugin.java"));
        String normal = Files.readString(generatedJei.resolve("XianqiaoInterfaceJeiGuiHandler.java"));
        String advanced = Files.readString(generatedJei.resolve("AdvancedXianqiaoInterfaceJeiGuiHandler.java"));
        assertFalse(plugin.contains("SuppressedGuiHandler"));
        assertFalse(plugin.contains("getGuiExtraAreas"));
        assertFalse(normal.contains("getGuiExtraAreas"));
        assertFalse(advanced.contains("getGuiExtraAreas"));
        assertTrue(plugin.contains("addGuiContainerHandler(XianqiaoStorageScreen.class"));
        assertTrue(plugin.contains("addGhostIngredientHandler"));
    }

    private static Path targetRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94");
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isDirectory(candidate)) return candidate;
            if (current.endsWith(marker)) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate target root");
    }
}
