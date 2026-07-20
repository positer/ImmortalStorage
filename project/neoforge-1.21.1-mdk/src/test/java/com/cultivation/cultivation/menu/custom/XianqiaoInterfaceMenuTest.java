package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.api.storage.terminal.StorageItemSummary;
import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.block.entity.XianqiaoInterfaceInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceMenuTest {
    @Test
    void externalResourcesDefaultToOneThousandUnits() {
        assertEquals(1_000L, XianqiaoInterfaceMenu.DEFAULT_EXTERNAL_CACHE_AMOUNT);
    }
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void layoutHasNineGhostTargetsNineRealBuffersAndThirtySixPlayerSlots() {
        assertEquals(9, XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT);
        assertEquals(9, XianqiaoInterfaceMenu.BUFFER_SLOT_COUNT);
        assertEquals(9, XianqiaoInterfaceMenu.BUFFER_START);
        assertEquals(18, XianqiaoInterfaceMenu.PLAYER_START);
        assertEquals(54, XianqiaoInterfaceMenu.MENU_SLOT_COUNT);
    }

    @Test
    void carriedStackConfiguresCompleteIdentityAndCountWithoutBeingConsumed() {
        XianqiaoInterfaceInventory backend = new XianqiaoInterfaceInventory(new EmptyStorage(), () -> true);
        ItemStack carried = new ItemStack(Items.IRON_INGOT, 23);
        carried.set(DataComponents.CUSTOM_NAME, Component.literal("configured identity"));

        assertTrue(XianqiaoInterfaceMenu.configureTarget(backend, 4, carried));

        assertEquals(23, carried.getCount(), "ghost configuration must not consume the carried stack");
        ItemStack configured = backend.getTarget(4);
        assertEquals(23, configured.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(carried, configured));
        carried.shrink(5);
        assertEquals(23, backend.getTarget(4).getCount(), "the configured target must be a defensive copy");
    }

    @Test
    void emptyCarriedStackClearsTargetAndRetiredAccessFailsClosed() {
        boolean[] live = {true};
        XianqiaoInterfaceInventory backend = new XianqiaoInterfaceInventory(new EmptyStorage(), () -> live[0]);
        assertTrue(XianqiaoInterfaceMenu.configureTarget(
                backend, 0, new ItemStack(Items.DIAMOND, 7)));
        assertTrue(XianqiaoInterfaceMenu.configureTarget(backend, 0, ItemStack.EMPTY));
        assertTrue(backend.getTarget(0).isEmpty());

        live[0] = false;
        assertFalse(XianqiaoInterfaceMenu.configureTarget(
                backend, 0, new ItemStack(Items.EMERALD, 3)));
        assertTrue(backend.getTarget(0).isEmpty());
    }

    @Test
    void leftClickKeepsAFluidContainerAsAnExactItemWhileRightClickSelectsItsFluid() {
        XianqiaoInterfaceInventory backend = new XianqiaoInterfaceInventory(new EmptyStorage(), () -> true);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bucket.set(DataComponents.CUSTOM_NAME, Component.literal("exact container"));

        assertTrue(XianqiaoInterfaceMenu.configureTargetFromCarried(backend, 0, bucket, 0));
        assertTrue(ItemStack.isSameItemSameComponents(bucket, backend.getTarget(0)));
        assertTrue(backend.getFluidTarget(0).isEmpty());

        assertTrue(XianqiaoInterfaceMenu.configureTargetFromCarried(backend, 0, bucket, 1));
        assertTrue(backend.getTarget(0).isEmpty());
        assertTrue(backend.getFluidTarget(0).is(Fluids.WATER));
    }

    private static final class EmptyStorage implements TerminalItemStorage {
        @Override public long revision() { return 0L; }
        @Override public List<StorageItemSummary> snapshot() { return List.of(); }
        @Override public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) { return amount; }
        @Override public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
    }
}
