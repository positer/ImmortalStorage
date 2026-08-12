package com.immortalstorage.immortalstorage.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Architectural regression tests for the server-authoritative Xianqiao boundary. */
final class XianqiaoAccessBoundaryTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void everyXianqiaoPayloadPathDelegatesToTheLiveMenuGuard() throws IOException {
        String network = source("network", "ModNetwork.java");
        for (String method : List.of(
                "handleTriggerTribulation", "handleTimeFlow", "handleSetStorageModule",
                "handleSetTerminalViewport", "handleSetTerminalQuery", "handleTerminalEntryAction",
                "handleSetTerminalChannel", "handleTerminalFluidEntryAction",
                "handleTerminalExternalResourceEntryAction", "handleTransferTerminalRecipe")) {
            assertTrue(methodBody(network, "private static void " + method + "(").contains("hasLiveXianqiaoMenu"),
                    () -> method + " must reject a stale or debug-downgraded Xianqiao menu");
        }
    }

    @Test
    void everyItemMutationPathChecksTheLiveMenuGuard() throws IOException {
        String menu = source("menu", "custom", "XianqiaoStorageMenu.java");
        assertTrue(menu.contains("boolean hasLiveTerminalAccess(Player actor)"));
        for (String signature : List.of(
                "public ItemStack quickMoveStack(", "public void clicked(",
                "public boolean handleEntryAction(", "public boolean clickMenuButton(",
                "public boolean transferCraftingRecipe(",
                "public boolean handleExternalResourceContainerAction(")) {
            String body = methodBody(menu, signature);
            assertTrue(body.contains("hasLiveTerminalAccess") || body.contains("hasLiveExternalAccess"),
                    () -> signature + " must reject item mutation after a debug downgrade");
        }
    }

    @Test
    void managerCapabilitiesUseOnlyTheContinuouslyRealmBoundResolver() throws IOException {
        String manager = source("block", "entity", "XianqiaoManagerBlockEntity.java");
        assertTrue(methodBody(manager, "public @Nullable PersonalStorageEndpoint storageEndpoint()")
                .contains("currentEndpoint()"));
        assertTrue(methodBody(manager, "private @Nullable PersonalStorageEndpoint fluidStorageEndpoint()")
                .contains("currentEndpoint()"));
        String currentEndpoint = methodBody(manager,
                "private @Nullable PersonalStorageNetwork.Endpoint currentEndpoint()");
        assertTrue(currentEndpoint.contains("PersonalStorageNetwork.resolveInOwnerRealm("));
        assertTrue(occurrences(manager, "PersonalStorageNetwork.resolveInOwnerRealm(") == 1,
                "item and fluid lookups must share the one continuously realm-bound resolver path");
        assertTrue(currentEndpoint.contains("PersistentPlayerIdentity.onlinePlayer"));
        assertTrue(currentEndpoint.contains("cachedEndpoint.data() == data"));
        assertTrue(currentEndpoint.contains("cachedEndpoint.itemHandler().getSlots() > 0"),
                "a cached endpoint must re-run its live stage/owner access predicate");
        assertTrue(currentEndpoint.contains("(cachedEndpoint.fluidHandler() != null) == needsFluids"),
                "a cached endpoint must be replaced when the stage-seven fluid layout changes");
        assertTrue(methodBody(manager,
                "public static void serverTick(ServerLevel level, BlockPos pos, XianqiaoManagerBlockEntity manager)")
                .contains("refreshCapabilityLayout"));
        assertTrue(methodBody(manager, "private void refreshCapabilityLayout(ServerLevel serverLevel)")
                .contains("invalidateCapabilities(worldPosition)"),
                "NeoForge capability caches must be invalidated when owner or stage layout changes");
        String managerBlock = source("block", "custom", "XianqiaoManagerBlock.java");
        assertTrue(methodBody(managerBlock,
                "public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(")
                .contains("XianqiaoManagerBlockEntity.serverTick"));
        assertFalse(manager.contains("PersonalStorageNetwork.resolveWithFluids("));
        assertFalse(manager.contains("PersonalStorageNetwork.resolve(serverLevel.getServer()"));
    }

    @Test
    void fluidUnlockBoundaryIsStageSevenAcrossDataMenuAndStandardEndpoints() throws IOException {
        String data = source("player", "ImmortalStoragePlayerData.java");
        assertTrue(data.contains("public static final int XIANQIAO_FLUID_UNLOCK_STAGE = 7;"));
        assertTrue(data.contains("public static final int XIANQIAO_EXTERNAL_UNLOCK_STAGE = 8;"));
        assertTrue(methodBody(data, "public long insertXianqiaoFluid(")
                .contains("XIANQIAO_FLUID_UNLOCK_STAGE"));
        assertTrue(methodBody(data, "public long extractXianqiaoFluid(")
                .contains("XIANQIAO_FLUID_UNLOCK_STAGE"));

        String handler = source("network", "storage", "PersonalStorageFluidHandler.java");
        assertTrue(methodBody(handler, "private boolean isAvailable()")
                .contains("ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE"),
                "a previously acquired IFluidHandler must recheck the live stage on every call");

        String network = source("network", "storage", "PersonalStorageNetwork.java");
        assertTrue(methodBody(network, "private static @Nullable Endpoint resolveWithFluids(")
                .contains("ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE"),
                "public fluid endpoint lookup must reject stage six");
        assertTrue(methodBody(network, "public static @Nullable Endpoint resolveInOwnerRealm(")
                .contains("ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE"),
                "realm-bound managers must expose items at stage six but add fluids only at stage seven");
        assertTrue(methodBody(network, "public static @Nullable Endpoint resolveInOwnerRealm(")
                .contains("data.getStage() >= 6"),
                "a stale manager endpoint must also stop item access below the Xianqiao boundary");

        String menu = source("menu", "custom", "XianqiaoStorageMenu.java");
        assertTrue(menu.contains("() -> data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE"));
        assertTrue(methodBody(menu, "public boolean hasLiveFluidAccess(")
                .contains("ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE"));
        assertTrue(methodBody(menu, "public boolean handleFluidContainerAction(")
                .contains("hasLiveFluidAccess"),
                "a menu opened at stage seven must reject fluid actions after a debug downgrade");

        String payloads = source("network", "ModNetwork.java");
        assertTrue(methodBody(payloads, "private static void handleTerminalFluidEntryAction(")
                .contains("hasLiveFluidAccess"),
                "the fluid payload must reject a stale stage-seven menu before revision or container work");
        assertTrue(methodBody(payloads, "private static void handleTerminalExternalResourceEntryAction(")
                .contains("hasLiveExternalAccess"),
                "the external-resource payload must reject a stale stage-eight menu before container work");
        assertTrue(methodBody(menu, "public boolean hasLiveExternalAccess(")
                .contains("XIANQIAO_EXTERNAL_UNLOCK_STAGE"));
    }

    @Test
    void xianqiaoResolverRechecksOwnerAndStageOnEveryOperation() throws IOException {
        String api = source("api", "storage", "PersonalStorageApi.java");
        assertTrue(api.contains("resolveXianqiao("),
                "ordinary Xianqiao-bound blocks need an explicit stage-six resolver");

        String network = source("network", "storage", "PersonalStorageNetwork.java");
        String resolver = methodBody(network, "public static @Nullable Endpoint resolveXianqiao(");
        assertTrue(resolver.contains("data.getStage() < 6"));
        assertTrue(resolver.contains("ImmortalStoragePlayerData.get(currentPlayer) == data"));
        assertTrue(resolver.contains("data.getStage() >= 6"));
    }

    private static String source(String... relative) throws IOException {
        Path path = MAIN;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources from "
                + Path.of("").toAbsolutePath());
    }

    private static String methodBody(String source, String signature) {
        int name = source.indexOf(signature);
        if (name < 0) return "";
        int opening = source.indexOf('{', name);
        if (opening < 0) return "";
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
