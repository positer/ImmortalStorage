package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.compressor.EnchantmentStackRules;
import com.immortalstorage.immortalstorage.compressor.EnchantmentTestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EnchantmentMachineLogicTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static RegistryAccess.Frozen registries;

    @BeforeAll static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static EnchantmentMachineLogic machine(boolean reversed) {
        return new EnchantmentMachineLogic(new MiniatureImmortalRuinBlockEntity(BlockPos.ZERO,
                ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState()
                        .setValue(MiniatureImmortalRuinBlock.REVERSED, reversed)));
    }

    @Test void slotValidityFollowsTheAcceptedClasses() {
        var compressor = machine(false);
        assertTrue(compressor.inventory.isItemValid(0, new ItemStack(Items.BOOK)));
        assertTrue(compressor.inventory.isItemValid(0, new ItemStack(Items.ENCHANTED_BOOK)));
        assertTrue(compressor.inventory.isItemValid(0, new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(compressor.inventory.isItemValid(11, new ItemStack(Items.ENCHANTED_BOOK)));
        assertFalse(compressor.inventory.isItemValid(0, new ItemStack(Items.DIRT)));
        assertFalse(compressor.inventory.isItemValid(12, new ItemStack(Items.BOOK)), "the product slot accepts nothing");
        assertEquals(Integer.MAX_VALUE, compressor.inventory.getSlotLimit(12));

        var decompressor = machine(true);
        var enchantedSword = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 3);
        assertTrue(decompressor.decompressionInventory.isItemValid(0, enchantedSword));
        assertFalse(decompressor.decompressionInventory.isItemValid(0, new ItemStack(Items.DIAMOND_SWORD)));
        assertFalse(decompressor.decompressionInventory.isItemValid(1, enchantedSword), "output slots accept nothing");
    }

    @Test void theActiveInventoryAndPortRangesFollowTheCorePolarity() {
        var compressor = machine(false);
        assertFalse(compressor.reversed());
        assertSame(compressor.inventory, compressor.activeInventory());
        assertEquals(12, compressor.inputEnd());
        assertEquals(12, compressor.outputStart());
        var decompressor = machine(true);
        assertTrue(decompressor.reversed());
        assertSame(decompressor.decompressionInventory, decompressor.activeInventory());
        assertEquals(1, decompressor.inputEnd());
        assertEquals(1, decompressor.outputStart());
    }

    @Test void aCycleProducesOnceAndTheUncollectedProductFollowsTheInputs() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        logic.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));

        logic.runCycle();
        assertFalse(logic.inputsLocked(), "RULING 4: the material row stays editable");
        assertTrue(logic.inventory.getStackInSlot(12).is(Items.DIAMOND_SWORD));
        assertEquals(5, EnchantmentStackRules.enchantments(logic.inventory.getStackInSlot(12)).getLevel(sharpness));
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount(), "the cost is deferred, not paid on production");

        logic.runCycle();
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount(), "a pending product must block the next cycle");

        // RULING 4: a later material refreshes the uncollected product in place and still pays nothing.
        logic.inventory.setStackInSlot(1, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD), unbreaking, 2));
        var refreshed = logic.inventory.getStackInSlot(12);
        assertEquals(5, EnchantmentStackRules.enchantments(refreshed).getLevel(sharpness));
        assertEquals(2, EnchantmentStackRules.enchantments(refreshed).getLevel(unbreaking),
                "the uncollected product must follow the current materials");
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount(), "the refresh deducts nothing");

        // Removing that material refreshes back, and removing everything leaves an empty product row.
        logic.inventory.setStackInSlot(1, ItemStack.EMPTY);
        assertEquals(0, EnchantmentStackRules.enchantments(logic.inventory.getStackInSlot(12)).getLevel(unbreaking));
        logic.inventory.setStackInSlot(0, ItemStack.EMPTY);
        assertTrue(logic.inventory.getStackInSlot(12).isEmpty(), "no materials produce no product, and nothing is owed");

        // Feeding it again produces again, and the take bills whatever the product was last made from.
        logic.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));
        logic.runCycle();
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount());
        logic.inventory.extractItem(12, 1, false);
        assertEquals(1, logic.inventory.getStackInSlot(0).getCount());
        logic.runCycle();
        assertTrue(logic.inventory.getStackInSlot(12).is(Items.DIAMOND_SWORD), "collection releases the next cycle");
        assertEquals(1, logic.inventory.getStackInSlot(0).getCount());
    }

    @Test void takingAProductPaysEveryRegisteredCostExactlyOnce() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        logic.inventory.setStackInSlot(1, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 4));
        logic.inventory.setStackInSlot(2, EnchantmentTestSupport.enchanted(new ItemStack(Items.IRON_SWORD, 3), sharpness, 2));
        logic.runCycle();
        assertFalse(logic.inputsLocked(), "RULING 4: the material row stays editable");
        assertFalse(logic.inventory.getStackInSlot(12).isEmpty());

        logic.inventory.extractItem(12, 1, false);
        assertFalse(logic.inputsLocked());
        assertEquals(1, logic.inventory.getStackInSlot(1).getCount());
        assertTrue(EnchantmentStackRules.enchantments(logic.inventory.getStackInSlot(2)).isEmpty(),
                "the non-target item is stripped in place and keeps its count");
        assertEquals(3, logic.inventory.getStackInSlot(2).getCount());
    }

    @Test void everyTeardownPathVoidsTheUncollectedProductAndKeepsTheMaterial() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        logic.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));
        logic.runCycle();
        assertFalse(logic.inputsLocked(), "RULING 4: the material row stays editable");
        assertFalse(logic.inventory.getStackInSlot(12).isEmpty());

        // What structure loss, polarity switch, kind switch, wrench pick-up, break and ejectCaches call.
        logic.discardPending();
        assertTrue(logic.inventory.getStackInSlot(12).isEmpty());
        assertTrue(logic.inventory.getStackInSlot(12).isEmpty(), "the uncollected product must not exist anywhere");
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount(), "the material was never produced, so it is not charged");
        assertEquals(5, EnchantmentStackRules.enchantments(logic.inventory.getStackInSlot(0)).getLevel(sharpness),
                "and it is not stripped either");
    }

    @Test void playerOwnedLeftoversSurviveATeardownAfterAPartialTake() {
        var logic = machine(true);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD), sharpness, 5);
        EnchantmentTestSupport.enchanted(input, unbreaking, 3);
        logic.decompressionInventory.setStackInSlot(0, input);

        logic.runCycle();
        logic.decompressionInventory.extractItem(1, 1, false);
        assertFalse(logic.inputsLocked(), "the partial take settled the whole input item");
        assertEquals(0, logic.decompressionInventory.getStackInSlot(0).getCount());

        logic.discardPending();
        assertTrue(logic.decompressionInventory.getStackInSlot(2).is(Items.ENCHANTED_BOOK),
                "a settled split leaves its remaining products to the player");
        assertTrue(logic.decompressionInventory.getStackInSlot(3).is(Items.DIAMOND_SWORD));
    }

    @Test void aCapturedMachineSettlesItsOwnInventoryAcrossAKindSwitch() {
        // A kind change only discards on the core's next tick, so in that gap the world already reports the
        // other machine while the open menu still settles against the captured one.
        var ruin = new MiniatureImmortalRuinBlockEntity(BlockPos.ZERO, ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState());
        var captured = ruin.enchanter();
        captured.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 5));
        captured.runCycle();
        assertFalse(captured.locksSlot(0), "RULING 4: no machine locks its material row");
        assertFalse(captured.locksSlot(12), "the product slot itself stays takeable");

        assertSame(ruin.compressor(), ruin.machine(), "without a world the controller already reports the other family");
        captured.settlePending();
        assertEquals(1, captured.inventory.getStackInSlot(0).getCount(),
                "the captured machine must bill its own inventory, not the one the world reports now");
    }

    @Test void aBookIsNeverTheCarrierSoTheLowestNonBookItemReceivesTheMerge() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        logic.inventory.setStackInSlot(0, new ItemStack(Items.BOOK, 3));
        logic.inventory.setStackInSlot(1, EnchantmentTestSupport.enchanted(new ItemStack(Items.GOLDEN_SWORD, 2), sharpness, 2));

        logic.runCycle();
        assertTrue(logic.inventory.getStackInSlot(12).is(Items.GOLDEN_SWORD),
                "the book is material: the sword one slot later is the received carrier");
        assertTrue(logic.inventory.extractItem(12, 1, false).is(Items.GOLDEN_SWORD));
        assertEquals(0, logic.inventory.getStackInSlot(0).getCount(), "the whole book stack is material");
        assertEquals(1, logic.inventory.getStackInSlot(1).getCount());
    }

    @Test void booksOnlyProduceOneMergedBook() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var protection = EnchantmentTestSupport.enchantment("minecraft:protection");
        logic.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 4));
        logic.inventory.setStackInSlot(3, EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), protection, 1));
        logic.inventory.setStackInSlot(5, new ItemStack(Items.BOOK, 2));

        logic.runCycle();
        var produced = logic.inventory.getStackInSlot(12);
        assertTrue(produced.is(Items.ENCHANTED_BOOK), "books alone merge into one enchanted book");
        assertEquals(1, produced.getCount());
        assertEquals(4, EnchantmentStackRules.enchantments(produced).getLevel(sharpness));
        assertEquals(1, EnchantmentStackRules.enchantments(produced).getLevel(protection));
        logic.inventory.extractItem(12, 1, false);
        assertEquals(0, logic.inventory.getStackInSlot(0).getCount());
        assertEquals(0, logic.inventory.getStackInSlot(3).getCount());
        assertEquals(0, logic.inventory.getStackInSlot(5).getCount(), "the whole paper book stack is material too");
    }

    @Test void splittingABookDoesNotReturnTheSpentBook() {
        var logic = machine(true);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 5);
        EnchantmentTestSupport.enchanted(input, unbreaking, 3);
        logic.decompressionInventory.setStackInSlot(0, input);

        logic.runCycle();
        assertFalse(logic.inputsLocked(), "RULING 4: the carrier slot stays editable");
        assertEquals("minecraft:sharpness", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(1)));
        assertEquals("minecraft:unbreaking", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(2)));
        assertTrue(logic.decompressionInventory.getStackInSlot(3).isEmpty(), "the spent book is consumed, never returned");

        logic.decompressionInventory.extractItem(1, 1, false);
        assertFalse(logic.inputsLocked());
        assertTrue(logic.decompressionInventory.getStackInSlot(0).isEmpty(), "the book is spent by the split");
    }

    /**
     * RULING 5: the menu's shift-click takes the product out first (it clears the source slot) and only then
     * calls onTake, so the take path must never write the product back. Rewriting it here duplicated the product.
     */
    @Test void aShiftClickStyleTakeNeverPutsTheProductBack() {
        var logic = machine(false);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        logic.inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2), sharpness, 5));
        logic.runCycle();
        assertFalse(logic.inventory.getStackInSlot(12).isEmpty());

        // Exactly what SpiritCompressorMenu.quickMoveStack + Slot#onTake do for the product row.
        ItemStack moved = logic.inventory.getStackInSlot(12).copy();
        logic.inventory.setStackInSlot(12, ItemStack.EMPTY);
        logic.settlePending();

        assertTrue(logic.inventory.getStackInSlot(12).isEmpty(), "a shift-click take must not re-create the product");
        assertEquals(1, logic.inventory.getStackInSlot(0).getCount(), "and it still pays the material cost once");
        assertTrue(logic.inventory.extractItem(12, 1, false).isEmpty(), "nothing is left to take twice");
        assertTrue(!moved.isEmpty());
    }

    @Test void aNumberKeySwapStyleTakeAlsoPaysOnceAndLeavesNoCopy() {
        var logic = machine(true);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD), sharpness, 5);
        EnchantmentTestSupport.enchanted(input, unbreaking, 3);
        logic.decompressionInventory.setStackInSlot(0, input);
        logic.runCycle();

        // Slot#setByPlayer + onTake on the first book of the split.
        ItemStack book = logic.decompressionInventory.getStackInSlot(1).copy();
        logic.decompressionInventory.setStackInSlot(1, ItemStack.EMPTY);
        logic.settlePending();

        assertTrue(logic.decompressionInventory.getStackInSlot(1).isEmpty(), "the taken book must not come back");
        assertTrue(logic.decompressionInventory.getStackInSlot(0).isEmpty(), "the carrier is spent exactly once");
        assertTrue(logic.decompressionInventory.getStackInSlot(2).is(Items.ENCHANTED_BOOK),
                "the other split books stay for the player");
        assertTrue(!book.isEmpty());
    }

    @Test void swappingTheDecompressorCarrierRefreshesTheWholeOutputRow() {
        var logic = machine(true);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        logic.decompressionInventory.setStackInSlot(0,
                EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), sharpness, 5));
        logic.runCycle();
        assertEquals("minecraft:sharpness", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(1)));

        logic.decompressionInventory.setStackInSlot(0,
                EnchantmentTestSupport.enchanted(new ItemStack(Items.ENCHANTED_BOOK), unbreaking, 3));
        assertEquals("minecraft:unbreaking", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(1)),
                "RULING 4: the split follows the current carrier");
        assertTrue(logic.decompressionInventory.getStackInSlot(2).isEmpty(), "the stale second book is gone");
        assertEquals(1, logic.decompressionInventory.getStackInSlot(0).getCount(), "the refresh still deducts nothing");

        logic.decompressionInventory.extractItem(1, 1, false);
        assertTrue(logic.decompressionInventory.getStackInSlot(0).isEmpty(), "the take spends the swapped-in carrier");
    }

    /** Item components holding enchantment holders need the datapack registry, so this proof uses plain stacks. */
    @Test void aPendingTransactionSurvivesASaveAndLoadCycle() {
        var logic = machine(false);
        logic.inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD, 2));
        logic.inventory.setStackInSlot(5, new ItemStack(Items.IRON_SWORD, 3));
        logic.inventory.setStackInSlot(12, new ItemStack(Items.BOOK));
        int[] consume = new int[13];
        consume[0] = 1;
        boolean[] strip = new boolean[13];
        strip[5] = true;

        CompoundTag armed = new CompoundTag();
        ItemStackHandler stored = new ItemStackHandler(13);
        stored.setStackInSlot(0, new ItemStack(Items.DIAMOND_SWORD, 2));
        stored.setStackInSlot(5, new ItemStack(Items.IRON_SWORD, 3));
        stored.setStackInSlot(12, new ItemStack(Items.BOOK));
        armed.put("EnchantMachineInventory", com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(stored, registries));
        armed.putInt("EnchantMachineOutputCount", 1);
        armed.putIntArray("EnchantMachinePendingConsume", consume);
        armed.putByteArray("EnchantMachinePendingStrip", pack(strip));
        logic.load(armed, registries);
        assertEquals(2, logic.inventory.getStackInSlot(0).getCount());
        assertTrue(logic.inventory.getStackInSlot(12).is(Items.BOOK), "a loaded pending product is restored as it was");
        assertFalse(logic.inputsLocked(), "RULING 4: a restored record never locks the material row");

        CompoundTag resaved = new CompoundTag();
        logic.save(resaved, registries);
        assertTrue(resaved.contains("EnchantMachinePendingConsume"), "the save path must write the pending cost out");
        assertTrue(resaved.contains("EnchantMachinePendingStrip"));

        var reloaded = machine(false);
        reloaded.load(resaved, registries);
        assertEquals(2, reloaded.inventory.getStackInSlot(0).getCount());
        assertEquals(3, reloaded.inventory.getStackInSlot(5).getCount());
        assertTrue(reloaded.inventory.getStackInSlot(12).is(Items.BOOK));

        reloaded.inventory.extractItem(12, 1, false);
        assertEquals(1, reloaded.inventory.getStackInSlot(0).getCount(), "the take pays the material cost recorded before the reload");
        assertEquals(3, reloaded.inventory.getStackInSlot(5).getCount());
    }

    private static byte[] pack(boolean[] values) {
        byte[] packed = new byte[values.length];
        for (int i = 0; i < values.length; i++) packed[i] = (byte) (values[i] ? 1 : 0);
        return packed;
    }

    @Test void theDecompressorSplitsOneItemAndBillsItWhenAProductIsTaken() {
        var logic = machine(true);
        var sharpness = EnchantmentTestSupport.enchantment("minecraft:sharpness");
        var unbreaking = EnchantmentTestSupport.enchantment("minecraft:unbreaking");
        var input = EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 3), sharpness, 5);
        EnchantmentTestSupport.enchanted(input, unbreaking, 3);
        logic.decompressionInventory.setStackInSlot(0, input);

        logic.runCycle();
        assertFalse(logic.inputsLocked(), "RULING 4: the carrier slot stays editable");
        assertEquals("minecraft:sharpness", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(1)));
        assertEquals("minecraft:unbreaking", EnchantmentTestSupport.singleName(logic.decompressionInventory.getStackInSlot(2)));
        assertTrue(logic.decompressionInventory.getStackInSlot(3).is(Items.DIAMOND_SWORD));
        assertEquals(3, logic.decompressionInventory.getStackInSlot(0).getCount());

        logic.decompressionInventory.extractItem(1, 1, false);
        assertFalse(logic.inputsLocked());
        assertEquals(2, logic.decompressionInventory.getStackInSlot(0).getCount(), "one input item is spent for the whole split");
    }

    @Test void aBlockedOutputSlotStopsTheSplitAndRegistersNothing() {
        var logic = machine(true);
        logic.decompressionInventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 1));
        logic.decompressionInventory.setStackInSlot(2, new ItemStack(Items.DIRT));

        logic.runCycle();
        assertFalse(logic.inputsLocked());
        assertEquals(1, logic.decompressionInventory.getStackInSlot(0).getCount());
        assertTrue(logic.decompressionInventory.getStackInSlot(1).isEmpty(), "nothing is placed when one slot cannot take its result");
    }

    @Test void facesCycleThroughFiveStatesAndXianqiaoToggles() {
        var logic = machine(false);
        for (int side = 0; side < 6; side++) assertEquals(RuinMachine.CLOSED, logic.mode(side));
        for (int state = 1; state <= 4; state++) {
            logic.cycle(2);
            assertEquals(state, logic.mode(2));
        }
        logic.cycle(2);
        assertEquals(RuinMachine.CLOSED, logic.mode(2));
        assertFalse(logic.xianqiaoOutput());
        logic.toggleXianqiao();
        assertTrue(logic.xianqiaoOutput());
    }

    @Test void theTitleKeyFollowsKindAndPolarity() {
        assertEquals("container.immortalstorage.enchantment_compressor", machine(false).titleKey());
        assertEquals("container.immortalstorage.enchantment_decompressor", machine(true).titleKey());
    }
}
