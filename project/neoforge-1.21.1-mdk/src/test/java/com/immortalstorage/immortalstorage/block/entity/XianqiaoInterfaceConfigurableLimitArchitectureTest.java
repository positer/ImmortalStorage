package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceConfigurableLimitArchitectureTest {
    @Test
    void serverConfigOwnsSharedDefaultsAndEveryClampUsesTheSharedReader() throws IOException {
        Path java = locateMainSources();
        String config = read(java, "config/ImmortalStorageConfig.java");
        String limits = read(java, "block/entity/XianqiaoInterfaceLimits.java");
        String inventory = read(java, "block/entity/XianqiaoInterfaceInventory.java");
        String network = read(java, "network/ModNetwork.java");

        assertTrue(config.contains("xianqiaoInterfaceItemSlotLimit"));
        assertTrue(config.contains("xianqiaoInterfaceFluidSlotLimitMb"));
        assertTrue(limits.contains("DEFAULT_ITEM_TARGET = 128"));
        assertTrue(limits.contains("DEFAULT_FLUID_TARGET_MB = 16_000"));
        assertTrue(inventory.contains("XianqiaoInterfaceLimits"));
        assertTrue(network.contains("XianqiaoInterfaceLimits.itemTargetLimit()"));
        assertTrue(network.contains("XianqiaoInterfaceLimits.fluidTargetLimitMb()"));
        assertFalse(network.contains("Math.min(99L"));
        assertFalse(network.contains("MAX_FLUID_TARGET_MB"));
    }

    @Test
    void serverLimitsAreSynchronizedToThePopupAndAmountsAreCustomRendered() throws IOException {
        Path java = locateMainSources();
        String menu = read(java, "menu/custom/XianqiaoInterfaceMenu.java");
        String screen = read(java, "client/screen/XianqiaoInterfaceScreen.java");

        assertTrue(menu.contains("getItemTargetLimit"));
        assertTrue(menu.contains("getFluidTargetLimitMb"));
        assertTrue(screen.contains("menu.getItemTargetLimit()"));
        assertTrue(screen.contains("menu.getFluidTargetLimitMb()"));
        assertTrue(screen.contains("renderSlotContents"));
        assertTrue(screen.contains("renderAmountOverlay"));
        assertFalse(screen.contains("Math.min(99"));
        assertFalse(screen.contains("MAX_FLUID_TARGET_MB"));
    }

    private static String read(Path java, String relative) throws IOException {
        return Files.readString(java.resolve(Path.of(relative)));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources");
    }
}
