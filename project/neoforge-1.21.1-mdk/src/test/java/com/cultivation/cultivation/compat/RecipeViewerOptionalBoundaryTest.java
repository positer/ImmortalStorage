package com.cultivation.cultivation.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hard class-loading boundary for the optional JEI and EMI client adapters. */
final class RecipeViewerOptionalBoundaryTest {
    private static final Pattern JEI_TYPE = Pattern.compile("\\bmezz\\.jei\\b");
    private static final Pattern EMI_TYPE = Pattern.compile("\\bdev\\.emi\\b");

    @Test
    void hardViewerTypesStayInsideTheirOwnOptionalCompatPackage() throws IOException {
        Path java = projectRoot().resolve(Path.of(
                "src", "main", "java", "com", "cultivation", "cultivation"));
        Path jeiRoot = java.resolve(Path.of("compat", "jei")).normalize();
        Path emiRoot = java.resolve(Path.of("compat", "emi")).normalize();

        try (var files = Files.walk(java)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String code = withoutCommentsAndStringLiterals(Files.readString(source));
                if (!source.normalize().startsWith(jeiRoot)) {
                    assertFalse(JEI_TYPE.matcher(code).find(),
                            () -> "hard JEI reference escaped compat/jei: " + source);
                }
                if (!source.normalize().startsWith(emiRoot)) {
                    assertFalse(EMI_TYPE.matcher(code).find(),
                            () -> "hard EMI reference escaped compat/emi: " + source);
                }
            }
        }
    }

    @Test
    void interfaceCoreScreenMenuPayloadAndNeutralBridgeDoNotLinkViewers() throws IOException {
        Path java = projectRoot().resolve(Path.of(
                "src", "main", "java", "com", "cultivation", "cultivation"));
        List<Path> coreFiles = List.of(
                java.resolve("client/screen/XianqiaoInterfaceViewerConfiguration.java"),
                java.resolve("client/screen/XianqiaoInterfaceScreen.java"),
                java.resolve("menu/custom/XianqiaoInterfaceMenu.java"),
                java.resolve("network/ModPayloads.java"));

        for (Path source : coreFiles) {
            String code = withoutCommentsAndStringLiterals(Files.readString(source));
            assertFalse(JEI_TYPE.matcher(code).find(),
                    () -> "interface core links JEI: " + source);
            assertFalse(EMI_TYPE.matcher(code).find(),
                    () -> "interface core links EMI: " + source);
        }
    }

    @Test
    void buildUsesCompileOnlyApisAndBothModsAreOptionalClientDependencies() throws IOException {
        Path root = projectRoot();
        String gradle = Files.readString(root.resolve("build.gradle"));
        assertTrue(gradle.contains(
                "compileOnly 'mezz.jei:jei-1.21.1-common-api:19.37.0.363'"));
        assertTrue(gradle.contains(
                "compileOnly 'mezz.jei:jei-1.21.1-neoforge-api:19.37.0.363'"));
        assertTrue(gradle.contains(
                "compileOnly 'dev.emi:emi-neoforge:1.1.24+1.21.1:api'"));
        assertFalse(Pattern.compile(
                        "(?m)^\\s*(?:implementation|api)\\s*['\"](?:mezz\\.jei|dev\\.emi)")
                .matcher(gradle).find(),
                "viewer APIs must not become shipped compile dependencies");

        String modsToml = Files.readString(root.resolve(
                "src/main/resources/META-INF/neoforge.mods.toml"));
        assertOptionalClientDependency(modsToml, "jei");
        assertOptionalClientDependency(modsToml, "emi");
    }

    private static void assertOptionalClientDependency(String toml, String modId) {
        String marker = "modId=\"" + modId + "\"";
        int start = toml.indexOf(marker);
        assertTrue(start >= 0, () -> "missing " + modId + " dependency declaration");
        int end = toml.indexOf("[[dependencies.", start + marker.length());
        String block = toml.substring(start, end < 0 ? toml.length() : end);
        assertTrue(block.contains("type=\"optional\""),
                () -> modId + " must remain optional");
        assertTrue(block.contains("side=\"CLIENT\""),
                () -> modId + " must remain client-only");
    }

    /** Removes reflection-only class-name strings before checking JVM type linkage. */
    private static String withoutCommentsAndStringLiterals(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ")
                .replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(?:\\\\.|[^'\\\\])'", "''");
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main/java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate NeoForge integration project");
    }
}
