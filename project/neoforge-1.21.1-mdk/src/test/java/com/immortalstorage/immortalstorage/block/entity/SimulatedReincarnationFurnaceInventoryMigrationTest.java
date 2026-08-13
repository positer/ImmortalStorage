package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimulatedReincarnationFurnaceInventoryMigrationTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void fifteenSlotSharedInventoryKeepsExistingOutputs() {
        ItemStackHandler legacy = new ItemStackHandler(15);
        legacy.setStackInSlot(SimulatedReincarnationFurnaceBlockEntity.OUTPUT_START,
                new ItemStack(Items.DIAMOND, 7));
        CompoundTag saved = new CompoundTag();
        saved.put("Items", legacy.serializeNBT(registries));

        SimulatedReincarnationFurnaceBlockEntity furnace = new SimulatedReincarnationFurnaceBlockEntity(
                BlockPos.ZERO, ModBlocks.SIMULATED_REINCARNATION_FURNACE.get().defaultBlockState());
        furnace.loadAdditional(saved, registries);

        assertEquals(SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT, furnace.itemHandler().getSlots());
        assertEquals(7, furnace.itemHandler().getStackInSlot(
                SimulatedReincarnationFurnaceBlockEntity.OUTPUT_START).getCount());
        assertTrue(furnace.reinforcementPlugin().isEmpty());
    }
}
