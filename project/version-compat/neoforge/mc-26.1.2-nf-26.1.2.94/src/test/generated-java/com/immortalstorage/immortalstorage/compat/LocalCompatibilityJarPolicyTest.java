package com.immortalstorage.immortalstorage.compat;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class LocalCompatibilityJarPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void compatibilityBuildIsPclLocalAndOfflineByContract() throws Exception {
        Path project = Path.of("").toAbsolutePath();
        while (project != null && !Files.isRegularFile(project.resolve("build.gradle"))) project = project.getParent();
        assertNotNull(project);
        String build = Files.readString(project.resolve("build.gradle"));
        String taste = Files.readString(project.getParent().getParent().resolve("taste.md"));
        assertTrue(build.contains("PCL/.minecraft/versions"));
        assertTrue(build.contains("external resolution is forbidden"));
        assertTrue(taste.contains("--offline"));
        assertTrue(taste.contains("Never download, clone, build, or resolve external compatibility-mod"));
    }
}
