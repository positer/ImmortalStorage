package com.immortalstorage.immortalstorage.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production ImmortalStorage must be operated and tested exclusively from external tools. */
final class ProductionDebugBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN_JAVA = locate("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source");
    private static final Path MAIN_RESOURCES = locate("src", "main", "resources");

    @Test
    void productionSourceRetainsOnlyTheApprovedStageCommandWithoutExternalDebugEndpoints() throws IOException {
        Path mod = MAIN_JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "ImmortalStorageMod.java"));
        Path commandPackage = MAIN_JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "command"));
        String entrypoint = Files.readString(mod);

        assertTrue(entrypoint.contains("ImmortalStorageCommands::register"));
        assertFalse(entrypoint.contains("RegisterCommandsEvent"));
        assertFalse(entrypoint.contains("onRegisterCommands"));
        Path command = commandPackage.resolve("ImmortalStorageCommands.java");
        assertTrue(Files.isRegularFile(command));
        String commandSource = Files.readString(command);
        assertTrue(commandSource.contains("Commands.literal(\"immortalstorage\")"));
        assertTrue(commandSource.contains("Commands.literal(\"stage\")"));
        assertTrue(commandSource.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatCommands.hasPermission(source, 2)"));
        assertFalse(commandSource.toLowerCase(Locale.ROOT).contains("numen"));
        assertFalse(commandSource.contains("HttpServer"));
        assertFalse(commandSource.contains("ServerSocket"));
    }

    @Test
    void productionSourceAndResourcesContainNoExternalQaBrandOrEndpoint() throws IOException {
        List<String> forbidden = List.of(
                "numen",
                "codex",
                "screen-state",
                "click-screen",
                "open_creative_catalog",
                "realmticktest",
                "selftest");

        try (Stream<Path> paths = Stream.concat(javaFiles(MAIN_JAVA), regularFiles(MAIN_RESOURCES))) {
            List<String> violations = paths
                    .filter(path -> !path.getFileName().toString().equals("ProductionDebugBoundaryTest.java"))
                    .flatMap(path -> violations(path, forbidden).stream())
                    .toList();
            assertTrue(violations.isEmpty(), "production QA boundary violations: " + violations);
        }
    }

    private static Stream<Path> javaFiles(Path root) throws IOException {
        return regularFiles(root).filter(path -> path.getFileName().toString().endsWith(".java"));
    }

    private static Stream<Path> regularFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) return Stream.empty();
        return Files.walk(root).filter(Files::isRegularFile);
    }

    private static List<String> violations(Path path, List<String> forbidden) {
        try {
            String content = Files.readString(path).toLowerCase(Locale.ROOT);
            return forbidden.stream()
                    .filter(content::contains)
                    .map(token -> path + ": " + token)
                    .toList();
        } catch (IOException | RuntimeException ignored) {
            // Binary resources do not expose an executable text endpoint through this source contract.
            return List.of();
        }
    }

    private static Path locate(String... relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current;
            for (String segment : relative) candidate = candidate.resolve(segment);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate " + String.join("/", relative));
    }
}
