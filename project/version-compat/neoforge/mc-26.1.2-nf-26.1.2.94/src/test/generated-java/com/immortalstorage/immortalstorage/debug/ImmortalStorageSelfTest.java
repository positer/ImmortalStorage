package com.immortalstorage.immortalstorage.debug;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageItemHandler;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

/** Test-source regression helper; never packaged in the production mod JAR. */
public final class ImmortalStorageSelfTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    public static Result run() {
        List<String> failures = new ArrayList<>();
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();

        int[][] stageExpectations = {
                {0, 0, 1, 0, 0, 0, 1},
                {1, 9, 1, 64, 0, 120, 1},
                {2, 18, 2, 128, 0, 360, 1},
                {3, 36, 4, 256, 0, 1440, 1},
                {4, 54, 8, 512, 0, 2560, 1},
                {5, 72, 16, 1024, 0, 10420, 1},
                {6, 0, 1, 0, 64, 0, 1},
                {7, 0, 1, 0, 256, 0, 3},
                {8, 0, 1, 0, 1024, 0, 9},
                {9, 0, 1, 0, -1, 0, 32},
                {10, 0, 1, 0, Integer.MAX_VALUE, 0, 1024}
        };
        for (int[] expected : stageExpectations) {
            int stage = expected[0];
            data.setStage(stage);
            expect(failures, data.getStage() == stage, "stage " + stage + " set");
            expect(failures, data.getKongqiaoMaxSlots() == expected[1], "stage " + stage + " kongqiao slots");
            expect(failures, data.getKongqiaoStackMultiplier() == expected[2], "stage " + stage + " stack multiplier");
            expect(failures, data.getTrueYuanCap() == expected[3], "stage " + stage + " true yuan cap");
            expect(failures, data.getImmortalYuanCap() == expected[4], "stage " + stage + " immortal yuan cap");
            expect(failures, data.getLingqiCap() == expected[5], "stage " + stage + " lingqi cap");
            expect(failures, data.getRealmRadiusChunks() == expected[6], "stage " + stage + " realm radius");
        }

        data.setStage(5);
        data.addTrueYuan(999_999);
        expect(failures, data.getTrueYuan() == 1024, "true yuan positive add clamps to cap");
        data.addTrueYuan(-128);
        expect(failures, data.getTrueYuan() == 896, "true yuan negative add subtracts");
        data.addTrueYuan(-10_000);
        expect(failures, data.getTrueYuan() == 0, "true yuan negative add clamps at zero");

        data.setStage(6);
        data.addImmortalYuan(999_999);
        expect(failures, data.getImmortalYuan() == 64, "immortal yuan positive add clamps to cap");
        data.addImmortalYuan(-8);
        expect(failures, data.getImmortalYuan() == 56, "immortal yuan negative add subtracts");
        data.addImmortalYuan(-10_000);
        expect(failures, data.getImmortalYuan() == 0, "immortal yuan negative add clamps at zero");
        expect(failures, data.getTrueYuanCap() == 0, "stage 6 clears true yuan cap");

        data.setStage(5);
        data.setLingqiProgress(data.getLingqiCap());
        expect(failures, data.getLingqiProgress() == 10420, "lingqi cap setup");
        data.setStage(6);
        expect(failures, data.getImmortalYuanCap() == 64 && data.getTrueYuanCap() == 0, "stage 5 to 6 clears old caps");

        data.setStage(10);
        data.setTimeScale(99f);
        expect(failures, data.getRealmTimeRatePermille() == 32000, "time scale clamps high");
        data.setTimeScale(-1f);
        expect(failures, data.getRealmTimeRatePermille() == 0, "time scale clamps low");

        data.setStage(9);
        java.util.UUID attempt = java.util.UUID.randomUUID();
        java.util.UUID target = java.util.UUID.randomUUID();
        data.beginTribulation(attempt, target, 10);
        data.completeTribulation(attempt, target, null);
        expect(failures, data.getStage() == 10 && !data.isTribulationActive(), "tribulation success advances and clears");

        ItemStack staff = new ItemStack(ModItems.SPIRIT_STAFF.get());
        SpiritStaffItem.setMode(staff, SpiritStaffItem.MODE_WRENCH);
        SpiritStaffItem.setMode(staff, SpiritStaffItem.getMode(staff) + 1);
        expect(failures, SpiritStaffItem.getMode(staff) == SpiritStaffItem.MODE_PICK, "staff mode cycles forward");
        SpiritStaffItem.setMode(staff, SpiritStaffItem.getMode(staff) - 2);
        expect(failures, SpiritStaffItem.getMode(staff) == SpiritStaffItem.MODE_BUILD, "staff mode wraps backward");

        verifyProgressionSmoke(failures);
        verifyPersonalRealmKeys(failures);
        verifyClientAssets(failures);
        verifyMenuLayouts(failures);
        verifyTerminalContracts(failures);
        verifySourceVeinRegistrations(failures);
        verifySourceOwnershipPolicy(failures);
        verifyPublicCompatibilityApi(failures);

        return new Result(failures);
    }

    private static void expect(List<String> failures, boolean condition, String label) {
        if (!condition) failures.add(label);
    }

    private static void verifyProgressionSmoke(List<String> failures) {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(1);
        expect(failures, data.isHasKongqiao() && !data.isHasXianqiao(), "stage 1 unlocks kongqiao only");
        data.setLingqiProgress(data.getLingqiCap());
        data.setStage(2);
        expect(failures, data.getStage() == 2 && data.getKongqiaoMaxSlots() == 20, "manual stage 2 progression state");

        data.setStage(5);
        data.addTrueYuan(512);
        ItemStack core = new ItemStack(ModItems.SPIRIT_CORE.get());
        data.setKongqiaoSlot(0, core);
        data.scanForSpiritCore();
        expect(failures, !data.hasSpiritCore(), "spirit core remains an inert crafting material");
        expect(failures, data.getTrueYuanCap() == 1024, "spirit core leaves true yuan cap unchanged");
        data.addTrueYuan(9999);
        expect(failures, data.getTrueYuan() == 1024, "ordinary true yuan cap clamps");

        ItemStack iron = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 70);
        ItemStack leftover = data.insertStack(iron, true);
        expect(failures, leftover.isEmpty(), "kongqiao stores inserted stack");
        ItemStack extracted = data.extractStack(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT), 32);
        expect(failures, extracted.getCount() == 32, "kongqiao extracts requested stack count");

        data.setStage(6);
        expect(failures, data.isHasXianqiao() && data.isHasXianqiaoRealm(), "stage 6 unlocks xianqiao realm");
        data.setXianqiaoSlot(80, new ItemStack(ModItems.SPIRIT_CORE.get()));
        expect(failures, data.getXianqiaoStorageItems().size() >= 81, "xianqiao storage grows on demand");
        data.scanForSpiritCore();
        expect(failures, !data.hasSpiritCore() && data.getImmortalYuanCap() == 64,
                "spirit core leaves immortal yuan cap unchanged");

        java.util.UUID attempt = java.util.UUID.randomUUID();
        java.util.UUID target = java.util.UUID.randomUUID();
        data.beginTribulation(attempt, target, 7);
        data.failTribulation();
        expect(failures, data.getStage() == 6 && !data.isTribulationActive(), "tribulation failure clears without advancing");
        attempt = java.util.UUID.randomUUID();
        target = java.util.UUID.randomUUID();
        data.beginTribulation(attempt, target, 7);
        data.completeTribulation(attempt, target, null);
        expect(failures, data.getStage() == 7 && !data.isTribulationActive(), "tribulation success advances");

        data.setStage(8);
        data.setTimeScale(8.0f);
        expect(failures, data.getRealmTimeRatePermille() == 8000, "stage 8 time flow value stores");

        data.setStage(10);
        expect(failures, data.getRealmRadiusChunks() == 1024
                        && data.getImmortalYuanCap() == Integer.MAX_VALUE
                        && data.getImmortalYuanCapLong() == Long.MAX_VALUE,
                "stage 10 ascension caps");
        expect(failures, com.immortalstorage.immortalstorage.dimension.RealmHelper.runtimeForcedRadiusChunks(data) == 3,
                "stage 10 runtime forced chunk radius is capped");
    }

    private static void verifyPersonalRealmKeys(List<String> failures) {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var firstKey = ImmortalStorageDimensions.personalRealmKey(first);
        var secondKey = ImmortalStorageDimensions.personalRealmKey(second);
        expect(failures, firstKey.equals(ImmortalStorageDimensions.personalRealmKey(first)), "personal realm key is stable");
        expect(failures, !firstKey.equals(secondKey), "personal realm keys differ per player uuid");
        expect(failures, ImmortalStorageDimensions.isPersonalRealmFor(firstKey, first), "personal realm key matches owner");
        expect(failures, !ImmortalStorageDimensions.isPersonalRealmFor(firstKey, second), "personal realm key rejects other owner");
        expect(failures, ImmortalStorageDimensions.personalRealmOwner(firstKey).orElseThrow().equals(first), "personal realm owner decodes");
        expect(failures, ImmortalStorageDimensions.isXianqiaoRealm(firstKey), "personal realm prefix recognized");
        expect(failures, !ImmortalStorageDimensions.isXianqiaoRealm(net.minecraft.world.level.Level.OVERWORLD), "overworld is not xianqiao realm");
        expect(failures, !ImmortalStorageDimensions.isXianqiaoRealm(ImmortalStorageDimensions.XIANQIAO_REALM_LEVEL), "template shared level is not a player realm");
    }

    private static void verifyClientAssets(List<String> failures) {
        String[] itemIds = {
                "jade_guide", "true_yuan", "immortal_yuan",
                "crude_pill_embryo", "crude_pill", "refined_pill_embryo", "refined_pill",
                "breakthrough_pill_embryo", "breakthrough_pill", "immortal_pill",
                "ascension_dan", "white_day_thunder", "spirit_iron", "crude_spirit_iron",
                "spirit_crystal", "spirit_core", "spirit_sword", "spirit_staff",
                "spirit_drive"
        };
        String[] blockIds = {
                "spirit_iron_ore", "spirit_crystal_ore", "immortal_furnace", "xianqiao_manager",
                "spirit_iron_block", "spirit_crystal_block", "crude_spirit_iron_block",
                "advanced_stabilized_miniature_immortal_ruin",
                "advanced_entangled_stabilized_miniature_immortal_ruin",
                "water_vein", "milk_vein", "lava_vein", "cobblestone_vein", "stone_vein", "smooth_stone_vein",
                "white_concrete_vein", "orange_concrete_vein", "magenta_concrete_vein",
                "light_blue_concrete_vein", "yellow_concrete_vein", "lime_concrete_vein",
                "pink_concrete_vein", "gray_concrete_vein", "light_gray_concrete_vein",
                "cyan_concrete_vein", "purple_concrete_vein", "blue_concrete_vein",
                "brown_concrete_vein", "green_concrete_vein", "red_concrete_vein",
                "black_concrete_vein",
                "dirt_vein", "oak_log_vein", "coal_vein", "raw_copper_vein", "raw_iron_vein",
                "raw_gold_vein", "lapis_vein", "redstone_vein", "crude_spirit_iron_vein",
                "spirit_crystal_vein", "diamond_vein", "emerald_vein", "echo_shard_vein",
                "ancient_debris_vein",
                "nether_star_vein", "enchanted_golden_apple_vein", "dragon_egg_vein"
        };
        expect(failures, resourceExists("assets/immortalstorage/lang/en_us.json"), "client lang en_us");
        for (String id : itemIds) {
            expect(failures, resourceExists("assets/immortalstorage/models/item/" + id + ".json"), "item model " + id);
            expect(failures, resourceExists("assets/immortalstorage/textures/item/" + id + ".png"), "item texture " + id);
        }
        for (String id : blockIds) {
            expect(failures, resourceExists("assets/immortalstorage/blockstates/" + id + ".json"), "blockstate " + id);
            expect(failures, resourceExists("assets/immortalstorage/models/block/" + id + ".json"), "block model " + id);
            expect(failures, resourceExists("assets/immortalstorage/models/item/" + id + ".json"), "block item model " + id);
            expect(failures, resourceExists("assets/immortalstorage/textures/block/" + id + ".png"), "block texture " + id);
        }
        expect(failures, resourceExists("assets/immortalstorage/textures/reference/image2_immortalstorage_texture_atlas.png"), "Image2 texture reference");
    }

    private static void verifyMenuLayouts(List<String> failures) {
        expect(failures, com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.COLUMNS == 9,
                "terminal uses nine storage columns");
        expect(failures, com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.MIN_ROWS == 2
                        && com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.DEFAULT_ROWS == 5
                        && com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.MAX_ROWS == 12,
                "terminal row limits are 2..12 with five-row default");
        expect(failures, com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu.VISIBLE_STORAGE_SLOTS == 108
                        && com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS == 216,
                "Kongqiao reserves twelve rows and Xianqiao preloads a double twenty-four-row window");
        expect(failures, com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu.CRAFT_START == 108
                        && com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu.CRAFT_START == 216,
                "terminal crafting ranges follow their storage proxy windows");
    }

    private static void verifyTerminalContracts(List<String> failures) {
        var viewport = new com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport(99, 99, 99, 7, -1);
        expect(failures, viewport.visibleRows() == 12 && viewport.baseRow() == 0 && viewport.revision() == 0,
                "terminal viewport clamps rows, base row, and revision");
        expect(failures, com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.clampBaseRow(20, 5, 12) == 7,
                "terminal viewport reaches the final partial page");
        expect(failures, Math.min(com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.MAX_ROWS,
                        com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.DEFAULT_ROWS + 1) == 6,
                "smooth terminal scrolling reserves one clipped buffer row");

        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT, 32);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Named Iron"));
        data.setXianqiaoSlot(0, new ItemStack(Items.IRON_INGOT, 64));
        data.setXianqiaoSlot(1, new ItemStack(Items.IRON_INGOT, 16));
        data.setXianqiaoSlot(2, namedIron);
        data.setXianqiaoSlot(3, new ItemStack(Items.GOLD_INGOT, 8));

        var catalog = new com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryCatalog();
        expect(failures, catalog.rebuildIfChanged(data.getXianqiaoStorageItems()),
                "terminal catalog detects initial backing state");
        long initialRevision = catalog.revision();
        List<com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntry> entries =
                catalog.entries(com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.DEFAULT);
        expect(failures, entries.size() == 3, "terminal catalog aggregates same components and separates different components");
        expect(failures, entries.stream().anyMatch(entry -> entry.displayStack().is(Items.IRON_INGOT) && entry.amount() == 80),
                "terminal catalog sums matching stacks using long amounts");
        long ironId = entries.stream().filter(entry -> entry.displayStack().is(Items.IRON_INGOT) && entry.amount() == 80)
                .findFirst().map(com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntry::entryId).orElse(0L);
        expect(failures, !catalog.rebuildIfChanged(data.getXianqiaoStorageItems()) && catalog.revision() == initialRevision,
                "terminal catalog does not revise unchanged backing state");
        data.setXianqiaoSlot(1, new ItemStack(Items.IRON_INGOT, 17));
        expect(failures, catalog.rebuildIfChanged(data.getXianqiaoStorageItems()) && catalog.revision() == initialRevision + 1,
                "terminal catalog revises changed counts");
        expect(failures, catalog.find(ironId) != null && catalog.find(ironId).amount() == 81,
                "terminal entry id remains stable across count-only revisions");

        var modQuery = new com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery("@minecraft",
                com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.SortOrder.NAME,
                com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.SortDirection.ASCENDING);
        expect(failures, catalog.entries(modQuery).size() == 3, "terminal namespace query filters entries");
        expect(failures, new com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery("x".repeat(256), null, null)
                        .text().length() == com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.MAX_SEARCH_LENGTH,
                "terminal query bounds untrusted search text");

        ImmortalStoragePlayerData kongqiao = new ImmortalStoragePlayerData();
        kongqiao.setStage(1);
        for (int i = 0; i < kongqiao.getKongqiaoMaxSlots(); i++) {
            kongqiao.setKongqiaoSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        ItemStack kongqiaoLeftover = com.immortalstorage.immortalstorage.menu.custom.TerminalMenuSupportTestAccess
                .insertKongqiao(kongqiao, new ItemStack(Items.DIRT, 3));
        expect(failures, kongqiaoLeftover.getCount() == 3,
                "crafting return never overwrites a full kongqiao");
    }

    private static void verifySourceVeinRegistrations(List<String> failures) {
        expect(failures, VeinKind.values().length == 39, "source vein kind count");
        expect(failures, VeinKind.WATER.fluid && VeinKind.MILK.fluid && VeinKind.LAVA.fluid, "fluid source vein kinds registered as fluids");
        expect(failures, !VeinKind.COBBLE.fluid && !VeinKind.DRAGON_EGG.fluid, "item source vein kinds registered as items");
        expect(failures, VeinKind.WATER.yuanCostPerBatch == 0 && VeinKind.LAVA.yuanCostPerBatch == 0, "fluid source veins are free");
        expect(failures, VeinKind.STONE.yuanCostPerBatch == 1 && VeinKind.STONE.outputsPerBatch == 64,
                "ordinary source vein batch cost registered");
        expect(failures, VeinKind.NETHER_STAR.yuanCostPerBatch == 8 && VeinKind.NETHER_STAR.outputsPerBatch == 1
                        && VeinKind.DRAGON_EGG.yuanCostPerBatch == 64 && VeinKind.DRAGON_EGG.outputsPerBatch == 1,
                "high tier source vein batch costs registered");
        expect(failures, VeinKind.ECHO_SHARD.yuanCostPerBatch == 16 && VeinKind.ECHO_SHARD.outputsPerBatch == 1
                        && VeinKind.ECHO_SHARD.minStage == 8 && !VeinKind.ECHO_SHARD.fluid,
                "echo shard source vein registered at 16 yuan per single output, stage 8");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.outputCostPerTick(VeinKind.STONE, 64) == 1,
                "stone source cost at 64 flux");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.outputCostPerTick(VeinKind.STONE, 65) == 2,
                "stone source cost rounds up over 64 flux");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.outputCostPerTick(VeinKind.NETHER_STAR, 1) == 8,
                "nether star source cost applies per-output batch");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.outputCostPerTick(VeinKind.DRAGON_EGG, 1) == 64,
                "dragon egg source costs 64 yuan per output");

        expectSourceBlock(failures, ModBlocks.WATER_VEIN.get(), VeinKind.WATER, "water");
        expectSourceBlock(failures, ModBlocks.MILK_VEIN.get(), VeinKind.MILK, "milk");
        expectSourceBlock(failures, ModBlocks.LAVA_VEIN.get(), VeinKind.LAVA, "lava");
        expectSourceBlock(failures, ModBlocks.COBBLESTONE_VEIN.get(), VeinKind.COBBLE, "cobblestone");
        expectSourceBlock(failures, ModBlocks.STONE_VEIN.get(), VeinKind.STONE, "stone");
        expectSourceBlock(failures, ModBlocks.SMOOTH_STONE_VEIN.get(), VeinKind.SMOOTH_STONE, "smooth stone");
        expectSourceBlock(failures, ModBlocks.WHITE_CONCRETE_VEIN.get(), VeinKind.WHITE_CONCRETE, "white concrete");
        expectSourceBlock(failures, ModBlocks.ORANGE_CONCRETE_VEIN.get(), VeinKind.ORANGE_CONCRETE, "orange concrete");
        expectSourceBlock(failures, ModBlocks.MAGENTA_CONCRETE_VEIN.get(), VeinKind.MAGENTA_CONCRETE, "magenta concrete");
        expectSourceBlock(failures, ModBlocks.LIGHT_BLUE_CONCRETE_VEIN.get(), VeinKind.LIGHT_BLUE_CONCRETE, "light blue concrete");
        expectSourceBlock(failures, ModBlocks.YELLOW_CONCRETE_VEIN.get(), VeinKind.YELLOW_CONCRETE, "yellow concrete");
        expectSourceBlock(failures, ModBlocks.LIME_CONCRETE_VEIN.get(), VeinKind.LIME_CONCRETE, "lime concrete");
        expectSourceBlock(failures, ModBlocks.PINK_CONCRETE_VEIN.get(), VeinKind.PINK_CONCRETE, "pink concrete");
        expectSourceBlock(failures, ModBlocks.GRAY_CONCRETE_VEIN.get(), VeinKind.GRAY_CONCRETE, "gray concrete");
        expectSourceBlock(failures, ModBlocks.LIGHT_GRAY_CONCRETE_VEIN.get(), VeinKind.LIGHT_GRAY_CONCRETE, "light gray concrete");
        expectSourceBlock(failures, ModBlocks.CYAN_CONCRETE_VEIN.get(), VeinKind.CYAN_CONCRETE, "cyan concrete");
        expectSourceBlock(failures, ModBlocks.PURPLE_CONCRETE_VEIN.get(), VeinKind.PURPLE_CONCRETE, "purple concrete");
        expectSourceBlock(failures, ModBlocks.BLUE_CONCRETE_VEIN.get(), VeinKind.BLUE_CONCRETE, "blue concrete");
        expectSourceBlock(failures, ModBlocks.BROWN_CONCRETE_VEIN.get(), VeinKind.BROWN_CONCRETE, "brown concrete");
        expectSourceBlock(failures, ModBlocks.GREEN_CONCRETE_VEIN.get(), VeinKind.GREEN_CONCRETE, "green concrete");
        expectSourceBlock(failures, ModBlocks.RED_CONCRETE_VEIN.get(), VeinKind.RED_CONCRETE, "red concrete");
        expectSourceBlock(failures, ModBlocks.BLACK_CONCRETE_VEIN.get(), VeinKind.BLACK_CONCRETE, "black concrete");
        expectSourceBlock(failures, ModBlocks.DIRT_VEIN.get(), VeinKind.DIRT, "dirt");
        expectSourceBlock(failures, ModBlocks.OAK_LOG_VEIN.get(), VeinKind.OAK_LOG, "oak log");
        expectSourceBlock(failures, ModBlocks.COAL_VEIN.get(), VeinKind.COAL, "coal");
        expectSourceBlock(failures, ModBlocks.RAW_COPPER_VEIN.get(), VeinKind.RAW_COPPER, "raw copper");
        expectSourceBlock(failures, ModBlocks.RAW_IRON_VEIN.get(), VeinKind.RAW_IRON, "raw iron");
        expectSourceBlock(failures, ModBlocks.RAW_GOLD_VEIN.get(), VeinKind.RAW_GOLD, "raw gold");
        expectSourceBlock(failures, ModBlocks.LAPIS_VEIN.get(), VeinKind.LAPIS, "lapis");
        expectSourceBlock(failures, ModBlocks.REDSTONE_VEIN.get(), VeinKind.REDSTONE, "redstone");
        expectSourceBlock(failures, ModBlocks.CRUDE_SPIRIT_IRON_VEIN.get(), VeinKind.CRUDE_SPIRIT_IRON, "crude spirit iron");
        expectSourceBlock(failures, ModBlocks.SPIRIT_CRYSTAL_VEIN.get(), VeinKind.SPIRIT_CRYSTAL, "spirit crystal");
        expectSourceBlock(failures, ModBlocks.DIAMOND_VEIN.get(), VeinKind.DIAMOND, "diamond");
        expectSourceBlock(failures, ModBlocks.EMERALD_VEIN.get(), VeinKind.EMERALD, "emerald");
        expectSourceBlock(failures, ModBlocks.ECHO_SHARD_VEIN.get(), VeinKind.ECHO_SHARD, "echo shard");
        expectSourceBlock(failures, ModBlocks.ANCIENT_DEBRIS_VEIN.get(), VeinKind.ANCIENT_DEBRIS, "ancient debris");
        expectSourceBlock(failures, ModBlocks.NETHER_STAR_VEIN.get(), VeinKind.NETHER_STAR, "nether star");
        expectSourceBlock(failures, ModBlocks.ENCHANTED_GOLDEN_APPLE_VEIN.get(), VeinKind.ENCHANTED_GOLDEN_APPLE, "enchanted golden apple");
        expectSourceBlock(failures, ModBlocks.DRAGON_EGG_VEIN.get(), VeinKind.DRAGON_EGG, "dragon egg");

        expectSourceOutput(failures, VeinKind.COBBLE, Items.COBBLESTONE, "cobble output");
        expectSourceOutput(failures, VeinKind.STONE, Items.STONE, "stone output");
        expectSourceOutput(failures, VeinKind.SMOOTH_STONE, Items.SMOOTH_STONE, "smooth stone output");
        expectSourceOutput(failures, VeinKind.WHITE_CONCRETE, Items.WHITE_CONCRETE, "white concrete output");
        expectSourceOutput(failures, VeinKind.ORANGE_CONCRETE, Items.ORANGE_CONCRETE, "orange concrete output");
        expectSourceOutput(failures, VeinKind.MAGENTA_CONCRETE, Items.MAGENTA_CONCRETE, "magenta concrete output");
        expectSourceOutput(failures, VeinKind.LIGHT_BLUE_CONCRETE, Items.LIGHT_BLUE_CONCRETE, "light blue concrete output");
        expectSourceOutput(failures, VeinKind.YELLOW_CONCRETE, Items.YELLOW_CONCRETE, "yellow concrete output");
        expectSourceOutput(failures, VeinKind.LIME_CONCRETE, Items.LIME_CONCRETE, "lime concrete output");
        expectSourceOutput(failures, VeinKind.PINK_CONCRETE, Items.PINK_CONCRETE, "pink concrete output");
        expectSourceOutput(failures, VeinKind.GRAY_CONCRETE, Items.GRAY_CONCRETE, "gray concrete output");
        expectSourceOutput(failures, VeinKind.LIGHT_GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, "light gray concrete output");
        expectSourceOutput(failures, VeinKind.CYAN_CONCRETE, Items.CYAN_CONCRETE, "cyan concrete output");
        expectSourceOutput(failures, VeinKind.PURPLE_CONCRETE, Items.PURPLE_CONCRETE, "purple concrete output");
        expectSourceOutput(failures, VeinKind.BLUE_CONCRETE, Items.BLUE_CONCRETE, "blue concrete output");
        expectSourceOutput(failures, VeinKind.BROWN_CONCRETE, Items.BROWN_CONCRETE, "brown concrete output");
        expectSourceOutput(failures, VeinKind.GREEN_CONCRETE, Items.GREEN_CONCRETE, "green concrete output");
        expectSourceOutput(failures, VeinKind.RED_CONCRETE, Items.RED_CONCRETE, "red concrete output");
        expectSourceOutput(failures, VeinKind.BLACK_CONCRETE, Items.BLACK_CONCRETE, "black concrete output");
        expectSourceOutput(failures, VeinKind.DIRT, Items.DIRT, "dirt output");
        expectSourceOutput(failures, VeinKind.OAK_LOG, Items.OAK_LOG, "oak log output");
        expectSourceOutput(failures, VeinKind.COAL, Items.COAL, "coal output");
        expectSourceOutput(failures, VeinKind.RAW_COPPER, Items.RAW_COPPER, "raw copper output");
        expectSourceOutput(failures, VeinKind.RAW_IRON, Items.RAW_IRON, "raw iron output");
        expectSourceOutput(failures, VeinKind.RAW_GOLD, Items.RAW_GOLD, "raw gold output");
        expectSourceOutput(failures, VeinKind.LAPIS, Items.LAPIS_LAZULI, "lapis output");
        expectSourceOutput(failures, VeinKind.REDSTONE, Items.REDSTONE, "redstone output");
        expectSourceOutput(failures, VeinKind.CRUDE_SPIRIT_IRON, ModItems.CRUDE_SPIRIT_IRON.get(), "crude spirit iron output");
        expectSourceOutput(failures, VeinKind.SPIRIT_CRYSTAL, ModItems.SPIRIT_CRYSTAL.get(), "spirit crystal output");
        expectSourceOutput(failures, VeinKind.DIAMOND, Items.DIAMOND, "diamond output");
        expectSourceOutput(failures, VeinKind.EMERALD, Items.EMERALD, "emerald output");
        expectSourceOutput(failures, VeinKind.ECHO_SHARD, Items.ECHO_SHARD, "echo shard output");
        expectSourceOutput(failures, VeinKind.ANCIENT_DEBRIS, Items.ANCIENT_DEBRIS, "ancient debris output");
        expectSourceOutput(failures, VeinKind.NETHER_STAR, Items.NETHER_STAR, "nether star output");
        expectSourceOutput(failures, VeinKind.ENCHANTED_GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, "enchanted golden apple output");
        expectSourceOutput(failures, VeinKind.DRAGON_EGG, Items.DRAGON_EGG, "dragon egg output");

        var water = sourceVein(ModBlocks.WATER_VEIN.get(), VeinKind.WATER);
        var milk = sourceVein(ModBlocks.MILK_VEIN.get(), VeinKind.MILK);
        var lava = sourceVein(ModBlocks.LAVA_VEIN.get(), VeinKind.LAVA);
        expect(failures, water.sampleOutput().isEmpty() && lava.sampleOutput().isEmpty(),
                "fluid source veins do not expose item sample output");
        expect(failures, water.sampleFluid() == Fluids.WATER
                        && milk.sampleFluid() == net.neoforged.neoforge.common.NeoForgeMod.MILK.value()
                        && lava.sampleFluid() == Fluids.LAVA,
                "fluid source veins expose correct source fluid");
        expect(failures, water.filledVanillaContainer().is(Items.WATER_BUCKET)
                        && milk.filledVanillaContainer().is(Items.MILK_BUCKET)
                        && lava.filledVanillaContainer().is(Items.LAVA_BUCKET),
                "fluid source veins expose matching vanilla containers");
        expect(failures, water.getFluidHandler() != null && lava.getFluidHandler() != null,
                "fluid source veins expose fluid handlers");
        water.setFluxLimit(250);
        expect(failures, water.getFluidHandler().drain(1000,
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "fluid capability cannot synthesize output from an empty cache");
        expect(failures, sourceVein(ModBlocks.COBBLESTONE_VEIN.get(), VeinKind.COBBLE).getFluidHandler() == null,
                "item source veins do not expose fluid handlers");

        var cobble = sourceVein(ModBlocks.COBBLESTONE_VEIN.get(), VeinKind.COBBLE);
        cobble.setFluxLimit(32);
        expect(failures, !cobble.isActiveOutput(), "source side io defaults to no active push");
        expect(failures, cobble.canExtractFrom(Direction.NORTH) && !cobble.canPushTo(Direction.NORTH),
                "source side io defaults to passive extraction");
        cobble.setSideMode(Direction.NORTH, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.SourceSideMode.DISABLED);
        expect(failures, cobble.canExtractFrom(Direction.NORTH) && cobble.canExtractFrom(Direction.SOUTH),
                "source side io never disables passive standard capability extraction");
        cobble.setSideMode(Direction.EAST, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.SourceSideMode.PUSH);
        expect(failures, cobble.isActiveOutput() && cobble.canPushTo(Direction.EAST) && !cobble.canPushTo(Direction.SOUTH),
                "source side io active push is face-specific");
        expect(failures, cobble.cycleSideMode(Direction.EAST)
                        == com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.SourceSideMode.DISABLED,
                "source side io cycles push to disabled");
        IItemHandler sourceItems = cobble.getItemHandler();
        expect(failures, sourceItems != null && sourceItems.getSlots() == 1, "item source veins expose read-only item handler");
        expect(failures, sourceItems != null && sourceItems.getStackInSlot(0).isEmpty(),
                "item source handler reports an empty persistent cache without synthesizing output");
        ItemStack refusedInsert = sourceItems == null ? ItemStack.EMPTY : sourceItems.insertItem(0, new ItemStack(Items.DIRT, 1), false);
        expect(failures, refusedInsert.is(Items.DIRT) && refusedInsert.getCount() == 1,
                "item source handler refuses insertion");
        ItemStack extractedSource = sourceItems == null ? ItemStack.EMPTY : sourceItems.extractItem(0, 64, true);
        expect(failures, extractedSource.isEmpty(),
                "item source handler cannot extract output from an empty cache");
        expect(failures, water.getItemHandler() == null, "fluid source veins do not expose item handlers");
        expect(failures, water.getFluidHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0,
                "fluid source handler refuses fill");

        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(9);
        data.addImmortalYuan(100_000);
        expect(failures, data.getImmortalYuan() == 100_000, "stage 9 immortal yuan cap is unbounded");
        data.setStage(10);
        data.addImmortalYuan(100_000);
        expect(failures, data.isInfiniteImmortalYuan()
                        ? data.getImmortalYuan() == Long.MAX_VALUE
                        : data.getImmortalYuan() == 200_000,
                "stage 10 immortal yuan follows the configured virtual or generated-unbounded channel");
        data.setStage(8);
        data.addImmortalYuan(100_000);
        expect(failures, data.getImmortalYuan() == 1024, "stage 8 immortal yuan cap clamps after recompute");
    }

    private static void verifyPublicCompatibilityApi(List<String> failures) {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(6);
        data.setXianqiaoSlot(0, new ItemStack(Items.IRON_INGOT, 16));
        IItemHandler handler = new PersonalStorageItemHandler(data, null, () -> {});

        ItemStack simulatedInsertLeftover = handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), true);
        expect(failures, simulatedInsertLeftover.isEmpty(), "personal storage API simulate insert accepts stack");
        expect(failures, data.getXianqiaoStorageItems().get(0).getCount() == 16,
                "personal storage API simulate insert does not mutate");
        ItemStack insertLeftover = handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), false);
        expect(failures, insertLeftover.isEmpty() && data.getXianqiaoStorageItems().get(0).getCount() == 48,
                "personal storage API native item handler inserts");

        ItemStack simulatedExtract = handler.extractItem(0, 12, true);
        expect(failures, simulatedExtract.getCount() == 12 && data.getXianqiaoStorageItems().get(0).getCount() == 48,
                "personal storage API simulate extract does not mutate");
        ItemStack extracted = handler.extractItem(0, 20, false);
        expect(failures, extracted.getCount() == 20 && data.getXianqiaoStorageItems().get(0).getCount() == 28,
                "personal storage API native item handler extracts");

        AtomicInteger handlerChanges = new AtomicInteger();
        IItemHandler changedHandler = new PersonalStorageItemHandler(data, null, handlerChanges::incrementAndGet);
        changedHandler.insertItem(0, new ItemStack(Items.GOLD_INGOT, 1), true);
        expect(failures, handlerChanges.get() == 0, "personal storage handler simulate insert does not mark changed");
        changedHandler.insertItem(0, new ItemStack(Items.GOLD_INGOT, 1), false);
        expect(failures, handlerChanges.get() == 1, "personal storage handler real insert marks changed");
        changedHandler.extractItem(0, 1, true);
        expect(failures, handlerChanges.get() == 1, "personal storage handler simulate extract does not mark changed");
        changedHandler.extractItem(0, 1, false);
        expect(failures, handlerChanges.get() == 2, "personal storage handler real extract marks changed");

        var waterEndpoint = com.immortalstorage.immortalstorage.api.source.SourceApi.resolve(sourceVein(ModBlocks.WATER_VEIN.get(), VeinKind.WATER));
        expect(failures, waterEndpoint != null && waterEndpoint.fluidSource() && waterEndpoint.fluid() == Fluids.WATER,
                "source API exposes water source endpoint");
        expect(failures, waterEndpoint != null && waterEndpoint.fluidHandler() != null,
                "source API exposes native fluid handler");

        var stoneEndpoint = com.immortalstorage.immortalstorage.api.source.SourceApi.resolve(sourceVein(ModBlocks.STONE_VEIN.get(), VeinKind.STONE));
        expect(failures, stoneEndpoint != null && !stoneEndpoint.fluidSource() && stoneEndpoint.itemSample(4).is(Items.STONE),
                "source API exposes item source endpoint");

        AtomicInteger endpointChanges = new AtomicInteger();
        var endpoint = new com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork.Endpoint(
                UUID.fromString("00000000-0000-0000-0000-000000000303"), data, null, endpointChanges::incrementAndGet);
        endpoint.insert(new ItemStack(Items.COPPER_INGOT, 2), true);
        expect(failures, endpointChanges.get() == 0, "personal storage endpoint simulate insert does not mark changed");
        endpoint.insert(new ItemStack(Items.COPPER_INGOT, 2), false);
        expect(failures, endpointChanges.get() == 1, "personal storage endpoint insert marks changed");
        endpoint.extract(new ItemStack(Items.COPPER_INGOT), 1, true);
        expect(failures, endpointChanges.get() == 1, "personal storage endpoint simulate extract does not mark changed");
        endpoint.extract(new ItemStack(Items.COPPER_INGOT), 1, false);
        expect(failures, endpointChanges.get() == 2, "personal storage endpoint extract marks changed");
    }

    private static void verifySourceOwnershipPolicy(List<String> failures) {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000202");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.canClaim(null, actor, 0, 0, false),
                "source ownership unowned claim is allowed");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.canClaim(owner, owner, 0, 0, false),
                "source ownership owner can reopen");
        expect(failures, !com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.canClaim(owner, actor, 0, 0, false),
                "source ownership other claim obeys config deny");
        expect(failures, com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.canClaim(owner, actor, 6, 6, true),
                "source ownership other claim obeys config allow");
        expect(failures, !com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.canClaim(owner, actor, 5, 6, true),
                "source ownership other claim requires minimum stage");
    }

    private static void expectSourceBlock(List<String> failures, Block block, VeinKind kind, String label) {
        expect(failures, block instanceof SourceVeinBlock source && source.getKind() == kind, label + " source vein block kind");
    }

    private static void expectSourceOutput(List<String> failures, VeinKind kind, net.minecraft.world.item.Item item, String label) {
        ItemStack stack = com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity.kindToStack(kind, 3);
        expect(failures, !stack.isEmpty() && stack.is(item) && stack.getCount() == 3, label);
    }

    private static com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity sourceVein(Block registeredBlock, VeinKind kind) {
        SourceVeinBlock block = (SourceVeinBlock) registeredBlock;
        BlockState state = block.defaultBlockState();
        return new com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity(BlockPos.ZERO, state, kind);
    }

    private static boolean resourceExists(String path) {
        return ImmortalStorageSelfTest.class.getClassLoader().getResource(path) != null;
    }

    public record Result(List<String> failures) {
        public boolean passed() {
            return failures.isEmpty();
        }

        public String summary() {
            return passed()
                    ? "ImmortalStorage selftest PASS: stages=11, yuan math, time scale, tribulation, staff mode, progression smoke, personal realm keys, client assets+textures, terminal contracts+aggregation, source vein registrations, source ownership policy, source side io, source read-only io, manager bidirectional io, public compat api"
                    : "ImmortalStorage selftest FAIL: " + String.join("; ", failures);
        }
    }

    private ImmortalStorageSelfTest() {}
}
