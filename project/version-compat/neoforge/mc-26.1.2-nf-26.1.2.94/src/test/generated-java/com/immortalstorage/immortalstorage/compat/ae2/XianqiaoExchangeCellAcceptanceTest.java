package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.api.inventories.InternalInventory;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.XianqiaoExchangeCellItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for AE2 19.2.17's two real Drive acceptance gates.
 *
 * <p>The official Drive menu and its backing inventory both delegate to
 * {@link StorageCells#isCellHandled(ItemStack)}. Keeping the private backing
 * filter check here prevents a handler that works in isolation but is rejected
 * by the physical Drive.</p>
 */
final class XianqiaoExchangeCellAcceptanceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        Ae2Compat.initialize();
    }

    @Test
    void aBoundExchangeCellPassesTheRegistryAndPhysicalDriveFilter() throws Exception {
        ItemStack bound = new ItemStack(ModItems.XIANQIAO_EXCHANGE_CELL.get());
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(
                bound,
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                UUID.fromString("00000000-0000-0000-0000-000000000102")));

        assertTrue(StorageCells.isCellHandled(bound),
                "the same registry queried by RestrictedInputSlot must recognize the bound cell");
        assertSame(XianqiaoExchangeCellHandler.INSTANCE, StorageCells.getHandler(bound));
        StorageCell inventory = StorageCells.getCellInventory(bound, null);
        assertNotNull(inventory, "a Drive must be able to mount the accepted stack");
        assertTrue(invokePhysicalDriveFilter(bound),
                "DriveBlockEntity.CellValidInventoryFilter must accept the bound cell");

        ItemStack clientCopy = networkRoundTrip(bound);
        assertTrue(XianqiaoExchangeCellItem.isBoundToOwner(
                clientCopy, UUID.fromString("00000000-0000-0000-0000-000000000101")),
                "owner and disk components must reach the client-side Drive menu");
        assertTrue(StorageCells.isCellHandled(clientCopy));
        assertTrue(invokePhysicalDriveFilter(clientCopy));
    }

    @Test
    void blankAndOwnerOnlyOutputsAreRejectedUntilAnAtomicSameOwnerBindingCompletes() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000201");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000202");
        ItemStack blank = new ItemStack(ModItems.XIANQIAO_EXCHANGE_CELL.get());

        assertFalse(StorageCells.isCellHandled(blank));
        assertFalse(invokePhysicalDriveFilter(blank));
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(blank, owner, disk, "DisplayName"));
        assertTrue(StorageCells.isCellHandled(blank));
        assertTrue(invokePhysicalDriveFilter(blank));

        ItemStack ownerOnly = new ItemStack(ModItems.XIANQIAO_EXCHANGE_CELL.get());
        CompoundTag legacyIdentity = new CompoundTag();
        com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(legacyIdentity, "immortalstorageOwner", owner);
        ownerOnly.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyIdentity));
        assertFalse(StorageCells.isCellHandled(ownerOnly));
        assertFalse(invokePhysicalDriveFilter(ownerOnly));
        assertFalse(XianqiaoExchangeCellItem.bindUnbound(
                ownerOnly, UUID.fromString("00000000-0000-0000-0000-000000000299"), disk),
                "another player must not repair an owner-only legacy output");
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(ownerOnly, owner, disk, "DisplayName"));
        assertTrue(StorageCells.isCellHandled(ownerOnly));
        assertTrue(invokePhysicalDriveFilter(ownerOnly));
    }

    @Test
    void usernameMetadataDoesNotParticipateInAe2MountIdentity() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000301");
        UUID disk = UUID.fromString("00000000-0000-0000-0000-000000000302");
        ItemStack stack = new ItemStack(ModItems.XIANQIAO_EXCHANGE_CELL.get());
        assertTrue(XianqiaoExchangeCellItem.bindUnbound(stack, owner, disk, "OldName"));

        XianqiaoExchangeStorageCell before = (XianqiaoExchangeStorageCell)
                StorageCells.getCellInventory(stack, null);
        assertNotNull(before);
        assertEquals(owner, before.owner());
        assertEquals(disk, before.diskId());

        assertTrue(XianqiaoExchangeCellItem.refreshOwnerName(stack, owner, "NewName"));
        XianqiaoExchangeStorageCell after = (XianqiaoExchangeStorageCell)
                StorageCells.getCellInventory(stack, null);
        assertNotNull(after);
        assertEquals(owner, after.owner());
        assertEquals(disk, after.diskId());
        assertTrue(StorageCells.isCellHandled(stack));
    }

    private static ItemStack networkRoundTrip(ItemStack stack) {
        ByteBuf bytes = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, REGISTRIES);
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            return ItemStack.STREAM_CODEC.decode(buffer);
        } finally {
            bytes.release();
        }
    }

    private static boolean invokePhysicalDriveFilter(ItemStack stack) throws Exception {
        Class<?> type = Class.forName(
                "appeng.blockentity.storage.DriveBlockEntity$CellValidInventoryFilter");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object filter = constructor.newInstance();
        Method allowInsert = type.getDeclaredMethod(
                "allowInsert", InternalInventory.class, int.class, ItemStack.class);
        allowInsert.setAccessible(true);
        return (boolean) allowInsert.invoke(filter, null, 0, stack);
    }
}
