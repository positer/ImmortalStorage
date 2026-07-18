package com.cultivation.cultivation.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritEquipmentEnchantmentContractTest {
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
            Path candidate = current.resolve("src/main/java/com/cultivation/cultivation");
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("main sources not found");
    }
}
