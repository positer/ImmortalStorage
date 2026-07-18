package com.cultivation.cultivation;

import com.cultivation.cultivation.advancement.CultivationCriteriaTriggers;
import com.cultivation.cultivation.block.ModBlocks;
import com.cultivation.cultivation.block.entity.ModBlockEntities;
import com.cultivation.cultivation.config.CultivationConfig;
import com.cultivation.cultivation.config.CultivationClientConfig;
import com.cultivation.cultivation.data.ModDataGeneration;
import com.cultivation.cultivation.dimension.CultivationDimensions;
import com.cultivation.cultivation.effect.ModEffects;
import com.cultivation.cultivation.enchantment.ModEnchantments;
import com.cultivation.cultivation.entity.ModEntities;
import com.cultivation.cultivation.item.ModCreativeTabs;
import com.cultivation.cultivation.item.ModDataComponents;
import com.cultivation.cultivation.item.ModItems;
import com.cultivation.cultivation.loot.ModLootModifiers;
import com.cultivation.cultivation.menu.ModMenus;
import com.cultivation.cultivation.network.ModNetwork;
import com.cultivation.cultivation.player.ModAttachments;
import com.cultivation.cultivation.recipe.ModRecipes;
import com.cultivation.cultivation.sound.ModSounds;
import com.cultivation.cultivation.villager.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CultivationMod.MODID)
public class CultivationMod {
    public static final String MODID = "cultivation";
    public static final Logger LOG = LoggerFactory.getLogger("Cultivation");

    public CultivationMod(IEventBus modBus, ModContainer modContainer) {
        IEventBus fbus = NeoForge.EVENT_BUS;

        net.neoforged.neoforge.common.NeoForgeMod.enableMilkFluid();
        com.cultivation.cultivation.api.source.CultivationChargeProviders.registerBuiltins();
        modBus.addListener(CultivationMod::commonSetup);

        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);
        ModVillagers.POI_TYPES.register(modBus);
        ModVillagers.PROFESSIONS.register(modBus);
        CultivationDimensions.CHUNK_GENERATOR_CODECS.register(modBus);
        CultivationDimensions.register();
        modBus.register(CultivationCriteriaTriggers.class);

        modBus.addListener(ModDataGeneration::gatherData);
        modBus.addListener(ModBlockEntities::registerCapabilities);

        modContainer.registerConfig(ModConfig.Type.COMMON, CultivationConfig.SPEC, "cultivation-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, CultivationClientConfig.SPEC, "cultivation-client.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.cultivation.cultivation.client.ClientSetup.init(modBus, fbus, modContainer);
        }

        modBus.addListener(ModNetwork::register);

        com.cultivation.cultivation.compat.CompatManager.initializeOptionalIntegrations(modBus);
        com.cultivation.cultivation.compat.CompatManager.logCompat();

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new com.cultivation.cultivation.event.CommonEvents());
        NeoForge.EVENT_BUS.register(new com.cultivation.cultivation.event.RealmTickRateEvents());
        NeoForge.EVENT_BUS.register(com.cultivation.cultivation.villager.ModTrades.class);
        NeoForge.EVENT_BUS.addListener(com.cultivation.cultivation.command.CultivationCommands::register);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.cultivation.cultivation.api.source.CultivationChargeProviders::freezeRegistration);
    }

    @SubscribeEvent
    public void onAddReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new com.cultivation.cultivation.worldshard.WorldShardMinerReloadListener(
                event.getRegistryAccess()));
        event.addListener(new com.cultivation.cultivation.worldshard.WorldShardLootReloadListener());
        event.addListener(new com.cultivation.cultivation.source.definition.SourceDefinitionReloadListener());
    }

    @SubscribeEvent
    public void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        com.cultivation.cultivation.worldshard.WorldShardMinerModes.rebuildPools(event.getServer());
        LOG.info("Rebuilt world shard miner ore catalogs from final server worldgen, generation {}",
                com.cultivation.cultivation.worldshard.WorldShardMinerModes.generation());
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        com.cultivation.cultivation.worldshard.WorldShardMinerModes.rebuildPools(
                event.getPlayerList().getServer());
        LOG.info("Rebuilt world shard miner ore catalogs after global datapack reload, generation {}. "
                        + "Worldgen biome-modifier changes still require a server restart to be fully reflected",
                com.cultivation.cultivation.worldshard.WorldShardMinerModes.generation());
    }
}
