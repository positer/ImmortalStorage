package com.immortalstorage.immortalstorage.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalStorageCommandsContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void stageCommandRemainsOperatorOnlyBoundedAndServerAuthoritative() throws Exception {
        String command = Files.readString(source("command", "ImmortalStorageCommands.java"));
        String mod = Files.readString(source("ImmortalStorageMod.java"));

        assertTrue(command.contains("Commands.literal(\"immortalstorage\")"));
        assertTrue(command.contains("Commands.literal(\"stage\")"));
        assertTrue(command.contains("IntegerArgumentType.integer(0, 10)"));
        assertTrue(command.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatCommands.hasPermission(source, 2)"));
        assertTrue(command.contains("data.setStage(stage, player)"));
        assertTrue(command.contains("data.syncTo(player)"));
        assertTrue(mod.contains("ImmortalStorageCommands::register"));
    }

    private static Path source(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage");
            for (String part : parts) candidate = candidate.resolve(part);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate source");
    }
}
