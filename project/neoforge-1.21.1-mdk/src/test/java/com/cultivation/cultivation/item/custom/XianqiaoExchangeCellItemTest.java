package com.cultivation.cultivation.item.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoExchangeCellItemTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void bindingStoresImmutableOwnerAndIndependentDiskIdentity() {
        // Binding is a Data Components contract. A registered vanilla carrier
        // avoids constructing an intrusive Item after test registries freeze.
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID owner = UUID.randomUUID();
        UUID disk = UUID.randomUUID();

        assertTrue(XianqiaoExchangeCellItem.bindUnbound(stack, owner, disk, "TestPlayer"));
        assertEquals(owner, XianqiaoExchangeCellItem.owner(stack).orElseThrow());
        assertEquals(disk, XianqiaoExchangeCellItem.diskId(stack).orElseThrow());
        assertEquals("TestPlayer", XianqiaoExchangeCellItem.ownerName(stack).orElseThrow());
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(stack, owner, disk));

        assertFalse(XianqiaoExchangeCellItem.bindUnbound(
                stack, UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(owner, XianqiaoExchangeCellItem.owner(stack).orElseThrow());
        assertEquals(disk, XianqiaoExchangeCellItem.diskId(stack).orElseThrow());

        ItemStack restored = CultivationPlayerData.loadStack(
                REGISTRIES, CultivationPlayerData.saveStack(REGISTRIES, stack));
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(restored, owner, disk),
                "owner and disk identities must survive an ordinary ItemStack codec round trip");
        assertEquals("TestPlayer", XianqiaoExchangeCellItem.ownerName(restored).orElseThrow());
    }

    @Test
    void automatedCraftResultRemainsUnboundUntilHandRegistration() {
        ItemStack automatedResult = new ItemStack(Items.PAPER);
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000102");

        assertTrue(XianqiaoExchangeCellItem.owner(automatedResult).isEmpty());
        assertTrue(XianqiaoExchangeCellItem.diskId(automatedResult).isEmpty());
        assertTrue(XianqiaoExchangeCellItem.ownerName(automatedResult).isEmpty());

        assertTrue(XianqiaoExchangeCellItem.bindUnbound(
                automatedResult, owner, disk, "CurrentPlayer"));
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(automatedResult, owner, disk));
        assertEquals("CurrentPlayer",
                XianqiaoExchangeCellItem.ownerName(automatedResult).orElseThrow());
    }

    @Test
    void usernameRefreshIsDisplayOnlyAndCannotRebindIdentity() {
        ItemStack stack = new ItemStack(Items.PAPER);
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000111");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000112");
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(stack, owner, disk, "OldName"));

        assertTrue(XianqiaoExchangeCellItem.refreshOwnerName(stack, owner, "NewName"));
        assertEquals(owner, XianqiaoExchangeCellItem.owner(stack).orElseThrow());
        assertEquals(disk, XianqiaoExchangeCellItem.diskId(stack).orElseThrow());
        assertEquals("NewName", XianqiaoExchangeCellItem.ownerName(stack).orElseThrow());

        CompoundTag beforeForeignRefresh = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(XianqiaoExchangeCellItem.refreshOwnerName(
                stack, UUID.randomUUID(), "Attacker"));
        assertEquals(beforeForeignRefresh, stack.get(DataComponents.CUSTOM_DATA).copyTag());
        assertFalse(XianqiaoExchangeCellItem.bindUnbound(
                stack, owner, UUID.randomUUID(), "AnotherName"));
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(stack, owner, disk));
    }

    @Test
    void oneOwnerCanHaveIndependentCellIdentitiesForPerGridDeduplication() {
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ItemStack firstCell = new ItemStack(Items.PAPER);
        ItemStack secondCell = new ItemStack(Items.PAPER);

        assertTrue(XianqiaoExchangeCellItem.bindUnbound(firstCell, owner, first));
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(secondCell, owner, second));
        assertEquals(owner, XianqiaoExchangeCellItem.owner(firstCell).orElseThrow());
        assertEquals(owner, XianqiaoExchangeCellItem.owner(secondCell).orElseThrow());
        assertEquals(first, XianqiaoExchangeCellItem.diskId(firstCell).orElseThrow());
        assertEquals(second, XianqiaoExchangeCellItem.diskId(secondCell).orElseThrow());
        assertFalse(XianqiaoExchangeCellItem.isBoundTo(firstCell, owner, second));
    }

    @Test
    void ownerOnlyHistoricalCellIsRepairedInPlaceForTheSameOwner() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID repairedDisk = UUID.fromString("00000000-0000-0000-0000-000000000202");
        ItemStack historical = new ItemStack(Items.PAPER);
        CompoundTag legacy = new CompoundTag();
        legacy.putUUID("cultivationOwner", owner);
        historical.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));

        assertTrue(XianqiaoExchangeCellItem.bindUnbound(
                historical, owner, repairedDisk, "LegacyOwner"),
                "an owner-only stack from the earlier format must gain its missing disk id");
        assertTrue(XianqiaoExchangeCellItem.isBoundTo(historical, owner, repairedDisk));
        assertTrue(XianqiaoExchangeCellItem.isBoundToOwner(historical, owner));
        assertEquals("LegacyOwner", XianqiaoExchangeCellItem.ownerName(historical).orElseThrow());
    }

    @Test
    void incompleteOrForeignBindingsCannotBeClaimed() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID otherOwner = UUID.fromString("00000000-0000-0000-0000-000000000302");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000303");

        ItemStack diskOnly = new ItemStack(Items.PAPER);
        CompoundTag invalid = new CompoundTag();
        invalid.putUUID("cultivationExchangeDisk", disk);
        diskOnly.set(DataComponents.CUSTOM_DATA, CustomData.of(invalid));
        assertFalse(XianqiaoExchangeCellItem.bindUnbound(diskOnly, owner, UUID.randomUUID()));
        assertTrue(XianqiaoExchangeCellItem.owner(diskOnly).isEmpty());
        assertEquals(disk, XianqiaoExchangeCellItem.diskId(diskOnly).orElseThrow());
        assertFalse(XianqiaoExchangeCellItem.isBoundToOwner(diskOnly, owner));

        ItemStack foreignOwnerOnly = new ItemStack(Items.PAPER);
        CompoundTag foreign = new CompoundTag();
        foreign.putUUID("cultivationOwner", otherOwner);
        foreignOwnerOnly.set(DataComponents.CUSTOM_DATA, CustomData.of(foreign));
        assertFalse(XianqiaoExchangeCellItem.bindUnbound(
                foreignOwnerOnly, owner, UUID.randomUUID()));
        assertEquals(otherOwner, XianqiaoExchangeCellItem.owner(foreignOwnerOnly).orElseThrow());
        assertTrue(XianqiaoExchangeCellItem.diskId(foreignOwnerOnly).isEmpty());
        assertFalse(XianqiaoExchangeCellItem.isBoundToOwner(foreignOwnerOnly, owner));
    }
}
