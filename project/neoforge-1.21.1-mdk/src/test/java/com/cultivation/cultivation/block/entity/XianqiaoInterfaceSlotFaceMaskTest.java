package com.cultivation.cultivation.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceSlotFaceMaskTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void eachConfiguredSlotPersistsItsOwnSixFaceOutputMask() {
        FakeStorage storage = new FakeStorage();
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                storage, () -> true);
        assertTrue(inventory.setTarget(0, new ItemStack(Items.STONE, 8)));
        assertTrue(inventory.setTarget(1, new ItemStack(Items.DIRT, 8)));

        assertTrue(inventory.setOutputFaceEnabled(0, Direction.NORTH, true));
        assertTrue(inventory.setOutputFaceEnabled(1, Direction.SOUTH, true));
        assertTrue(inventory.isOutputFaceEnabled(0, Direction.NORTH));
        assertFalse(inventory.isOutputFaceEnabled(0, Direction.SOUTH));
        assertTrue(inventory.isOutputFaceEnabled(1, Direction.SOUTH));

        CompoundTag saved = new CompoundTag();
        inventory.saveState(saved, registries);
        XianqiaoInterfaceInventory restored = new XianqiaoInterfaceInventory(storage, () -> true);
        restored.loadState(saved, registries);

        assertTrue(restored.isOutputFaceEnabled(0, Direction.NORTH));
        assertFalse(restored.isOutputFaceEnabled(0, Direction.SOUTH));
        assertTrue(restored.isOutputFaceEnabled(1, Direction.SOUTH));
        assertEquals(inventory.getOutputFaceMask(0), restored.getOutputFaceMask(0));
    }

    @Test
    void clearingOrReplacingAResourceResetsItsOutputMask() {
        XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
                new FakeStorage(), () -> true);
        assertTrue(inventory.setTarget(0, new ItemStack(Items.STONE, 8)));
        assertTrue(inventory.setOutputFaceEnabled(0, Direction.UP, true));
        assertTrue(inventory.clearSlot(0));
        assertEquals(0, inventory.getOutputFaceMask(0));

        assertTrue(inventory.setTarget(0, new ItemStack(Items.DIRT, 8)));
        assertFalse(inventory.isOutputFaceEnabled(0, Direction.UP));
    }

    private static final class FakeStorage implements com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage {
        @Override public long revision() { return 0L; }
        @Override public java.util.List<com.cultivation.cultivation.api.storage.terminal.StorageItemSummary> snapshot() {
            return java.util.List.of();
        }
        @Override public long insert(com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey key,
                                     long amount,
                                     com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction action) {
            return amount;
        }
        @Override public long extract(com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey key,
                                      long amount,
                                      com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction action) {
            return amount;
        }
    }
}
