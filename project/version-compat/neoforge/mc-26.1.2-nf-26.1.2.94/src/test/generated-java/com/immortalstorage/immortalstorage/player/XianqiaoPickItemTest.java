package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class XianqiaoPickItemTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    private static ImmortalStoragePlayerData data() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        return data;
    }

    private static long stored(ImmortalStoragePlayerData data, ItemStack stack) {
        return data.extractXianqiaoItem(TerminalEntryKey.of(stack), Long.MAX_VALUE, TerminalStorageAction.SIMULATE);
    }

    @Test void withdrawsOneNormalStackFromLongStockAndCommitsOneRevision() {
        var data = data();
        var inventory = new SimpleContainer(41);
        var target = new ItemStack(Items.STONE);
        long amount = (long) Integer.MAX_VALUE + 100;
        data.insertXianqiaoItem(TerminalEntryKey.of(target), amount, TerminalStorageAction.EXECUTE);
        long revision = data.getXianqiaoStorageRevision();
        assertTrue(XianqiaoPickItem.moveToHand(data, inventory, 0, target));
        assertEquals(64, inventory.getItem(0).getCount());
        assertEquals(amount - 64, stored(data, target));
        assertEquals(revision + 1, data.getXianqiaoStorageRevision());
    }

    @Test void mergesOldHandBeforeEmptySlotsThenStoresOnlyTheRemainder() {
        var data = data();
        var inventory = new SimpleContainer(41);
        inventory.setItem(0, new ItemStack(Items.DIRT, 64));
        inventory.setItem(2, new ItemStack(Items.DIRT, 60));
        data.insertStack(new ItemStack(Items.STONE, 3), true);
        assertTrue(XianqiaoPickItem.moveToHand(data, inventory, 0, new ItemStack(Items.STONE)));
        assertEquals(3, inventory.getItem(0).getCount());
        assertEquals(64, inventory.getItem(2).getCount());
        assertEquals(60, inventory.getItem(1).getCount());
        assertEquals(0, stored(data, new ItemStack(Items.DIRT)));
    }

    @Test void fullInventoryStoresOldHandWithoutUsingArmorOrOffhandAndPreservesComponents() {
        var data = data();
        var inventory = new SimpleContainer(41);
        for (int i = 0; i < 36; i++) inventory.setItem(i, new ItemStack(Items.DIRT, 64));
        var old = new ItemStack(Items.DIAMOND_SWORD);
        old.set(DataComponents.CUSTOM_NAME, Component.literal("keep me"));
        old.setDamageValue(17);
        inventory.setItem(0, old.copy());
        data.insertStack(new ItemStack(Items.STONE, 8), true);
        long revision = data.getXianqiaoStorageRevision();
        assertTrue(XianqiaoPickItem.moveToHand(data, inventory, 0, new ItemStack(Items.STONE)));
        assertEquals(1, stored(data, old));
        assertEquals(0, stored(data, new ItemStack(Items.DIAMOND_SWORD)));
        assertEquals(revision + 1, data.getXianqiaoStorageRevision());
        for (int i = 36; i < 41; i++) assertTrue(inventory.getItem(i).isEmpty());
    }

    @Test void missingOrComponentMismatchedTargetLeavesEverythingUntouched() {
        var data = data();
        var inventory = new SimpleContainer(41);
        inventory.setItem(0, new ItemStack(Items.DIRT, 12));
        var named = new ItemStack(Items.STONE, 5);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("different"));
        data.insertStack(named, true);
        long revision = data.getXianqiaoStorageRevision();
        assertFalse(XianqiaoPickItem.moveToHand(data, inventory, 0, new ItemStack(Items.STONE)));
        assertEquals(12, inventory.getItem(0).getCount());
        assertEquals(5, stored(data, named));
        assertEquals(revision, data.getXianqiaoStorageRevision());
    }

    @Test void sameHandAndUnavailableStageAreNoOps() {
        var data = data();
        var inventory = new SimpleContainer(41);
        inventory.setItem(0, new ItemStack(Items.STONE, 7));
        data.insertStack(new ItemStack(Items.STONE, 64), true);
        long revision = data.getXianqiaoStorageRevision();
        assertTrue(XianqiaoPickItem.moveToHand(data, inventory, 0, new ItemStack(Items.STONE)));
        assertEquals(7, inventory.getItem(0).getCount());
        assertEquals(revision, data.getXianqiaoStorageRevision());
        data.setStage(5);
        assertFalse(XianqiaoPickItem.moveToHand(data, inventory, 0, new ItemStack(Items.STONE)));
    }
}
