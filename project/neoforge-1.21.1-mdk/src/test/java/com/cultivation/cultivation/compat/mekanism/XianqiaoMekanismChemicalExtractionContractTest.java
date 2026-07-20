package com.cultivation.cultivation.compat.mekanism;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoMekanismChemicalExtractionContractTest {
    @Test
    void adapterOverridesBothPipeExtractionEntrypoints() throws IOException {
        Path source = locateProject().resolve(Path.of(
                "src", "main", "java", "com", "cultivation", "cultivation",
                "compat", "mekanism", "XianqiaoMekanismChemicalAdapter.java"));
        String adapter = Files.readString(source);

        assertTrue(adapter.contains("extractChemical(ChemicalStack stack, Action action)"));
        assertTrue(adapter.contains("extractChemical(long amount, Action action)"));
        assertTrue(adapter.contains("return extract(stack.getChemical(), stack.getAmount(), action)"));
        assertTrue(adapter.contains("current.extract(amount, transferAction(action))"));
        org.junit.jupiter.api.Assertions.assertFalse(adapter.contains("mode.get()"));
        assertTrue(adapter.contains("current.extract(amount, transferAction(action))"));
    }

    @Test
    void adapterOverridesWholeStackPipeInsertion() throws IOException {
        Path source = locateProject().resolve(Path.of(
                "src", "main", "java", "com", "cultivation", "cultivation",
                "compat", "mekanism", "XianqiaoMekanismChemicalAdapter.java"));
        String adapter = Files.readString(source);

        assertTrue(adapter.contains("insertChemical(ChemicalStack stack, Action action)"));
        assertTrue(adapter.contains("storage.apply(key(stack.getChemical()))"));
        assertTrue(adapter.contains("current.insert(stack.getAmount(), transferAction(action))"));
        org.junit.jupiter.api.Assertions.assertFalse(adapter.contains("mode.get()"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
