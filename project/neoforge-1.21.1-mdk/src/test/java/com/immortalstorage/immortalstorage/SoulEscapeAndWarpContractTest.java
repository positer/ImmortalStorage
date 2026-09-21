package com.immortalstorage.immortalstorage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level contracts for the two 0.1.5 client actions: the stage-8 Soul
 * Escape key and the immortal artifact's off-hand Immortal Yuan warp. The
 * state machine itself is covered behaviourally by
 * {@code player/SoulEscapeStateTest}.
 */
final class SoulEscapeAndWarpContractTest {
    private static final List<String> NEW_KEYS = List.of(
            "key.immortalstorage.soul_escape",
            "message.immortalstorage.soul_escape.entered",
            "message.immortalstorage.soul_escape.exited",
            "message.immortalstorage.soul_escape.requires_stage",
            "message.immortalstorage.spirit_staff.warp_completed",
            "message.immortalstorage.spirit_staff.teleport_completed",
            "message.immortalstorage.spirit_staff.warp_no_safe_landing");

    @Test
    void soulEscapeIsARebindableKeyDrivenServerAction() throws IOException {
        Path project = locateProject();
        String keybinds = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/keybind/ImmortalStorageKeybinds.java"));
        String payloads = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/network/ModPayloads.java"));
        String network = Files.readString(project.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/network/ModNetwork.java"));

        assertTrue(keybinds.contains("SOUL_ESCAPE = new KeyMapping("));
        assertTrue(keybinds.contains("GLFW.GLFW_KEY_F7"));
        assertTrue(keybinds.contains("e.register(SOUL_ESCAPE)"));
        assertTrue(keybinds.contains("new ModPayloads.ToggleSoulEscape()"));
        assertTrue(payloads.contains("record ToggleSoulEscape()"));
        assertTrue(network.contains("registrar.playToServer(ModPayloads.ToggleSoulEscape.TYPE"));

        String handler = methodBody(network, "handleToggleSoulEscape(");
        assertTrue(handler.contains("SoulEscapeState.isActive(persistent)"));
        assertTrue(handler.contains("SoulEscapeState.finish(persistent)"));
        assertTrue(handler.contains("ImmortalStoragePlayerData.get(player).getStage()"));
        assertTrue(handler.contains("stage < SoulEscapeState.REQUIRED_STAGE"));
        assertTrue(handler.contains("SoulEscapeState.canStart(stage, false, player.isSpectator())"));
        assertTrue(handler.contains("player.gameMode.getGameModeForPlayer().getId()"));
        assertTrue(handler.contains("GameType.SPECTATOR"));
        assertFalse(handler.contains("hasPermission"),
                "Soul Escape must not be gated behind operator permissions");
        assertFalse(handler.contains("isHardcore"),
                "Soul Escape must stay available in hardcore and cheat-free worlds");
    }

    @Test
    void warpTravelRequiresTheTravelModeOffhandImmortalYuanAndDoesNotSpendOnFailure()
            throws IOException {
        String item = Files.readString(locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java"));
        String teleport = methodBody(item, "teleport(Level level");

        assertTrue(teleport.contains("stack.getItem() instanceof ImmortalArtifactItem"));
        assertTrue(teleport.contains("getMode(stack) == MODE_TELEPORT"),
                "the warp belongs to the immortal artifact's travel mode");
        assertTrue(teleport.contains("offhand.is(ModItems.IMMORTAL_YUAN.get())"));
        assertTrue(teleport.contains("yuan * 100"));
        assertTrue(teleport.contains("findHighestSafeLanding"));
        assertTrue(teleport.indexOf("warp_no_safe_landing") < teleport.indexOf("offhand.shrink(yuan)"),
                "a cancelled warp must not consume the off-hand stack");
        assertTrue(teleport.contains("warp_completed"));
        assertTrue(teleport.contains("teleport_completed"),
                "the ordinary teleport needs its own completion message");
        assertTrue(teleport.contains("prepareWarpLanding(serverPlayer, destination)"),
                "only the warp prepares its landing area");
        assertTrue(teleport.contains("finishMove(serverPlayer, destination)"),
                "both teleports must share the cheap final move");
    }

    @Test
    void warpPinsItsLandingWhileTheOrdinaryTeleportStaysChunkFree() throws IOException {
        String item = Files.readString(locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java"));
        String warp = methodBody(item, "prepareWarpLanding(");
        String ordinary = methodBody(item, "finishMove(");

        assertTrue(warp.contains("ServerLevel level ="));
        assertTrue(warp.contains("level.getChunk(landing.getX(), landing.getZ())"),
                "the landing column must be generated before the move");
        assertTrue(warp.contains("addRegionTicket(TicketType.PORTAL, landingChunk, 2, landing)"),
                "the landing area must stay loaded while the client receives chunks");
        assertTrue(ordinary.contains("serverPlayer.teleportTo("));
        assertTrue(ordinary.contains("serverPlayer.setOnGround(true)"));
        assertFalse(ordinary.contains("getChunk"),
                "the ordinary teleport must not generate chunks for a short hop");
        assertFalse(ordinary.contains("addRegionTicket"),
                "the ordinary teleport must not pin chunks");
        assertTrue(teleportBranchOrder(item),
                "the warp landing preparation must sit inside the travel branch only");
    }

    private static boolean teleportBranchOrder(String item) {
        String teleport = methodBody(item, "teleport(Level level");
        int travel = teleport.indexOf("if (travel) {");
        int prepare = teleport.indexOf("prepareWarpLanding(");
        int shared = teleport.indexOf("finishMove(serverPlayer, destination)");
        return travel >= 0 && prepare > travel && shared > prepare;
    }

    @Test
    void landingSearchAcceptsOnlyAirOverSturdyDryGround() throws IOException {
        String item = Files.readString(locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java"));
        String search = methodBody(item, "findHighestSafeLanding(");

        assertTrue(search.contains("getFluidState(feet).isEmpty()"));
        assertTrue(search.contains("getFluidState(feet.above()).isEmpty()"));
        assertTrue(search.contains("feetState.isAir()"));
        assertTrue(search.contains("isFaceSturdy(level, ground, Direction.UP)"));
        // Compare the stable receiver prefixes: the 26.1.2 migration rewrites
        // getMaxBuildHeight()/getMinBuildHeight() into getMaxY()/getMinY().
        assertTrue(search.indexOf("level.getMax") < search.indexOf("level.getMin"),
                "the search must start at the build limit and walk downwards");
        assertTrue(search.contains("y--"), "the column scan must descend");
    }

    @Test
    void bothLocalesDocumentTheNewControlsAndMessages() throws IOException {
        Path resources = locateProject().resolve("src/main/resources/assets/immortalstorage");
        JsonObject en = JsonParser.parseString(Files.readString(resources.resolve("lang/en_us.json")))
                .getAsJsonObject();
        JsonObject zh = JsonParser.parseString(Files.readString(resources.resolve("lang/zh_cn.json")))
                .getAsJsonObject();
        for (JsonObject locale : new JsonObject[]{en, zh}) {
            for (String key : NEW_KEYS) {
                assertTrue(locale.has(key), "missing locale entry " + key);
                assertFalse(locale.get(key).getAsString().isBlank(), "blank locale entry " + key);
            }
        }
        assertEquals("灵魂出窍", zh.get("key.immortalstorage.soul_escape").getAsString());
        assertEquals("Soul Escape", en.get("key.immortalstorage.soul_escape").getAsString());
    }

    /**
     * Slices one method out of a source file. The marker deliberately stops
     * before the return type: the 26.1.2 migration rewrites
     * {@code InteractionResultHolder} into {@code InteractionResult}, and this
     * contract also runs against the migrated test tree.
     */
    private static String methodBody(String source, String marker) {
        String normalized = source.replace("\r\n", "\n");
        int hit = -1;
        int searchFrom = 0;
        while (true) {
            int candidate = normalized.indexOf(marker, searchFrom);
            if (candidate < 0) break;
            String beforeMarker = normalized.substring(normalized.lastIndexOf('\n', candidate) + 1, candidate).trim();
            if (beforeMarker.startsWith("private static ") || beforeMarker.startsWith("public static ")) {
                hit = candidate;
                break;
            }
            searchFrom = candidate + marker.length();
        }
        assertTrue(hit >= 0, "missing method " + marker);
        int start = normalized.lastIndexOf('\n', hit) + 1;
        int next = normalized.indexOf("\n    private static ", hit);
        int nextPublic = normalized.indexOf("\n    public static ", hit);
        int end = next < 0 ? nextPublic : (nextPublic < 0 ? next : Math.min(next, nextPublic));
        return end < 0 ? normalized.substring(start) : normalized.substring(start, end);
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
