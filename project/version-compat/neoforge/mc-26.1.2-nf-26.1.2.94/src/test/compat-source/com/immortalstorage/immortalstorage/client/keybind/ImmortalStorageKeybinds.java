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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ImmortalStorageKeybinds {
    private static final net.minecraft.client.KeyMapping.Category IMMORTALSTORAGE_CATEGORY =
            net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("immortalstorage", "immortalstorage"));
    public static final KeyMapping OPEN_STORAGE = new KeyMapping(
            "key.immortalstorage.open_storage", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, IMMORTALSTORAGE_CATEGORY);
    public static final KeyMapping OPEN_LINGQI_OR_REALM = new KeyMapping(
            "key.immortalstorage.lingqi_or_realm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, IMMORTALSTORAGE_CATEGORY);
    public static final KeyMapping TIME_FLOW = new KeyMapping(
            "key.immortalstorage.time_flow", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, IMMORTALSTORAGE_CATEGORY);
    public static final KeyMapping SPECIAL_OPERATION = new KeyMapping(
            "key.immortalstorage.special_operation", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT, IMMORTALSTORAGE_CATEGORY);

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
                ClientPacketDistributor.sendToServer(new ModPayloads.ToggleRealm());
            } else if (d.getStage() >= 1) {
                int cur = d.getLingqiProgress();
                int cap = d.getLingqiCap();
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.literal("Lingqi: " + cur + " / " + cap)
                        .withStyle(ChatFormatting.AQUA), true);
            } else {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.literal("Not cultivating yet").withStyle(ChatFormatting.RED), true);
            }
        }

        if (TIME_FLOW.consumeClick()) {
            if (d.getStage() < 7) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.literal("Stage 7+ required to adjust time flow")
                        .withStyle(ChatFormatting.RED), true);
            } else {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(p, Component.translatable("container.immortalstorage.terminal.time_open_realm")
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
        if (!OPEN_STORAGE.matches(new net.minecraft.client.input.KeyEvent(event.getKeyCode(), event.getScanCode(), 0))) return;
        requestStorageOpen(mc.player, ImmortalStoragePlayerData.get(mc.player), event.getScreen());
        event.setCanceled(true);
    }

    private static void requestStorageOpen(Player player, ImmortalStoragePlayerData data,
                                           net.minecraft.client.gui.screens.Screen returnScreen) {
        if (data.getStage() < 1) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.not_cultivating")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        com.immortalstorage.immortalstorage.client.screen.TerminalReturnNavigation.arm(returnScreen);
        if (data.getStage() >= 6) {
            ClientPacketDistributor.sendToServer(new ModPayloads.OpenXianqiaoStorage());
        } else {
            ClientPacketDistributor.sendToServer(new ModPayloads.OpenKongqiao());
        }
    }

    private static boolean hasShiftDown() {
        var h = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(h, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(h, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @SubscribeEvent
    public static void onMouseScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack main = mc.player.getMainHandItem();
        if (!(main.getItem() instanceof SpiritStaffItem)) return;
        int delta = e.getScrollDeltaY() > 0 ? 1 : -1;
        if (SPECIAL_OPERATION.isDown() && SpiritStaffItem.getMode(main) == SpiritStaffItem.MODE_TELEPORT) {
            ClientPacketDistributor.sendToServer(new ModPayloads.AdjustStaffTeleportDistance(delta));
        } else if (hasShiftDown()) {
            ClientPacketDistributor.sendToServer(new ModPayloads.CycleStaffMode(delta));
        } else return;
        e.setCanceled(true);
    }

    private ImmortalStorageKeybinds() {}
}
