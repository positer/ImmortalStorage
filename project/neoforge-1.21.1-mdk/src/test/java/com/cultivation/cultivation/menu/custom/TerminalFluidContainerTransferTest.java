package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.api.storage.terminal.TerminalEntry;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.network.storage.PersonalStorageFluidHandler;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

final class TerminalFluidContainerTransferTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void waterBucketDepositsThroughTheOfficialItemFluidCapability() {
        FluidTank destination = new FluidTank(FluidType.BUCKET_VOLUME * 4);
        ItemStackHandler returns = new ItemStackHandler(9);

        var result = TerminalFluidContainerTransfer.deposit(
                new ItemStack(Items.WATER_BUCKET), destination, returns);

        assertTrue(result.success());
        assertTrue(result.carried().is(Items.BUCKET));
        assertEquals(FluidType.BUCKET_VOLUME, destination.getFluidAmount());
        assertTrue(destination.getFluid().is(Fluids.WATER));
    }

    @Test
    void partialSourceCannotFillABucketAndDoesNotLoseFluid() {
        CultivationPlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        data.insertXianqiaoFluid(water, FluidType.BUCKET_VOLUME / 4L, TerminalStorageAction.EXECUTE);
        PersonalStorageFluidHandler storage = new PersonalStorageFluidHandler(data, () -> {});

        var result = TerminalFluidContainerTransfer.withdraw(new ItemStack(Items.BUCKET),
                TerminalFluidContainerTransfer.exactSource(storage, water), new ItemStackHandler(9));

        assertFalse(result.success());
        assertEquals(FluidType.BUCKET_VOLUME / 4L, data.getXianqiaoFluidAmount(water));
    }

    @Test
    void stackedContainerWithFullReturnInventoryRejectsBeforeExecute() {
        FluidTank destination = new FluidTank(FluidType.BUCKET_VOLUME * 4);
        ItemStackHandler fullInventory = new ItemStackHandler(9);
        for (int slot = 0; slot < fullInventory.getSlots(); slot++) {
            fullInventory.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        ItemStack stackedBuckets = new ItemStack(Items.WATER_BUCKET, 2);

        var result = TerminalFluidContainerTransfer.deposit(stackedBuckets, destination, fullInventory);

        assertFalse(result.success());
        assertEquals(0, destination.getFluidAmount());
        assertEquals(2, stackedBuckets.getCount());
        for (int slot = 0; slot < fullInventory.getSlots(); slot++) {
            assertEquals(64, fullInventory.getStackInSlot(slot).getCount());
        }
    }

    @Test
    void exactSourceNeverDrainsSameFluidWithDifferentComponents() {
        CultivationPlayerData data = stageSevenData();
        FluidStack plain = new FluidStack(Fluids.WATER, 1);
        FluidStack named = new FluidStack(Fluids.WATER, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("灵泉"));
        TerminalFluidKey plainKey = TerminalFluidKey.of(plain);
        TerminalFluidKey namedKey = TerminalFluidKey.of(named);
        data.insertXianqiaoFluid(plainKey, 1_000L, TerminalStorageAction.EXECUTE);
        data.insertXianqiaoFluid(namedKey, 750L, TerminalStorageAction.EXECUTE);
        PersonalStorageFluidHandler storage = new PersonalStorageFluidHandler(data, () -> {});
        IFluidHandler exactNamed = TerminalFluidContainerTransfer.exactSource(storage, namedKey);

        assertTrue(exactNamed.drain(new FluidStack(Fluids.WATER, 250),
                IFluidHandler.FluidAction.SIMULATE).isEmpty());
        FluidStack namedRequest = named.copyWithAmount(250);
        assertEquals(250, exactNamed.drain(namedRequest, IFluidHandler.FluidAction.EXECUTE).getAmount());
        assertEquals(1_000L, data.getXianqiaoFluidAmount(plainKey));
        assertEquals(500L, data.getXianqiaoFluidAmount(namedKey));
    }

    @Test
    void depositIgnoresTheHoveredEntryAndStoresTheContainersOwnFluid() {
        CultivationPlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        TerminalFluidKey lava = TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1));
        data.insertXianqiaoFluid(water, 2_000L, TerminalStorageAction.EXECUTE);
        PersonalStorageFluidHandler storage = new PersonalStorageFluidHandler(data, () -> {});

        var result = TerminalFluidContainerTransfer.deposit(
                new ItemStack(Items.LAVA_BUCKET), storage, new ItemStackHandler(9));

        assertTrue(result.success(), "right-click deposit is container-driven, not hovered-entry-driven");
        assertEquals(2_000L, data.getXianqiaoFluidAmount(water));
        assertEquals(FluidType.BUCKET_VOLUME, data.getXianqiaoFluidAmount(lava));
    }

    @Test
    void nativeLongStorageRollsBackAPartialDepositCommit() {
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        PartialCommitStorage storage = new PartialCommitStorage(water, 0L, true, false);

        var result = TerminalFluidContainerTransfer.depositToStorage(
                new ItemStack(Items.WATER_BUCKET), storage, new ItemStackHandler(9));

        assertFalse(result.success());
        assertEquals(0L, storage.amount,
                "a partial int-facing execute must be compensated before the transaction fails");
    }

    @Test
    void nativeLongStorageRollsBackAPartialWithdrawalCommit() {
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        PartialCommitStorage storage = new PartialCommitStorage(
                water, FluidType.BUCKET_VOLUME, false, true);

        var result = TerminalFluidContainerTransfer.withdrawFromStorage(
                new ItemStack(Items.BUCKET), storage, water, new ItemStackHandler(9));

        assertFalse(result.success());
        assertEquals(FluidType.BUCKET_VOLUME, storage.amount);
    }

    @Test
    void returnInventoryExecutionRefusalRollsBackNativeStorage() {
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        PartialCommitStorage storage = new PartialCommitStorage(water, 0L, false, false);
        ItemStack twoBuckets = new ItemStack(Items.WATER_BUCKET, 2);

        var result = TerminalFluidContainerTransfer.depositToStorage(
                twoBuckets, storage, new SimulateAcceptExecuteRejectInventory());

        assertFalse(result.success());
        assertEquals(0L, storage.amount);
        assertEquals(2, twoBuckets.getCount());
    }

    @Test
    void itemProxySlotsAreInactiveOnlyWhileTheFluidChannelIsSelected() {
        assertTrue(XianqiaoStorageMenu.isStorageProxyActive(false, 0, 10));
        assertTrue(XianqiaoStorageMenu.isStorageProxyActive(false, 89, 10));
        assertFalse(XianqiaoStorageMenu.isStorageProxyActive(false, 90, 10));
        assertTrue(XianqiaoStorageMenu.isStorageProxyActive(true, 0, 10),
                "the v2 channel flag is ignored by the shared item/fluid page");
        assertTrue(XianqiaoStorageMenu.isStorageProxyActive(true, 89, 10));
    }

    @Test
    void activeProxySlotsAreBoundedToTheViewportPlusOneClippedRow() {
        int active = 0;
        for (int viewIndex = 0; viewIndex < 10 * 9; viewIndex++) {
            if (XianqiaoStorageMenu.isStorageProxyViewportActive(
                    viewIndex, 40, 10, 43, 5)) {
                active++;
            }
        }

        assertEquals(9 * 6, active);
        assertFalse(XianqiaoStorageMenu.isStorageProxyViewportActive(26, 40, 10, 43, 5));
        assertTrue(XianqiaoStorageMenu.isStorageProxyViewportActive(27, 40, 10, 43, 5));
        assertTrue(XianqiaoStorageMenu.isStorageProxyViewportActive(80, 40, 10, 43, 5));
        assertFalse(XianqiaoStorageMenu.isStorageProxyViewportActive(81, 40, 10, 43, 5));
    }

    @Test
    void serverProxySlotsStayEmptyWhileClientProxiesUseTheCustomSnapshot() {
        TerminalEntry entry = new TerminalEntry(7L, new ItemStack(Items.DIAMOND), 128L);

        assertTrue(XianqiaoStorageMenu.storageProxyDisplayStack(false, entry).isEmpty(),
                "vanilla server slot synchronization must not duplicate terminal snapshots");
        ItemStack clientView = XianqiaoStorageMenu.storageProxyDisplayStack(true, entry);
        assertTrue(clientView.is(Items.DIAMOND));
        assertEquals(1, clientView.getCount());
    }

    @Test
    void combinedDirectoryMapsItemsThenFluidsInsideOneRowWindow() {
        assertEquals(0, XianqiaoStorageMenu.combinedDirectoryRows(0, 0));
        assertEquals(1, XianqiaoStorageMenu.combinedDirectoryRows(8, 1));
        assertEquals(2, XianqiaoStorageMenu.combinedDirectoryRows(8, 2));

        assertEquals(7, XianqiaoStorageMenu.combinedItemIndex(0, 7, 8));
        assertEquals(-1, XianqiaoStorageMenu.combinedItemIndex(0, 8, 8));
        assertEquals(0, XianqiaoStorageMenu.combinedFluidIndex(0, 8, 8, 4));
        assertEquals(1, XianqiaoStorageMenu.combinedFluidIndex(1, 0, 8, 4));
        assertEquals(-1, XianqiaoStorageMenu.combinedFluidIndex(1, 4, 8, 4));
    }

    private static CultivationPlayerData stageSevenData() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(7);
        return data;
    }

    private static final class PartialCommitStorage implements TerminalFluidStorage {
        private final TerminalFluidKey key;
        private final boolean partialInsert;
        private final boolean partialExtract;
        private long amount;
        private long revision;

        private PartialCommitStorage(TerminalFluidKey key, long amount,
                                     boolean partialInsert, boolean partialExtract) {
            this.key = key;
            this.amount = amount;
            this.partialInsert = partialInsert;
            this.partialExtract = partialExtract;
        }

        @Override public long revision() { return revision; }
        @Override public Map<TerminalFluidKey, Long> snapshot() {
            return amount <= 0L ? Map.of() : Map.of(key, amount);
        }
        @Override public long insert(TerminalFluidKey requestedKey, long requested, TerminalStorageAction action) {
            if (!key.equals(requestedKey) || requested <= 0L) return 0L;
            long accepted = action == TerminalStorageAction.EXECUTE && partialInsert
                    ? requested / 2L : requested;
            if (action == TerminalStorageAction.EXECUTE && accepted > 0L) {
                amount += accepted;
                revision++;
            }
            return accepted;
        }
        @Override public long extract(TerminalFluidKey requestedKey, long requested, TerminalStorageAction action) {
            if (!key.equals(requestedKey) || requested <= 0L) return 0L;
            long extracted = Math.min(requested, amount);
            if (action == TerminalStorageAction.EXECUTE && partialExtract) extracted /= 2L;
            if (action == TerminalStorageAction.EXECUTE && extracted > 0L) {
                amount -= extracted;
                revision++;
            }
            return extracted;
        }
    }

    private static final class SimulateAcceptExecuteRejectInventory implements IItemHandler {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return simulate ? ItemStack.EMPTY : stack.copy();
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
    }
}
