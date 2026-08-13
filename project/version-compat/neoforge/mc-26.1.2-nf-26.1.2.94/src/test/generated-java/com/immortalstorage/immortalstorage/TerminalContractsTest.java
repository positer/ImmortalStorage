package com.immortalstorage.immortalstorage;

import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryCatalog;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalCraftingLayout;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalVirtualEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport;
import com.immortalstorage.immortalstorage.client.screen.TerminalLayout;
import com.immortalstorage.immortalstorage.client.screen.TerminalAmountFormatter;
import com.immortalstorage.immortalstorage.client.screen.TerminalTabStyle;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.api.source.SourceChargePlan;
import com.immortalstorage.immortalstorage.api.source.SourceChargeRegistry;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import net.minecraft.server.Bootstrap;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalContractsTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void viewportClampsRowsAndScrollWindow() {
        assertEquals(2, TerminalViewport.clampRows(1));
        assertEquals(12, TerminalViewport.clampRows(99));
        assertEquals(8, TerminalViewport.clampBaseRow(99, 12, 20));
        assertEquals(0, TerminalViewport.clampBaseRow(-4, 5, 3));
        assertEquals(10, TerminalViewport.bufferedRows(5));
        assertEquals(24, TerminalViewport.bufferedRows(12));
        assertEquals(0, TerminalViewport.ensureBufferBase(0, 3, 5, 100));
        assertEquals(3, TerminalViewport.ensureBufferBase(0, 5, 5, 100));
        assertEquals(4, TerminalViewport.ensureBufferBase(0, 6, 5, 100));
        assertEquals(4, TerminalViewport.ensureBufferBase(4, 7, 5, 100));
        assertEquals(7, TerminalViewport.ensureBufferBase(4, 9, 5, 100));
        assertEquals(7, TerminalViewport.recenterBufferBase(9, 5, 100));
    }

    @Test
    void scrollbarThumbTracksContentAndKeepsAUsableGrabArea() {
        assertEquals(15, TerminalLayout.scrollbarThumbHeight(90, 5, 5));
        assertEquals(15, TerminalLayout.scrollbarThumbHeight(90, 5, 10));
        assertEquals(15, TerminalLayout.scrollbarThumbHeight(90, 2, 100));
        assertEquals(73, TerminalLayout.scrollbarTravel(90, 2, 100));
        assertEquals(0, TerminalLayout.scrollbarTravel(90, 5, 5));
        assertEquals(0, TerminalLayout.scrollbarThumbOffset(90, 2, 100, 0.0F));
        assertEquals(73, TerminalLayout.scrollbarThumbOffset(90, 2, 100, 1.0F));
    }

    @Test
    void vanillaSlotTilesMeetExactlyAndViewportUsesTheirOuterEdge() {
        Rect2i first = TerminalLayout.slotTileBounds(8, 18);
        Rect2i next = TerminalLayout.slotTileBounds(26, 18);
        Rect2i last = TerminalLayout.slotTileBounds(8 + 8 * 18, 18 + 4 * 18);
        Rect2i viewport = TerminalLayout.storageBounds(0, 0, 5);

        assertEquals(7, first.getX());
        assertEquals(17, first.getY());
        assertEquals(18, first.getWidth());
        assertEquals(first.getX() + first.getWidth(), next.getX(),
                "adjacent runtime slot sprites meet without overlap");
        assertEquals(first.getX(), viewport.getX());
        assertEquals(first.getY(), viewport.getY());
        assertEquals(9 * 18, viewport.getWidth());
        assertEquals(5 * 18, viewport.getHeight());
        assertEquals(viewport.getX() + viewport.getWidth(), last.getX() + last.getWidth());
        assertEquals(viewport.getY() + viewport.getHeight(), last.getY() + last.getHeight());
    }

    @Test
    void realmCompositeRecentersWithoutClippingAtScaleThreeWidth() {
        int left = TerminalLayout.compositeLeft(427, TerminalLayout.WIDTH,
                TerminalLayout.MODULE_RAIL_X, 4, 104);
        assertEquals(76, left);
        assertTrue(left + TerminalLayout.MODULE_RAIL_X >= 0);
        assertTrue(left + TerminalLayout.WIDTH + 4 + 104 <= 427);
        assertEquals(28, TerminalLayout.compositeLeft(320, TerminalLayout.WIDTH,
                        TerminalLayout.MODULE_RAIL_X, 4, 104),
                "when the composite cannot fit, preserve the left module rail and minimize right overflow");
    }

    @Test
    void leftModuleTabsUseExactVanillaAdvancementGeometryAndRuntimeSprites() {
        assertEquals(32, TerminalTabStyle.WIDTH);
        assertEquals(28, TerminalTabStyle.HEIGHT);
        assertEquals(-28, TerminalLayout.MODULE_RAIL_X,
                "left tabs overlap the terminal panel by the vanilla four pixels");
        assertEquals(10, TerminalTabStyle.Side.LEFT.iconInsetX());
        assertEquals(6, TerminalTabStyle.Side.RIGHT.iconInsetX());

        assertEquals(TerminalTabStyle.Segment.TOP, TerminalTabStyle.segment(0, 3));
        assertEquals(TerminalTabStyle.Segment.MIDDLE, TerminalTabStyle.segment(1, 3));
        assertEquals(TerminalTabStyle.Segment.BOTTOM, TerminalTabStyle.segment(2, 3));
        assertEquals("minecraft:advancements/tab_left_top",
                TerminalTabStyle.sprite(TerminalTabStyle.Side.LEFT,
                        TerminalTabStyle.Segment.TOP, false).toString());
        assertEquals("minecraft:advancements/tab_left_middle_selected",
                TerminalTabStyle.sprite(TerminalTabStyle.Side.LEFT,
                        TerminalTabStyle.Segment.MIDDLE, true).toString());
        assertEquals("minecraft:advancements/tab_right_bottom_selected",
                TerminalTabStyle.sprite(TerminalTabStyle.Side.RIGHT,
                        TerminalTabStyle.Segment.BOTTOM, true).toString());

        assertEquals(-20, TerminalLayout.railControlX(),
                "compact controls remain centered under the 32px primary tabs");
        assertEquals(152, TerminalLayout.railHeight(3));
        assertEquals(TerminalLayout.imageHeight(2, false), TerminalLayout.railHeight(3),
                "three primary tabs plus four compact controls fit the minimum two-row terminal");
        assertThrows(IllegalArgumentException.class, () -> TerminalTabStyle.segment(3, 3));
    }

    @Test
    void terminalAndSlotHitBoundsAreHalfOpenAndExposeOnlyVisibleSlotPixels() {
        Rect2i viewport = TerminalLayout.storageBounds(100, 50, 5);
        int firstSlotX = 100 + TerminalLayout.STORAGE_X;
        int firstSlotY = 50 + TerminalLayout.STORAGE_Y;
        Rect2i firstSlot = TerminalLayout.clippedSlotBounds(viewport, firstSlotX, firstSlotY);
        Rect2i topPartial = TerminalLayout.clippedSlotBounds(
                viewport, firstSlotX, viewport.getY() - 8);

        assertTrue(TerminalLayout.terminalContains(100, 50, 200, 100, 50));
        assertTrue(!TerminalLayout.terminalContains(100, 50, 200,
                100 + TerminalLayout.WIDTH, 50));
        assertTrue(TerminalLayout.containsHalfOpen(viewport,
                viewport.getX(), viewport.getY()));
        assertTrue(!TerminalLayout.containsHalfOpen(viewport,
                viewport.getX() + viewport.getWidth(), viewport.getY()));
        assertEquals(TerminalLayout.SLOT_SIZE, firstSlot.getWidth());
        assertEquals(TerminalLayout.SLOT_SIZE, firstSlot.getHeight());
        assertEquals(viewport.getY(), topPartial.getY());
        assertEquals(8, topPartial.getHeight());
        assertTrue(TerminalLayout.containsHalfOpen(topPartial,
                topPartial.getX(), topPartial.getY()));
        assertTrue(!TerminalLayout.containsHalfOpen(topPartial,
                topPartial.getX(), topPartial.getY() - 1));
        assertTrue(!TerminalLayout.containsHalfOpen(topPartial,
                topPartial.getX(), topPartial.getY() + topPartial.getHeight()));
        assertEquals(-9.0D, TerminalLayout.wheelScrollDelta(1.0D), 0.0001D);
        assertEquals(2.25D, TerminalLayout.wheelScrollDelta(-0.25D), 0.0001D);
    }

    @Test
    void continuousScrollUsesHalfStepsAndWholeRowProxyBoundaries() {
        assertEquals(0, TerminalLayout.baseRow(9.0D));
        assertEquals(9, TerminalLayout.fractionalScrollOffset(9.0D));
        assertEquals(1, TerminalLayout.baseRow(18.0D));
        assertEquals(0, TerminalLayout.fractionalScrollOffset(18.0D));
        assertEquals(-1, TerminalLayout.visualStorageRow(0, 0, 1));
        assertEquals(0, TerminalLayout.visualStorageRow(9, 0, 1));
        assertEquals(0, TerminalLayout.visualStorageRow(0, 1, 1));
    }

    @Test
    void embeddedFurnaceUsesEightRealSlotsAndVerticalPluginFlameFuelColumn() {
        int height = TerminalLayout.imageHeight(5, true);
        assertEquals(3, TerminalLayout.FURNACE_LANE_COUNT);
        assertEquals(129, TerminalLayout.furnaceLaneY(height, 0));
        assertEquals(147, TerminalLayout.furnaceLaneY(height, 1));
        assertEquals(165, TerminalLayout.furnaceLaneY(height, 2));
        assertEquals(TerminalLayout.furnaceLaneY(height, 0), TerminalLayout.furnacePluginY(height));
        assertEquals(TerminalLayout.furnaceLaneY(height, 1) + 3, TerminalLayout.furnaceFlameY(height));
        assertEquals(TerminalLayout.furnaceLaneY(height, 2), TerminalLayout.furnaceFuelY(height));
        assertTrue(TerminalLayout.furnaceLaneY(height, 2) + TerminalLayout.SLOT_SIZE
                < TerminalLayout.inventoryY(height), "all three furnace lanes stay above the player inventory");

        assertEquals(8, XianqiaoStorageMenu.FURNACE_END - XianqiaoStorageMenu.FURNACE_START);
        assertEquals(XianqiaoStorageMenu.FURNACE_RESULT_3_SLOT + 1,
                XianqiaoStorageMenu.FURNACE_PLUGIN_SLOT);
        assertEquals(XianqiaoStorageMenu.FURNACE_END, XianqiaoStorageMenu.ARMOR_START);
        assertEquals(4, XianqiaoStorageMenu.ARMOR_END - XianqiaoStorageMenu.ARMOR_START);
        assertEquals(XianqiaoStorageMenu.ARMOR_END, XianqiaoStorageMenu.PLAYER_START);
        assertTrue(XianqiaoStorageMenu.isFurnaceInputSlotIndex(XianqiaoStorageMenu.FURNACE_INPUT_3_SLOT));
        assertTrue(XianqiaoStorageMenu.isFurnaceResultSlotIndex(XianqiaoStorageMenu.FURNACE_RESULT_3_SLOT));
        assertEquals(2, XianqiaoStorageMenu.furnaceChannelForSlot(
                XianqiaoStorageMenu.FURNACE_RESULT_3_SLOT));

        assertEquals(8, KongqiaoMenu.FURNACE_END - KongqiaoMenu.FURNACE_START);
        assertEquals(KongqiaoMenu.FURNACE_RESULT_3_SLOT + 1, KongqiaoMenu.FURNACE_PLUGIN_SLOT);
        assertEquals(KongqiaoMenu.FURNACE_END, KongqiaoMenu.ARMOR_START);
        assertEquals(4, KongqiaoMenu.ARMOR_END - KongqiaoMenu.ARMOR_START);
        assertEquals(KongqiaoMenu.ARMOR_END, KongqiaoMenu.PLAYER_START);
        assertTrue(!KongqiaoMenu.isFurnaceUnlockedAtStage(4));
        assertTrue(KongqiaoMenu.isFurnaceUnlockedAtStage(5));
        assertTrue(!KongqiaoMenu.isFurnaceUnlockedAtStage(6));
        assertEquals(1, KongqiaoMenu.furnaceChannelForSlot(KongqiaoMenu.FURNACE_INPUT_2_SLOT));
    }

    @Test
    void craftingOutputFrameAndInteractiveSlotShareOneLogicalAnchorForEveryRowCount() {
        for (int rows = TerminalLayout.MIN_ROWS; rows <= TerminalLayout.MAX_ROWS; rows++) {
            int imageHeight = TerminalLayout.imageHeight(rows, true);
            Rect2i firstInput = TerminalLayout.craftInputSlotBounds(imageHeight, 0, 0);
            Rect2i lastInput = TerminalLayout.craftInputSlotBounds(imageHeight, 2, 2);
            Rect2i arrow = TerminalLayout.craftArrowBounds(imageHeight);
            Rect2i slot = TerminalLayout.craftResultSlotBounds(imageHeight);
            Rect2i frame = TerminalLayout.craftResultFrameBounds(imageHeight);

            assertRectEquals(new Rect2i(26, imageHeight - 158, 16, 16),
                    firstInput, "first crafting input");
            assertRectEquals(new Rect2i(62, imageHeight - 122, 16, 16),
                    lastInput, "last crafting input");
            assertRectEquals(new Rect2i(92, imageHeight - 138, 22, 15),
                    arrow, "crafting arrow");
            assertEquals(TerminalLayout.CRAFT_RESULT_X, slot.getX());
            assertEquals(imageHeight - 140, slot.getY());
            assertEquals(TerminalLayout.SLOT_SIZE, slot.getWidth());
            assertEquals(TerminalLayout.SLOT_SIZE, slot.getHeight());
            assertEquals(TerminalLayout.CRAFT_RESULT_FRAME_MARGIN,
                    slot.getX() - frame.getX());
            assertEquals(TerminalLayout.CRAFT_RESULT_FRAME_MARGIN,
                    slot.getY() - frame.getY());
            assertEquals(TerminalLayout.CRAFT_RESULT_FRAME_MARGIN,
                    frame.getX() + frame.getWidth() - (slot.getX() + slot.getWidth()));
            assertEquals(TerminalLayout.CRAFT_RESULT_FRAME_MARGIN,
                    frame.getY() + frame.getHeight() - (slot.getY() + slot.getHeight()));

            for (int guiScale = 2; guiScale <= 4; guiScale++) {
                assertRectEquals(slot, TerminalLayout.craftResultSlotBounds(imageHeight),
                        "GUI scale " + guiScale + " must not change logical slot coordinates");
                assertRectEquals(frame, TerminalLayout.craftResultFrameBounds(imageHeight),
                        "GUI scale " + guiScale + " must not change logical frame coordinates");
            }
        }

        int defaultHeight = TerminalLayout.imageHeight(TerminalLayout.DEFAULT_ROWS, true);
        assertEquals(TerminalCraftingLayout.MENU_BASELINE_IMAGE_HEIGHT, defaultHeight,
                "menu slots use the same five-row baseline as the client layout");
        assertRectEquals(new Rect2i(134, 147, 16, 16),
                TerminalLayout.craftResultSlotBounds(defaultHeight), "default result slot");
        assertRectEquals(new Rect2i(129, 142, 26, 26),
                TerminalLayout.craftResultFrameBounds(defaultHeight), "default result frame");
    }

    private static void assertRectEquals(Rect2i expected, Rect2i actual, String message) {
        assertEquals(expected.getX(), actual.getX(), message + " x");
        assertEquals(expected.getY(), actual.getY(), message + " y");
        assertEquals(expected.getWidth(), actual.getWidth(), message + " width");
        assertEquals(expected.getHeight(), actual.getHeight(), message + " height");
    }

    @Test
    void terminalAmountsUseCompactLongAwareLabels() {
        assertEquals("999", TerminalAmountFormatter.format(999L));
        assertEquals("1.2k", TerminalAmountFormatter.format(1_234L));
        assertEquals("321k", TerminalAmountFormatter.format(321_999L));
        assertEquals("4.9M", TerminalAmountFormatter.format(4_999_999L));
        assertEquals("9.2E", TerminalAmountFormatter.format(Long.MAX_VALUE));
    }

    @Test
    void kongqiaoStageCapacityRemainsStable() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        int[] expected = {9, 18, 36, 54, 72};
        for (int stage = 1; stage <= expected.length; stage++) {
            data.setStage(stage);
            assertEquals(expected[stage - 1], data.getKongqiaoMaxSlots());
        }
    }

    @Test
    void finiteKongqiaoRowsNeverExceedTheirPhysicalCapacity() {
        int[] capacities = {9, 18, 36, 54, 72};
        int[] expectedRows = {2, 2, 4, 6, 8};
        for (int stage = 0; stage < capacities.length; stage++) {
            int contentRows = (capacities[stage] + TerminalLayout.COLUMNS - 1)
                    / TerminalLayout.COLUMNS;
            assertEquals(expectedRows[stage], Math.max(TerminalLayout.MIN_ROWS, contentRows));
            assertEquals(expectedRows[stage],
                    TerminalLayout.effectiveRows(12, 1_000, false, contentRows),
                    "stage " + (stage + 1) + " must not draw complete empty rows");
            assertEquals(expectedRows[stage],
                    KongqiaoMenu.clampViewportRows(12, contentRows),
                    "the server must reject a client viewport larger than finite storage");
        }

        assertEquals(2, TerminalLayout.effectiveRows(12, 152, false, 9),
                "screen-height clamping remains stricter than the storage capacity");
        assertEquals(5, TerminalLayout.effectiveRows(5, 1_000, false, 9),
                "the user's configured row preference remains effective when it is stricter");
    }

    @Test
    void sourceChargePlansUseWholeBatchesWithoutOverflow() {
        SourceChargePlan perStack = new SourceChargePlan(SourceChargeRegistry.IMMORTAL_YUAN, 1, 64);
        assertEquals(0, perStack.requiredUnits(0));
        assertEquals(1, perStack.requiredUnits(1));
        assertEquals(1, perStack.requiredUnits(64));
        assertEquals(2, perStack.requiredUnits(65));
        assertEquals(1, perStack.requiredUnits(63));
        assertEquals(0, SourceChargeRegistry.refundableUnits(perStack, 64, 63));
        assertEquals(1, SourceChargeRegistry.refundableUnits(perStack, 65, 64));
        assertEquals(1, SourceChargeRegistry.refundableUnits(perStack, 1, 0));

        SourceChargePlan expensive = new SourceChargePlan(SourceChargeRegistry.IMMORTAL_YUAN, 64, 1);
        assertEquals(128, expensive.requiredUnits(2));
        assertEquals(Long.MAX_VALUE, expensive.requiredUnits(Long.MAX_VALUE));
    }

    @Test
    void catalogAggregatesCountsButSeparatesComponents() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        stacks.add(new ItemStack(Items.IRON_INGOT, 64));
        stacks.add(new ItemStack(Items.IRON_INGOT, 16));
        ItemStack named = new ItemStack(Items.IRON_INGOT, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named Iron"));
        stacks.add(named);

        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfChanged(stacks);
        var entries = catalog.entries(TerminalQuery.DEFAULT);

        assertEquals(2, entries.size());
        assertEquals(80L, entries.stream().filter(e -> !e.displayStack().has(DataComponents.CUSTOM_NAME))
                .findFirst().orElseThrow().amount());
        assertEquals(7L, entries.stream().filter(e -> e.displayStack().has(DataComponents.CUSTOM_NAME))
                .findFirst().orElseThrow().amount());
    }

    @Test
    void onePhysicalInsertRegistersExactlyOneAggregatedUnit() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        assertTrue(data.insertStack(new ItemStack(Items.AMETHYST_SHARD, 1), true).isEmpty());

        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfChanged(data.getXianqiaoStorageItems());
        assertEquals(1L, catalog.entries(TerminalQuery.DEFAULT).getFirst().amount());

        assertTrue(data.insertStack(new ItemStack(Items.AMETHYST_SHARD, 1), true).isEmpty());
        catalog.rebuildIfChanged(data.getXianqiaoStorageItems());
        assertEquals(2L, catalog.entries(TerminalQuery.DEFAULT).getFirst().amount(),
                "a second distinct insert increments the total once, not twice per request");
    }

    @Test
    void catalogKeepsRenderAmountsAsLongInsteadOfItemStackCounts() {
        NonNullList<ItemStack> tenThousand = NonNullList.create();
        for (int index = 0; index < 156; index++) {
            tenThousand.add(new ItemStack(Items.GOLD_INGOT, 64));
        }
        tenThousand.add(new ItemStack(Items.GOLD_INGOT, 16));

        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfChanged(tenThousand);
        var tenThousandEntry = catalog.entries(TerminalQuery.DEFAULT).getFirst();

        assertEquals(10_000L, tenThousandEntry.amount());
        assertEquals(1, tenThousandEntry.displayStack().getCount(),
                "the proxy stack carries identity only; the overlay must read the long amount");

        NonNullList<ItemStack> aboveIntegerMax = NonNullList.create();
        ItemStack first = new ItemStack(Items.DIAMOND);
        first.setCount(Integer.MAX_VALUE);
        ItemStack second = new ItemStack(Items.DIAMOND);
        second.setCount(42);
        aboveIntegerMax.add(first);
        aboveIntegerMax.add(second);

        catalog.rebuildIfChanged(aboveIntegerMax);
        var largeEntry = catalog.entries(TerminalQuery.DEFAULT).getFirst();
        assertEquals((long) Integer.MAX_VALUE + 42L, largeEntry.amount());
        assertTrue(largeEntry.amount() > Integer.MAX_VALUE);
        assertEquals(1, largeEntry.displayStack().getCount());
    }

    @Test
    void virtualEntryOverridesPhysicalAggregateAndInvalidatesAtTheSameStorageRevision() {
        List<ItemStack> physical = List.of(new ItemStack(Items.EMERALD, 64));
        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        List<TerminalVirtualEntry> infinite = List.of(
                new TerminalVirtualEntry(new ItemStack(Items.EMERALD), Long.MAX_VALUE));

        assertTrue(catalog.rebuildIfStale(physical, 7L, infinite));
        var first = catalog.entries(TerminalQuery.DEFAULT).getFirst();
        assertEquals(Long.MAX_VALUE, first.amount(),
                "the logical total replaces rather than adds the physical stack count");
        assertEquals(1, first.displayStack().getCount());
        long stableId = first.entryId();
        assertTrue(!catalog.rebuildIfStale(physical, 7L, infinite));

        assertTrue(catalog.rebuildIfStale(physical, 7L, List.of(
                new TerminalVirtualEntry(new ItemStack(Items.EMERALD), Long.MAX_VALUE - 1L))));
        assertEquals(stableId, catalog.entries(TerminalQuery.DEFAULT).getFirst().entryId());
        assertEquals(Long.MAX_VALUE - 1L, catalog.entries(TerminalQuery.DEFAULT).getFirst().amount());
    }

    @Test
    void stableIdsSurviveQuantityOnlyChangesAndRevisionAdvances() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        stacks.add(new ItemStack(Items.DIAMOND, 3));
        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        catalog.rebuildIfChanged(stacks);
        long firstId = catalog.entries(TerminalQuery.DEFAULT).getFirst().entryId();
        long firstRevision = catalog.revision();

        stacks.getFirst().grow(5);
        catalog.rebuildIfChanged(stacks);

        assertEquals(firstId, catalog.entries(TerminalQuery.DEFAULT).getFirst().entryId());
        assertNotEquals(firstRevision, catalog.revision());
    }

    @Test
    void xianqiaoMutationRevisionAdvancesOncePerCommittedBatch() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        assertEquals(0L, data.getXianqiaoStorageRevision());

        data.batchXianqiaoMutations(() -> {
            data.insertStack(new ItemStack(Items.IRON_INGOT, 64), true);
            data.insertStack(new ItemStack(Items.IRON_INGOT, 64), true);
            data.insertStack(new ItemStack(Items.GOLD_INGOT, 3), true);
        });
        assertEquals(1L, data.getXianqiaoStorageRevision());

        data.simulateExtractStack(new ItemStack(Items.IRON_INGOT), 80);
        assertEquals(1L, data.getXianqiaoStorageRevision());
        assertEquals(80, data.extractStack(new ItemStack(Items.IRON_INGOT), 80).getCount());
        assertEquals(2L, data.getXianqiaoStorageRevision());

        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(data, registries, () -> {});
        int goldSlot = -1;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(Items.GOLD_INGOT)) {
                goldSlot = slot;
                break;
            }
        }
        assertTrue(goldSlot >= 0);
        handler.extractItem(goldSlot, 1, true);
        assertEquals(2L, data.getXianqiaoStorageRevision());
        handler.extractItem(goldSlot, 1, false);
        assertEquals(3L, data.getXianqiaoStorageRevision());
    }

    @Test
    void emptyPersonalStorageExposesWritablePhysicalSlotsToStorageBuses() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(data, registries, () -> {});

        assertTrue(handler.getSlots() > 0);
        assertTrue(handler.getStackInSlot(0).isEmpty());
        assertTrue(handler.insertItem(0, new ItemStack(Items.DIAMOND, 3), true).isEmpty());
        assertTrue(data.getXianqiaoStorageItems().stream().allMatch(ItemStack::isEmpty));
        assertTrue(handler.insertItem(0, new ItemStack(Items.DIAMOND, 3), false).isEmpty());
        assertTrue(handler.getStackInSlot(0).isEmpty(), "the fixed import slot remains empty");
        assertEquals(3, handler.getStackInSlot(1).getCount());
    }

    @Test
    void fullVisibleXianqiaoStillExposesAnEmptyImportSlotToGenericBuses() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        for (int slot = 0; slot < data.getXianqiaoStorageItems().size(); slot++) {
            ItemStack unique = new ItemStack(Items.PAPER);
            unique.set(DataComponents.CUSTOM_NAME, Component.literal("occupied-" + slot));
            data.setXianqiaoSlot(slot, unique);
        }
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, RegistryAccess.EMPTY, () -> {});
        int importSlot = 0;
        assertTrue(handler.getStackInSlot(importSlot).isEmpty());

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                handler, new ItemStack(Items.DIAMOND, 3), false);

        assertTrue(remainder.isEmpty());
        assertEquals(3, data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum());
        assertTrue(handler.getStackInSlot(importSlot).isEmpty(),
                "growth must not move or remove the synthetic import slot");
    }

    @Test
    void personalStorageRejectsOutOfRangeSlots() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        PersonalStorageItemHandler handler = new PersonalStorageItemHandler(
                data, RegistryAccess.EMPTY, () -> {});
        ItemStack offered = new ItemStack(Items.DIAMOND, 3);

        assertEquals(0, handler.getSlotLimit(-1));
        assertEquals(0, handler.getSlotLimit(handler.getSlots()));
        assertTrue(!handler.isItemValid(-1, offered));
        assertTrue(!handler.isItemValid(handler.getSlots(), offered));
        assertEquals(3, handler.insertItem(-1, offered.copy(), false).getCount());
        assertEquals(3, handler.insertItem(handler.getSlots(), offered.copy(), false).getCount());
        assertTrue(data.getXianqiaoStorageItems().stream().allMatch(ItemStack::isEmpty));
    }

    @Test
    void xianqiaoStorageViewCannotBypassRevisionTracking() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.insertStack(new ItemStack(Items.DIAMOND, 3), true);
        long before = data.getXianqiaoStorageRevision();

        assertThrows(UnsupportedOperationException.class,
                () -> data.getXianqiaoStorageItems().set(0, new ItemStack(Items.DIRT)));
        assertEquals(before, data.getXianqiaoStorageRevision());

        ItemStack detached = data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.DIAMOND)).findFirst().orElseThrow();
        detached.shrink(1);
        detached.setCount(31);
        detached.set(DataComponents.CUSTOM_NAME, Component.literal("mutated snapshot"));
        ItemStack authoritative = data.getXianqiaoStorageItems().stream()
                .filter(stack -> stack.is(Items.DIAMOND)).findFirst().orElseThrow();
        assertEquals(3, authoritative.getCount());
        assertTrue(!authoritative.has(DataComponents.CUSTOM_NAME),
                "mutating a returned ItemStack must not bypass the authoritative revision/index");
        assertEquals(before, data.getXianqiaoStorageRevision());
    }

    @Test
    void tenThousandEntryCatalogDoesNotRescanDuringTwoHundredIdleRefreshes() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.batchXianqiaoMutations(() -> {
            for (int index = 0; index < 10_000; index++) {
                ItemStack stack = new ItemStack(Items.PAPER);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("idle-entry-" + index));
                data.setXianqiaoSlot(index, stack);
            }
        });
        assertEquals(1L, data.getXianqiaoStorageRevision());

        TerminalEntryCatalog catalog = new TerminalEntryCatalog();
        assertTrue(catalog.rebuildIfStale(data.getXianqiaoStorageItems(), data.getXianqiaoStorageRevision()));
        assertEquals(1L, catalog.fullScanCount());
        for (int tick = 0; tick < 200; tick++) {
            assertTrue(!catalog.rebuildIfStale(data.getXianqiaoStorageItems(), data.getXianqiaoStorageRevision()));
        }
        assertEquals(1L, catalog.fullScanCount(), "idle terminal refreshes must remain O(1)");

        data.insertStack(new ItemStack(Items.PAPER), true);
        assertTrue(catalog.rebuildIfStale(data.getXianqiaoStorageItems(), data.getXianqiaoStorageRevision()));
        assertEquals(2L, catalog.fullScanCount());
    }

    @Test
    void xianqiaoInsertExpandsForTenThousandDistinctComponentVariants() {
        assertTimeout(Duration.ofSeconds(20), () -> {
            ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
            data.setStage(6);
            for (int index = 0; index < 10_000; index++) {
                ItemStack stack = new ItemStack(Items.PAPER);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("terminal-entry-" + index));
                assertTrue(data.insertStack(stack, true).isEmpty());
            }

            TerminalEntryCatalog catalog = new TerminalEntryCatalog();
            catalog.rebuildIfChanged(data.getXianqiaoStorageItems());

            assertEquals(10_000, data.getXianqiaoStorageItems().stream().filter(stack -> !stack.isEmpty()).count());
            assertEquals(10_000, data.getXianqiaoStorageItems().stream()
                    .filter(stack -> !stack.isEmpty()).map(ItemStack::hashItemAndComponents).distinct().count());
            assertEquals(10_000, catalog.entries(TerminalQuery.DEFAULT).size());
            assertEquals(1, catalog.entries(new TerminalQuery("terminal-entry-9999",
                    TerminalQuery.SortOrder.NAME, TerminalQuery.SortDirection.ASCENDING)).size());
        });
    }

    @Test
    void xianqiaoPersistencePreservesFullDataComponentsAfterExpansion() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        for (int index = 0; index < 300; index++) {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("persisted-entry-" + index));
            assertTrue(data.insertStack(stack, true).isEmpty());
        }

        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var serialized = com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(data, registries);
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(restored, registries, serialized);

        assertEquals(300, restored.getXianqiaoStorageItems().stream().filter(stack -> !stack.isEmpty()).count());
        assertEquals("persisted-entry-299", restored.getXianqiaoStorageItems().stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.get(DataComponents.CUSTOM_NAME))
                .filter(java.util.Objects::nonNull)
                .map(Component::getString)
                .filter("persisted-entry-299"::equals)
                .findFirst().orElseThrow());
    }

    @Test
    void legacyStorageStackEncodingStillLoads() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", "minecraft:paper");
        legacy.putInt("Count", 17);
        CompoundTag custom = new CompoundTag();
        custom.putString("legacy", "kept");
        legacy.put("tag", custom);

        RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack restored = ImmortalStoragePlayerData.loadStack(registries, legacy);

        assertEquals(17, restored.getCount());
        assertEquals("kept", restored.getOrDefault(DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("legacy", ""));
    }
}
