package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceMultiFaceContractTest {
    @Test
    void faceEditorSupportsRepeatedIndependentTogglesAndUsesBrightnessOnly() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("cannot locate project root");
        String screen = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/screen/XianqiaoInterfaceScreen.java"));
        String network = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/network/ModNetwork.java"));
        String menu = Files.readString(root.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/menu/custom/XianqiaoInterfaceMenu.java"));

        assertTrue(screen.contains("setAlpha(enabled ? 1.0F : 0.35F)"));
        assertFalse(screen.contains("enabled ? \"✓\" : \"×\""));
        assertTrue(network.contains("player, payload.containerId(), payload.blockPos());"));
        assertTrue(network.contains("player.containerMenu.broadcastChanges();"));
        assertTrue(menu.contains("!\"mekanism_chemical\".equals(key.channel())"));
    }
}
