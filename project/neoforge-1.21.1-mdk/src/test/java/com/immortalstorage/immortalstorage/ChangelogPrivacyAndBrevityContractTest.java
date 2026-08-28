package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangelogPrivacyAndBrevityContractTest {
    private static final Pattern DATE = Pattern.compile("(?m)\\b(?:19|20)\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b");
    private static final Pattern ABSOLUTE_PATH = Pattern.compile(
            "(?im)(?:[a-z]:[\\\\/]|/(?:users|home|mnt|var|tmp)/|\\\\\\\\[^\\s`]+\\\\[^\\s`]+)");
    private static final Pattern COMMIT_HASH = Pattern.compile("(?i)\\b[0-9a-f]{7,40}\\b");
    private static final Pattern INTERNAL_DETAIL = Pattern.compile(
            "(?i)(sha-?256|\\bbytes?\\b|字节|build --offline|test build|compatibility-matrix|"
                    + "production-jar-boundary|version-artifact|version-composition|no-ae2-runtime|"
                    + "pcl|archive/|src/|build/libs|github release|提交号|构建门禁|测试套件)");

    @Test
    void everyEntryIsPrivateConciseAndGroupedByLanguage() throws IOException {
        String changelog = Files.readString(locateWorkspaceRoot().resolve("CHANGELOG.md"));
        String[] entries = changelog.split("(?m)(?=^## \\[)");
        for (String entry : entries) {
            if (!entry.startsWith("## [")) continue;
            String version = entry.lines().findFirst().orElse("unknown version");
            int english = entry.indexOf("### English");
            int chinese = entry.indexOf("### 简体中文");
            assertTrue(english >= 0 && chinese > english,
                    version + " must contain complete English before complete Simplified Chinese");
            assertTrue(entry.length() <= 4_500, version + " must remain a concise update summary");
            assertTrue(entry.lines().filter(line -> line.startsWith("- ")).count() <= 24,
                    version + " must not become an internal implementation ledger");

            assertFalse(DATE.matcher(entry).find(), version + " must not contain dates or timestamps");
            assertFalse(ABSOLUTE_PATH.matcher(entry).find(), version + " must not expose local paths");
            assertFalse(COMMIT_HASH.matcher(entry).find(), version + " must not expose commit or artifact hashes");
            assertFalse(INTERNAL_DETAIL.matcher(entry).find(),
                    version + " must omit local deployment, artifact, build, and test details");
        }
    }

    private static Path locateWorkspaceRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("CHANGELOG.md"))
                    && Files.isDirectory(current.resolve("project/neoforge-1.21.1-mdk"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate workspace root");
    }
}
