package com.cultivation.cultivation.block.entity;

import java.util.function.Supplier;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CultivationMod.MODID);

    public static final Supplier<BlockEntityType<ImmortalFurnaceBlockEntity>> IMMORTAL_FURNACE =
            BLOCK_ENTITIES.register("immortal_furnace", () ->
                    BlockEntityType.Builder.of(ImmortalFurnaceBlockEntity::new,
                            ModBlocks.IMMORTAL_FURNACE.get()).build(null));

    public static final Supplier<BlockEntityType<SourceVeinBlockEntity>> SOURCE_VEIN =
            BLOCK_ENTITIES.register("source_vein", () ->
                    BlockEntityType.Builder.of(
                            (BlockPos p, BlockState s) -> new SourceVeinBlockEntity(p, s,
                                    ((com.cultivation.cultivation.block.custom.SourceVeinBlock) s.getBlock()).getKind()),
                            ModBlocks.WATER_VEIN.get(), ModBlocks.MILK_VEIN.get(), ModBlocks.LAVA_VEIN.get(),
                            ModBlocks.COBBLESTONE_VEIN.get(), ModBlocks.STONE_VEIN.get(),
                            ModBlocks.SMOOTH_STONE_VEIN.get(), ModBlocks.WHITE_CONCRETE_VEIN.get(),
                            ModBlocks.ORANGE_CONCRETE_VEIN.get(), ModBlocks.MAGENTA_CONCRETE_VEIN.get(),
                            ModBlocks.LIGHT_BLUE_CONCRETE_VEIN.get(), ModBlocks.YELLOW_CONCRETE_VEIN.get(),
                            ModBlocks.LIME_CONCRETE_VEIN.get(), ModBlocks.PINK_CONCRETE_VEIN.get(),
                            ModBlocks.GRAY_CONCRETE_VEIN.get(), ModBlocks.LIGHT_GRAY_CONCRETE_VEIN.get(),
                            ModBlocks.CYAN_CONCRETE_VEIN.get(), ModBlocks.PURPLE_CONCRETE_VEIN.get(),
                            ModBlocks.BLUE_CONCRETE_VEIN.get(), ModBlocks.BROWN_CONCRETE_VEIN.get(),
                            ModBlocks.GREEN_CONCRETE_VEIN.get(), ModBlocks.RED_CONCRETE_VEIN.get(),
                            ModBlocks.BLACK_CONCRETE_VEIN.get(), ModBlocks.DIRT_VEIN.get(),
                            ModBlocks.OAK_LOG_VEIN.get(), ModBlocks.COAL_VEIN.get(),
                            ModBlocks.RAW_COPPER_VEIN.get(), ModBlocks.RAW_IRON_VEIN.get(),
                            ModBlocks.RAW_GOLD_VEIN.get(), ModBlocks.LAPIS_VEIN.get(),
                            ModBlocks.REDSTONE_VEIN.get(), ModBlocks.CRUDE_SPIRIT_IRON_VEIN.get(),
                            ModBlocks.SPIRIT_CRYSTAL_VEIN.get(), ModBlocks.DIAMOND_VEIN.get(),
                            ModBlocks.EMERALD_VEIN.get(), ModBlocks.ANCIENT_DEBRIS_VEIN.get(),
                            ModBlocks.NETHER_STAR_VEIN.get(), ModBlocks.ENCHANTED_GOLDEN_APPLE_VEIN.get(),
                            ModBlocks.DRAGON_EGG_VEIN.get(), ModBlocks.CUSTOM_SOURCE_VEIN.get()).build(null));

    public static final Supplier<BlockEntityType<XianqiaoManagerBlockEntity>> XIANQIAO_MANAGER =
            BLOCK_ENTITIES.register("xianqiao_manager", () ->
                    BlockEntityType.Builder.of(XianqiaoManagerBlockEntity::new,
                            ModBlocks.XIANQIAO_MANAGER.get()).build(null));

    public static final Supplier<BlockEntityType<XianqiaoInterfaceBlockEntity>> XIANQIAO_INTERFACE =
            BLOCK_ENTITIES.register("xianqiao_interface", () ->
                    BlockEntityType.Builder.of(XianqiaoInterfaceBlockEntity::new,
                            ModBlocks.XIANQIAO_INTERFACE.get()).build(null));

    public static final Supplier<BlockEntityType<SourceVeinManagerBlockEntity>> SOURCE_VEIN_MANAGER =
            BLOCK_ENTITIES.register("source_vein_manager", () ->
                    BlockEntityType.Builder.of(SourceVeinManagerBlockEntity::new,
                            ModBlocks.SOURCE_VEIN_MANAGER.get()).build(null));

    public static final Supplier<BlockEntityType<WorldShardMinerBlockEntity>> WORLD_SHARD_MINER =
            BLOCK_ENTITIES.register("world_shard_miner", () ->
                    BlockEntityType.Builder.of(WorldShardMinerBlockEntity::new,
                            ModBlocks.WORLD_SHARD_MINER.get()).build(null));

    public static final Supplier<BlockEntityType<TreasureBasinBlockEntity>> TREASURE_BASIN =
            BLOCK_ENTITIES.register("treasure_basin", () ->
                    BlockEntityType.Builder.of(TreasureBasinBlockEntity::new,
                            ModBlocks.TREASURE_BASIN.get()).build(null));
    public static final Supplier<BlockEntityType<YuanLightBlockEntity>> YUAN_LIGHT =
            BLOCK_ENTITIES.register("yuan_light", () -> BlockEntityType.Builder.of(
                    YuanLightBlockEntity::new,
                    ModBlocks.TRUE_YUAN_LIGHT.get(), ModBlocks.IMMORTAL_YUAN_LIGHT.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IMMORTAL_FURNACE.get(),
                ImmortalFurnaceBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, XIANQIAO_MANAGER.get(),
                (be, side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> be.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, XIANQIAO_MANAGER.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WORLD_SHARD_MINER.get(),
                (be, side) -> be.getCacheHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TREASURE_BASIN.get(),
                (be, side) -> be.getCacheHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SOURCE_VEIN.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SOURCE_VEIN.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SOURCE_VEIN_MANAGER.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SOURCE_VEIN_MANAGER.get(),
                (be, side) -> be.getFluidHandler(side));
    }

    private ModBlockEntities() {}
}
