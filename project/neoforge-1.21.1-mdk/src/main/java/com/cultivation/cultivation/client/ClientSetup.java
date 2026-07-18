package com.cultivation.cultivation.client;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.client.keybind.CultivationKeybinds;
import com.cultivation.cultivation.client.render.SourceVeinRenderer;
import com.cultivation.cultivation.client.render.SourceVeinManagerRenderer;
import com.cultivation.cultivation.client.render.SpiritStaffBuildPreview;
import com.cultivation.cultivation.client.render.XianqiaoManagerRenderer;
import com.cultivation.cultivation.client.render.WorldShardMinerRenderer;
import com.cultivation.cultivation.client.render.YuanLightRenderer;
import com.cultivation.cultivation.client.screen.ImmortalFurnaceScreen;
import com.cultivation.cultivation.client.screen.KongqiaoScreen;
import com.cultivation.cultivation.client.screen.SourceVeinScreen;
import com.cultivation.cultivation.client.screen.XianqiaoStorageScreen;
import com.cultivation.cultivation.client.screen.XianqiaoInterfaceScreen;
import com.cultivation.cultivation.client.screen.TreasureBasinScreen;
import com.cultivation.cultivation.client.screen.SourceVeinManagerScreen;
import com.cultivation.cultivation.block.entity.ModBlockEntities;
import com.cultivation.cultivation.item.ModItems;
import com.cultivation.cultivation.item.custom.SpiritStaffItem;
import com.cultivation.cultivation.menu.ModMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        CultivationKeybinds.init(modBus, forgeBus);
        forgeBus.addListener(ClientItemTooltips::onTooltip);
        SpiritStaffBuildPreview.init(forgeBus);
    }

    private static void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> ItemProperties.register(
                ModItems.SPIRIT_STAFF.get(),
                ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, "staff_mode"),
                (stack, level, entity, seed) -> SpiritStaffItem.getMode(stack)));
    }

    private static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(ModMenus.KONGQIAO.get(), KongqiaoScreen::new);
        e.register(ModMenus.XIANQIAO_STORAGE.get(), XianqiaoStorageScreen::new);
        e.register(ModMenus.XIANQIAO_INTERFACE.get(), XianqiaoInterfaceScreen::new);
        e.register(ModMenus.IMMORTAL_FURNACE.get(), ImmortalFurnaceScreen::new);
        e.register(ModMenus.SOURCE_VEIN.get(), SourceVeinScreen::new);
        e.register(ModMenus.TREASURE_BASIN.get(), TreasureBasinScreen::new);
        e.register(ModMenus.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.XIANQIAO_MANAGER.get(), XianqiaoManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN.get(), SourceVeinRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOURCE_VEIN_MANAGER.get(), SourceVeinManagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORLD_SHARD_MINER.get(), WorldShardMinerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.YUAN_LIGHT.get(), YuanLightRenderer::new);
    }

    private ClientSetup() {}
}
