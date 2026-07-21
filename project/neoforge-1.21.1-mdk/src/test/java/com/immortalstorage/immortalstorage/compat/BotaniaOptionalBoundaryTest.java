package com.immortalstorage.immortalstorage.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotaniaOptionalBoundaryTest {
    private static final Path PROJECT = locateProject();
    private static final Path JAVA = PROJECT.resolve(Path.of("src", "main", "java"));

    @Test
    void botaniaTypesStayInsideTheIndependentOptionalModule() throws IOException {
        Path optionalRoot = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "botania"));
        Path appliedBotanicsShim = JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "mixin", "appliedbotanics"));
        assertTrue(Files.isDirectory(optionalRoot));
        try (var files = Files.walk(JAVA)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.startsWith(optionalRoot) || source.startsWith(appliedBotanicsShim)) continue;
                String text = Files.readString(source);
                assertFalse(text.contains("vazkii.botania"),
                        () -> "hard Botania reference escaped optional module: " + source);
            }
        }
    }

    @Test
    void buildAndMetadataPinTheAuditedOptionalBotaniaRange() throws IOException {
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertTrue(build.contains(
                "compileOnly('vazkii.botania:botania-neoforge-1.21.1:454-SNAPSHOT')"));
        assertTrue(build.contains("transitive = false"));

        String mods = Files.readString(PROJECT.resolve(Path.of(
                "src", "main", "resources", "META-INF", "neoforge.mods.toml")));
        int dependency = mods.indexOf("modId=\"botania\"");
        assertTrue(dependency >= 0);
        String botaniaSection = mods.substring(dependency);
        assertTrue(botaniaSection.contains("type=\"optional\""));
        assertTrue(botaniaSection.contains("versionRange=\"[454-SNAPSHOT,456)\""));
        assertTrue(botaniaSection.contains("side=\"BOTH\""));
    }

    @Test
    void adapterUsesOnlyTheOfficialManaPoolAndSparkCapabilityContracts() throws IOException {
        String adapter = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "botania",
                "XianqiaoBotaniaManaAdapter.java")));
        assertTrue(adapter.contains("implements ManaPool, SparkAttachable"));
        assertTrue(adapter.contains("receiveMana(int delta)"));
        assertTrue(adapter.contains("canAttachSpark(ItemStack stack)"));
        assertTrue(adapter.contains("return true;"),
                "spark attachment must remain stable while the owner briefly disconnects");
        assertFalse(adapter.contains("SideMode"),
                "face settings must not gate Botania's directionless spark network");
        String window = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "botania",
                "BotaniaManaWindow.java")));
        assertTrue(window.contains("-(long) delta"),
                "Integer.MIN_VALUE must not overflow while converting a negative Botania delta");
        assertTrue(window.contains("LongAmountBridge.saturatingInt"));

        String compat = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "botania", "BotaniaCompat.java")));
        assertTrue(compat.contains("ManaReceiver.LOOKUP"));
        assertTrue(compat.contains("ManaReceiver.LOOKUP.find(level, pos, side)"));
        assertTrue(compat.contains("receiver instanceof ManaPool pool"));
        assertTrue(compat.contains("SparkAttachable.LOOKUP"));
        assertTrue(compat.contains("BlockCapability.createVoid"));
        assertTrue(compat.contains("BotaniaForgeCapabilities.getBlockApiLookupById"));
        assertTrue(compat.contains("RegisterCapabilitiesEvent"));

        String manager = Files.readString(JAVA.resolve(Path.of(
                "com", "immortalstorage", "immortalstorage", "compat", "CompatManager.java")));
        assertTrue(manager.contains(
                "blockEntity.resolveDirectionlessExternalResource("));
        assertTrue(manager.contains("ExternalResourceChannels.BOTANIA_MANA"));
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
