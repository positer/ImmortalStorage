package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinManagerInventoryTest {
    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void seventyTwoMemberSlotsAcceptExactlyOneSourceBlockOnly() {
        SourceVeinManagerInventory inventory = new SourceVeinManagerInventory(() -> {});
        ItemStack source = source(VeinKind.COBBLE, 4);

        assertEquals(72, inventory.getSlots());
        assertTrue(inventory.isItemValid(0, source));
        assertFalse(inventory.isItemValid(0, new ItemStack(Items.COBBLESTONE)));

        ItemStack remainder = inventory.insertItem(0, source, false);
        assertEquals(3, remainder.getCount());
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertEquals(1, inventory.getSlotLimit(0));
        assertEquals(1, inventory.extractItem(0, 64, false).getCount());
    }

    @Test
    void freeMemberUsesItsPersistedBlockEntityCacheInsteadOfADisplayOnlySentinel() {
        ItemStack source = source(VeinKind.COBBLE, 1);

        assertEquals(Long.MAX_VALUE,
                SourceVeinManagerInventory.reconcileMemberCache(source, VeinKind.COBBLE, 0L));
        assertEquals(Long.MAX_VALUE, SourceVeinManagerInventory.cachedUnits(source));
        assertEquals(Long.MAX_VALUE,
                source.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getLong("CachedUnits"));
    }

    @Test
    void oneManagerRejectsASecondMemberWithTheSameStableDefinitionId() {
        SourceVeinManagerInventory inventory = new SourceVeinManagerInventory(() -> {});
        ItemStack first = source(VeinKind.COBBLE, 1);
        ItemStack duplicate = source(VeinKind.COBBLE, 1);

        assertTrue(inventory.insertItem(0, first, false).isEmpty());
        assertEquals(1, inventory.insertItem(1, duplicate, false).getCount());
        assertTrue(inventory.getStackInSlot(1).isEmpty());
        assertFalse(inventory.isItemValid(1, duplicate));
    }

    @Test
    void legacyDuplicateMembersRemainExtractableButOnlyTheFirstIsActive() {
        ItemStackHandler legacy = new ItemStackHandler(SourceVeinManagerInventory.SLOT_COUNT);
        legacy.setStackInSlot(2, source(VeinKind.COBBLE, 1));
        legacy.setStackInSlot(9, source(VeinKind.COBBLE, 1));

        SourceVeinManagerInventory inventory = new SourceVeinManagerInventory(() -> {});
        inventory.deserializeNBT(registries, legacy.serializeNBT(registries));
        inventory.reconcileLoadedMembers();

        assertFalse(inventory.getStackInSlot(2).isEmpty());
        assertFalse(inventory.getStackInSlot(9).isEmpty());
        assertTrue(inventory.isActiveMember(2));
        assertFalse(inventory.isActiveMember(9));
        assertEquals(1, inventory.inactiveDuplicateCount());
        assertEquals(1, inventory.extractItem(9, 1, false).getCount(),
                "inactive legacy duplicates must remain removable without data loss");
    }

    @Test
    void preservedManagerOwnershipCannotBePlacedByAnotherPlayer() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        ItemStack manager = new ItemStack(ModBlocks.SOURCE_VEIN_MANAGER.get());
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", owner);
        manager.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));

        assertTrue(SourceVeinManagerBlockEntity.canPlaceStackFor(manager, owner));
        assertFalse(SourceVeinManagerBlockEntity.canPlaceStackFor(manager, stranger));
        assertTrue(SourceVeinManagerBlockEntity.canPlaceStackFor(
                new ItemStack(ModBlocks.SOURCE_VEIN_MANAGER.get()), stranger));
    }

    private static ItemStack source(VeinKind kind, int count) {
        if (kind != VeinKind.COBBLE) throw new IllegalArgumentException("test source not registered: " + kind);
        return new ItemStack(ModBlocks.COBBLESTONE_VEIN.get(), count);
    }
}
