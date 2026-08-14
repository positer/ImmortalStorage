package com.immortalstorage.immortalstorage;

import com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.config.ImmortalStorageClientConfig;
import com.immortalstorage.immortalstorage.data.ModDataGeneration;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.effect.ModEffects;
import com.immortalstorage.immortalstorage.enchantment.ModEnchantments;
import com.immortalstorage.immortalstorage.entity.ModEntities;
import com.immortalstorage.immortalstorage.item.ModCreativeTabs;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.loot.ModLootModifiers;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.network.ModNetwork;
import com.immortalstorage.immortalstorage.player.ModAttachments;
import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import com.immortalstorage.immortalstorage.sound.ModSounds;
import com.immortalstorage.immortalstorage.villager.ModVillagers;
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

@Mod(ImmortalStorageMod.MODID)
public class ImmortalStorageMod {
    public static final String MODID = "immortalstorage";
    public static final Logger LOG = LoggerFactory.getLogger("ImmortalStorage");

    public ImmortalStorageMod(IEventBus modBus, ModContainer modContainer) {
        IEventBus fbus = NeoForge.EVENT_BUS;

        net.neoforged.neoforge.common.NeoForgeMod.enableMilkFluid();
        com.immortalstorage.immortalstorage.api.source.ImmortalStorageChargeProviders.registerBuiltins();
        modBus.addListener(ImmortalStorageMod::commonSetup);

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
        ImmortalStorageDimensions.CHUNK_GENERATOR_CODECS.register(modBus);
        ImmortalStorageDimensions.register();
        modBus.register(ImmortalStorageCriteriaTriggers.class);

        modBus.addListener(ModDataGeneration::gatherData);
        modBus.addListener(ModBlockEntities::registerCapabilities);

        modContainer.registerConfig(ModConfig.Type.COMMON, ImmortalStorageConfig.SPEC, "immortalstorage-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ImmortalStorageClientConfig.SPEC, "immortalstorage-client.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.immortalstorage.immortalstorage.client.ClientSetup.init(modBus, fbus, modContainer);
        }

        modBus.addListener(ModNetwork::register);

        com.immortalstorage.immortalstorage.compat.CompatManager.initializeOptionalIntegrations(modBus);
        com.immortalstorage.immortalstorage.compat.CompatManager.logCompat();

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new com.immortalstorage.immortalstorage.event.CommonEvents());
        NeoForge.EVENT_BUS.register(new com.immortalstorage.immortalstorage.event.RealmTickRateEvents());
        NeoForge.EVENT_BUS.register(com.immortalstorage.immortalstorage.villager.ModTrades.class);
        NeoForge.EVENT_BUS.addListener(com.immortalstorage.immortalstorage.command.ImmortalStorageCommands::register);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.immortalstorage.immortalstorage.api.source.ImmortalStorageChargeProviders::freezeRegistration);
        event.enqueueWork(com.immortalstorage.immortalstorage.api.worldshard.WorldShardApi::freezeRegistration);
    }

    @SubscribeEvent
    public void onAddReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new com.immortalstorage.immortalstorage.worldshard.WorldShardMinerReloadListener(
                event.getRegistryAccess()));
        event.addListener(new com.immortalstorage.immortalstorage.worldshard.WorldShardLootReloadListener(
                event.getRegistryAccess()));
        event.addListener(new com.immortalstorage.immortalstorage.source.definition.SourceDefinitionReloadListener());
        event.addListener(new com.immortalstorage.immortalstorage.spiritfield.SimulatedSpiritFieldCropCatalog.ReloadListener());
    }

    @SubscribeEvent
    public void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes.rebuildPools(event.getServer());
        LOG.info("Rebuilt world shard miner ore catalogs from final server worldgen, generation {}",
                com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes.generation());
    }

    @SubscribeEvent
    public void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes.rebuildPools(
                event.getPlayerList().getServer());
        LOG.info("Rebuilt world shard miner ore catalogs after global datapack reload, generation {}. "
                        + "Worldgen biome-modifier changes still require a server restart to be fully reflected",
                com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes.generation());
    }
}
