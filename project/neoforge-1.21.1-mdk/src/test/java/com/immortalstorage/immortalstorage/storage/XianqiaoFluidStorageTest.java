package com.immortalstorage.immortalstorage.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidCatalog;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageFluidHandler;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoFluidStorageTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void identicalFluidsAggregateWhileFullComponentsRemainDistinct() {
        ImmortalStoragePlayerData data = stageSevenData();
        FluidStack plainWater = new FluidStack(Fluids.WATER, 1);
        FluidStack namedWater = new FluidStack(Fluids.WATER, 1);
        namedWater.set(DataComponents.CUSTOM_NAME, Component.literal("灵泉"));

        assertEquals(1_250L, data.insertXianqiaoFluid(
                TerminalFluidKey.of(plainWater), 1_250L, TerminalStorageAction.EXECUTE));
        assertEquals(750L, data.insertXianqiaoFluid(
                TerminalFluidKey.of(plainWater), 750L, TerminalStorageAction.EXECUTE));
        assertEquals(400L, data.insertXianqiaoFluid(
                TerminalFluidKey.of(namedWater), 400L, TerminalStorageAction.EXECUTE));

        assertEquals(2, data.getXianqiaoFluidAmounts().size());
        assertEquals(2_000L, data.getXianqiaoFluidAmount(TerminalFluidKey.of(plainWater)));
        assertEquals(400L, data.getXianqiaoFluidAmount(TerminalFluidKey.of(namedWater)));
    }

    @Test
    void emptyFluidCatalogStillPublishesItsFirstRevisionBoundSnapshot() {
        TerminalFluidCatalog catalog = new TerminalFluidCatalog();
        assertTrue(catalog.rebuildIfStale(Map.of(), 0L));
        assertEquals(1L, catalog.revision());
        assertTrue(!catalog.rebuildIfStale(Map.of(), 0L));
        assertEquals(1L, catalog.fullScanCount());
    }

    @Test
    void stageSixRejectsNativeAndStandardFluidAccessAndStaleHandlerRechecksStage() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        PersonalStorageFluidHandler handler = new PersonalStorageFluidHandler(data, () -> {}, () -> true);

        assertEquals(0L, data.insertXianqiaoFluid(water, 1_000L, TerminalStorageAction.SIMULATE));
        assertEquals(0L, data.insertXianqiaoFluid(water, 1_000L, TerminalStorageAction.EXECUTE));
        assertEquals(0L, data.extractXianqiaoFluid(water, 1_000L, TerminalStorageAction.SIMULATE));
        assertEquals(0, handler.getTanks());
        assertTrue(handler.snapshot().isEmpty());
        assertEquals(0, handler.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));

        data.setStage(7);
        assertEquals(1_000L, data.insertXianqiaoFluid(water, 1_000L, TerminalStorageAction.EXECUTE));
        assertEquals(2, handler.getTanks(), "one stored identity plus one permanent import tank");

        data.setStage(6);
        assertEquals(0L, handler.revision());
        assertEquals(0, handler.getTanks());
        assertTrue(handler.snapshot().isEmpty());
        assertTrue(handler.drain(1_000, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        assertEquals(1_000L, data.getXianqiaoFluidAmount(water),
                "a debug downgrade hides fluid data without deleting it");
    }

    @Test
    void simulateNeverMutatesAmountsOrRevision() {
        ImmortalStoragePlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));

        assertEquals(5_000L, data.insertXianqiaoFluid(water, 5_000L, TerminalStorageAction.SIMULATE));
        assertEquals(0L, data.getXianqiaoFluidAmount(water));
        assertEquals(0L, data.getXianqiaoFluidStorageRevision());

        data.insertXianqiaoFluid(water, 2_000L, TerminalStorageAction.EXECUTE);
        long committedRevision = data.getXianqiaoFluidStorageRevision();
        assertEquals(1_500L, data.extractXianqiaoFluid(water, 1_500L, TerminalStorageAction.SIMULATE));
        assertEquals(2_000L, data.getXianqiaoFluidAmount(water));
        assertEquals(committedRevision, data.getXianqiaoFluidStorageRevision());
    }

    @Test
    void longAmountsSaturateWithoutWrappingNegative() {
        ImmortalStoragePlayerData data = stageSevenData();
        TerminalFluidKey lava = TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1));

        assertEquals(Long.MAX_VALUE - 7L, data.insertXianqiaoFluid(
                lava, Long.MAX_VALUE - 7L, TerminalStorageAction.EXECUTE));
        assertEquals(7L, data.insertXianqiaoFluid(lava, 100L, TerminalStorageAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, data.getXianqiaoFluidAmount(lava));
        assertEquals(0L, data.insertXianqiaoFluid(lava, 1L, TerminalStorageAction.SIMULATE));
        assertEquals(Long.MAX_VALUE, data.getXianqiaoFluidAmount(lava));
    }

    @Test
    void nestedFluidMutationsCommitOneMonotonicRevision() {
        ImmortalStoragePlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        TerminalFluidKey lava = TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1));

        data.batchXianqiaoFluidMutations(() -> {
            data.insertXianqiaoFluid(water, 1_000L, TerminalStorageAction.EXECUTE);
            data.insertXianqiaoFluid(water, 500L, TerminalStorageAction.EXECUTE);
            data.insertXianqiaoFluid(lava, 750L, TerminalStorageAction.EXECUTE);
        });
        assertEquals(1L, data.getXianqiaoFluidStorageRevision());

        data.extractXianqiaoFluid(water, 250L, TerminalStorageAction.EXECUTE);
        assertEquals(2L, data.getXianqiaoFluidStorageRevision());
    }

    @Test
    void saturatedFluidRevisionStillRefreshesExistingCompatibilityHandler() {
        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ImmortalStoragePlayerData seed = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        seed.insertXianqiaoFluid(water, 1_000L, TerminalStorageAction.EXECUTE);
        CompoundTag saved = seed.serializeNBT(registries);
        saved.putLong("xianqiaoFluidStorageRevision", Long.MAX_VALUE);

        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.deserializeNBT(registries, saved);
        PersonalStorageFluidHandler handler = new PersonalStorageFluidHandler(data, () -> {});
        assertEquals(Long.MAX_VALUE, data.getXianqiaoFluidStorageRevision());
        assertEquals(2, handler.getTanks());

        TerminalFluidKey lava = TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1));
        data.insertXianqiaoFluid(lava, 500L, TerminalStorageAction.EXECUTE);

        assertEquals(Long.MAX_VALUE, data.getXianqiaoFluidStorageRevision());
        assertEquals(3, handler.getTanks(),
                "the non-persistent generation must refresh fluid enumeration after revision saturation");
    }

    @Test
    void fluidPersistencePreservesComponentsLongAmountsAndLegacyEmptyDefault() {
        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ImmortalStoragePlayerData data = stageSevenData();
        FluidStack namedWater = new FluidStack(Fluids.WATER, 1);
        namedWater.set(DataComponents.CUSTOM_NAME, Component.literal("持久灵泉"));
        TerminalFluidKey key = TerminalFluidKey.of(namedWater);
        data.insertXianqiaoFluid(key, (long) Integer.MAX_VALUE + 9_999L, TerminalStorageAction.EXECUTE);

        CompoundTag saved = data.serializeNBT(registries);
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(registries, saved);
        assertEquals((long) Integer.MAX_VALUE + 9_999L, restored.getXianqiaoFluidAmount(key));
        assertEquals(data.getXianqiaoFluidStorageRevision(), restored.getXianqiaoFluidStorageRevision());

        CompoundTag legacyItemOnlySave = new CompoundTag();
        legacyItemOnlySave.putInt("stage", 6);
        ImmortalStoragePlayerData legacyRestored = new ImmortalStoragePlayerData();
        legacyRestored.deserializeNBT(registries, legacyItemOnlySave);
        assertTrue(legacyRestored.getXianqiaoFluidAmounts().isEmpty());
        assertEquals(0L, legacyRestored.getXianqiaoFluidStorageRevision());
    }

    @Test
    void fluidCatalogPagesUseStableIdsAndSkipIdleRescansAtTenThousandEntries() {
        assertTimeout(Duration.ofSeconds(20), () -> {
            ImmortalStoragePlayerData data = stageSevenData();
            data.batchXianqiaoFluidMutations(() -> {
                for (int index = 0; index < 10_000; index++) {
                    FluidStack variant = new FluidStack(Fluids.WATER, 1);
                    variant.set(DataComponents.CUSTOM_NAME, Component.literal("fluid-entry-" + index));
                    data.insertXianqiaoFluid(TerminalFluidKey.of(variant), index + 1L,
                            TerminalStorageAction.EXECUTE);
                }
            });

            TerminalFluidCatalog catalog = new TerminalFluidCatalog();
            assertTrue(catalog.rebuildIfStale(
                    data.getXianqiaoFluidAmounts(), data.getXianqiaoFluidStorageRevision()));
            assertEquals(1L, catalog.fullScanCount());
            TerminalFluidEntry first = catalog.page(0, 256).entries().getFirst();
            long stableId = first.entryId();
            assertEquals(10_000, catalog.page(9_984, 256).entries().size() + 9_984);
            assertEquals(1, catalog.entries(new TerminalQuery("fluid-entry-9999",
                    TerminalQuery.SortOrder.NAME, TerminalQuery.SortDirection.ASCENDING)).size());
            assertEquals(10_000, catalog.entries(new TerminalQuery("@minecraft",
                    TerminalQuery.SortOrder.MOD_ID, TerminalQuery.SortDirection.ASCENDING)).size());
            assertEquals(10_000L, catalog.entries(new TerminalQuery("",
                    TerminalQuery.SortOrder.AMOUNT, TerminalQuery.SortDirection.DESCENDING))
                    .getFirst().amountMb());

            for (int tick = 0; tick < 200; tick++) {
                assertTrue(!catalog.rebuildIfStale(
                        data.getXianqiaoFluidAmounts(), data.getXianqiaoFluidStorageRevision()));
            }
            assertEquals(1L, catalog.fullScanCount());

            data.insertXianqiaoFluid(TerminalFluidKey.of(first.displayStack()), 5L,
                    TerminalStorageAction.EXECUTE);
            assertTrue(catalog.rebuildIfStale(
                    data.getXianqiaoFluidAmounts(), data.getXianqiaoFluidStorageRevision()));
            assertEquals(stableId, catalog.page(0, 1).entries().getFirst().entryId());
            assertEquals(2L, catalog.fullScanCount());
        });
    }

    @Test
    void neoForgeFluidHandlerBridgesIntCallsWithoutChangingItemHandlerSemantics() {
        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ImmortalStoragePlayerData data = stageSevenData();
        PersonalStorageItemHandler items = new PersonalStorageItemHandler(data, registries, () -> {});
        int itemSlotsBefore = items.getSlots();
        long itemRevisionBefore = data.getXianqiaoStorageRevision();

        PersonalStorageFluidHandler fluids = new PersonalStorageFluidHandler(data, () -> {});
        assertEquals(1_000, fluids.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.SIMULATE));
        assertEquals(1, fluids.getTanks(), "an empty accepting handler keeps one writable virtual tank");
        assertEquals(1_000, fluids.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(2, fluids.getTanks());
        assertEquals(250, fluids.drain(new FluidStack(Fluids.WATER, 250),
                IFluidHandler.FluidAction.SIMULATE).getAmount());
        assertEquals(1_000, fluids.getFluidInTank(0).getAmount());
        assertEquals(250, fluids.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount());

        assertEquals(itemSlotsBefore, items.getSlots());
        assertEquals(itemRevisionBefore, data.getXianqiaoStorageRevision());
        assertTrue(items.getStackInSlot(0).isEmpty());

        ItemStack leftover = items.insertItem(0, new ItemStack(Items.DIAMOND, 3), false);
        assertTrue(leftover.isEmpty());
        assertNotEquals(itemRevisionBefore, data.getXianqiaoStorageRevision());
        assertEquals(750L, data.getXianqiaoFluidAmount(
                TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1))));
    }

    @Test
    void fluidCapabilityGuardIsCheckedPerCallAndLegacyEndpointStaysItemOnly() {
        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ImmortalStoragePlayerData data = stageSevenData();
        AtomicInteger changes = new AtomicInteger();
        ImmortalStoragePlayerData[] currentAttachment = {null};
        PersonalStorageFluidHandler guarded = new PersonalStorageFluidHandler(
                data, changes::incrementAndGet, () -> currentAttachment[0] == data);

        assertEquals(0, guarded.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(0, guarded.getTanks());
        assertEquals(0, changes.get());

        currentAttachment[0] = data;
        assertEquals(1_000, guarded.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.SIMULATE));
        assertEquals(0, changes.get());
        assertEquals(1_000, guarded.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(1, changes.get());

        currentAttachment[0] = stageSevenData();
        assertEquals(0, guarded.getTanks());
        assertTrue(guarded.drain(1_000, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        assertEquals(1_000L, data.getXianqiaoFluidAmount(
                TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1))));

        PersonalStorageNetwork.Endpoint legacyItemEndpoint = new PersonalStorageNetwork.Endpoint(
                UUID.randomUUID(), data, registries, () -> {});
        assertNotNull(legacyItemEndpoint.itemHandler());
        assertTrue(legacyItemEndpoint.insert(new ItemStack(Items.EMERALD, 1), false).isEmpty());
        assertNull(legacyItemEndpoint.fluidStorage());
        assertNull(legacyItemEndpoint.fluidHandler());
        assertThrows(UnsupportedOperationException.class,
                () -> data.getXianqiaoFluidAmounts().put(
                        TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1)), 1L));
    }

    @Test
    void intFluidCapabilityKeepsWorkingAboveIntegerMaxWithoutOverflow() {
        ImmortalStoragePlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        long initial = (long) Integer.MAX_VALUE + 12_345L;
        assertEquals(initial, data.insertXianqiaoFluid(
                water, initial, TerminalStorageAction.EXECUTE));
        PersonalStorageFluidHandler handler = new PersonalStorageFluidHandler(data, () -> {});

        assertEquals(Integer.MAX_VALUE, handler.getFluidInTank(0).getAmount(),
                "IFluidHandler's int view saturates but never wraps negative");
        assertEquals(2_000, handler.fill(new FluidStack(Fluids.WATER, 2_000),
                IFluidHandler.FluidAction.SIMULATE));
        assertEquals(initial, data.getXianqiaoFluidAmount(water));
        assertEquals(2_000, handler.fill(new FluidStack(Fluids.WATER, 2_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(initial + 2_000L, data.getXianqiaoFluidAmount(water));

        FluidStack templateDrain = handler.drain(new FluidStack(Fluids.WATER, 1_500),
                IFluidHandler.FluidAction.EXECUTE);
        assertEquals(1_500, templateDrain.getAmount());
        FluidStack genericDrain = handler.drain(500, IFluidHandler.FluidAction.EXECUTE);
        assertEquals(500, genericDrain.getAmount());
        assertEquals(initial, data.getXianqiaoFluidAmount(water));
        assertTrue(handler.getFluidInTank(0).getAmount() > 0);
    }

    @Test
    void cappedVisibleFluidStillLeavesAnEmptyImportTankForNewIdentities() {
        ImmortalStoragePlayerData data = stageSevenData();
        TerminalFluidKey water = TerminalFluidKey.of(new FluidStack(Fluids.WATER, 1));
        long initial = (long) Integer.MAX_VALUE + 1L;
        data.insertXianqiaoFluid(water, initial, TerminalStorageAction.EXECUTE);
        PersonalStorageFluidHandler handler = new PersonalStorageFluidHandler(data, () -> {});

        assertEquals(Integer.MAX_VALUE, handler.getFluidInTank(0).getAmount());
        int importTank = handler.getTanks() - 1;
        assertTrue(handler.getFluidInTank(importTank).isEmpty());
        assertEquals(Integer.MAX_VALUE, handler.getTankCapacity(importTank));
        assertEquals(1_000, handler.fill(new FluidStack(Fluids.LAVA, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(2_000, handler.fill(new FluidStack(Fluids.WATER, 2_000),
                IFluidHandler.FluidAction.EXECUTE));

        assertEquals(1_000L, data.getXianqiaoFluidAmount(
                TerminalFluidKey.of(new FluidStack(Fluids.LAVA, 1))));
        assertEquals(initial + 2_000L, data.getXianqiaoFluidAmount(water));
        assertTrue(handler.getFluidInTank(handler.getTanks() - 1).isEmpty());
    }

    private static ImmortalStoragePlayerData stageSevenData() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(7);
        return data;
    }
}
