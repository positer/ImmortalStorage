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
import com.immortalstorage.immortalstorage.client.render.XianqiaoManagerItemDecorator;
import com.immortalstorage.immortalstorage.client.screen.ImmortalFurnaceScreen;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.SourceVeinScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.TreasureBasinScreen;
import com.immortalstorage.immortalstorage.client.screen.SourceVeinManagerScreen;
import com.immortalstorage.immortalstorage.client.screen.StabilizedMiniatureImmortalRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.MiniatureImmortalRuinScreen;
import com.immortalstorage.immortalstorage.client.screen.SimulatedReincarnationFurnaceScreen;
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
        SpiritStaffBuildPreview.init(forgeBus);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.OneQiBeamRenderer::render);
    }

    private static void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
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
        e.register(ModMenus.IMMORTAL_FURNACE.get(), ImmortalFurnaceScreen::new);
        e.register(ModMenus.SIMULATED_REINCARNATION_FURNACE.get(), SimulatedReincarnationFurnaceScreen::new);
        e.register(ModMenus.SOURCE_VEIN.get(), SourceVeinScreen::new);
        e.register(ModMenus.TREASURE_BASIN.get(), TreasureBasinScreen::new);
        e.register(ModMenus.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerScreen::new);
        e.register(ModMenus.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), StabilizedMiniatureImmortalRuinScreen::new);
        e.register(ModMenus.MINIATURE_IMMORTAL_RUIN.get(), MiniatureImmortalRuinScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.XIANQIAO_MANAGER.get(), XianqiaoManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN.get(), SourceVeinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORLD_SHARD_MINER.get(), WorldShardMinerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.YUAN_LIGHT.get(), YuanLightRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MINIATURE_IMMORTAL_RUIN.get(), MiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), StabilizedMiniatureImmortalRuinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIMULATED_REINCARNATION_FURNACE.get(),
                com.immortalstorage.immortalstorage.client.render.SimulatedReincarnationFurnaceRenderer::new);
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
        event.register(ModBlocks.XIANQIAO_MANAGER.get().asItem(),
                XianqiaoManagerItemDecorator.INSTANCE);
    }

    private ClientSetup() {}
}
