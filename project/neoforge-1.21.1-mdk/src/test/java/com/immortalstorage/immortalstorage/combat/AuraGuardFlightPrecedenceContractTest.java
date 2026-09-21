package com.immortalstorage.immortalstorage.combat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The virtual elytra must never take the jump key away from creative (or
 * cultivation) flight.
 *
 * <p>Real-client QA reported that an actively flying player who tapped jump got
 * the virtual elytra deployed instead of toggling flight, because the guard
 * expanded for a flying player and then consumed the following jump presses.
 * Vanilla ({@code LocalPlayer#aiStep}) only deploys an elytra while
 * {@code !abilities.flying} and never on the second press of a flight
 * double-tap, so every lane now mirrors those two conditions.</p>
 */
final class AuraGuardFlightPrecedenceContractTest {
    @Test
    void clientJumpHandlingLeavesActiveFlightToVanilla() throws IOException {
        Path project = locateProject();
        String keybinds = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/keybind/ImmortalStorageKeybinds.java"));

        int toggleWindow = keybinds.indexOf("if (jumpFlightToggleTicks > 0) jumpFlightToggleTicks--;");
        int toggleGuard = keybinds.indexOf("boolean flightTogglePress = jumpFlightToggleTicks > 0;");
        int toggleReturn = keybinds.indexOf("if (flightTogglePress) return;");
        int stopBranch = keybinds.indexOf("boolean virtualExpanded = player.isFallFlying()");
        int startBranch = keybinds.indexOf("player.tryToStartFallFlying()");
        assertTrue(toggleWindow >= 0 && toggleGuard > toggleWindow && toggleReturn > toggleGuard,
                "the flight double-tap window must be replicated before any elytra handling");
        assertTrue(stopBranch > toggleReturn && startBranch > stopBranch,
                "the second tap of a flight double-tap must never reach the elytra branch");
        assertTrue(keybinds.contains("!player.onClimbable()"),
                "the deploy branch must mirror vanilla's climbing guard");
        assertTrue(keybinds.contains("&& !player.getAbilities().flying"),
                "the deploy branch must mirror vanilla's !abilities.flying guard");
    }

    @Test
    void everyLaneRefusesToDeployOrKeepAGlideWhileFlying() throws IOException {
        Path project = locateProject();
        String playerMixin = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/mixin/core/PlayerAuraElytraMixin.java"));
        assertTrue(playerMixin.contains("!self.getAbilities().flying"),
                "the shared tryToStartFallFlying hook must mirror vanilla's !abilities.flying condition");

        String livingMixin = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/mixin/core/LivingEntityAuraElytraMixin.java"));
        assertTrue(livingMixin.contains("boolean activelyFlying"),
                "the per-tick marker maintenance must detect active flight");
        assertTrue(livingMixin.contains("player.getAbilities().flying"));
        assertTrue(livingMixin.contains("remove(\"ImmortalStorageVirtualElytra\")"));

        String network = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/network/ModNetwork.java"));
        int handler = network.indexOf("private static void handleAuraGuardFlightState(");
        assertTrue(handler >= 0, "flight-state handler missing");
        String body = network.substring(handler, network.indexOf("\n    private static ", handler + 1));
        assertTrue(body.contains("player.getAbilities().flying"),
                "the server must not start a virtual glide for a flying player");
        assertFalse(body.contains("getAbilities().mayfly"),
                "cultivation flight (mayfly) must still allow the aura-guard glide");
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
