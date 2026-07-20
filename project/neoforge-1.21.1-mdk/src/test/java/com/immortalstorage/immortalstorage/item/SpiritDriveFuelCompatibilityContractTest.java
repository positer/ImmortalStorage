package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritDriveFuelCompatibilityContractTest {
    @Test
    void driveIsDiscoverableInStandardFuelSlotsButPaymentRemainsServerAuthoritative() throws Exception {
        Path root = locate("src/main");
        String item = Files.readString(root.resolve("java/com/immortalstorage/immortalstorage/item/custom/SpiritDriveItem.java"));
        String mixin = Files.readString(root.resolve("java/com/immortalstorage/immortalstorage/mixin/core/AbstractFurnaceSpiritDriveMixin.java"));
        assertTrue(item.contains("getBurnTime(ItemStack stack"));
        assertTrue(item.contains("owner(stack).isPresent() ? 1 : 0"));
        assertTrue(item.contains("hasCraftingRemainingItem(ItemStack stack)"));
        assertTrue(mixin.contains("payVanillaFurnaceFuel"));
        assertTrue(mixin.contains("if (!(fuel.getItem() instanceof SpiritDriveItem)) fuel.shrink(amount)"));
        for (String path : new String[]{"resources/data/c/tags/item/fuels.json",
                "resources/data/c/tags/item/furnace_fuels.json",
                "resources/data/neoforge/tags/item/fuels.json",
                "resources/data/minecraft/tags/item/furnace_fuels.json"}) {
            assertTrue(Files.readString(root.resolve(path)).contains("immortalstorage:spirit_drive"));
        }
    }

    private static Path locate(String relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException(relative + " not found");
    }
}
