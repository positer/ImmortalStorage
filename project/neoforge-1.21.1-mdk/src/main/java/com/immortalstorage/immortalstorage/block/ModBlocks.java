package com.immortalstorage.immortalstorage.block;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.custom.*;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.SourceVeinBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ImmortalStorageMod.MODID);

    private static BlockBehaviour.Properties setBlockId(String name, BlockBehaviour.Properties props) {
        return props;
    }

    public static final Supplier<Block> SPIRIT_IRON_ORE = reg("spirit_iron_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(2, 5),
                    setBlockId("spirit_iron_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE))));
    public static final Supplier<Block> SPIRIT_CRYSTAL_ORE = reg("spirit_crystal_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(3, 7),
                    setBlockId("spirit_crystal_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE))));
    public static final Supplier<Block> DEEPSLATE_SPIRIT_IRON_ORE = reg("deepslate_spirit_iron_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(2, 5),
                    setBlockId("deepslate_spirit_iron_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE))));
    public static final Supplier<Block> DEEPSLATE_SPIRIT_CRYSTAL_ORE = reg("deepslate_spirit_crystal_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(3, 7),
                    setBlockId("deepslate_spirit_crystal_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE))));

    public static final Supplier<Block> IMMORTAL_FURNACE = reg("immortal_furnace",
            () -> new ImmortalFurnaceBlock(setBlockId("immortal_furnace", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5f).requiresCorrectToolForDrops().lightLevel(s -> 7))));
    public static final Supplier<Block> XIANQIAO_MANAGER = reg("xianqiao_manager",
            () -> new XianqiaoManagerBlock(setBlockId("xianqiao_manager",
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(4.0f)
                            .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 7))));
    public static final Supplier<Block> XIANQIAO_INTERFACE = reg("xianqiao_interface",
            () -> new XianqiaoInterfaceBlock(setBlockId("xianqiao_interface",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                            .mapColor(MapColor.COLOR_CYAN).strength(4.0f)
                            .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK))));
    public static final Supplier<Block> SIMULATED_REINCARNATION_FURNACE = reg("simulated_reincarnation_furnace",
            () -> new SimulatedReincarnationFurnaceBlock(setBlockId("simulated_reincarnation_furnace",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.TRIAL_SPAWNER).strength(5.0F)
                            .requiresCorrectToolForDrops().lightLevel(state -> 6))));
    public static final Supplier<Block> SIMULATED_SPIRIT_FIELD = reg("simulated_spirit_field",
            () -> new SimulatedSpiritFieldBlock(setBlockId("simulated_spirit_field",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)
                            .strength(3.5F).requiresCorrectToolForDrops().noOcclusion())));
    public static final Supplier<Block> SOURCE_VEIN_MANAGER = reg("source_vein_manager",
            () -> new SourceVeinManagerBlock(setBlockId("source_vein_manager",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                            .mapColor(MapColor.COLOR_GRAY).strength(4.0f)
                            .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK))));
    /** Frozen registry carrier for config/datapack-defined sourceDefinitionId values. */
    public static final Supplier<Block> CUSTOM_SOURCE_VEIN = reg("custom_source_vein",
            () -> new SourceVeinBlock(VeinKind.COBBLE, true));
    public static final Supplier<Block> WORLD_SHARD_MINER = reg("world_shard_miner",
            () -> new WorldShardMinerBlock(setBlockId("world_shard_miner",
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0f)
                            .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 7))));
    public static final Supplier<Block> TREASURE_BASIN = reg("treasure_basin",
            () -> new TreasureBasinBlock(setBlockId("treasure_basin",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
                            .mapColor(MapColor.GOLD).lightLevel(state -> 4))));
    public static final Supplier<Block> TRUE_YUAN_LIGHT = BLOCKS.register("true_yuan_light",
            () -> new YuanLightBlock(false));
    public static final Supplier<Block> IMMORTAL_YUAN_LIGHT = BLOCKS.register("immortal_yuan_light",
            () -> new YuanLightBlock(true));

    public static final Supplier<Block> SPIRIT_IRON_BLOCK = reg("spirit_iron_block",
            () -> new net.minecraft.world.level.block.Block(setBlockId("spirit_iron_block",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))));
    public static final Supplier<Block> SPIRIT_CRYSTAL_BLOCK = reg("spirit_crystal_block",
            () -> new net.minecraft.world.level.block.Block(setBlockId("spirit_crystal_block",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK))));
    public static final Supplier<Block> INACTIVE_NURTURING_CRYSTAL_BEDROCK = reg("inactive_nurturing_crystal_bedrock",
            () -> new Block(setBlockId("inactive_nurturing_crystal_bedrock",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK))));
    public static final Supplier<Block> NURTURING_CRYSTAL_BEDROCK = reg("nurturing_crystal_bedrock",
            () -> new NurturingCrystalBedrockBlock(setBlockId("nurturing_crystal_bedrock",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BUDDING_AMETHYST).randomTicks())));
    public static final Supplier<Block> SMALL_NURTURING_CRYSTAL_BUD = reg("small_nurturing_crystal_bud",
            () -> new NurturingCrystalClusterBlock(3, 4, setBlockId("small_nurturing_crystal_bud",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD))));
    public static final Supplier<Block> MEDIUM_NURTURING_CRYSTAL_BUD = reg("medium_nurturing_crystal_bud",
            () -> new NurturingCrystalClusterBlock(4, 3, setBlockId("medium_nurturing_crystal_bud",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.MEDIUM_AMETHYST_BUD))));
    public static final Supplier<Block> LARGE_NURTURING_CRYSTAL_BUD = reg("large_nurturing_crystal_bud",
            () -> new NurturingCrystalClusterBlock(5, 3, setBlockId("large_nurturing_crystal_bud",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_AMETHYST_BUD))));
    public static final Supplier<Block> NURTURING_CRYSTAL_CLUSTER = reg("nurturing_crystal_cluster",
            () -> new NurturingCrystalClusterBlock(7, 3, setBlockId("nurturing_crystal_cluster",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER))));
    public static final Supplier<Block> STABILIZED_MINIATURE_IMMORTAL_RUIN = reg("stabilized_miniature_immortal_ruin",
            () -> new StabilizedMiniatureImmortalRuinBlock(setBlockId("stabilized_miniature_immortal_ruin",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion())));
    public static final Supplier<Block> ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN = reg("entangled_stabilized_miniature_immortal_ruin",
            () -> new EntangledStabilizedMiniatureImmortalRuinBlock(setBlockId("entangled_stabilized_miniature_immortal_ruin",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion())));
    public static final Supplier<Block> ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN = reg("advanced_stabilized_miniature_immortal_ruin",
            () -> new AdvancedStabilizedMiniatureImmortalRuinBlock(setBlockId("advanced_stabilized_miniature_immortal_ruin",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion())));
    public static final Supplier<Block> ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN = reg("advanced_entangled_stabilized_miniature_immortal_ruin",
            () -> new AdvancedEntangledStabilizedMiniatureImmortalRuinBlock(setBlockId("advanced_entangled_stabilized_miniature_immortal_ruin",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion())));
    public static final Supplier<Block> MINIATURE_IMMORTAL_RUIN = BLOCKS.register("miniature_immortal_ruin_block",
            () -> new MiniatureImmortalRuinBlock(setBlockId("miniature_immortal_ruin_block",
                    BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(-1.0F, 3_600_000.0F)
                            .pushReaction(PushReaction.BLOCK))));
    public static final Supplier<Block> CRUDE_SPIRIT_IRON_BLOCK = reg("crude_spirit_iron_block",
            () -> new net.minecraft.world.level.block.Block(setBlockId("crude_spirit_iron_block",
                    BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK))));

    public static final Supplier<Block> WATER_VEIN = reg("water_vein", () -> new SourceVeinBlock(VeinKind.WATER));
    public static final Supplier<Block> MILK_VEIN = reg("milk_vein", () -> new SourceVeinBlock(VeinKind.MILK));
    public static final Supplier<Block> LAVA_VEIN = reg("lava_vein", () -> new SourceVeinBlock(VeinKind.LAVA));
    public static final Supplier<Block> COBBLESTONE_VEIN = reg("cobblestone_vein", () -> new SourceVeinBlock(VeinKind.COBBLE));
    public static final Supplier<Block> STONE_VEIN = reg("stone_vein", () -> new SourceVeinBlock(VeinKind.STONE));
    public static final Supplier<Block> SMOOTH_STONE_VEIN = reg("smooth_stone_vein", () -> new SourceVeinBlock(VeinKind.SMOOTH_STONE));
    public static final Supplier<Block> WHITE_CONCRETE_VEIN = reg("white_concrete_vein", () -> new SourceVeinBlock(VeinKind.WHITE_CONCRETE));
    public static final Supplier<Block> ORANGE_CONCRETE_VEIN = reg("orange_concrete_vein", () -> new SourceVeinBlock(VeinKind.ORANGE_CONCRETE));
    public static final Supplier<Block> MAGENTA_CONCRETE_VEIN = reg("magenta_concrete_vein", () -> new SourceVeinBlock(VeinKind.MAGENTA_CONCRETE));
    public static final Supplier<Block> LIGHT_BLUE_CONCRETE_VEIN = reg("light_blue_concrete_vein", () -> new SourceVeinBlock(VeinKind.LIGHT_BLUE_CONCRETE));
    public static final Supplier<Block> YELLOW_CONCRETE_VEIN = reg("yellow_concrete_vein", () -> new SourceVeinBlock(VeinKind.YELLOW_CONCRETE));
    public static final Supplier<Block> LIME_CONCRETE_VEIN = reg("lime_concrete_vein", () -> new SourceVeinBlock(VeinKind.LIME_CONCRETE));
    public static final Supplier<Block> PINK_CONCRETE_VEIN = reg("pink_concrete_vein", () -> new SourceVeinBlock(VeinKind.PINK_CONCRETE));
    public static final Supplier<Block> GRAY_CONCRETE_VEIN = reg("gray_concrete_vein", () -> new SourceVeinBlock(VeinKind.GRAY_CONCRETE));
    public static final Supplier<Block> LIGHT_GRAY_CONCRETE_VEIN = reg("light_gray_concrete_vein", () -> new SourceVeinBlock(VeinKind.LIGHT_GRAY_CONCRETE));
    public static final Supplier<Block> CYAN_CONCRETE_VEIN = reg("cyan_concrete_vein", () -> new SourceVeinBlock(VeinKind.CYAN_CONCRETE));
    public static final Supplier<Block> PURPLE_CONCRETE_VEIN = reg("purple_concrete_vein", () -> new SourceVeinBlock(VeinKind.PURPLE_CONCRETE));
    public static final Supplier<Block> BLUE_CONCRETE_VEIN = reg("blue_concrete_vein", () -> new SourceVeinBlock(VeinKind.BLUE_CONCRETE));
    public static final Supplier<Block> BROWN_CONCRETE_VEIN = reg("brown_concrete_vein", () -> new SourceVeinBlock(VeinKind.BROWN_CONCRETE));
    public static final Supplier<Block> GREEN_CONCRETE_VEIN = reg("green_concrete_vein", () -> new SourceVeinBlock(VeinKind.GREEN_CONCRETE));
    public static final Supplier<Block> RED_CONCRETE_VEIN = reg("red_concrete_vein", () -> new SourceVeinBlock(VeinKind.RED_CONCRETE));
    public static final Supplier<Block> BLACK_CONCRETE_VEIN = reg("black_concrete_vein", () -> new SourceVeinBlock(VeinKind.BLACK_CONCRETE));
    public static final Supplier<Block> DIRT_VEIN = reg("dirt_vein", () -> new SourceVeinBlock(VeinKind.DIRT));
    public static final Supplier<Block> OAK_LOG_VEIN = reg("oak_log_vein", () -> new SourceVeinBlock(VeinKind.OAK_LOG));
    public static final Supplier<Block> COAL_VEIN = reg("coal_vein", () -> new SourceVeinBlock(VeinKind.COAL));
    public static final Supplier<Block> RAW_COPPER_VEIN = reg("raw_copper_vein", () -> new SourceVeinBlock(VeinKind.RAW_COPPER));
    public static final Supplier<Block> RAW_IRON_VEIN = reg("raw_iron_vein", () -> new SourceVeinBlock(VeinKind.RAW_IRON));
    public static final Supplier<Block> RAW_GOLD_VEIN = reg("raw_gold_vein", () -> new SourceVeinBlock(VeinKind.RAW_GOLD));
    public static final Supplier<Block> LAPIS_VEIN = reg("lapis_vein", () -> new SourceVeinBlock(VeinKind.LAPIS));
    public static final Supplier<Block> REDSTONE_VEIN = reg("redstone_vein", () -> new SourceVeinBlock(VeinKind.REDSTONE));
    public static final Supplier<Block> CRUDE_SPIRIT_IRON_VEIN = reg("crude_spirit_iron_vein", () -> new SourceVeinBlock(VeinKind.CRUDE_SPIRIT_IRON));
    public static final Supplier<Block> SPIRIT_CRYSTAL_VEIN = reg("spirit_crystal_vein", () -> new SourceVeinBlock(VeinKind.SPIRIT_CRYSTAL));
    public static final Supplier<Block> DIAMOND_VEIN = reg("diamond_vein", () -> new SourceVeinBlock(VeinKind.DIAMOND));
    public static final Supplier<Block> EMERALD_VEIN = reg("emerald_vein", () -> new SourceVeinBlock(VeinKind.EMERALD));
    public static final Supplier<Block> ECHO_SHARD_VEIN = reg("echo_shard_vein", () -> new SourceVeinBlock(VeinKind.ECHO_SHARD));
    public static final Supplier<Block> ANCIENT_DEBRIS_VEIN = reg("ancient_debris_vein", () -> new SourceVeinBlock(VeinKind.ANCIENT_DEBRIS));
    public static final Supplier<Block> NETHER_STAR_VEIN = reg("nether_star_vein", () -> new SourceVeinBlock(VeinKind.NETHER_STAR));
    public static final Supplier<Block> ENCHANTED_GOLDEN_APPLE_VEIN = reg("enchanted_golden_apple_vein", () -> new SourceVeinBlock(VeinKind.ENCHANTED_GOLDEN_APPLE));
    public static final Supplier<Block> DRAGON_EGG_VEIN = reg("dragon_egg_vein", () -> new SourceVeinBlock(VeinKind.DRAGON_EGG));

    private static Supplier<Block> reg(String id, Supplier<Block> b) {
        Supplier<Block> ro = BLOCKS.register(id, b);
        ModItems.ITEMS.register(id, () -> ro.get() instanceof SourceVeinBlock source
                ? new SourceVeinBlockItem(source, ModItems.setItemId(id, new Item.Properties()))
                : new BlockItem(ro.get(), ModItems.setItemId(id, new Item.Properties())));
        return ro;
    }

    private ModBlocks() {}
}
