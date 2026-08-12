package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Target-only contract for the 26.1 client item-model resource migration. */
final class TargetItemModelResourceContractTest {
    @Test
    void migratedItemDefinitionsCoverCanonicalItemModelsAndDynamicPreviews() throws IOException {
        Path workspace = locateWorkspace();
        Path canonicalModels = workspace.resolve(Path.of(
                "project", "neoforge-1.21.1-mdk", "src", "main", "resources",
                "assets", "immortalstorage", "models", "item"));
        Path targetItems = workspace.resolve(Path.of(
                "project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94",
                "src", "main", "resources", "assets", "immortalstorage", "items"));

        long canonicalModelCount;
        try (Stream<Path> files = Files.list(canonicalModels)) {
            canonicalModelCount = files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().endsWith("_base.json"))
                    .count();
        }
        try (Stream<Path> files = Files.list(targetItems)) {
            assertEquals(canonicalModelCount, files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count(), "every canonical item model needs a 26.1 item definition");
        }

        assertSpecial(targetItems.resolve("water_vein.json"), "immortalstorage:source_vein");
        assertSpecial(targetItems.resolve("custom_source_vein.json"), "immortalstorage:source_vein");
        assertSpecial(targetItems.resolve("source_vein_manager.json"), "immortalstorage:source_vein_manager");
        assertSpecial(targetItems.resolve("stabilized_miniature_immortal_ruin.json"), "immortalstorage:dynamic_preview");
        assertSpecial(targetItems.resolve("xianqiao_manager.json"), "immortalstorage:dynamic_preview");
        assertTrue(Files.readString(targetItems.resolve("spirit_staff.json"))
                .contains("minecraft:select"), "the migrated staff property model must remain target-native");
    }

    private static void assertSpecial(Path file, String rendererType) throws IOException {
        assertTrue(Files.isRegularFile(file), "missing 26.1 item definition: " + file);
        String json = Files.readString(file);
        assertTrue(json.contains("minecraft:special"), "missing special model wrapper: " + file);
        assertTrue(json.contains(rendererType), "missing renderer type " + rendererType + ": " + file);
    }

    private static Path locateWorkspace() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "resources");
        while (current != null) {
            if (Files.isDirectory(current.resolve(marker))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate workspace for target item resources");
    }
}
