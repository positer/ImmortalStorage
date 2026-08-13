package com.immortalstorage.immortalstorage.effect;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedWeaknessRegistrationTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void effectIsRegisteredProjectedAndHasAVisibleIcon() throws IOException {
        Path project = locateProject();
        String effects = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/effect/ModEffects.java"));
        String data = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/player/ImmortalStoragePlayerData.java"));
        String custom = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/effect/custom/AdvancedWeaknessEffect.java"));
        assertTrue(effects.contains("EFFECTS.register(\"advanced_weakness\""));
        assertTrue(data.contains("ModEffects.ADVANCED_WEAKNESS.get()"));
        assertTrue(data.contains("new net.minecraft.world.effect.MobEffectInstance"));
        assertTrue(custom.contains("must not decrement that timer itself"));
        assertTrue(Files.isRegularFile(project.resolve(
                "src/main/resources/assets/immortalstorage/textures/mob_effect/advanced_weakness.png")));
    }

    @Test
    void modListConfigFactoryAndNormalDefaultsArePresent() throws IOException {
        Path project = locateProject();
        String client = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/ClientSetup.java"));
        String mod = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"));
        String config = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/config/ImmortalStorageConfig.java"));
        assertTrue(client.contains("IConfigScreenFactory.class"));
        assertTrue(client.contains("new ConfigurationScreen(container, parent)"));
        assertTrue(!mod.contains("net.neoforged.neoforge.client.gui"));
        assertTrue(config.contains("define(\"startWithJadeGuide\", true)"));
        assertTrue(config.contains("defineInRange(\"maximumStage\", 10, 1, 10)"));
        assertTrue(config.contains("define(\"stageTenInfiniteImmortalYuan\", false)"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
