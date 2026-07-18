package com.cultivation.cultivation.effect;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedWeaknessRegistrationTest {
    @Test
    void effectIsRegisteredProjectedAndHasAVisibleIcon() throws IOException {
        Path project = locateProject();
        String effects = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/effect/ModEffects.java"));
        String data = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/player/CultivationPlayerData.java"));
        String custom = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/effect/custom/AdvancedWeaknessEffect.java"));
        assertTrue(effects.contains("EFFECTS.register(\"advanced_weakness\""));
        assertTrue(data.contains("ModEffects.ADVANCED_WEAKNESS.get()"));
        assertTrue(data.contains("new net.minecraft.world.effect.MobEffectInstance"));
        assertTrue(custom.contains("must not decrement that timer itself"));
        assertTrue(Files.isRegularFile(project.resolve(
                "src/main/resources/assets/cultivation/textures/mob_effect/advanced_weakness.png")));
    }

    @Test
    void modListConfigFactoryAndNormalDefaultsArePresent() throws IOException {
        Path project = locateProject();
        String client = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/client/ClientSetup.java"));
        String mod = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/CultivationMod.java"));
        String config = Files.readString(project.resolve(
                "src/main/java/com/cultivation/cultivation/config/CultivationConfig.java"));
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
            if (Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
