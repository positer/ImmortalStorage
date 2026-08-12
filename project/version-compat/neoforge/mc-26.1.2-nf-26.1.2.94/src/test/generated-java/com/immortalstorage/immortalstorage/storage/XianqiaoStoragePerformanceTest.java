package com.immortalstorage.immortalstorage.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoStoragePerformanceTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @AfterEach
    void restoreStageTenDefault() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(false);
    }

    @Test
    void repeatedInsertsUseOneOverstackedPhysicalEntryInsteadOfThousandsOfVanillaStacks() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);

        data.batchXianqiaoMutations(() -> {
            for (int index = 0; index < 10_000; index++) {
                assertTrue(data.insertStack(new ItemStack(Items.IRON_INGOT, 64), true).isEmpty());
            }
        });

        assertEquals(640_000L, data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT))
                .mapToLong(ItemStack::getCount)
                .sum());
        assertEquals(1L, data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT))
                .count(), "one component identity should occupy one physical entry until int saturation");
        assertEquals(1L, data.getXianqiaoStorageRevision(),
                "the whole bulk mutation should publish one storage revision");
    }

    @Test
    void genericItemHandlerExposesAggregatedIdentitiesRatherThanFragmentedPhysicalSlots() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        for (int slot = 0; slot < 4_096; slot++) {
            data.setXianqiaoSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);

        assertEquals(2, handler.getSlots(), "one permanent import slot plus one aggregate identity");
        assertTrue(handler.getStackInSlot(0).isEmpty());
        assertTrue(handler.getStackInSlot(1).is(Items.COBBLESTONE));
        assertEquals(4_096 * 64, handler.getStackInSlot(1).getCount());
        assertEquals(64, handler.extractItem(1, Integer.MAX_VALUE, true).getCount(),
                "IItemHandler extraction must still return a legal max-stack chunk");
    }

    @Test
    void loadingLegacyFragmentsCompactsThemWithoutMergingDifferentDataComponents() {
        ImmortalStoragePlayerData legacy = new ImmortalStoragePlayerData();
        legacy.setStage(6);
        for (int slot = 0; slot < 1_000; slot++) {
            legacy.setXianqiaoSlot(slot, new ItemStack(Items.IRON_INGOT, 64));
        }
        ItemStack named = new ItemStack(Items.IRON_INGOT, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("separate identity"));
        legacy.setXianqiaoSlot(1_000, named);

        CompoundTag saved = legacy.serializeNBT(REGISTRIES);
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);

        assertEquals(2L, restored.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT))
                .count());
        assertEquals(64_000L, restored.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT)
                        && !stack.has(DataComponents.CUSTOM_NAME))
                .mapToLong(ItemStack::getCount)
                .sum());
        assertEquals(7L, restored.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT)
                        && stack.has(DataComponents.CUSTOM_NAME))
                .mapToLong(ItemStack::getCount)
                .sum());
        assertEquals(ImmortalStoragePlayerData.XIANQIAO_INITIAL_SLOTS,
                restored.getXianqiaoStorageItems().size(),
                "legacy tail capacity should be released after logical compaction");

        ImmortalStoragePlayerData secondLoad = new ImmortalStoragePlayerData();
        secondLoad.deserializeNBT(REGISTRIES, restored.serializeNBT(REGISTRIES));
        assertEquals(64_007L, secondLoad.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.IRON_INGOT))
                .mapToLong(ItemStack::getCount)
                .sum(), "extended-count compact stacks must survive another save cycle");
    }

    @Test
    void logicalDirectoryIsSharedUntilTheAuthoritativeRevisionChanges() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.insertStack(new ItemStack(Items.DIAMOND, 3), true);

        List<StorageItemSummary> first = data.getXianqiaoItemSummary();
        assertSame(first, data.getXianqiaoItemSummary());

        data.insertStack(new ItemStack(Items.DIAMOND, 2), true);
        List<StorageItemSummary> changed = data.getXianqiaoItemSummary();
        assertNotSame(first, changed);
        assertEquals(5L, changed.getFirst().amount());
        assertSame(changed, data.getXianqiaoItemSummary());
    }

    @Test
    void legacySparseSlotIdsCannotAmplifyMemoryDuringLoad() {
        CompoundTag saved = new CompoundTag();
        saved.putInt("stage", 6);
        ListTag entries = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putInt("slot", Integer.MAX_VALUE);
        entry.put("item", ImmortalStoragePlayerData.saveStack(REGISTRIES,
                new ItemStack(Items.EMERALD, 3)));
        entries.add(entry);
        saved.put("xianqiaoStorage", entries);

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);

        assertEquals(ImmortalStoragePlayerData.XIANQIAO_INITIAL_SLOTS,
                restored.getXianqiaoStorageItems().size());
        assertEquals(3, restored.getXianqiaoStorageItems().getFirst().getCount());
    }

    @Test
    void logicalSummaryKeepsLongTotalsWhileIntCapabilitiesSaturate() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        ItemStack first = new ItemStack(Items.REDSTONE);
        first.setCount(Integer.MAX_VALUE);
        ItemStack second = new ItemStack(Items.REDSTONE);
        second.setCount(Integer.MAX_VALUE);
        data.setXianqiaoSlot(0, first);
        data.setXianqiaoSlot(1, second);

        assertEquals(2L * Integer.MAX_VALUE, data.getXianqiaoItemSummary().getFirst().amount());
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        assertEquals(2, handler.getSlots());
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(1).getCount());
        assertEquals(64, handler.extractItem(1, Integer.MAX_VALUE, true).getCount());
    }

    @Test
    void existingHandlerKeepsUnrelatedLogicalSlotsStableAcrossDirectoryChanges() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.insertStack(new ItemStack(Items.DIAMOND, 3), true);
        data.insertStack(new ItemStack(Items.GOLD_INGOT, 5), true);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        int goldSlot = IntStream.range(0, handler.getSlots())
                .filter(slot -> handler.getStackInSlot(slot).is(Items.GOLD_INGOT))
                .findFirst().orElseThrow();

        data.extractStack(new ItemStack(Items.DIAMOND), 3);
        data.insertStack(new ItemStack(Items.EMERALD, 4), true);

        assertTrue(handler.getStackInSlot(goldSlot).is(Items.GOLD_INGOT));
        assertEquals(5, handler.getStackInSlot(goldSlot).getCount());
        assertEquals(2, handler.extractItem(goldSlot, 2, false).getCount());
        assertEquals(3L, data.getXianqiaoItemSummary().stream()
                .filter(summary -> summary.prototype().is(Items.GOLD_INGOT))
                .findFirst().orElseThrow().amount());
    }

    @Test
    void samePersistedRevisionReloadInvalidatesAnExistingHandlerGeneration() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.insertStack(new ItemStack(Items.DIAMOND, 3), true);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        assertTrue(IntStream.range(0, handler.getSlots())
                .anyMatch(slot -> handler.getStackInSlot(slot).is(Items.DIAMOND)));

        ImmortalStoragePlayerData replacement = new ImmortalStoragePlayerData();
        replacement.setStage(6);
        replacement.insertStack(new ItemStack(Items.GOLD_INGOT, 7), true);
        assertEquals(data.getXianqiaoStorageRevision(), replacement.getXianqiaoStorageRevision());

        data.deserializeNBT(REGISTRIES, replacement.serializeNBT(REGISTRIES));

        assertTrue(IntStream.range(0, handler.getSlots())
                .anyMatch(slot -> handler.getStackInSlot(slot).is(Items.GOLD_INGOT)));
        assertTrue(IntStream.range(0, handler.getSlots())
                .noneMatch(slot -> handler.getStackInSlot(slot).is(Items.DIAMOND)));
    }

    @Test
    void saturatedPersistedRevisionStillInvalidatesLogicalCaches() {
        ImmortalStoragePlayerData seed = new ImmortalStoragePlayerData();
        seed.setStage(6);
        seed.insertStack(new ItemStack(Items.DIAMOND, 3), true);
        CompoundTag saved = seed.serializeNBT(REGISTRIES);
        saved.putLong("xianqiaoStorageRevision", Long.MAX_VALUE);

        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.deserializeNBT(REGISTRIES, saved);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        assertEquals(Long.MAX_VALUE, data.getXianqiaoStorageRevision());
        assertTrue(IntStream.range(0, handler.getSlots())
                .anyMatch(slot -> handler.getStackInSlot(slot).is(Items.DIAMOND)));

        data.insertStack(new ItemStack(Items.GOLD_INGOT, 7), true);

        assertEquals(Long.MAX_VALUE, data.getXianqiaoStorageRevision(),
                "the persisted protocol revision saturates instead of wrapping");
        assertTrue(IntStream.range(0, handler.getSlots())
                .anyMatch(slot -> handler.getStackInSlot(slot).is(Items.GOLD_INGOT)),
                "the non-persistent generation must still invalidate cached enumeration");
    }

    @Test
    void handlerLayoutInvalidatesAcrossFiniteToVirtualStageBoundary() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(true);
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(9);
        PersonalStorageItemHandler finite = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        assertTrue(finite.getSlots() > 0);

        data.setStage(10);

        assertEquals(0, finite.getSlots(), "cached finite layout must be retired at stage ten");
        PersonalStorageItemHandler virtual = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);
        assertEquals(Integer.MAX_VALUE, virtual.getStackInSlot(0).getCount());
    }
}
