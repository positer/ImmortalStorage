package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalRuinForgedSpiritSwordContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void teleportedTargetsAreRestrainedAndPlayerTargetingIsConfigurable() throws IOException {
        String sword = read("item/custom/ImmortalRuinForgedSpiritSwordItem.java");
        String config = read("config/ImmortalStorageConfig.java");

        assertTrue(sword.contains("TELEPORT_RESTRAINT_TICKS = 40"));
        assertTrue(sword.contains("inflate(13.0D)"));
        assertTrue(sword.contains("AbsoluteRestraint.apply("));
        assertTrue(sword.contains("MobEffects.SLOWNESS"));
        assertTrue(sword.contains("TELEPORT_RESTRAINT_TICKS, 255"));
        assertTrue(sword.contains("entity -> canTeleport(serverPlayer, entity)"));
        assertTrue(sword.contains("!(entity instanceof Player)"));
        assertTrue(sword.contains("IMMORTAL_RUIN_SWORD_AFFECTS_OTHER_PLAYERS.get()"));
        assertTrue(config.contains("define(\"affectsOtherPlayers\", true)"));
    }

    private static String read(String relative) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com",
                    "immortalstorage", "immortalstorage")).resolve(relative);
            if (Files.isRegularFile(candidate)) return Files.readString(candidate);
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate " + relative);
    }
}
