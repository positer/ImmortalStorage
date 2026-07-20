package com.immortalstorage.immortalstorage.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalStorageCommandsContractTest {
    @Test
    void stageCommandRemainsOperatorOnlyBoundedAndServerAuthoritative() throws Exception {
        String command = Files.readString(source("command", "ImmortalStorageCommands.java"));
        String mod = Files.readString(source("ImmortalStorageMod.java"));

        assertTrue(command.contains("Commands.literal(\"immortalstorage\")"));
        assertTrue(command.contains("Commands.literal(\"stage\")"));
        assertTrue(command.contains("IntegerArgumentType.integer(0, 10)"));
        assertTrue(command.contains("source.hasPermission(2)"));
        assertTrue(command.contains("data.setStage(stage, player)"));
        assertTrue(command.contains("data.syncTo(player)"));
        assertTrue(mod.contains("ImmortalStorageCommands::register"));
    }

    private static Path source(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/immortalstorage/immortalstorage");
            for (String part : parts) candidate = candidate.resolve(part);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate source");
    }
}
