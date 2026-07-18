package com.cultivation.cultivation.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CultivationCommandContractTest {
    @Test
    void stageCommandSupportsSelfAndOptionalOnlinePlayerTarget() throws IOException {
        String source = Files.readString(locateMainSources().resolve("command/CultivationCommands.java"));

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
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources");
    }
}
