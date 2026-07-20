package com.immortalstorage.immortalstorage.compat.emi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalEmiInteractionTest {
    @Test
    void terminalIngredientsRemainQueryableWithoutCapturingNormalMouseClicks() throws IOException {
        Path compat = locateMainSources().resolve(Path.of("compat", "emi"));
        String policy = Files.readString(compat.resolve("TerminalEmiInteraction.java"));
        String plugin = Files.readString(compat.resolve("ImmortalStorageEmiPlugin.java"));

        assertTrue(policy.contains("new EmiStackInteraction(ingredient, null, false)"),
                "the ingredient remains available for R/U lookup without claiming normal mouse buttons");
        assertEquals(4, occurrences(plugin, "TerminalEmiInteraction.lookupOnly("),
                "terminal item/fluid entries and interface item/fluid cache entries must all use the non-clickable policy");
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources");
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
