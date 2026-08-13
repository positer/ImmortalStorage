package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.client.keybind.ImmortalStorageKeybinds;
import com.immortalstorage.immortalstorage.client.render.SourceVeinRenderer;
import com.immortalstorage.immortalstorage.client.render.SourceVeinManagerRenderer;
import com.immortalstorage.immortalstorage.client.render.SpiritStaffBuildPreview;
import com.immortalstorage.immortalstorage.client.render.XianqiaoManagerRenderer;
import com.immortalstorage.immortalstorage.client.render.WorldShardMinerRenderer;
import com.immortalstorage.immortalstorage.client.render.YuanLightRenderer;
import com.immortalstorage.immortalstorage.client.render.MiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.RuinCoreItemDecorator;
import com.immortalstorage.immortalstorage.client.render.SourceVeinOutputDecorator;
import com.immortalstorage.immortalstorage.client.render.StabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.EntangledStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.AdvancedStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.AdvancedEntangledStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.EntangledRuinCoreItemDecorator;
import com.immortalstorage.immortalstorage.client.render.SimulatedSpiritFieldItemDecorator;
import com.immortalstorage.immortalstorage.client.render.XianqiaoManagerItemDecorator;
import com.immortalstorage.immortalstorage.client.render.AdvancedXianqiaoInterfaceRenderer;
import com.immortalstorage.immortalstorage.client.screen.ImmortalFurnaceScreen;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.SourceVeinScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.AdvancedXianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.TreasureBasinScreen;
import com.immortalstorage.immortalstorage.client.screen.WorldShardMinerScreen;
import com.immortalstorage.immortalstorage.client.screen.SourceVeinManagerScreen;
import com.immortalstorage.immortalstorage.client.screen.StabilizedMiniatureImmortalRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.MiniatureImmortalRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.EntangledMiniatureRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.AdvancedStabilizedMiniatureImmortalRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.AdvancedEntangledMiniatureRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.SimulatedReincarnationFurnaceScreen;
import com.immortalstorage.immortalstorage.client.screen.SimulatedSpiritFieldScreen;
import com.immortalstorage.immortalstorage.client.screen.EnergyCrystalScreen;
import com.immortalstorage.immortalstorage.client.render.SimulatedSpiritFieldRenderer;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    public static void init(IEventBus modBus, IEventBus forgeBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(container, parent));
        modBus.addListener(ClientSetup::clientSetup);
        modBus.addListener(ClientSetup::registerScreens);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerAdditionalModels);
        modBus.addListener(ClientSetup::registerItemDecorations);
        ImmortalStorageKeybinds.init(modBus, forgeBus);
        forgeBus.addListener(ClientItemTooltips::onTooltip);
        forgeBus.addListener(ClientRealmSnowfall::onClientTick);
        SpiritStaffBuildPreview.init(forgeBus);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.OneQiBeamRenderer::render);
    }

    private static void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.ENERGY_CRYSTAL.get(), net.minecraft.client.renderer.RenderType.translucent());
            if (ModBlocks.MANA_CRYSTAL != null) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        ModBlocks.MANA_CRYSTAL.get(), net.minecraft.client.renderer.RenderType.translucent());
            }
            if (ModBlocks.SOURCE_CRYSTAL != null) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        ModBlocks.SOURCE_CRYSTAL.get(), net.minecraft.client.renderer.RenderType.translucent());
            }
            ItemProperties.register(
                    ModItems.SPIRIT_STAFF.get(),
                    ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "staff_mode"),
                    (stack, level, entity, seed) -> SpiritStaffItem.getMode(stack));
            com.immortalstorage.immortalstorage.compat.CompatManager.initializeClientIntegrations();
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(ModMenus.KONGQIAO.get(), KongqiaoScreen::new);
        e.register(ModMenus.XIANQIAO_STORAGE.get(), XianqiaoStorageScreen::new);
        e.register(ModMenus.XIANQIAO_INTERFACE.get(), XianqiaoInterfaceScreen::new);
        e.register(ModMenus.ADVANCED_XIANQIAO_INTERFACE.get(), AdvancedXianqiaoInterfaceScreen::new);
        e.register(ModMenus.IMMORTAL_FURNACE.get(), ImmortalFurnaceScreen::new);
        e.register(ModMenus.SIMULATED_REINCARNATION_FURNACE.get(), SimulatedReincarnationFurnaceScreen::new);
        e.register(ModMenus.SIMULATED_SPIRIT_FIELD.get(), SimulatedSpiritFieldScreen::new);
        e.register(ModMenus.ENERGY_CRYSTAL.get(), EnergyCrystalScreen::new);
        e.register(ModMenus.SOURCE_VEIN.get(), SourceVeinScreen::new);
        e.register(ModMenus.TREASURE_BASIN.get(), TreasureBasinScreen::new);
        e.register(ModMenus.WORLD_SHARD_MINER.get(), WorldShardMinerScreen::new);
        e.register(ModMenus.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerScreen::new);
        e.register(ModMenus.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(),
                (com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu menu,
                 net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.chat.Component title)
                        -> new StabilizedMiniatureImmortalRuinScreen<>(menu, inv, title));
        e.register(ModMenus.MINIATURE_IMMORTAL_RUIN.get(), MiniatureImmortalRuinScreen::new);
        e.register(ModMenus.ENTANGLED_MINIATURE_IMMORTAL_RUIN.get(), EntangledMiniatureRuinScreen::new);
        e.register(ModMenus.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), AdvancedStabilizedMiniatureImmortalRuinScreen::new);
        e.register(ModMenus.ADVANCED_ENTANGLED_MINIATURE_IMMORTAL_RUIN.get(), AdvancedEntangledMiniatureRuinScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.XIANQIAO_MANAGER.get(), XianqiaoManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_XIANQIAO_INTERFACE.get(), AdvancedXianqiaoInterfaceRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN.get(), SourceVeinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORLD_SHARD_MINER.get(), WorldShardMinerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.YUAN_LIGHT.get(), YuanLightRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MINIATURE_IMMORTAL_RUIN.get(), MiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), StabilizedMiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), EntangledStabilizedMiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), AdvancedStabilizedMiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), AdvancedEntangledStabilizedMiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIMULATED_REINCARNATION_FURNACE.get(),
                com.immortalstorage.immortalstorage.client.render.SimulatedReincarnationFurnaceRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIMULATED_SPIRIT_FIELD.get(),
                SimulatedSpiritFieldRenderer::new);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof com.immortalstorage.immortalstorage.item.SourceVeinBlockItem)) continue;
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            id.getNamespace(), "item/" + id.getPath() + "_base")));
        }
    }

    private static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.immortalstorage.immortalstorage.item.SourceVeinBlockItem) {
                event.register(item, SourceVeinOutputDecorator.INSTANCE);
            }
        }
        event.register(ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem(),
                RuinCoreItemDecorator.INSTANCE);
        event.register(ModBlocks.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem(),
                RuinCoreItemDecorator.INSTANCE);
        event.register(ModBlocks.ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem(),
                EntangledRuinCoreItemDecorator.INSTANCE);
        event.register(ModBlocks.ADVANCED_ENTANGLED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem(),
                EntangledRuinCoreItemDecorator.INSTANCE);
        event.register(ModBlocks.SIMULATED_SPIRIT_FIELD.get().asItem(),
                SimulatedSpiritFieldItemDecorator.INSTANCE);
        event.register(ModBlocks.XIANQIAO_MANAGER.get().asItem(),
                XianqiaoManagerItemDecorator.INSTANCE);
    }

    private ClientSetup() {}
}
