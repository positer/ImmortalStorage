package com.immortalstorage.immortalstorage.dimension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protects the 26.1 WorldClock/WeatherData migration from regressing to legacy level-data fields. */
final class PersonalRealmEnvironmentTargetContractTest {
    @Test
    void realmUsesTargetClockPacketsAndPrivateWeatherData() throws Exception {
        String source = Files.readString(sourceFile());
        assertTrue(source.contains("public WeatherData getWeatherData()"));
        assertTrue(source.contains("this.realmWeatherData = new WeatherData()"));
        assertTrue(source.contains("new ClientboundSetTimePacket"));
        assertTrue(source.contains("new ClockNetworkState(lockedClockTime(), 0.0F, 0.0F)"));
        assertTrue(source.contains("ClientboundGameEventPacket.RAIN_LEVEL_CHANGE"));
        assertTrue(source.contains("ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE"));
        assertFalse(source.contains("realmLevelData.setDayTime("));
        assertFalse(source.contains("realmLevelData.setRaining("));
        assertFalse(source.contains("realmLevelData.setThundering("));
    }

    @Test
    void targetOverrideReplacesGeneratedLegacyClass() {
        Path generated = sourceFile().getParent().getParent().getParent().getParent().getParent()
                .resolve(Path.of("generated-java", "com", "immortalstorage", "immortalstorage",
                        "dimension", "PersonalRealmServerLevel.java"));
        assertFalse(Files.exists(generated),
                "the generated legacy class must be excluded when the 26.1 override exists");
    }

    private static Path sourceFile() {
        Path marker = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", "java", "com",
                "immortalstorage", "immortalstorage", "dimension",
                "PersonalRealmServerLevel.java");
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(marker);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate 26.1.2 personal realm override");
    }
}
