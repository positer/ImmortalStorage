package com.immortalstorage.immortalstorage.gametest;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.RuinMachine;
import com.immortalstorage.immortalstorage.compressor.EnchantmentStackRules;
import com.immortalstorage.immortalstorage.enchantment.ModEnchantments;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 26.1.2 GameTest behaviour acceptance for the 0.1.7 enchantment compressor and decompressor.
 *
 * <p>This file is a HAND-WRITTEN TARGET ARTEFACT, not a generated one. Minecraft 26.1 replaced the
 * annotation-based game test framework: {@code net.minecraft.gametest.framework.GameTest} does not
 * exist any more and NeoForge ships neither {@code GameTestHolder} nor {@code PrefixGameTestTemplate},
 * so the canonical 1.21.1 suite cannot be transformed. Registration now happens on the mod bus through
 * {@link RegisterGameTestsEvent}, which carries a writable {@code Registry<TestEnvironmentDefinition<?>>}
 * and a writable {@code Registry<GameTestInstance>}. The seven semantics below are the SAME seven the
 * 1.21.1 suite pins; nothing here weakens what that suite asserts.
 *
 * <p>Test ids are {@code immortalstorage:<name>} so a failure is reported against the name in
 * {@link #register}. The template is {@code immortalstorage:empty}, the 1x1x1 all-air structure the
 * canonical line uses (data/immortalstorage/structure/empty.nbt).
 *
 * <p>Unlike the canonical suite this file is never rewritten by generate-compat-source.ps1, so every
 * identifier below must already be the 26.1.2 spelling (Identifier, Blocks.IRON_CHAIN, and so on).
 */
public final class EnchantmentMachineGameTest {
    private static final int CORE_X = 1, CORE_Y = 1, CORE_Z = 1;
    private static final BlockPos CORE = new BlockPos(CORE_X, CORE_Y, CORE_Z);
    private static final int COMPRESSOR_OUTPUT = 12;
    private static final int DECOMPRESSOR_OUTPUT = 1;
    private static final int ENCHANTER_COMPRESSION_INVENTORY = 2;
    private static final int ENCHANTER_DECOMPRESSION_INVENTORY = 3;

    /** Mod-bus entry point; wired from the mod constructor by an explicit generation rule. */
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(identifier("empty_environment"));
        registerTest(event, environment, "enchantment_shell_forms_and_titles_itself", 400,
                EnchantmentMachineGameTest::enchantmentShellFormsAndTitlesItself);
        registerTest(event, environment, "treasure_basin_shell_stays_on_the_spirit_path", 400,
                EnchantmentMachineGameTest::treasureBasinShellStaysOnTheSpiritPath);
        registerTest(event, environment, "compressor_defers_the_cost_until_the_product_is_taken", 600,
                EnchantmentMachineGameTest::compressorDefersTheCostUntilTheProductIsTaken);
        registerTest(event, environment, "compressor_carries_onto_the_lowest_non_book_item", 600,
                EnchantmentMachineGameTest::compressorCarriesOntoTheLowestNonBookItem);
        registerTest(event, environment, "immortal_equipment_is_recognised_as_enchantable", 200,
                EnchantmentMachineGameTest::theImmortalEquipmentIsRecognisedAsEnchantable);
        registerTest(event, environment, "spirit_repair_is_offered_by_a_level_thirty_table", 200,
                EnchantmentMachineGameTest::spiritRepairIsOfferedByALevelThirtyTable);
        registerTest(event, environment, "compressor_shift_click_take_does_not_duplicate_the_product", 600,
                EnchantmentMachineGameTest::compressorShiftClickTakeDoesNotDuplicateTheProduct);
        registerTest(event, environment, "decompressor_splits_a_book_and_does_not_return_the_spent_carrier", 600,
                EnchantmentMachineGameTest::decompressorSplitsABookAndDoesNotReturnTheSpentCarrier);
        registerTest(event, environment, "decompressor_returns_the_item_carrier", 600,
                EnchantmentMachineGameTest::decompressorReturnsTheItemCarrier);
        registerTest(event, environment, "teardown_voids_the_uncollected_product_and_keeps_the_material", 600,
                EnchantmentMachineGameTest::teardownVoidsTheUncollectedProductAndKeepsTheMaterial);
    }

    private static void registerTest(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String name, int maxTicks, Consumer<GameTestHelper> body) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, identifier("empty"), maxTicks, 1, true);
        event.registerTest(identifier(name), new ScriptedTest(data, body));
    }

    private static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, path);
    }

    /**
     * A code-only test instance: the vanilla framework also allows data-driven instances, but these
     * seven are Java bodies, so the instance carries the body and a placeholder codec (a programmatic
     * registry entry is never decoded from or encoded to a data pack).
     */
    private static final class ScriptedTest extends GameTestInstance {
        private final Consumer<GameTestHelper> body;

        ScriptedTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, Consumer<GameTestHelper> body) {
            super(data);
            this.body = body;
        }

        @Override
        public void run(GameTestHelper helper) {
            body.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return MapCodec.unit(this);
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("immortalstorage scripted game test");
        }
    }

    /** The shared 3x3x3 shell; only the top centre and the core polarity differ between the families. */
    private static void buildShell(GameTestHelper helper, boolean reversed, boolean enchanting) {
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 2; x++) {
                for (int z = 0; z <= 2; z++) {
                    boolean centre = x == CORE_X && z == CORE_Z;
                    boolean edge = x == CORE_X || z == CORE_Z;
                    if (y == CORE_Y) {
                        if (centre) {
                            helper.setBlock(x, y, z, ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState()
                                    .setValue(MiniatureImmortalRuinBlock.REVERSED, reversed));
                        } else {
                            helper.setBlock(x, y, z, edge ? Blocks.AIR : Blocks.IRON_CHAIN);
                        }
                    } else if (centre) {
                        if (y < CORE_Y) helper.setBlock(x, y, z, ModBlocks.WORLD_SHARD_MINER.get());
                        else helper.setBlock(x, y, z, enchanting ? Blocks.ENCHANTING_TABLE : ModBlocks.TREASURE_BASIN.get());
                    } else {
                        helper.setBlock(x, y, z, edge ? Blocks.IRON_CHAIN : ModBlocks.WORLD_BARRIER.get());
                    }
                }
            }
        }
    }

    private static Holder<Enchantment> holder(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, Map<ResourceKey<Enchantment>, Integer> levels) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        levels.forEach((key, level) -> mutable.set(holder(helper, key), level));
        EnchantmentStackRules.write(stack, mutable.toImmutable());
        return stack;
    }

    private static ItemStack enchantedBook(GameTestHelper helper, Map<ResourceKey<Enchantment>, Integer> levels) {
        return enchanted(helper, new ItemStack(Items.ENCHANTED_BOOK), levels);
    }

    private static RuinMachine machine(GameTestHelper helper) {
        MiniatureImmortalRuinBlockEntity ruin = helper.getBlockEntity(CORE, MiniatureImmortalRuinBlockEntity.class);
        helper.assertTrue(ruin != null, "the miniature ruin core must exist in the shell");
        return ruin.machine();
    }

    private static ItemStackHandler enchanterInventory(GameTestHelper helper, int index) {
        MiniatureImmortalRuinBlockEntity ruin = helper.getBlockEntity(CORE, MiniatureImmortalRuinBlockEntity.class);
        helper.assertTrue(ruin != null, "the miniature ruin core must exist in the shell");
        return ruin.machineInventories().get(index);
    }

    /** The shell forms, and the family is decided by the block above the core. */
    public static void enchantmentShellFormsAndTitlesItself(GameTestHelper helper) {
        buildShell(helper, false, true);
        helper.succeedWhen(() -> {
            RuinMachine machine = machine(helper);
            helper.assertTrue(machine.formed(), "the enchanting-table shell must form the enchantment machine");
            helper.assertTrue("container.immortalstorage.enchantment_compressor".equals(machine.titleKey()),
                    "forming the compressor polarity must use the enchantment compressor title");
        });
    }

    /** The spirit family is untouched: a treasure basin still forms the 0.1.6 spirit compressor. */
    public static void treasureBasinShellStaysOnTheSpiritPath(GameTestHelper helper) {
        buildShell(helper, false, false);
        helper.succeedWhen(() -> {
            RuinMachine machine = machine(helper);
            helper.assertTrue(machine.formed(), "the treasure-basin shell must still form the spirit machine");
            helper.assertTrue("container.immortalstorage.spirit_compressor".equals(machine.titleKey()),
                    "the spirit path keeps its own title");
        });
    }

    /**
     * Rulings 1+2 for the compressor: two enchanted books merge into one book on the carrier rule, the
     * product is produced BEFORE the books are consumed, and the real output extraction pays the cost.
     */
    public static void compressorDefersTheCostUntilTheProductIsTaken(GameTestHelper helper) {
        buildShell(helper, false, true);
        Map<ResourceKey<Enchantment>, Integer> first = new LinkedHashMap<>();
        first.put(Enchantments.SHARPNESS, 3);
        Map<ResourceKey<Enchantment>, Integer> second = new LinkedHashMap<>();
        second.put(Enchantments.SHARPNESS, 1);
        second.put(Enchantments.UNBREAKING, 2);
        ItemStackHandler inputs = machine(helper).activeInventory();
        inputs.insertItem(0, enchantedBook(helper, first), false);
        inputs.insertItem(1, enchantedBook(helper, second), false);

        helper.runAtTickTime(80, () -> {
            RuinMachine machine = machine(helper);
            ItemStackHandler inventory = machine.activeInventory();
            ItemStack product = inventory.getStackInSlot(COMPRESSOR_OUTPUT);
            helper.assertTrue(product.is(Items.ENCHANTED_BOOK), "two books must merge into one enchanted book");
            helper.assertTrue(EnchantmentStackRules.enchantments(product).getLevel(holder(helper, Enchantments.SHARPNESS)) == 3,
                    "a repeated enchantment keeps its highest level");
            helper.assertTrue(EnchantmentStackRules.enchantments(product).getLevel(holder(helper, Enchantments.UNBREAKING)) == 2,
                    "the merged set keeps the other book's enchantment");
            helper.assertTrue(inventory.getStackInSlot(0).getCount() == 1 && inventory.getStackInSlot(1).getCount() == 1,
                    "RULING 2: the material is still in the input slots while the product is uncollected");
            helper.assertTrue(!machine.inputsLocked(), "RULING 4: the material row stays editable");

            // RULING 4: a material added while the product is uncollected refreshes it in place.
            Map<ResourceKey<Enchantment>, Integer> third = new LinkedHashMap<>();
            third.put(Enchantments.UNBREAKING, 5);
            inventory.insertItem(2, enchantedBook(helper, third), false);
            ItemStack refreshed = inventory.getStackInSlot(COMPRESSOR_OUTPUT);
            helper.assertTrue(EnchantmentStackRules.enchantments(refreshed).getLevel(holder(helper, Enchantments.UNBREAKING)) == 5,
                    "RULING 4: a later material raises the uncollected product's level");

            ItemStack taken = inventory.extractItem(COMPRESSOR_OUTPUT, 1, false);
            helper.assertTrue(!taken.isEmpty(), "the product must be takeable");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()
                            && inventory.getStackInSlot(2).isEmpty(),
                    "taking the product pays the deferred material cost exactly once, for the refreshed inputs");
            helper.assertTrue(!machine.inputsLocked(), "no transaction may survive the take");
            helper.succeed();
        });
    }

    /**
     * RULING 5: a shift-click take moves the product out of its slot first and only then calls onTake, so the
     * take path must never rewrite the product. Rewriting it there duplicated the product on every shift-click.
     */
    public static void compressorShiftClickTakeDoesNotDuplicateTheProduct(GameTestHelper helper) {
        buildShell(helper, false, true);
        Map<ResourceKey<Enchantment>, Integer> book = new LinkedHashMap<>();
        book.put(Enchantments.SHARPNESS, 3);
        ItemStackHandler inputs = machine(helper).activeInventory();
        inputs.insertItem(0, enchantedBook(helper, book), false);

        helper.runAtTickTime(80, () -> {
            RuinMachine machine = machine(helper);
            ItemStackHandler inventory = machine.activeInventory();
            ItemStack product = inventory.getStackInSlot(COMPRESSOR_OUTPUT);
            helper.assertTrue(!product.isEmpty(), "one book must produce one product");

            // Exactly what SpiritCompressorMenu.quickMoveStack + Slot#onTake do: move out, clear the slot, settle.
            ItemStack taken = product.copy();
            inventory.setStackInSlot(COMPRESSOR_OUTPUT, ItemStack.EMPTY);
            machine.settlePending();

            helper.assertTrue(inventory.getStackInSlot(COMPRESSOR_OUTPUT).isEmpty(),
                    "RULING 5: a shift-click take must not put the product back");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty(),
                    "RULING 5: and it still pays the material cost once");
            helper.assertTrue(!taken.isEmpty(), "the player keeps the taken product");
            helper.succeed();
        });
    }

    /**
     * User ruling B: the mod's own enchantment is an ordinary table enchantment now. The third slot of a
     * thirty-level table (fifteen bookshelves) always costs 60, the candidate list comes from
     * #minecraft:in_enchanting_table, and it may land on anything durability-based, this mod's gear included.
     */
    public static void spiritRepairIsOfferedByALevelThirtyTable(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> repair = enchantments.getOrThrow(ModEnchantments.SPIRIT_REPAIR);
        helper.assertTrue(!repair.is(EnchantmentTags.TREASURE), "spirit_repair must not be treasure any more");

        // The table's own candidate pool is #minecraft:in_enchanting_table, not the whole registry.
        var tablePool = enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE);
        helper.assertTrue(tablePool.stream().anyMatch(holder -> holder.is(ModEnchantments.SPIRIT_REPAIR)),
                "spirit_repair must be part of the table's candidate pool");
        for (ItemStack stack : List.of(new ItemStack(ModItems.SPIRIT_STAFF.get()),
                new ItemStack(ModItems.IMMORTAL_ARTIFACT.get()), new ItemStack(ModItems.SPIRIT_SWORD.get()),
                new ItemStack(Items.DIAMOND_SWORD))) {
            // Third slot with fifteen bookshelves: max(j, 15 * 2), so the cost is 30 or 31.
            int cost = EnchantmentHelper.getEnchantmentCost(net.minecraft.util.RandomSource.create(), 2, 15, stack);
            helper.assertTrue(cost >= 30, "a 30-level table's third slot is at least 30, got " + cost);
            // The offer then needs the item to accept it and the cost to sit inside its window.
            helper.assertTrue(stack.isPrimaryItemFor(repair), stack + " must be allowed to receive spirit_repair");
            helper.assertTrue(repair.value().getMinCost(1) <= cost && cost <= repair.value().getMaxCost(1),
                    stack + " cost " + cost + " must sit inside spirit_repair's cost window");
        }
        helper.succeed();
    }

    /**
     * The equipment is registered as a genuinely enchantable item, so the anvil and other mods that inspect
     * enchantment support recognise it. This is the item-side contract the enchantment compressor relies on; it is
     * deliberately not a compressor-private whitelist.
     */
    public static void theImmortalEquipmentIsRecognisedAsEnchantable(GameTestHelper helper) {
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> mending = enchantments.getOrThrow(Enchantments.MENDING);
        Holder<Enchantment> repair = enchantments.getOrThrow(ModEnchantments.SPIRIT_REPAIR);

        for (ItemStack stack : immortalEquipment()) {
            helper.assertTrue(stack.isEnchantable(), stack + " must be an enchantable item");
            helper.assertTrue(stack.supportsEnchantment(mending),
                    stack + " must accept a durability enchantment like the anvil expects");
            helper.assertTrue(stack.supportsEnchantment(repair), stack + " must accept the mod's own enchantment");
        }
        helper.assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(ModItems.SPIRIT_STAFF.get())),
                "the enchantment compressor must accept the spirit instrument without a private whitelist");
        helper.assertTrue(EnchantmentStackRules.acceptsCarrier(new ItemStack(ModItems.IMMORTAL_ARTIFACT.get())),
                "the enchantment compressor must accept the immortal artifact without a private whitelist");
        helper.succeed();
    }

    private static List<ItemStack> immortalEquipment() {
        return List.of(new ItemStack(ModItems.SPIRIT_STAFF.get()), new ItemStack(ModItems.IMMORTAL_ARTIFACT.get()),
                new ItemStack(ModItems.SPIRIT_SWORD.get()),
                new ItemStack(ModItems.IMMORTAL_RUIN_FORGED_SPIRIT_SWORD.get()),
                new ItemStack(ModItems.ONE_QI_RETURNING_ORIGIN_SWORD.get()));
    }

    /**
     * Ruling 1 for the compressor: books are materials only, so with an item present the lowest-index
     * NON-book item receives the merged set and the other item is disenchanted where it stands.
     */
    public static void compressorCarriesOntoTheLowestNonBookItem(GameTestHelper helper) {
        buildShell(helper, false, true);
        Map<ResourceKey<Enchantment>, Integer> book = new LinkedHashMap<>();
        book.put(Enchantments.MENDING, 1);
        Map<ResourceKey<Enchantment>, Integer> donor = new LinkedHashMap<>();
        donor.put(Enchantments.KNOCKBACK, 2);
        ItemStackHandler inputs = machine(helper).activeInventory();
        inputs.insertItem(0, enchantedBook(helper, book), false);
        inputs.insertItem(1, enchanted(helper, new ItemStack(Items.IRON_SWORD), donor), false);

        helper.runAtTickTime(80, () -> {
            ItemStackHandler inventory = machine(helper).activeInventory();
            ItemStack product = inventory.getStackInSlot(COMPRESSOR_OUTPUT);
            helper.assertTrue(product.is(Items.IRON_SWORD),
                    "RULING 1: the carrier is the lowest non-book item, never the book");
            helper.assertTrue(EnchantmentStackRules.enchantments(product).getLevel(holder(helper, Enchantments.MENDING)) == 1
                            && EnchantmentStackRules.enchantments(product).getLevel(holder(helper, Enchantments.KNOCKBACK)) == 2,
                    "the carrier receives every merged enchantment");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty() == false, "the book stays until the take");
            helper.succeed();
        });
    }

    /** Ruling 3: a spent book carrier is not returned; the split fills one slot per enchantment. */
    public static void decompressorSplitsABookAndDoesNotReturnTheSpentCarrier(GameTestHelper helper) {
        buildShell(helper, true, true);
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(Enchantments.EFFICIENCY, 4);
        levels.put(Enchantments.SILK_TOUCH, 1);
        levels.put(Enchantments.FORTUNE, 3);
        machine(helper).activeInventory().insertItem(0, enchantedBook(helper, levels), false);

        helper.runAtTickTime(80, () -> {
            ItemStackHandler inventory = machine(helper).activeInventory();
            for (int slot = DECOMPRESSOR_OUTPUT; slot < DECOMPRESSOR_OUTPUT + 3; slot++) {
                helper.assertTrue(inventory.getStackInSlot(slot).is(Items.ENCHANTED_BOOK),
                        "every split enchantment becomes its own book (slot " + slot + ")");
                helper.assertTrue(EnchantmentStackRules.enchantments(inventory.getStackInSlot(slot)).keySet().size() == 1,
                        "a split book carries exactly one enchantment");
            }
            for (int slot = DECOMPRESSOR_OUTPUT + 3; slot <= 12; slot++) {
                helper.assertTrue(inventory.getStackInSlot(slot).isEmpty(),
                        "RULING 3: the spent book carrier is NOT returned (slot " + slot + ")");
            }
            inventory.extractItem(DECOMPRESSOR_OUTPUT, 1, false);
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty(), "taking a split book pays the single input item");
            helper.succeed();
        });
    }

    /** Ruling 3 for the item carrier: an item is always handed back, holding whatever did not fit. */
    public static void decompressorReturnsTheItemCarrier(GameTestHelper helper) {
        buildShell(helper, true, true);
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(Enchantments.PROTECTION, 4);
        levels.put(Enchantments.THORNS, 3);
        levels.put(Enchantments.UNBREAKING, 3);
        machine(helper).activeInventory().insertItem(0,
                enchanted(helper, new ItemStack(Items.DIAMOND_CHESTPLATE), levels), false);

        helper.runAtTickTime(80, () -> {
            ItemStackHandler inventory = machine(helper).activeInventory();
            for (int slot = DECOMPRESSOR_OUTPUT; slot < DECOMPRESSOR_OUTPUT + 3; slot++) {
                helper.assertTrue(inventory.getStackInSlot(slot).is(Items.ENCHANTED_BOOK), "three split books");
            }
            ItemStack carrier = inventory.getStackInSlot(DECOMPRESSOR_OUTPUT + 3);
            helper.assertTrue(carrier.is(Items.DIAMOND_CHESTPLATE), "the item carrier is always returned");
            helper.assertTrue(EnchantmentStackRules.enchantments(carrier).isEmpty(),
                    "a fully split item comes back with no enchantment left");
            helper.succeed();
        });
    }

    /** Ruling 3 teardown: losing the structure voids the product and keeps the material in the machine. */
    public static void teardownVoidsTheUncollectedProductAndKeepsTheMaterial(GameTestHelper helper) {
        buildShell(helper, false, true);
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(Enchantments.SHARPNESS, 2);
        machine(helper).activeInventory().insertItem(0, enchantedBook(helper, levels), false);

        helper.runAtTickTime(80, () -> {
            helper.assertTrue(!machine(helper).activeInventory().getStackInSlot(COMPRESSOR_OUTPUT).isEmpty(),
                    "a product must exist before the teardown");
            helper.setBlock(CORE_X, CORE_Y + 1, CORE_Z, Blocks.AIR);
        });
        helper.runAtTickTime(140, () -> {
            ItemStackHandler inventory = enchanterInventory(helper, ENCHANTER_COMPRESSION_INVENTORY);
            helper.assertTrue(inventory.getStackInSlot(COMPRESSOR_OUTPUT).isEmpty(),
                    "RULING 3 teardown: the uncollected product is voided, not kept");
            helper.assertTrue(inventory.getStackInSlot(0).getCount() == 1,
                    "RULING 3 teardown: the material stays in the machine and is never charged");
            ItemStackHandler decompression = enchanterInventory(helper, ENCHANTER_DECOMPRESSION_INVENTORY);
            for (int slot = 0; slot < decompression.getSlots(); slot++) {
                helper.assertTrue(decompression.getStackInSlot(slot).isEmpty(), "no stock is stranded in the other cache");
            }
            helper.succeed();
        });
    }

    private EnchantmentMachineGameTest() {}
}
