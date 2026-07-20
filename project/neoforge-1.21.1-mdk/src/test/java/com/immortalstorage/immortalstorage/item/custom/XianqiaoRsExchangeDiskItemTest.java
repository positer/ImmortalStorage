package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoRsExchangeDiskItemTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void bindingUsesRsSpecificPersistentIdentityAndCannotBeReassigned() {
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID owner = UUID.randomUUID();
        UUID disk = UUID.randomUUID();

        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(stack, owner, disk, "RsPlayer"));
        assertEquals(owner, XianqiaoRsExchangeDiskItem.owner(stack).orElseThrow());
        assertEquals(disk, XianqiaoRsExchangeDiskItem.diskId(stack).orElseThrow());
        assertEquals("RsPlayer", XianqiaoRsExchangeDiskItem.ownerName(stack).orElseThrow());
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(
                stack, UUID.randomUUID(), UUID.randomUUID()));

        ItemStack restored = ImmortalStoragePlayerData.loadStack(
                REGISTRIES, ImmortalStoragePlayerData.saveStack(REGISTRIES, stack));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(restored, owner, disk));
        assertEquals("RsPlayer", XianqiaoRsExchangeDiskItem.ownerName(restored).orElseThrow());
    }

    @Test
    void automatedCraftResultCanBeRegisteredFromThePlayersHand() {
        ItemStack automatedResult = new ItemStack(Items.PAPER);
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000401");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000402");

        assertTrue(XianqiaoRsExchangeDiskItem.owner(automatedResult).isEmpty());
        assertTrue(XianqiaoRsExchangeDiskItem.diskId(automatedResult).isEmpty());
        assertTrue(XianqiaoRsExchangeDiskItem.ownerName(automatedResult).isEmpty());
        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(
                automatedResult, owner, disk, "CurrentPlayer"));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(automatedResult, owner, disk));
        assertEquals("CurrentPlayer",
                XianqiaoRsExchangeDiskItem.ownerName(automatedResult).orElseThrow());
    }

    @Test
    void usernameRefreshNeverChangesRsMountIdentity() {
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000411");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000412");
        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(stack, owner, disk, "OldName"));

        assertTrue(XianqiaoRsExchangeDiskItem.refreshOwnerName(stack, owner, "NewName"));
        assertEquals(owner, XianqiaoRsExchangeDiskItem.owner(stack).orElseThrow());
        assertEquals(disk, XianqiaoRsExchangeDiskItem.diskId(stack).orElseThrow());
        assertEquals("NewName", XianqiaoRsExchangeDiskItem.ownerName(stack).orElseThrow());

        CompoundTag beforeForeignRefresh = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(XianqiaoRsExchangeDiskItem.refreshOwnerName(
                stack, UUID.randomUUID(), "Attacker"));
        assertEquals(beforeForeignRefresh, stack.get(DataComponents.CUSTOM_DATA).copyTag());
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(
                stack, owner, UUID.randomUUID(), "AnotherName"));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(stack, owner, disk));
    }

    @Test
    void rsAndAe2DiskIdentitiesNeverAlias() {
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID rsOwner = UUID.randomUUID();
        UUID rsDisk = UUID.randomUUID();
        UUID aeOwner = UUID.randomUUID();
        UUID aeDisk = UUID.randomUUID();

        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(stack, rsOwner, rsDisk));
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(stack, aeOwner, aeDisk));

        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(stack, rsOwner, rsDisk));
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(stack, aeOwner, aeDisk));
        assertFalse(XianqiaoRsExchangeDiskItem.isBoundTo(stack, aeOwner, aeDisk));
        assertFalse(XianqiaoExchangeCellItem.isBoundTo(stack, rsOwner, rsDisk));
    }

    @Test
    void ownerOnlyHistoricalDiskIsRepairedInPlaceForTheSameOwner() {
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID owner = UUID.randomUUID();
        UUID disk = UUID.randomUUID();
        CompoundTag legacy = new CompoundTag();
        legacy.putUUID("immortalstorageRsOwner", owner);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));

        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(
                stack, owner, disk, "LegacyRsOwner"));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundToOwner(stack, owner));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(stack, owner, disk));
        assertEquals("LegacyRsOwner",
                XianqiaoRsExchangeDiskItem.ownerName(stack).orElseThrow());
    }

    @Test
    void incompleteForeignOrAlreadyBoundDisksCannotBeClaimed() {
        UUID owner = UUID.randomUUID();
        UUID disk = UUID.randomUUID();

        ItemStack diskOnly = tagged("immortalstorageRsExchangeDisk", disk);
        CompoundTag beforeDiskOnly = diskOnly.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(diskOnly, owner, UUID.randomUUID()));
        assertEquals(beforeDiskOnly, diskOnly.get(DataComponents.CUSTOM_DATA).copyTag());

        ItemStack foreignOwner = tagged("immortalstorageRsOwner", UUID.randomUUID());
        CompoundTag beforeForeign = foreignOwner.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(foreignOwner, owner, disk));
        assertEquals(beforeForeign, foreignOwner.get(DataComponents.CUSTOM_DATA).copyTag());

        ItemStack bound = new ItemStack(Items.PAPER);
        assertTrue(XianqiaoRsExchangeDiskItem.bindUnbound(bound, owner, disk));
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(bound, owner, UUID.randomUUID()));
        assertTrue(XianqiaoRsExchangeDiskItem.isBoundTo(bound, owner, disk));

        ItemStack malformedOwner = new ItemStack(Items.PAPER);
        CompoundTag malformed = new CompoundTag();
        malformed.putString("immortalstorageRsOwner", owner.toString());
        malformedOwner.set(DataComponents.CUSTOM_DATA, CustomData.of(malformed));
        assertFalse(XianqiaoRsExchangeDiskItem.bindUnbound(malformedOwner, owner, disk));
        assertFalse(XianqiaoRsExchangeDiskItem.isBoundToOwner(malformedOwner, owner));
    }

    private static ItemStack tagged(String key, UUID value) {
        ItemStack stack = new ItemStack(Items.PAPER);
        CompoundTag tag = new CompoundTag();
        tag.putUUID(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
