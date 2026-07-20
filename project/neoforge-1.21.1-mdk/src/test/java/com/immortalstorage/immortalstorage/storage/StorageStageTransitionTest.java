package com.immortalstorage.immortalstorage.storage;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryCatalog;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageFluidHandler;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.yuan.YuanItemPolicy;
import com.immortalstorage.immortalstorage.player.yuan.YuanKind;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageStageTransitionTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Test
    void stageFiveUsesSeventyTwoFiniteSlotsAtSixteenTimesNativeStackSize() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);

        assertEquals(72, data.getKongqiaoMaxSlots());
        assertEquals(16, data.getKongqiaoStackMultiplier());
        assertEquals(1_024, data.getKongqiaoStackLimit(new ItemStack(Items.COBBLESTONE)));
        assertEquals(16, data.getKongqiaoStackLimit(new ItemStack(Items.DIAMOND_SWORD)));

        int finiteCapacity = 72 * 1_024;
        assertTrue(data.insertStack(new ItemStack(Items.COBBLESTONE, finiteCapacity), true).isEmpty());
        assertEquals(72, data.getKongqiaoItems().stream().filter(stack -> !stack.isEmpty()).count());
        assertTrue(data.getKongqiaoItems().stream().allMatch(
                stack -> stack.isEmpty() || stack.getCount() == 1_024));

        ItemStack rejected = data.insertStack(new ItemStack(Items.COBBLESTONE), true);
        assertEquals(1, rejected.getCount(), "stage five must remain finite when all physical slots are full");
    }

    @Test
    void stageFiveExtendedCountsSurvivePersistenceCodec() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        assertTrue(data.insertStack(new ItemStack(Items.IRON_INGOT, 1_024), true).isEmpty());

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, data.serializeNBT(REGISTRIES));

        assertEquals(5, restored.getStage());
        assertEquals(1_024, restored.getKongqiaoItems().getFirst().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(
                data.getKongqiaoItems().getFirst(), restored.getKongqiaoItems().getFirst()));
    }

    @Test
    void oldStageSixSaveWithStrandedKongqiaoItemsRepairsOnceOnLoad() {
        ImmortalStoragePlayerData legacy = new ImmortalStoragePlayerData();
        legacy.setStage(5);
        legacy.setKongqiaoSlot(79, new ItemStack(Items.GOLD_INGOT, 777));
        var legacyTag = legacy.serializeNBT(REGISTRIES);
        legacyTag.putInt("stage", 6);
        legacyTag.remove("ascensionTrueYuanConverted");

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, legacyTag);

        assertEquals(6, restored.getStage());
        assertTrue(restored.getKongqiaoItems().stream().allMatch(ItemStack::isEmpty));
        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfStale(restored.getXianqiaoStorageItems(), restored.getXianqiaoStorageRevision());
        assertEquals(777L, catalog.entries(TerminalQuery.DEFAULT).getFirst().amount());

        ImmortalStoragePlayerData secondLoad = new ImmortalStoragePlayerData();
        secondLoad.deserializeNBT(REGISTRIES, restored.serializeNBT(REGISTRIES));
        TerminalEntryCatalog secondCatalog = new TerminalEntryCatalog();
        secondCatalog.rebuildIfStale(secondLoad.getXianqiaoStorageItems(), secondLoad.getXianqiaoStorageRevision());
        assertEquals(777L, secondCatalog.entries(TerminalQuery.DEFAULT).getFirst().amount(),
                "the persisted repair must not duplicate on subsequent loads");
    }

    @Test
    void fiveToSixMigratesOnceAggregatesCountsAndSeparatesComponents() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        assertEquals(1_024L, data.depositTrueYuan(1_024L));
        assertTrue(data.insertStack(new ItemStack(Items.AMETHYST_SHARD, 1_024), true).isEmpty());

        ItemStack named = new ItemStack(Items.AMETHYST_SHARD, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("bound-identity"));
        assertTrue(data.insertStack(named, true).isEmpty());

        data.setStage(6);

        assertEquals(0L, data.getTrueYuan());
        assertEquals(64L, data.getImmortalYuan(),
                "the stage-five balance converts at sixteen true yuan per immortal yuan");
        assertTrue(data.getKongqiaoItems().stream().allMatch(ItemStack::isEmpty),
                "the finite source inventory must be cleared only after successful migration");
        assertEquals(1L, data.getXianqiaoStorageRevision(),
                "the complete boundary migration is one authoritative batch");

        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfStale(data.getXianqiaoStorageItems(), data.getXianqiaoStorageRevision());
        var entries = catalog.entries(TerminalQuery.DEFAULT);
        assertEquals(3, entries.size(), "physical immortal yuan must be visible in the terminal catalog");
        assertEquals(64L, entries.stream()
                .filter(entry -> YuanItemPolicy.kindOf(entry.displayStack()) == YuanKind.IMMORTAL)
                .findFirst().orElseThrow().amount());
        assertEquals(1_024L, entries.stream()
                .filter(entry -> entry.displayStack().is(Items.AMETHYST_SHARD)
                        && !entry.displayStack().has(DataComponents.CUSTOM_NAME))
                .findFirst().orElseThrow().amount());
        assertEquals(7L, entries.stream()
                .filter(entry -> entry.displayStack().has(DataComponents.CUSTOM_NAME))
                .findFirst().orElseThrow().amount());

        assertTrue(data.insertStack(new ItemStack(Items.AMETHYST_SHARD, 5_000), true).isEmpty());
        catalog.rebuildIfStale(data.getXianqiaoStorageItems(), data.getXianqiaoStorageRevision());
        assertEquals(6_024L, catalog.entries(TerminalQuery.DEFAULT).stream()
                .filter(entry -> entry.displayStack().is(Items.AMETHYST_SHARD)
                        && !entry.displayStack().has(DataComponents.CUSTOM_NAME))
                .findFirst().orElseThrow().amount());
        assertEquals(ImmortalStoragePlayerData.XIANQIAO_INITIAL_SLOTS,
                data.getXianqiaoStorageItems().size(),
                "same-component growth must stay in one compact internal overstack");
        assertEquals(1L, data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.AMETHYST_SHARD)
                        && !stack.has(DataComponents.CUSTOM_NAME))
                .count());

        long beforeDebugRoundTrip = data.getXianqiaoStorageRevision();
        data.setStage(5);
        data.setStage(6);
        assertEquals(beforeDebugRoundTrip, data.getXianqiaoStorageRevision(),
                "a debug downgrade/re-entry cannot duplicate an already migrated inventory");
    }

    @Test
    void debugReentryDoesNotRepeatAscensionConversionForLaterTrueYuan() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        assertEquals(16L, data.depositTrueYuan(16L));
        data.setStage(6);
        assertEquals(1L, data.getImmortalYuan());

        assertEquals(16L, data.depositTrueYuan(16L));
        assertEquals(16L, data.getTrueYuan());
        data.setStage(5); // defensive debug-only path
        data.setStage(6);

        assertEquals(16L, data.getTrueYuan(),
                "only the first real ascension boundary may convert true yuan");
        assertEquals(1L, data.getImmortalYuan());
    }

    @Test
    void itemHandlerTracksXianqiaoAtSixWhileFluidHandlerStartsAtSeven() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        PersonalStorageItemHandler kongqiaoItems = new PersonalStorageItemHandler(data, REGISTRIES, () -> {});
        PersonalStorageFluidHandler fluids = new PersonalStorageFluidHandler(data, () -> {});

        assertEquals(72, kongqiaoItems.getSlots());
        assertEquals(0, fluids.getTanks());
        assertEquals(0, fluids.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertTrue(kongqiaoItems.insertItem(0, new ItemStack(Items.DIAMOND, 3), false).isEmpty());
        assertEquals(3, kongqiaoItems.getStackInSlot(0).getCount());

        data.setStage(6);
        assertEquals(0, kongqiaoItems.getSlots(),
                "the finite physical-slot handler must retire when stage six changes slot semantics");
        PersonalStorageItemHandler items = new PersonalStorageItemHandler(data, REGISTRIES, () -> {});
        assertTrue(items.getSlots() > 0);
        assertEquals(0, fluids.getTanks());
        assertEquals(0, fluids.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));

        data.setStage(7);
        assertEquals(1, fluids.getTanks(), "an empty storage exposes its synthetic import tank");
        assertEquals(1_000, fluids.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(2, fluids.getTanks(), "stored fluid plus the synthetic import tank");

        data.setStage(6); // defensive debug-only path, not normal progression
        assertTrue(items.getSlots() > 0);
        assertEquals(0, fluids.getTanks());
        assertEquals(0, fluids.fill(new FluidStack(Fluids.LAVA, 1_000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(1_000L, data.getXianqiaoFluidAmounts().values().stream()
                .mapToLong(Long::longValue).sum(), "debug downgrade hides but never deletes Xianqiao data");
    }

    @Test
    void customItemHandlerAccessPredicateStillUsesTheCurrentKongqiaoNamespace() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        PersonalStorageItemHandler items = new PersonalStorageItemHandler(
                data, REGISTRIES, () -> {}, () -> true);

        assertEquals(72, items.getSlots());
        assertTrue(items.insertItem(0, new ItemStack(Items.DIAMOND, 3), false).isEmpty());
        assertEquals(3, data.getKongqiaoItems().stream()
                .filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum(),
                "the public item bridge must expose stage-five Kongqiao without enabling the manager block");
    }
}
