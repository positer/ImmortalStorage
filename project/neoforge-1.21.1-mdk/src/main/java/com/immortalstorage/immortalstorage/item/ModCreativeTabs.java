package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compat.CompatManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalStorageMod.MODID);

    public static final Supplier<CreativeModeTab> CULTIVATION = TABS.register("immortalstorage",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.JADE_GUIDE.get()))
                    .title(Component.translatable("itemGroup.immortalstorage"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.JADE_GUIDE.get());
                        output.accept(ModItems.TRUE_YUAN.get());
                        output.accept(ModItems.IMMORTAL_YUAN.get());
                        output.accept(ModItems.CRUDE_PILL_EMBRYO.get());
                        output.accept(ModItems.CRUDE_PILL.get());
                        output.accept(ModItems.REFINED_PILL_EMBRYO.get());
                        output.accept(ModItems.REFINED_PILL.get());
                        output.accept(ModItems.BREAKTHROUGH_PILL_EMBRYO.get());
                        output.accept(ModItems.BREAKTHROUGH_PILL.get());
                        output.accept(ModItems.IMMORTAL_PILL.get());
                        output.accept(ModItems.ASCENSION_DAN.get());
                        output.accept(ModItems.WHITE_DAY_THUNDER.get());
                        output.accept(ModItems.SPIRIT_IRON.get());
                        output.accept(ModItems.SPIRIT_IRON_NUGGET.get());
                        output.accept(ModItems.CRUDE_SPIRIT_IRON.get());
                        output.accept(ModItems.SPIRIT_CRYSTAL.get());
                        output.accept(ModItems.NURTURING_CRYSTAL.get());
                        output.accept(ModItems.SPIRIT_CORE.get());
                        output.accept(ModItems.PREMIXED_HEAVY_COMPOUND.get());
                        output.accept(ModItems.SUBSTITUTE_PUPPET.get());
                        output.accept(ModItems.MINIATURE_IMMORTAL_RUIN.get());
                        output.accept(ModItems.QI_COLLECTING_BOTTLE.get());
                        output.accept(ModItems.DISPOSABLE_QI_COLLECTING_BOTTLE.get());
                        output.accept(ModItems.PRIMORDIAL_QI.get());
                        output.accept(ModItems.SPIRIT_SWORD.get());
                        output.accept(ModItems.IMMORTAL_RUIN_FORGED_SPIRIT_SWORD.get());
                        output.accept(ModItems.SPIRIT_STAFF.get());
                        params.holders().lookupOrThrow(Registries.ENCHANTMENT)
                                .get(com.immortalstorage.immortalstorage.enchantment.ModEnchantments.SPIRIT_REPAIR)
                                .ifPresent(enchantment -> output.accept(EnchantedBookItem.createForEnchantment(
                                        new EnchantmentInstance(enchantment, 1))));
                        output.accept(ModItems.SPIRIT_DRIVE.get());
                        if (CompatManager.AE2_LOADED) {
                            output.accept(ModItems.XIANQIAO_EXCHANGE_CELL.get());
                        }
                        if (CompatManager.RS_LOADED) {
                            output.accept(ModItems.XIANQIAO_RS_EXCHANGE_DISK.get());
                        }

                        output.accept(ModBlocks.SPIRIT_IRON_ORE.get());
                        output.accept(ModBlocks.SPIRIT_CRYSTAL_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_SPIRIT_IRON_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_SPIRIT_CRYSTAL_ORE.get());
                        output.accept(ModBlocks.IMMORTAL_FURNACE.get());
                        output.accept(ModBlocks.XIANQIAO_MANAGER.get());
                        output.accept(ModBlocks.XIANQIAO_INTERFACE.get());
                        output.accept(ModBlocks.SOURCE_VEIN_MANAGER.get());
                        output.accept(ModBlocks.WORLD_SHARD_MINER.get());
                        output.accept(ModBlocks.TREASURE_BASIN.get());
                        output.accept(ModBlocks.SPIRIT_IRON_BLOCK.get());
                        output.accept(ModBlocks.SPIRIT_CRYSTAL_BLOCK.get());
                        output.accept(ModBlocks.INACTIVE_NURTURING_CRYSTAL_BEDROCK.get());
                        output.accept(ModBlocks.NURTURING_CRYSTAL_BEDROCK.get());
                        output.accept(ModBlocks.SMALL_NURTURING_CRYSTAL_BUD.get());
                        output.accept(ModBlocks.MEDIUM_NURTURING_CRYSTAL_BUD.get());
                        output.accept(ModBlocks.LARGE_NURTURING_CRYSTAL_BUD.get());
                        output.accept(ModBlocks.NURTURING_CRYSTAL_CLUSTER.get());
                        output.accept(ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get());
                        output.accept(ModBlocks.CRUDE_SPIRIT_IRON_BLOCK.get());
                        output.accept(ModBlocks.WATER_VEIN.get());
                        output.accept(ModBlocks.MILK_VEIN.get());
                        output.accept(ModBlocks.LAVA_VEIN.get());
                        output.accept(ModBlocks.COBBLESTONE_VEIN.get());
                        output.accept(ModBlocks.STONE_VEIN.get());
                        output.accept(ModBlocks.SMOOTH_STONE_VEIN.get());
                        output.accept(ModBlocks.WHITE_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.ORANGE_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.MAGENTA_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.LIGHT_BLUE_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.YELLOW_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.LIME_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.PINK_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.GRAY_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.LIGHT_GRAY_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.CYAN_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.PURPLE_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.BLUE_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.BROWN_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.GREEN_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.RED_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.BLACK_CONCRETE_VEIN.get());
                        output.accept(ModBlocks.DIRT_VEIN.get());
                        output.accept(ModBlocks.OAK_LOG_VEIN.get());
                        output.accept(ModBlocks.COAL_VEIN.get());
                        output.accept(ModBlocks.RAW_COPPER_VEIN.get());
                        output.accept(ModBlocks.RAW_IRON_VEIN.get());
                        output.accept(ModBlocks.RAW_GOLD_VEIN.get());
                        output.accept(ModBlocks.LAPIS_VEIN.get());
                        output.accept(ModBlocks.REDSTONE_VEIN.get());
                        output.accept(ModBlocks.CRUDE_SPIRIT_IRON_VEIN.get());
                        output.accept(ModBlocks.SPIRIT_CRYSTAL_VEIN.get());
                        output.accept(ModBlocks.DIAMOND_VEIN.get());
                        output.accept(ModBlocks.EMERALD_VEIN.get());
                        output.accept(ModBlocks.ANCIENT_DEBRIS_VEIN.get());
                        output.accept(ModBlocks.NETHER_STAR_VEIN.get());
                        output.accept(ModBlocks.ENCHANTED_GOLDEN_APPLE_VEIN.get());
                        output.accept(ModBlocks.DRAGON_EGG_VEIN.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
