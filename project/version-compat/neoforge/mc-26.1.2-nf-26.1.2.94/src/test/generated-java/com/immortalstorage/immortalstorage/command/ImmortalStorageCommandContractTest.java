package com.immortalstorage.immortalstorage.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalStorageCommandContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void stageCommandSupportsSelfAndOptionalOnlinePlayerTarget() throws IOException {
        String source = Files.readString(locateMainSources().resolve("command/ImmortalStorageCommands.java"));

        assertTrue(source.contains("getPlayerOrException()"));
        assertTrue(source.contains("Commands.argument(\"player\", EntityArgument.player())"));
        assertTrue(source.contains("EntityArgument.getPlayer(context, \"player\")"));
        assertTrue(source.contains("CommonEvents.restoreStageEffects(player)"));
        assertTrue(source.contains("Commands.literal(\"unload\")"));
        assertTrue(source.contains("Commands.literal(\"reload\")"));
        assertTrue(source.contains("Commands.literal(\"speed\")"));
        assertTrue(source.contains("RealmTimeScalePolicy"));
        assertTrue(source.contains("isAllowedStep"));
        assertTrue(source.contains("isPersonalRealmFor"));
        assertTrue(source.contains("RealmHelper.exitRealm(player)"));
        assertTrue(source.contains("Float.isFinite"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
