package com.immortalstorage.immortalstorage.menu.custom;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contract for persistent embedded-stonecutter recipe selection. */
final class EmbeddedStonecutterContinuityContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void takingAResultConsumesInputAndRebuildsTheSameSelection() throws IOException {
        String source = Files.readString(locateMainSources().resolve(Path.of(
                "menu", "custom", "EmbeddedStonecutterBackend.java")));
        String onTake = methodBody(source, "void onTake(Player actor, ItemStack crafted)");

        assertFalse(onTake.contains("if (!mayTake()) return;"),
                "the result slot is already empty when vanilla invokes onTake");
        assertTrue(onTake.contains("isValidRecipeIndex(selectedRecipeIndex.get())"));
        assertTrue(onTake.contains("input.getItem(INPUT).isEmpty()"));
        assertTrue(onTake.contains("current.shrink(1)"));
        assertTrue(onTake.contains("setupResultSlot()"),
                "remaining input must immediately regenerate the selected output");
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources");
    }

    private static String methodBody(String source, String signature) {
        int name = source.indexOf(signature);
        if (name < 0) return "";
        int opening = source.indexOf('{', name);
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
    }
}
