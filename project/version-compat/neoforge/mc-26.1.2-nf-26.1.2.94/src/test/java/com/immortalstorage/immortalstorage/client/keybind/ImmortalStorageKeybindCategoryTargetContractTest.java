package com.immortalstorage.immortalstorage.client.keybind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the 26.1.2 migration against registering one key category per mapping. */
@ExtendWith(com.immortalstorage.immortalstorage.compat.CompatTestBootstrapExtension.class)
final class ImmortalStorageKeybindCategoryTargetContractTest {
    @Test
    void generatedKeyMappingsReuseOneRegisteredCategory() throws IOException {
        Path relative = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "generated-java",
                "com", "immortalstorage", "immortalstorage", "client", "keybind",
                "ImmortalStorageKeybinds.java");
        Path workspace = Path.of("").toAbsolutePath();
        while (workspace != null && !Files.exists(workspace.resolve(relative))) {
            workspace = workspace.getParent();
        }
        assertTrue(workspace != null, "workspace root for generated keybind source is not visible");

        String source = Files.readString(workspace.resolve(relative));
        assertEquals(1, count(source, "KeyMapping.Category.register("));
        assertEquals(6, count(source, "IMMORTALSTORAGE_CATEGORY"));
        assertTrue(source.contains("private static final net.minecraft.client.KeyMapping.Category IMMORTALSTORAGE_CATEGORY"));
    }

    private static int count(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
