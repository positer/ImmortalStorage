package com.immortalstorage.immortalstorage.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalArmorLayoutContractTest {
    @Test
    void bothTerminalsBindVanillaArmorSlotsToDynamicInventoryGeometry() throws Exception {
        Path root = locate();
        String kong = Files.readString(root.resolve("menu/custom/KongqiaoMenu.java"));
        String xian = Files.readString(root.resolve("menu/custom/XianqiaoStorageMenu.java"));
        String kongScreen = Files.readString(root.resolve("client/screen/KongqiaoScreen.java"));
        String xianScreen = Files.readString(root.resolve("client/screen/XianqiaoStorageScreen.java"));
        String armor = Files.readString(root.resolve("menu/custom/TerminalArmorSlot.java"));
        assertTrue(kong.contains("new TerminalArmorSlot"));
        assertTrue(xian.contains("new TerminalArmorSlot"));
        assertTrue(kongScreen.contains("TerminalLayout.inventoryY(this.imageHeight)"));
        assertTrue(xianScreen.contains("TerminalLayout.inventoryY(this.imageHeight)"));
        assertTrue(armor.contains("owner.onEquipItem"));
        assertTrue(armor.contains("PREVENT_ARMOR_CHANGE"));
        assertTrue(armor.contains("getNoItemIcon"));
    }

    private static Path locate() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/immortalstorage/immortalstorage");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("main sources not found");
    }
}
