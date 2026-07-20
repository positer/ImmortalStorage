package com.immortalstorage.immortalstorage.player.yuan;

import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class YuanStorageBoundaryTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static Item trueYuanItem;
    private static Item immortalYuanItem;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        trueYuanItem = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("cultivation_test", "true_yuan"),
                new TrueYuanItem(new Item.Properties().stacksTo(64)));
        immortalYuanItem = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("cultivation_test", "immortal_yuan"),
                new ImmortalYuanItem(new Item.Properties().stacksTo(64)));
        BuiltInRegistries.ITEM.freeze();
    }

    @AfterEach
    void restoreStageTenDefault() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(false);
    }

    @Test
    void stageOneTrueYuanUsesPhysicalStacksAndTheSixtyFourItemTotalCap() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(1);
        ItemStack offered = trueYuan(70);

        assertEquals(6, data.simulateInsertStack(offered.copy()).getCount());
        assertEquals(0L, activeCount(data, YuanKind.TRUE), "simulation must not create items");

        assertEquals(6, data.insertStack(offered.copy(), true).getCount());
        assertEquals(64L, activeCount(data, YuanKind.TRUE));
        assertEquals(64L, data.getTrueYuan());

        assertEquals(10, data.simulateExtractStack(trueYuan(1), 10).getCount());
        assertEquals(64L, activeCount(data, YuanKind.TRUE), "extract simulation must not mutate storage");
        assertEquals(10, data.extractStack(trueYuan(1), 10).getCount());
        assertEquals(54L, activeCount(data, YuanKind.TRUE));
    }

    @Test
    void stageSixImmortalYuanAggregatesAndUsesTheSixtyFourItemTotalCap() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);

        assertTrue(data.insertStack(immortalYuan(32), true).isEmpty());
        assertTrue(data.insertStack(immortalYuan(32), true).isEmpty());
        assertEquals(64L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(1L, activeStacks(data, YuanKind.IMMORTAL),
                "same-component immortal yuan must merge into one physical stack");
        assertEquals(1, data.simulateInsertStack(immortalYuan(1)).getCount());

        assertEquals(20, data.simulateExtractStack(immortalYuan(1), 20).getCount());
        assertEquals(64L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(20, data.extractStack(immortalYuan(1), 20).getCount());
        assertEquals(44L, activeCount(data, YuanKind.IMMORTAL));
    }

    @Test
    void publicItemHandlerReadsSimulatesAndExecutesAtBothStorageStages() {
        assertHandlerRoundTrip(1, trueYuanItem, YuanKind.TRUE);
        assertHandlerRoundTrip(6, trueYuanItem, YuanKind.TRUE);
        assertHandlerRoundTrip(6, immortalYuanItem, YuanKind.IMMORTAL);
    }

    @Test
    void intHandlerExtractsOverstackedKongqiaoSlotsInLegalItemStackChunks() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        ItemStack thousand = trueYuan(1);
        thousand.setCount(1_000);
        assertTrue(data.insertStack(thousand, true).isEmpty());
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, null, () -> true);
        int slot = findKindSlot(handler, YuanKind.TRUE);

        assertEquals(trueYuan(1).getMaxStackSize(),
                handler.extractItem(slot, Integer.MAX_VALUE, true).getCount());
        assertEquals(1_000L, activeCount(data, YuanKind.TRUE));
        assertEquals(trueYuan(1).getMaxStackSize(),
                handler.extractItem(slot, Integer.MAX_VALUE, false).getCount());
        assertEquals(1_000L - trueYuan(1).getMaxStackSize(), activeCount(data, YuanKind.TRUE));
    }

    @Test
    void fiveToSixConversionHappensOnceAndDiscardsTheIncompleteGroup() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        assertTrue(data.insertStack(trueYuan(31), true).isEmpty());

        data.setStage(6);

        assertEquals(0L, allStorageCount(data, YuanKind.TRUE));
        assertEquals(1L, activeCount(data, YuanKind.IMMORTAL));

        CompoundTag saved = data.serializeNBT(REGISTRIES);
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        assertEquals(1L, activeCount(restored, YuanKind.IMMORTAL));

        assertEquals(1L, restored.depositTrueYuan(1L),
                "post-ascension true yuan must remain an ordinary physical item");
        assertEquals(1L, activeCount(restored, YuanKind.TRUE));
        assertEquals(1L, activeCount(restored, YuanKind.IMMORTAL),
                "later true yuan must never complete the discarded ascension remainder");

        ImmortalStoragePlayerData secondReload = new ImmortalStoragePlayerData();
        secondReload.deserializeNBT(REGISTRIES, restored.serializeNBT(REGISTRIES));
        assertEquals(1L, activeCount(secondReload, YuanKind.TRUE),
                "loading an ascended save must not convert newly stored true yuan");
        assertEquals(1L, activeCount(secondReload, YuanKind.IMMORTAL));
    }

    @Test
    void postAscensionTrueYuanIsUnboundedAndNeverConverts() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);

        assertEquals(2_048L, data.depositTrueYuan(2_048L));
        assertEquals(2_048L, activeCount(data, YuanKind.TRUE));
        assertEquals(0L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(1L, data.depositTrueYuan(1L));
        assertEquals(2_049L, activeCount(data, YuanKind.TRUE),
                "stage-six true yuan has no cap but remains a counted physical total");

        assertEquals(64L, data.depositImmortalYuan(64L));
        assertEquals(64L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(2_049L, activeCount(data, YuanKind.TRUE),
                "true-yuan and immortal-yuan caps are independent physical totals");
    }

    @Test
    void oneLongDepositCallHasABoundedPhysicalMaterializationBudget() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
            data.setStage(6);

            long accepted = data.depositTrueYuan(Long.MAX_VALUE);

            assertTrue(accepted > 100_000L);
            assertTrue(accepted < Long.MAX_VALUE);
            assertEquals(accepted, activeCount(data, YuanKind.TRUE));
        });
    }

    @Test
    void stageTenImmortalYuanIsVirtualLongMaxAcrossPersistenceAndIntHandlers() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(true);
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(10);

        assertTrue(data.isInfiniteImmortalYuan());
        assertEquals(Long.MAX_VALUE, data.getImmortalYuan());
        assertEquals(Long.MAX_VALUE, data.getImmortalYuanCapLong());
        assertEquals(123L, data.depositImmortalYuan(123L));
        assertTrue(data.consumeImmortalYuan(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, data.getImmortalYuan());
        assertEquals(0L, activeCount(data, YuanKind.IMMORTAL),
                "the infinite balance must not materialize physical stacks");

        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, null, () -> true);
        int virtualSlot = findKindSlot(handler, YuanKind.IMMORTAL);
        assertEquals(0, virtualSlot, "the compatibility slot must stay stable while storage grows");
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(virtualSlot).getCount());
        assertEquals(Integer.MAX_VALUE, handler.getSlotLimit(virtualSlot));
        int legalStack = handler.getStackInSlot(virtualSlot).getMaxStackSize();
        assertEquals(legalStack,
                handler.extractItem(virtualSlot, Integer.MAX_VALUE, true).getCount());
        assertEquals(legalStack,
                handler.extractItem(virtualSlot, Integer.MAX_VALUE, false).getCount());
        assertEquals(legalStack,
                handler.extractItem(virtualSlot, Integer.MAX_VALUE, false).getCount(),
                "repeated legal chunks must not consume the creative balance");
        assertEquals(Long.MAX_VALUE, data.getImmortalYuan());
        assertTrue(handler.insertItem(virtualSlot,
                handler.getStackInSlot(virtualSlot).copyWithCount(64), false).isEmpty());
        assertEquals(Long.MAX_VALUE, data.getImmortalYuan());
        assertEquals(0L, activeCount(data, YuanKind.IMMORTAL));

        CompoundTag saved = data.serializeNBT(REGISTRIES);
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        assertTrue(restored.isInfiniteImmortalYuan());
        assertEquals(Long.MAX_VALUE, restored.getImmortalYuan());
        assertEquals(0L, activeCount(restored, YuanKind.IMMORTAL));
    }

    @Test
    void stageTenIntHandlerReplacesRatherThanAddsPreexistingPhysicalImmortalYuan() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(true);
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(10);
        ItemStack canonical = data.getInfiniteImmortalYuanPrototype();
        data.setStage(9);
        assertTrue(data.insertStack(canonical.copyWithCount(3), true).isEmpty());
        data.setStage(10);

        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, null, () -> true);
        long visibleImmortalSlots = IntStream.range(0, handler.getSlots())
                .mapToObj(handler::getStackInSlot)
                .filter(stack -> YuanItemPolicy.kindOf(stack) == YuanKind.IMMORTAL)
                .count();
        long visibleIntTotal = IntStream.range(0, handler.getSlots())
                .mapToObj(handler::getStackInSlot)
                .filter(stack -> YuanItemPolicy.kindOf(stack) == YuanKind.IMMORTAL)
                .mapToLong(ItemStack::getCount)
                .sum();

        assertEquals(1L, visibleImmortalSlots);
        assertEquals(Integer.MAX_VALUE, visibleIntTotal,
                "int capabilities must see one saturating replacement, never MAX plus physical stacks");
        assertEquals(3L, activeCount(data, YuanKind.IMMORTAL),
                "entering stage ten must not destroy persisted physical items");
    }

    @Test
    void stageTenVirtualizesOnlyTheCanonicalImmortalYuanComponentIdentity() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(true);
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(10);
        ItemStack named = data.getInfiniteImmortalYuanPrototype();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("named physical immortal yuan"));

        assertTrue(data.insertStack(named.copy(), true).isEmpty());
        assertEquals(1L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(1, data.simulateExtractStack(named, 5).getCount());
        assertEquals(1, data.extractStack(named, 5).getCount(),
                "a custom-component template must not duplicate from the canonical virtual entry");
        assertEquals(0L, activeCount(data, YuanKind.IMMORTAL));
        assertEquals(5, data.extractStack(data.getInfiniteImmortalYuanPrototype(), 5).getCount());
        assertEquals(Long.MAX_VALUE, data.getImmortalYuan());
    }

    @Test
    void stageTenVirtualCompatibilitySlotDoesNotMoveWhenXianqiaoExpands() {
        com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.set(true);
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(10);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, null, () -> true);
        assertEquals(YuanKind.IMMORTAL, YuanItemPolicy.kindOf(handler.getStackInSlot(0)));

        data.setXianqiaoSlot(500, new ItemStack(Items.PAPER));

        assertEquals(YuanKind.IMMORTAL, YuanItemPolicy.kindOf(handler.getStackInSlot(0)));
        assertEquals(Integer.MAX_VALUE, handler.getStackInSlot(0).getCount());
    }

    @Test
    void replaceStoragePreservesYuanStacksInBothNamespaces() {
        ImmortalStoragePlayerData kongqiao = new ImmortalStoragePlayerData();
        kongqiao.setStage(1);
        kongqiao.replaceStorage(false, List.of(trueYuan(5), new ItemStack(Items.DIAMOND)));
        assertEquals(5L, activeCount(kongqiao, YuanKind.TRUE));
        assertTrue(kongqiao.getKongqiaoItems().stream().anyMatch(stack -> stack.is(Items.DIAMOND)));

        ImmortalStoragePlayerData xianqiao = new ImmortalStoragePlayerData();
        xianqiao.setStage(6);
        xianqiao.replaceStorage(true, List.of(immortalYuan(6), new ItemStack(Items.EMERALD)));
        assertEquals(6L, activeCount(xianqiao, YuanKind.IMMORTAL));
        assertTrue(xianqiao.getXianqiaoStorageItems().stream().anyMatch(stack -> stack.is(Items.EMERALD)));
    }

    private static void assertHandlerRoundTrip(int stage, Item item, YuanKind kind) {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(stage);
        AtomicInteger changes = new AtomicInteger();
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, REGISTRIES, changes::incrementAndGet, () -> true);
        ItemStack offered = new ItemStack(item, 3);

        assertTrue(handler.isItemValid(0, offered));
        assertEquals(64, handler.getSlotLimit(0));
        assertTrue(handler.insertItem(0, offered.copy(), true).isEmpty());
        assertEquals(0L, activeCount(data, kind));
        assertEquals(0, changes.get());

        assertTrue(handler.insertItem(0, offered.copy(), false).isEmpty());
        assertEquals(3L, activeCount(data, kind));
        assertEquals(1, changes.get());
        int slot = findKindSlot(handler, kind);
        assertEquals(3, handler.getStackInSlot(slot).getCount());

        ItemStack simulated = handler.extractItem(slot, 2, true);
        assertEquals(kind, YuanItemPolicy.kindOf(simulated));
        assertEquals(2, simulated.getCount());
        assertEquals(3L, activeCount(data, kind));
        assertEquals(1, changes.get());

        ItemStack extracted = handler.extractItem(slot, 2, false);
        assertEquals(kind, YuanItemPolicy.kindOf(extracted));
        assertEquals(2, extracted.getCount());
        assertEquals(1L, activeCount(data, kind));
        assertEquals(2, changes.get());
    }

    private static int findKindSlot(PersonalStorageItemHandler handler, YuanKind kind) {
        return IntStream.range(0, handler.getSlots())
                .filter(slot -> YuanItemPolicy.kindOf(handler.getStackInSlot(slot)) == kind)
                .findFirst().orElseThrow();
    }

    private static long activeCount(ImmortalStoragePlayerData data, YuanKind kind) {
        List<ItemStack> active = data.isStorageIsKongqiaoLegacy()
                ? data.getKongqiaoItems().subList(0, data.getKongqiaoMaxSlots())
                : data.getXianqiaoStorageItems();
        return countKind(active, kind);
    }

    private static long activeStacks(ImmortalStoragePlayerData data, YuanKind kind) {
        List<ItemStack> active = data.isStorageIsKongqiaoLegacy()
                ? data.getKongqiaoItems().subList(0, data.getKongqiaoMaxSlots())
                : data.getXianqiaoStorageItems();
        return active.stream().filter(stack -> YuanItemPolicy.kindOf(stack) == kind).count();
    }

    private static long allStorageCount(ImmortalStoragePlayerData data, YuanKind kind) {
        return countKind(data.getKongqiaoItems(), kind)
                + countKind(data.getXianqiaoStorageItems(), kind);
    }

    private static long countKind(List<ItemStack> stacks, YuanKind kind) {
        return stacks.stream()
                .filter(stack -> YuanItemPolicy.kindOf(stack) == kind)
                .mapToLong(ItemStack::getCount)
                .sum();
    }

    private static ItemStack trueYuan(int count) {
        return new ItemStack(trueYuanItem, count);
    }

    private static ItemStack immortalYuan(int count) {
        return new ItemStack(immortalYuanItem, count);
    }
}
