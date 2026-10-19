package com.immortalstorage.immortalstorage.gametest;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.RuinMachine;
import com.immortalstorage.immortalstorage.compressor.EnchantmentStackRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-game behaviour verification for the 0.1.7 enchantment compressor and decompressor, executed on a real
 * dedicated server through `gradlew gameTestServer`. Every assertion below runs against the live block
 * entity tick loop: the shell is placed in the world, the machine produces on its own ticks, and the
 * deferred material cost is settled by a real output-slot extraction.
 *
 * <p>The three user rulings recorded in tasks/0.1.7-enchantment-machines-design.md are pinned here:
 * (1) the carrier is the lowest-index NON-book item, (2) a spent book carrier is not returned by the
 * decompressor, and (3) a teardown voids an uncollected product while the material stays in the machine.
 */
@GameTestHolder(ImmortalStorageMod.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentMachineGameTest {
    private static final int CORE_X = 1, CORE_Y = 1, CORE_Z = 1;
    private static final BlockPos CORE = new BlockPos(CORE_X, CORE_Y, CORE_Z);
    private static final int COMPRESSOR_OUTPUT = 12;
    private static final int DECOMPRESSOR_OUTPUT = 1;
    private static final int ENCHANTER_COMPRESSION_INVENTORY = 2;
    private static final int ENCHANTER_DECOMPRESSION_INVENTORY = 3;

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
                            helper.setBlock(x, y, z, edge ? Blocks.AIR : Blocks.CHAIN);
                        }
                    } else if (centre) {
                        if (y < CORE_Y) helper.setBlock(x, y, z, ModBlocks.WORLD_SHARD_MINER.get());
                        else helper.setBlock(x, y, z, enchanting ? Blocks.ENCHANTING_TABLE : ModBlocks.TREASURE_BASIN.get());
                    } else {
                        helper.setBlock(x, y, z, edge ? Blocks.CHAIN : ModBlocks.WORLD_BARRIER.get());
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
        MiniatureImmortalRuinBlockEntity ruin = helper.getBlockEntity(CORE);
        helper.assertTrue(ruin != null, "the miniature ruin core must exist in the shell");
        return ruin.machine();
    }

    private static ItemStackHandler enchanterInventory(GameTestHelper helper, int index) {
        MiniatureImmortalRuinBlockEntity ruin = helper.getBlockEntity(CORE);
        helper.assertTrue(ruin != null, "the miniature ruin core must exist in the shell");
        return ruin.machineInventories().get(index);
    }

    /** The shell forms, and the family is decided by the block above the core. */
    @GameTest(template = "empty", timeoutTicks = 400)
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
    @GameTest(template = "empty", timeoutTicks = 400)
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
    @GameTest(template = "empty", timeoutTicks = 600)
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
            helper.assertTrue(machine.inputsLocked(), "an uncollected product locks the input slots");

            ItemStack taken = inventory.extractItem(COMPRESSOR_OUTPUT, 1, false);
            helper.assertTrue(!taken.isEmpty(), "the product must be takeable");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty(),
                    "taking the product pays the deferred material cost exactly once");
            helper.assertTrue(!machine.inputsLocked(), "no transaction may survive the take");
            helper.succeed();
        });
    }

    /**
     * Ruling 1 for the compressor: books are materials only, so with an item present the lowest-index
     * NON-book item receives the merged set and the other item is disenchanted where it stands.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
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
    @GameTest(template = "empty", timeoutTicks = 600)
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
    @GameTest(template = "empty", timeoutTicks = 600)
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
    @GameTest(template = "empty", timeoutTicks = 600)
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
