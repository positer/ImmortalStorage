package com.immortalstorage.immortalstorage.compat.jei;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures JEI delegates terminal ingredient validation to the authoritative server. */
final class TerminalJeiTransferAuthorityTest {
    @Test
    void craftingAndSmithingAlwaysSubmitServerTransferRequests() throws IOException {
        String source = Files.readString(locateMainSources().resolve(Path.of(
                "compat", "jei", "ImmortalStorageJeiPlugin.java")));

        assertFalse(source.contains("maxTransferableSets("),
                "stale client terminal snapshots must not reject crafting transfers");
        assertFalse(source.contains("hasSmithingIngredients("),
                "stale client terminal snapshots must not reject smithing transfers");
        assertTrue(source.contains("maxTransfer ? 64 : 1"),
                "max-transfer intent must be preserved for server-side placement");
        assertTrue(count(source, "PacketDistributor.sendToServer(") >= 2,
                "both crafting and smithing handlers must submit authoritative requests");
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

    private static int count(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }
}
