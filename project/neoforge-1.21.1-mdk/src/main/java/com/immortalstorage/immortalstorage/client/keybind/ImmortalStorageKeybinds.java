package com.immortalstorage.immortalstorage.client.keybind;

import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ImmortalStorageKeybinds {
    public static final KeyMapping OPEN_STORAGE = new KeyMapping(
            "key.immortalstorage.open_storage", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.immortalstorage");
    public static final KeyMapping OPEN_LINGQI_OR_REALM = new KeyMapping(
            "key.immortalstorage.lingqi_or_realm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.immortalstorage");
    public static final KeyMapping TIME_FLOW = new KeyMapping(
            "key.immortalstorage.time_flow", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, "key.categories.immortalstorage");
    public static final KeyMapping SPECIAL_OPERATION = new KeyMapping(
            "key.immortalstorage.special_operation", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.immortalstorage");

    public static void init(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(ImmortalStorageKeybinds::registerKeys);
        forgeBus.addListener(ImmortalStorageKeybinds::onClientTick);
        forgeBus.addListener(ImmortalStorageKeybinds::onScreenKeyPressed);
        forgeBus.addListener(ImmortalStorageKeybinds::onMouseScroll);
    }

    private static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_STORAGE);
        e.register(OPEN_LINGQI_OR_REALM);
        e.register(TIME_FLOW);
        e.register(SPECIAL_OPERATION);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Player p = mc.player;
        ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);

        if (OPEN_STORAGE.consumeClick()) requestStorageOpen(p, d, null);

        if (OPEN_LINGQI_OR_REALM.consumeClick()) {
            if (d.getStage() >= 6) {
                PacketDistributor.sendToServer(new ModPayloads.ToggleRealm());
            } else if (d.getStage() >= 1) {
                int cur = d.getLingqiProgress();
                int cap = d.getLingqiCap();
                p.displayClientMessage(Component.literal("Lingqi: " + cur + " / " + cap)
                        .withStyle(ChatFormatting.AQUA), true);
            } else {
                p.displayClientMessage(Component.literal("Not cultivating yet").withStyle(ChatFormatting.RED), true);
            }
        }

        if (TIME_FLOW.consumeClick()) {
            if (d.getStage() < 7) {
                p.displayClientMessage(Component.literal("Stage 7+ required to adjust time flow")
                        .withStyle(ChatFormatting.RED), true);
            } else {
                p.displayClientMessage(Component.translatable("container.immortalstorage.terminal.time_open_realm")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getScreen() instanceof com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess) {
            return;
        }
        if (event.getScreen() instanceof ChatScreen || event.getScreen() instanceof ControlsScreen
                || event.getScreen().getFocused() instanceof EditBox) return;
        if (!OPEN_STORAGE.matches(event.getKeyCode(), event.getScanCode())) return;
        requestStorageOpen(mc.player, ImmortalStoragePlayerData.get(mc.player), event.getScreen());
        event.setCanceled(true);
    }

    private static void requestStorageOpen(Player player, ImmortalStoragePlayerData data,
                                           net.minecraft.client.gui.screens.Screen returnScreen) {
        if (data.getStage() < 1) {
            player.displayClientMessage(Component.translatable("message.immortalstorage.not_cultivating")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        com.immortalstorage.immortalstorage.client.screen.TerminalReturnNavigation.arm(returnScreen);
        if (data.getStage() >= 6) {
            PacketDistributor.sendToServer(new ModPayloads.OpenXianqiaoStorage());
        } else {
            PacketDistributor.sendToServer(new ModPayloads.OpenKongqiao());
        }
    }

    private static boolean hasShiftDown() {
        long h = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(h, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(h, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @SubscribeEvent
    public static void onMouseScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !hasShiftDown()) return;
        ItemStack main = mc.player.getMainHandItem();
        if (!(main.getItem() instanceof SpiritStaffItem)) return;
        int delta = e.getScrollDeltaY() > 0 ? 1 : -1;
        PacketDistributor.sendToServer(new ModPayloads.CycleStaffMode(delta));
        e.setCanceled(true);
    }

    private ImmortalStorageKeybinds() {}
}
