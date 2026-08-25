package com.immortalstorage.immortalstorage.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraGuardAbilityContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @Test
    void jumpPressUsesAnAirborneEdgeToggleAndNeverSendsTheLegacyLeap() throws IOException {
        String keys = read("src/main/java/com/immortalstorage/immortalstorage/client/keybind/ImmortalStorageKeybinds.java");
        String network = read("src/main/java/com/immortalstorage/immortalstorage/network/ModNetwork.java");
        assertTrue(keys.contains("ClientTickEvent.Pre"));
        assertTrue(keys.contains("boolean jumpPressed = jumpDown && !jumpWasDown"));
        assertTrue(keys.contains("if (player.onGround()) return"));
        assertTrue(keys.contains("new ModPayloads.AuraGuardFlightState(false)"));
        assertTrue(keys.contains("new ModPayloads.AuraGuardFlightState(true)"));
        assertTrue(!keys.contains("new ModPayloads.AuraGuardLeap"));
        assertTrue(network.contains("handleAuraGuardFlightState"));
        assertTrue(network.contains("player.onGround() || player.isPassenger()"));
    }

    @Test
    void virtualElytraAndDamageImmunitiesAreInstalled() throws IOException {
        String living = read("src/main/java/com/immortalstorage/immortalstorage/mixin/core/LivingEntityAuraElytraMixin.java");
        String player = read("src/main/java/com/immortalstorage/immortalstorage/mixin/core/PlayerAuraElytraMixin.java");
        String combat = read("src/main/java/com/immortalstorage/immortalstorage/combat/ImmortalMasterTalismanService.java");
        String layer = read("src/main/java/com/immortalstorage/immortalstorage/client/render/AuraGuardElytraLayer.java");
        assertTrue(living.contains("method = \"updateFallFlying\""));
        assertTrue(living.contains("method = \"travel\""));
        assertTrue(living.contains("ordinal = 2"));
        assertTrue(living.contains("movement.scale(2.0D)"));
        assertTrue(player.contains("method = \"tryToStartFallFlying\""));
        assertTrue(player.contains("self.startFallFlying()"));
        assertTrue(!living.contains("!self.isFallFlying()\n                && !self.onGround()"));
        assertTrue(living.contains("getBoolean(\"ImmortalStorageVirtualElytra\")"));
        assertTrue(layer.contains("player.isFallFlying()"));
        assertTrue(layer.contains("ImmortalStorageVirtualElytra"));
        assertTrue(layer.contains("getParentModel().copyPropertiesTo(model)"));
        assertTrue(layer.contains("poses.translate(0.0F, 0.0F, 0.125F)"));
        assertTrue(read("src/main/java/com/immortalstorage/immortalstorage/network/ModNetwork.java")
                .contains("player.startFallFlying()"));
        assertTrue(combat.contains("DamageTypeTags.IS_FALL"));
        assertTrue(combat.contains("DamageTypes.FLY_INTO_WALL"));
        assertTrue(combat.contains("repairBypassedDamage"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
