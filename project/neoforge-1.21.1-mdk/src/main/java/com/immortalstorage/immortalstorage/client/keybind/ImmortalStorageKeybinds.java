package com.immortalstorage.immortalstorage.client.keybind;

import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ImmortalStorageKeybinds {
    private static boolean jumpWasDown;
    /**
     * Mirrors vanilla's {@code LocalPlayer#jumpTriggerTime}: while it is
     * counting down, the next jump press is the second half of the flight
     * double-tap and must stay with the game.
     */
    private static int jumpFlightToggleTicks;
    private static final DoubleTapDetector FORWARD_BOOST = new DoubleTapDetector();
    private static Player boostPlayer;
    public static final KeyMapping OPEN_STORAGE = new KeyMapping(
            "key.immortalstorage.open_storage", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.immortalstorage");
    /** Toggles realm entry at stage 6+; shift+V expands/collapses the domain or
     *  teleports to the realm center depending on the current dimension. */
    public static final KeyMapping OPEN_LINGQI_OR_REALM = new KeyMapping(
            "key.immortalstorage.lingqi_or_realm", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.immortalstorage");
    public static final KeyMapping TIME_FLOW = new KeyMapping(
            "key.immortalstorage.time_flow", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_S, "key.categories.immortalstorage");
    public static final KeyMapping SPECIAL_OPERATION = new KeyMapping(
            "key.immortalstorage.special_operation", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.immortalstorage");
    /** Shows the stage-1..5 lingqi progress (and a stage 6+ summary) on request. */
    public static final KeyMapping SHOW_PROGRESS = new KeyMapping(
            "key.immortalstorage.show_progress", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, "key.categories.immortalstorage");
    public static final KeyMapping SOUL_ESCAPE = new KeyMapping("key.immortalstorage.soul_escape", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.immortalstorage");

    public static void init(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(ImmortalStorageKeybinds::registerKeys);
        forgeBus.addListener(ImmortalStorageKeybinds::onClientTickPre);
        forgeBus.addListener(ImmortalStorageKeybinds::onClientTickPost);
        forgeBus.addListener(ImmortalStorageKeybinds::onKeyInput);
        forgeBus.addListener(ImmortalStorageKeybinds::onScreenKeyPressed);
        forgeBus.addListener(ImmortalStorageKeybinds::onMouseScroll);
    }

    private static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_STORAGE);
        e.register(OPEN_LINGQI_OR_REALM);
        e.register(TIME_FLOW);
        e.register(SPECIAL_OPERATION);
        e.register(SHOW_PROGRESS);
        e.register(SOUL_ESCAPE);
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        updateBoostContext(mc);
        if (jumpFlightToggleTicks > 0) jumpFlightToggleTicks--;
        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;
        if (!jumpPressed || mc.player == null || mc.screen != null) return;

        Player player = mc.player;
        // Pre runs before vanilla turns a grounded jump into an airborne state.
        // That press remains an ordinary jump; a later airborne press toggles flight.
        if (player.onGround()) return;

        // Vanilla spends the second press of a double-tap on the flight toggle
        // (LocalPlayer#aiStep sets flag9 there) and never on an elytra. Replicating
        // that window is what keeps creative and cultivation flight working while
        // the aura guard is worn.
        if (player.getAbilities().mayfly && !player.isSwimming()) {
            boolean flightTogglePress = jumpFlightToggleTicks > 0;
            jumpFlightToggleTicks = flightTogglePress ? 0 : 7;
            if (flightTogglePress) return;
        }

        boolean virtualExpanded = player.isFallFlying()
                && player.getPersistentData().getBoolean("ImmortalStorageVirtualElytra");
        if (virtualExpanded) {
            player.stopFallFlying();
            player.getPersistentData().remove("ImmortalStorageVirtualElytra");
            PacketDistributor.sendToServer(new ModPayloads.AuraGuardFlightState(false));
            return;
        }
        // Deploy conditions copied from vanilla's own elytra branch
        // (LocalPlayer#aiStep): airborne, not flying, not riding, not climbing,
        // no levitation. The aura guard replaces the chest-slot elytra.
        if (!player.isFallFlying() && !player.isPassenger() && !player.isInWater()
                && !player.onClimbable()
                && !player.getAbilities().flying
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)
                && ImmortalMasterTalismanService.hasAuraGuard(player)
                && player.tryToStartFallFlying()) {
            PacketDistributor.sendToServer(new ModPayloads.AuraGuardFlightState(true));
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Player p = mc.player;
        SpiritStaffItem.reconcileArtifactWrenchReach(p);
        ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(p);

        if (OPEN_STORAGE.consumeClick()) requestStorageOpen(p, d, null);

        if (OPEN_LINGQI_OR_REALM.consumeClick()) handleRealmOrDomain(p, d);

        if (SHOW_PROGRESS.consumeClick()) showProgress(p, d);
        // The server is the authority for the stage gate and the saved game
        // mode. Sending the intent even after a stage change also lets a
        // player leave a previously active Soul Escape state safely.
        if (SOUL_ESCAPE.consumeClick()) PacketDistributor.sendToServer(new ModPayloads.ToggleSoulEscape());

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

    /**
     * The realm-toggle bind (V).  shift+V expands/collapses the Domain outside
     * the realm or teleports to the realm center inside it; plain V toggles
     * realm entry at stage 6+.
     */
    private static void handleRealmOrDomain(Player p, ImmortalStoragePlayerData d) {
        if (hasShiftDown()) {
            if (inXianqiaoRealm(p)) {
                PacketDistributor.sendToServer(new ModPayloads.RealmCenterTeleport());
            } else {
                PacketDistributor.sendToServer(new ModPayloads.DomainToggle());
                com.immortalstorage.immortalstorage.client.render.DomainExpansionHighlight.toggle();
            }
            return;
        }
        if (d.getStage() >= 6) {
            PacketDistributor.sendToServer(new ModPayloads.ToggleRealm());
            return;
        }
        // Pre-ascension plain-V no longer prints lingqi; point the player at T.
        if (d.getStage() >= 1) {
            p.displayClientMessage(Component.literal("Press T for your cultivation progress")
                    .withStyle(ChatFormatting.AQUA), true);
        } else {
            p.displayClientMessage(Component.literal("Not cultivating yet").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void showProgress(Player p, ImmortalStoragePlayerData d) {
        if (d.getStage() >= 1 && d.getStage() <= 5) {
            int cur = d.getLingqiProgress();
            int cap = d.getLingqiCap();
            p.displayClientMessage(Component.literal("Lingqi: " + cur + " / " + cap)
                    .withStyle(ChatFormatting.AQUA), true);
        } else if (d.getStage() >= 6) {
            p.displayClientMessage(Component.literal("Stage " + d.getStage())
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            p.displayClientMessage(Component.literal("Not cultivating yet").withStyle(ChatFormatting.RED), true);
        }
    }

    private static boolean inXianqiaoRealm(Player p) {
        ResourceLocation loc = p.level().dimension().location();
        return "immortalstorage".equals(loc.getNamespace())
                && loc.getPath().startsWith("xianqiao_realm/");
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
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (!updateBoostContext(mc)
                || !mc.options.keyUp.matches(event.getKey(), event.getScanCode())) return;
        // Use actual press/release events so quick taps between ticks are not lost.
        // Keyboard auto-repeat never counts as another tap or consumes vanilla input.
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_RELEASE) return;
        if (FORWARD_BOOST.update(event.getAction() == GLFW.GLFW_PRESS, System.nanoTime())) {
            PacketDistributor.sendToServer(new ModPayloads.AuraGuardBoost());
        }
    }

    private static boolean updateBoostContext(Minecraft mc) {
        Player player = mc.player;
        boolean eligible = player != null && mc.screen == null && mc.isWindowActive() && !mc.isPaused()
                && player.isFallFlying()
                && player.getPersistentData().getBoolean("ImmortalStorageVirtualElytra");
        if (player != boostPlayer || !eligible) FORWARD_BOOST.reset();
        boostPlayer = player;
        return eligible;
    }

    @SubscribeEvent
    public static void onMouseScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack main = mc.player.getMainHandItem();
        if (!(main.getItem() instanceof SpiritStaffItem)) return;
        int delta = e.getScrollDeltaY() > 0 ? 1 : -1;
        if (SPECIAL_OPERATION.isDown() && SpiritStaffItem.getMode(main) == SpiritStaffItem.MODE_TELEPORT) {
            PacketDistributor.sendToServer(new ModPayloads.AdjustStaffTeleportDistance(delta));
        } else if (hasShiftDown()) {
            PacketDistributor.sendToServer(new ModPayloads.CycleStaffMode(delta));
        } else return;
        e.setCanceled(true);
    }

    private ImmortalStorageKeybinds() {}
}
