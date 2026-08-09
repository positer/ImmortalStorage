package com.immortalstorage.immortalstorage.compat.mekanism;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contracts for the optional Mekanism terminal-container transaction. */
final class MekanismChemicalContainerTransferContractTest {
    private static final Path PROJECT = locateProject();

    @Test
    void containerWorkUsesAOneItemCopyAndSimulationBeforeStorageExecution() throws IOException {
        String source = source("src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "compat", "mekanism", "MekanismChemicalContainerTransfer.java");
        assertTrue(source.contains("carried.copyWithCount(1)"));
        assertTrue(source.contains("Action.SIMULATE"));
        assertTrue(source.contains("Action.EXECUTE"));
        assertTrue(source.indexOf("ResourceTransferAction.SIMULATE")
                        < source.indexOf("ResourceTransferAction.EXECUTE"));
        assertTrue(source.contains("storage.insert(key, planned, ResourceTransferAction.EXECUTE)"));
        assertTrue(source.contains("storage.extract(key, plan.accepted(), ResourceTransferAction.EXECUTE)"));
    }

    @Test
    void containerTransactionProtectsStackedReturnsAndRollsBackStorageOnCommitFailure() throws IOException {
        String source = source("src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "compat", "mekanism", "MekanismChemicalContainerTransfer.java");
        assertTrue(source.contains("ItemHandlerHelper.insertItemStacked(inventory, output.copy(), true)"));
        assertTrue(source.contains("ItemHandlerHelper.insertItemStacked(inventory, output.copy(), false)"));
        assertTrue(source.contains("rollbackInsert(storage, key, committed)"));
        assertTrue(source.contains("rollbackExtract(storage, key, committed)"));
    }

    @Test
    void optionalBootstrapRegistersTheTerminalHookOnlyInsideMekanismCompat() throws IOException {
        String source = source("src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "compat", "mekanism", "MekanismCompat.java");
        assertTrue(source.contains("TerminalExternalResourceCompatHooks.register"));
        assertTrue(source.contains("MekanismChemicalContainerTransfer.depositToStorage"));
        assertTrue(source.contains("MekanismChemicalContainerTransfer.withdrawFromStorage"));
    }

    private static String source(String... relative) throws IOException {
        Path path = PROJECT;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
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
