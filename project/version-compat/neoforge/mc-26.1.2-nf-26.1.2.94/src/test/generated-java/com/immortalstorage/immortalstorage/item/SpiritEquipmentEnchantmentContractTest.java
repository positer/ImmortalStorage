package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritEquipmentEnchantmentContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void enchantmentTableIsDisabledButAnvilSupportDelegatesDynamically() throws Exception {
        Path root = locate();
        String sword = Files.readString(root.resolve("item/custom/SpiritSwordItem.java"));
        String instrument = Files.readString(root.resolve("item/custom/SpiritStaffItem.java"));
        assertTrue(sword.contains("getEnchantmentValue(ItemStack stack)"));
        assertTrue(sword.contains("return 0;"));
        assertTrue(sword.contains("new ItemStack(Items.NETHERITE_SWORD).supportsEnchantment(enchantment)"));
        assertTrue(instrument.contains("getEnchantmentValue(ItemStack stack)"));
        assertTrue(instrument.contains("return 0;"));
        assertTrue(instrument.contains("new ItemStack(Items.NETHERITE_PICKAXE).supportsEnchantment(enchantment)"));
    }

    private static Path locate() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("main sources not found");
    }
}
