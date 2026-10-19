package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritEquipmentEnchantmentContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    /**
     * User ruling B (2026-10-19): the equipment is a real enchantable item (positive value, so the anvil, the
     * enchanting table and other mods recognise it) and the table may offer enchantments on it — there is no
     * primary-item veto left. The mod's own enchantment is an ordinary table candidate, not treasure.
     */
    @Test
    void equipmentIsEnchantableAndTheTableIsOpen() throws Exception {
        Path root = locate();
        String sword = Files.readString(root.resolve("item/custom/SpiritSwordItem.java"));
        String instrument = Files.readString(root.resolve("item/custom/SpiritStaffItem.java"));
        String artifact = Files.readString(root.resolve("item/custom/ImmortalArtifactItem.java"));

        assertTrue(instrument.contains("public static final int ENCHANTMENT_VALUE = 30;"));
        assertTrue(instrument.contains("getEnchantmentValue(ItemStack stack)"));
        assertTrue(instrument.contains("return ENCHANTMENT_VALUE;"));
        assertTrue(instrument.contains("new ItemStack(Items.NETHERITE_PICKAXE).supportsEnchantment(enchantment)"));

        assertTrue(sword.contains("getEnchantmentValue(ItemStack stack)"));
        assertTrue(sword.contains("return SpiritStaffItem.ENCHANTMENT_VALUE;"));
        assertTrue(sword.contains("new ItemStack(Items.NETHERITE_SWORD).supportsEnchantment(enchantment)"));

        // User ruling 2026-10-19 B: no primary-item veto anywhere, so the table may offer enchantments here.
        assertFalse(instrument.contains("isPrimaryItemFor"), "the instrument must not veto table enchantments");
        assertFalse(sword.contains("isPrimaryItemFor"), "the swords must not veto table enchantments");

        // The enchantment's own data (table candidate, no longer treasure) is pinned by SpiritRepairResourceTest,
        // which walks every resource copy of the tag; the mod's target line ships a vanilla copy of the same path.

        // The durability-free artifact needs its own gate. 1.21.1 declares it on the item; 26.1.2 has no such
        // method and the ENCHANTABLE component that ModItems writes carries both the gate and the value instead.
        boolean targetLine = root.toString().replace('\\', '/').contains("mc-26.1.2");
        if (targetLine) {
            assertFalse(artifact.contains("isEnchantable"), "26.1.2 has no Item#isEnchantable to override");
            assertTrue(Files.readString(root.resolve("item/ModItems.java")).contains("return props.enchantable(value);"),
                    "the target line must register the value as the ENCHANTABLE component");
        } else {
            assertTrue(artifact.contains("public boolean isEnchantable(ItemStack stack) {"));
            assertTrue(artifact.contains("return true;"));
        }
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
