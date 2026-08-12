package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceAutomationToggleTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void activePullAndPushDefaultOffAndPersistIndependently() {
        XianqiaoInterfaceBlockEntity original = entity();
        assertFalse(original.isActivePullEnabled());
        assertFalse(original.isActivePushEnabled());

        original.setActivePullEnabled(true);
        assertTrue(original.isActivePullEnabled());
        assertFalse(original.isActivePushEnabled());

        CompoundTag tag = new CompoundTag();
        original.saveAdditionalLegacy(tag, registries);
        XianqiaoInterfaceBlockEntity restored = entity();
        restored.loadAdditionalLegacy(tag, registries);
        assertTrue(restored.isActivePullEnabled());
        assertFalse(restored.isActivePushEnabled());
    }

    private static XianqiaoInterfaceBlockEntity entity() {
        return new XianqiaoInterfaceBlockEntity(
                BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
    }
}
