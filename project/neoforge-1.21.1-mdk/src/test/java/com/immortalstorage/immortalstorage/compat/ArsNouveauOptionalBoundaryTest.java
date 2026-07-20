package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArsNouveauOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void arsTypesStayInsideTheOptionalPackage() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "arsnouveau"));
        Path optionalMixinRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "mixin", "arsnouveau"));
        assertTrue(Files.isDirectory(optionalRoot));
        assertTrue(Files.isDirectory(optionalMixinRoot));
        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot) || source.startsWith(optionalMixinRoot)) continue;
                assertFalse(Files.readString(source).contains("com.hollingsworth.arsnouveau"),
                        () -> "hard Ars Nouveau reference escaped optional module: " + source);
            }
        }
    }

    @Test
    void buildAndMetadataPinTheAuditedOfficialRelease() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(build.contains("compileOnly('curse.maven:ars-nouveau-401955:6640732')"));
        String mods = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "resources", "META-INF", "neoforge.mods.toml")));
        int dependency = mods.indexOf("modId=\"ars_nouveau\"");
        assertTrue(dependency >= 0);
        String section = mods.substring(dependency);
        assertTrue(section.contains("type=\"optional\""));
        assertTrue(section.contains("versionRange=\"[5.8.4,6)\""));
        assertTrue(section.contains("side=\"BOTH\""));
    }

    @Test
    void providerUsesOfficialSourceContractsAndSharedChannel() throws IOException {
        String compat = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "arsnouveau",
                "ArsNouveauCompat.java")));
        assertTrue(compat.contains("implements ISpecialSourceProvider"));
        assertTrue(compat.contains("SourceManager.INSTANCE.addInterface"));
        String adapter = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "arsnouveau",
                "XianqiaoArsSourceAdapter.java")));
        assertTrue(adapter.contains("implements ISourceTile"));
        assertTrue(adapter.contains("ResourceTransferAction.SIMULATE"));
        assertFalse(adapter.contains("SideMode"),
                "face settings must not gate Ars Nouveau's position-based Source provider");
        String manager = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "CompatManager.java")));
        assertTrue(manager.contains("ExternalResourceChannels.ARS_NOUVEAU_SOURCE"));
        assertTrue(manager.contains("blockEntity.resolveExternalResourceCache("),
                "Ars must transact with the interface block cache, not the owner ledger directly");
        assertFalse(manager.contains("import com.hollingsworth.arsnouveau"));
    }

    @Test
    void relayWandAndTransferMixinAcceptsTheXianqiaoProvider() throws IOException {
        String mixin = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "mixin", "arsnouveau",
                "RelayTileXianqiaoMixin.java")));
        assertTrue(mixin.contains("method = \"onFinishedConnectionFirst\""));
        assertTrue(mixin.contains("method = \"onFinishedConnectionLast\""));
        assertTrue(mixin.contains("self.setSendTo(storedPos.immutable())"));
        assertTrue(mixin.contains("self.setTakeFrom(storedPos.immutable())"));
        assertTrue(mixin.contains("method = \"tick\""));

        String block = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "block", "custom",
                "XianqiaoInterfaceBlock.java")));
        assertTrue(block.contains("XianqiaoInterfaceCompatHooks.useItemOn"));
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
