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
import com.immortalstorage.immortalstorage.client.render.SourceVeinOutputDecorator;
import com.immortalstorage.immortalstorage.client.render.StabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.EntangledStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.AdvancedStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.AdvancedEntangledStabilizedMiniatureImmortalRuinRenderer;
import com.immortalstorage.immortalstorage.client.render.AdvancedXianqiaoInterfaceRenderer;
import com.immortalstorage.immortalstorage.client.screen.ImmortalFurnaceScreen;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.SourceVeinScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.AdvancedXianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoRedstoneInterfaceScreen;
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
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    public static void init(IEventBus modBus, IEventBus forgeBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(container, parent));
        modBus.addListener(ClientSetup::clientSetup);
        modBus.addListener(ClientSetup::registerScreens);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(com.immortalstorage.immortalstorage.client.AuraGuardClientCompat::addLayers);
        modBus.addListener(com.immortalstorage.immortalstorage.client.AuraGuardClientCompat::registerStateModifier);
        modBus.addListener(ClientSetup::registerSpecialModelRenderers);
        modBus.addListener(ClientSetup::registerStaffModelProperties);
        modBus.addListener(ClientSetup::registerItemDecorations);
        ImmortalStorageKeybinds.init(modBus, forgeBus);
        forgeBus.register(com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.class);
        forgeBus.addListener(ClientItemTooltips::onTooltip);
        forgeBus.addListener(ClientRealmSnowfall::onClientTick);
        SpiritStaffBuildPreview.init(forgeBus);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.OneQiBeamRenderer::render);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.DomainExpansionHighlight::render);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.ToolLocatorRenderer::render);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.ToolLocatorClient::onTick);
        forgeBus.addListener(com.immortalstorage.immortalstorage.client.render.TimeShiftBorderRenderer::render);
        forgeBus.addListener(ClientSetup::onMouseScroll);
    }

    private static void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            com.immortalstorage.immortalstorage.compat.CompatManager.initializeClientIntegrations();
        });
    }

    private static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(ModMenus.KONGQIAO.get(), KongqiaoScreen::new);
        e.register(ModMenus.XIANQIAO_STORAGE.get(), XianqiaoStorageScreen::new);
        e.register(ModMenus.XIANQIAO_INTERFACE.get(), XianqiaoInterfaceScreen::new);
        e.register(ModMenus.ADVANCED_XIANQIAO_INTERFACE.get(), AdvancedXianqiaoInterfaceScreen::new);
        e.register(ModMenus.XIANQIAO_REDSTONE_INTERFACE.get(), XianqiaoRedstoneInterfaceScreen::new);
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
        e.register(ModMenus.SPIRIT_COMPRESSOR.get(), com.immortalstorage.immortalstorage.client.screen.SpiritCompressorScreen::new);
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
    private static void registerStaffModelProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "staff_mode"),
                SpiritStaffModeProperty.TYPE);
        event.register(Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "artifact_tool"),
                ImmortalArtifactToolProperty.TYPE);
    }

    private static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "world_block_item"),
                com.immortalstorage.immortalstorage.client.render.WorldBlockItemRenderer.Unbaked.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "source_vein"),
                com.immortalstorage.immortalstorage.client.render.SourceVeinItemRenderer.Unbaked.MAP_CODEC);
    }

    private static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.immortalstorage.immortalstorage.item.SourceVeinBlockItem) {
                event.register(item, SourceVeinOutputDecorator.INSTANCE);
            }
        }

    }

    /**
     * Sneak+scroll with the time-shift staff cycles its permanent tier on the
     * server.  The event is cancelled so the same scroll does not also move the
     * hotbar selection, exactly like the spirit staff's own scroll binding.
     */
    private static void onMouseScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isShiftKeyDown()) return;
        Item staff = ModItems.TIME_SHIFT_STAFF.get();
        if (player.getMainHandItem().getItem() != staff && player.getOffhandItem().getItem() != staff) return;
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new ModPayloads.TimeShiftTier(event.getScrollDeltaY() > 0.0D ? 1 : -1));
    }

    private ClientSetup() {}
}
