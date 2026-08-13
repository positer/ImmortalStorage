package com.immortalstorage.immortalstorage.block.entity;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compat.CompatManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ImmortalStorageMod.MODID);

    public static final Supplier<BlockEntityType<ImmortalFurnaceBlockEntity>> IMMORTAL_FURNACE =
            BLOCK_ENTITIES.register("immortal_furnace", () ->
                    new BlockEntityType<>(ImmortalFurnaceBlockEntity::new,
                            ModBlocks.IMMORTAL_FURNACE.get()));
    public static final Supplier<BlockEntityType<SimulatedReincarnationFurnaceBlockEntity>> SIMULATED_REINCARNATION_FURNACE =
            BLOCK_ENTITIES.register("simulated_reincarnation_furnace", () ->
                    new BlockEntityType<>(SimulatedReincarnationFurnaceBlockEntity::new,
                            ModBlocks.SIMULATED_REINCARNATION_FURNACE.get()));
    public static final Supplier<BlockEntityType<SimulatedSpiritFieldBlockEntity>> SIMULATED_SPIRIT_FIELD =
            BLOCK_ENTITIES.register("simulated_spirit_field", () ->
                    new BlockEntityType<>(SimulatedSpiritFieldBlockEntity::new,
                            ModBlocks.SIMULATED_SPIRIT_FIELD.get()));
    public static final Supplier<BlockEntityType<EnergyCrystalBlockEntity>> ENERGY_CRYSTAL =
            BLOCK_ENTITIES.register("energy_crystal", () ->
                    new BlockEntityType<>(EnergyCrystalBlockEntity::new,
                            ModBlocks.ENERGY_CRYSTAL.get()));
    public static final @Nullable Supplier<BlockEntityType<EnergyCrystalBlockEntity>> MANA_CRYSTAL =
            CompatManager.BOTANIA_LOADED
                    ? BLOCK_ENTITIES.register("mana_crystal", () ->
                    new BlockEntityType<>(
                            (p, s) -> new EnergyCrystalBlockEntity(p, s, CrystalKind.MANA),
                            ModBlocks.MANA_CRYSTAL.get()))
                    : null;
    public static final @Nullable Supplier<BlockEntityType<EnergyCrystalBlockEntity>> SOURCE_CRYSTAL =
            CompatManager.ARS_NOUVEAU_LOADED
                    ? BLOCK_ENTITIES.register("source_crystal", () ->
                    new BlockEntityType<>(
                            (p, s) -> new EnergyCrystalBlockEntity(p, s, CrystalKind.SOURCE),
                            ModBlocks.SOURCE_CRYSTAL.get()))
                    : null;

    public static Supplier<BlockEntityType<EnergyCrystalBlockEntity>> typeFor(CrystalKind kind) {
        return switch (kind) {
            case ELECTRIC -> ENERGY_CRYSTAL;
            case MANA -> MANA_CRYSTAL;
            case SOURCE -> SOURCE_CRYSTAL;
        };
    }

    public static final Supplier<BlockEntityType<MiniatureImmortalRuinBlockEntity>> MINIATURE_IMMORTAL_RUIN =
            BLOCK_ENTITIES.register("miniature_immortal_ruin", () ->
                    new BlockEntityType<>(MiniatureImmortalRuinBlockEntity::new,
                            ModBlocks.MINIATURE_IMMORTAL_RUIN.get()));

    public static final Supplier<BlockEntityType<StabilizedMiniatureImmortalRuinBlockEntity>> STABILIZED_MINIATURE_IMMORTAL_RUIN =
            BLOCK_ENTITIES.register("stabilized_miniature_immortal_ruin", () ->
                    new BlockEntityType<>(StabilizedMiniatureImmortalRuinBlockEntity::new,
                            ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get()));

    public static final Supplier<BlockEntityType<EntangledStabilizedMiniatureImmortalRuinBlockEntity>> ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN =
            BLOCK_ENTITIES.register("entangled_stabilized_miniature_immortal_ruin", () ->
                    new BlockEntityType<>(EntangledStabilizedMiniatureImmortalRuinBlockEntity::new,
                            ModBlocks.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get()));

    public static final Supplier<BlockEntityType<AdvancedStabilizedMiniatureImmortalRuinBlockEntity>> ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN =
            BLOCK_ENTITIES.register("advanced_stabilized_miniature_immortal_ruin", () ->
                    new BlockEntityType<>(AdvancedStabilizedMiniatureImmortalRuinBlockEntity::new,
                            ModBlocks.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get()));

    public static final Supplier<BlockEntityType<AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity>> ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN =
            BLOCK_ENTITIES.register("advanced_entangled_stabilized_miniature_immortal_ruin", () ->
                    new BlockEntityType<>(AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity::new,
                            ModBlocks.ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get()));

    public static final Supplier<BlockEntityType<SourceVeinBlockEntity>> SOURCE_VEIN =
            BLOCK_ENTITIES.register("source_vein", () ->
                    new BlockEntityType<>(
                            (BlockPos p, BlockState s) -> new SourceVeinBlockEntity(p, s,
                                    ((com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock) s.getBlock()).getKind()),
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
                            ModBlocks.DRAGON_EGG_VEIN.get(), ModBlocks.CUSTOM_SOURCE_VEIN.get()));

    public static final Supplier<BlockEntityType<XianqiaoManagerBlockEntity>> XIANQIAO_MANAGER =
            BLOCK_ENTITIES.register("xianqiao_manager", () ->
                    new BlockEntityType<>(XianqiaoManagerBlockEntity::new,
                            ModBlocks.XIANQIAO_MANAGER.get()));

    public static final Supplier<BlockEntityType<XianqiaoInterfaceBlockEntity>> XIANQIAO_INTERFACE =
            BLOCK_ENTITIES.register("xianqiao_interface", () ->
                    new BlockEntityType<>(XianqiaoInterfaceBlockEntity::new,
                            ModBlocks.XIANQIAO_INTERFACE.get()));

    public static final Supplier<BlockEntityType<AdvancedXianqiaoInterfaceBlockEntity>> ADVANCED_XIANQIAO_INTERFACE =
            BLOCK_ENTITIES.register("advanced_xianqiao_interface", () ->
                    new BlockEntityType<>(AdvancedXianqiaoInterfaceBlockEntity::new,
                            ModBlocks.ADVANCED_XIANQIAO_INTERFACE.get()));

    public static final Supplier<BlockEntityType<SourceVeinManagerBlockEntity>> SOURCE_VEIN_MANAGER =
            BLOCK_ENTITIES.register("source_vein_manager", () ->
                    new BlockEntityType<>(SourceVeinManagerBlockEntity::new,
                            ModBlocks.SOURCE_VEIN_MANAGER.get()));

    public static final Supplier<BlockEntityType<WorldShardMinerBlockEntity>> WORLD_SHARD_MINER =
            BLOCK_ENTITIES.register("world_shard_miner", () ->
                    new BlockEntityType<>(WorldShardMinerBlockEntity::new,
                            ModBlocks.WORLD_SHARD_MINER.get()));

    public static final Supplier<BlockEntityType<TreasureBasinBlockEntity>> TREASURE_BASIN =
            BLOCK_ENTITIES.register("treasure_basin", () ->
                    new BlockEntityType<>(TreasureBasinBlockEntity::new,
                            ModBlocks.TREASURE_BASIN.get()));
    public static final Supplier<BlockEntityType<YuanLightBlockEntity>> YUAN_LIGHT =
            BLOCK_ENTITIES.register("yuan_light", () -> new BlockEntityType<>(
                    YuanLightBlockEntity::new,
                    ModBlocks.TRUE_YUAN_LIGHT.get(), ModBlocks.IMMORTAL_YUAN_LIGHT.get()));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, IMMORTAL_FURNACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Item.BLOCK, SIMULATED_REINCARNATION_FURNACE.get(),
                (be, side) -> side == null || !be.automaticOutput() || !be.outputFace(side) ? null
                        : com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(new net.neoforged.neoforge.items.wrapper.RangedWrapper(
                                be.itemHandler(), SimulatedReincarnationFurnaceBlockEntity.OUTPUT_START,
                                SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT)));
        event.registerBlockEntity(Capabilities.Item.BLOCK, SIMULATED_SPIRIT_FIELD.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENERGY_CRYSTAL.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ENERGY_CRYSTAL.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energy(be.getEnergyHandler(side)));
        if (MANA_CRYSTAL != null) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, MANA_CRYSTAL.get(),
                    (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        }
        if (SOURCE_CRYSTAL != null) {
            event.registerBlockEntity(Capabilities.Item.BLOCK, SOURCE_CRYSTAL.get(),
                    (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        }
        event.registerBlockEntity(Capabilities.Item.BLOCK, XIANQIAO_MANAGER.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluid(be.getFluidHandler(side)));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energy(be.getEnergyHandler(side)));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ADVANCED_XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ADVANCED_XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluid(be.getFluidHandler(side)));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ADVANCED_XIANQIAO_INTERFACE.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energy(be.getEnergyHandler(side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, XIANQIAO_MANAGER.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluid(be.getFluidHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, WORLD_SHARD_MINER.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getCacheHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, TREASURE_BASIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getCacheHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.itemHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.itemHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.itemHandler()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, SOURCE_VEIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SOURCE_VEIN.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluid(be.getFluidHandler(side)));
        event.registerBlockEntity(Capabilities.Item.BLOCK, SOURCE_VEIN_MANAGER.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.item(be.getItemHandler(side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, SOURCE_VEIN_MANAGER.get(),
                (be, side) -> com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluid(be.getFluidHandler(side)));
    }

    private ModBlockEntities() {}
}
