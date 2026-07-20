package com.immortalstorage.immortalstorage.event;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TribulationFunctionalContractTest {
    private static final Path MAIN = locateMainSources();

    @Test
    void startIsBoundToTheOwnersPersonalRealmAndOneConfiguredTarget() throws IOException {
        String network = source("network", "ModNetwork.java");
        String body = methodBody(network, "private static void handleTriggerTribulation(");

        assertTrue(body.contains("isPersonalRealmFor"));
        assertTrue(body.contains("TribulationHelper.start"));
        assertFalse(body.contains("spawnWave"));
        assertFalse(body.contains("20 * 60"));
    }

    @Test
    void activeAttemptsSuppressPermanentBuffsAndHaveNoTimerFailure() throws IOException {
        String events = source("event", "CommonEvents.java");
        String tick = methodBody(events, "public void onPlayerTick(");

        assertTrue(tick.contains("suppressImmortalStorageBuffs"));
        assertTrue(tick.contains("tickTribulation"));
        assertFalse(events.contains("decrementTribulationTimer"));
        assertFalse(events.contains("getTribulationTimer"));
    }

    @Test
    void deathInEveryGameModeIsCancelledAfterFailureAndInPlaceRecovery() throws IOException {
        String events = source("event", "CommonEvents.java");
        String death = methodBody(events, "public void onLivingDeath(");

        assertTrue(death.contains("e.setCanceled(true)"));
        assertTrue(death.contains("setHealth"));
        assertTrue(death.contains("TribulationHelper.fail"));
        assertFalse(death.contains("isHardcore"));
    }

    @Test
    void targetCarriesPersistentIdentityAndExactCombatProfile() throws IOException {
        String helper = source("event", "TribulationHelper.java");

        assertTrue(helper.contains("setPersistenceRequired"));
        assertTrue(helper.contains("setGlowingTag(true)"));
        assertTrue(helper.contains("MobEffects.DAMAGE_BOOST"));
        assertTrue(helper.contains("startStage == 8"));
        assertTrue(helper.contains("MobEffects.DAMAGE_RESISTANCE"));
        assertTrue(helper.contains("Attributes.MAX_HEALTH"));
        assertTrue(helper.contains("setBaseValue(baseHealth * 10.0D)"));
        assertTrue(helper.contains("Enchantments.PROTECTION"));
        assertTrue(helper.contains("Enchantments.SHARPNESS"));
        assertTrue(helper.contains("startStage == 6 || startStage == 7"),
                "configured stage-six/seven hostiles receive the mandated equipment regardless of entity type");
    }

    @Test
    void restoredTargetsUseABoundedSameDimensionLoadGrace() throws IOException {
        String helper = source("event", "TribulationHelper.java");
        String reconcile = methodBody(helper, "public static void reconcile(");

        assertTrue(reconcile.contains("player.serverLevel().getEntity(targetId)"));
        assertTrue(reconcile.contains("noteTribulationTargetMissing(TARGET_LOAD_GRACE_TICKS)"));
        assertFalse(reconcile.contains("getAllLevels"),
                "active combat must not scan every server dimension every tick");
    }

    @Test
    void stageEightUsesVindicatorAndNeverAddsWither() throws IOException {
        String config = source("config", "ImmortalStorageConfig.java");
        String policy = source("progression", "TribulationPolicy.java");

        assertTrue(config.contains("define(\"stage8To9\", \"minecraft:vindicator\")"));
        assertTrue(policy.contains("case 8 -> ResourceLocation.withDefaultNamespace(\"vindicator\")"));
        assertTrue(policy.contains("return currentStage >= 9;"));
    }

    private static String source(String... parts) throws IOException {
        Path path = MAIN;
        for (String part : parts) path = path.resolve(part);
        return Files.readString(path);
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, () -> "missing method " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int index = open; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            else if (current == '}' && --depth == 0) return source.substring(open, index + 1);
        }
        throw new AssertionError("unterminated method " + signature);
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate main sources from "
                + Path.of("").toAbsolutePath());
    }
}
