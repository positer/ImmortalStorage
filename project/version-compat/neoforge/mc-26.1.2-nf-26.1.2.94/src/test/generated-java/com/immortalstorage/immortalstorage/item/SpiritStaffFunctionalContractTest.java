package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the server-authoritative boundaries of all four Spirit Staff modes. */
final class SpiritStaffFunctionalContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void useOnDispatchesAllFourModesOnlyOnTheServer() throws IOException {
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String useOn = methodBody(staff, "public InteractionResult useOn(");

        assertTrue(useOn.contains("context.getLevel().isClientSide()"));
        assertTrue(useOn.contains("case MODE_EXPLORE -> InteractionResult.PASS"),
                "explore collection now runs only after the player's real menu page opens");
        assertTrue(useOn.contains("case MODE_WRENCH -> wrench"));
        assertTrue(useOn.contains("case MODE_PICK -> preciseHarvest"));
        assertTrue(useOn.contains("case MODE_BUILD -> build"));
    }

    @Test
    void exploreModeUsesHeldEnabledInstrumentAndTheOpenedPlayerMenuPage() throws IOException {
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String transfer = methodBody(staff, "public static void transferOpenedLootMenu(");
        String use = methodBody(staff, "public InteractionResult use(");
        String events = source("event", "CommonEvents.java");

        assertTrue(use.contains("getMode(stack) != MODE_EXPLORE"));
        assertTrue(use.contains("HitResult.Type.MISS"),
                "opening a block container must never toggle the air-use switch");
        assertTrue(use.contains("setExploreEnabled(stack, enabled)"));
        assertTrue(events.contains("PlayerContainerEvent.Open"));
        assertTrue(events.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).execute"),
                "menu contents are processed after the real player page has been established");
        assertTrue(events.contains("player.containerMenu == opened"));
        assertTrue(transfer.contains("enabledExploreInstrument(player)"));
        assertTrue(transfer.contains("isLootPageMenu(menu, player)"));
        assertTrue(transfer.contains("slot.container == player.getInventory()"));
        assertTrue(transfer.contains("OpenedLootSlotTransfer.move("));
        assertTrue(transfer.contains("endpoint::insert"));
        String transaction = source("item", "custom", "OpenedLootSlotTransfer.java");
        assertTrue(transaction.contains("target.insert(visible.copy(), true)"));
        assertTrue(transaction.contains("target.insert(offer, false)"));
        assertTrue(transaction.contains("source.restore("));
        assertFalse(transfer.contains("placeItemBackInInventory"),
                "uncommitted loot must remain on the opened player page");
        assertTrue(staff.contains("containerType.startsWith(\"noobanidus.mods.lootr.\")"),
                "custom Lootr MenuBuilder pages are recognized through their real per-player inventory");
        assertFalse(staff.contains("unpackLootTable(player)"),
                "the instrument must use the player's opened page, not unpack a shared block entity directly");
        assertFalse(staff.contains("Capabilities.Item.BLOCK"));
    }

    @Test
    void pickModeUsesVanillaBreakFlowAndRefundsYuanWhenBreakingIsDenied() throws IOException {
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String pick = methodBody(staff, "private static InteractionResult preciseHarvest(");

        assertTrue(pick.contains("consumeImmortalYuan(1L)"));
        assertTrue(pick.contains("Enchantments.SILK_TOUCH"));
        assertTrue(pick.contains("new ItemEnchantments.Mutable(ItemEnchantments.EMPTY)"),
                "right-click silk touch must ignore the instrument's anvil enchantments");
        assertTrue(pick.contains("player.gameMode.destroyBlock(pos)"),
                "protection events and block-specific drops must run through vanilla destruction");
        assertTrue(pick.contains("depositImmortalYuan(1L)"),
                "a denied vanilla break must refund the prepaid Immortal Yuan");
        assertFalse(pick.contains("Block.getDrops("));
        assertFalse(pick.contains("setBlock("));
        assertFalse(pick.contains("Blocks.AIR"));
    }

    @Test
    void buildModeSharesOnePlanAndUsesEscrowAroundNormalBlockPlacement() throws IOException {
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String executor = source("item", "custom", "SpiritStaffBuildExecutor.java");
        String build = methodBody(staff, "private static InteractionResult build(");
        String execute = methodBody(executor, "static Result execute(");
        String preview = methodBody(executor, "public static Preview preview(");
        String prepare = methodBody(executor, "private static PreparedJob prepare(");
        String reserve = methodBody(executor, "static MaterialEscrow reserve(");
        String extract = methodBody(executor, "private static int extractFromStorage(");

        assertTrue(build.contains("SpiritStaffBuildExecutor.execute("));
        assertTrue(build.contains("ImmortalStorageConfig.SPIRIT_STAFF_BUILD_LIMIT.get()"));
        assertTrue(execute.contains("prepare(player,"));
        assertTrue(preview.contains("prepare(player, hand,"));
        assertTrue(prepare.contains("SpiritStaffBuildPlan.create("),
                "preview and commit must share the same prepared placement job");
        assertTrue(execute.contains("MaterialEscrow.reserve("));
        assertTrue(execute.contains("blockItem().place(placeContext)"),
                "batch building must retain normal placement validation and events");
        assertTrue(execute.contains("finally"));
        assertTrue(execute.contains("escrow.refund(player)"));
        assertTrue(reserve.contains("removeFromPlayer("));
        assertTrue(reserve.contains("extractFromStorage("));
        assertTrue(extract.contains("TerminalStorageAction.SIMULATE"));
        assertTrue(extract.contains("TerminalStorageAction.EXECUTE"));
        assertFalse(execute.contains("level.setBlock("));
        assertTrue(execute.contains("hurtAndBreak(\n                    1,"),
                "one successful placement layer consumes exactly one durability");
        assertTrue(executor.contains("public static Result removeLayer("));
        assertTrue(executor.contains("CommonHooks.fireBlockBreak("));
        assertTrue(executor.contains("level.removeBlock(pos, false)"),
                "control-use removal must not produce drops");
    }

    @Test
    void buildPreviewIsServerAuthoritativeStaleSafeAndStrictlyBounded() throws IOException {
        String network = source("network", "ModNetwork.java");
        String payloads = source("network", "ModPayloads.java");
        String client = source("client", "render", "SpiritStaffBuildPreview.java");

        assertTrue(network.contains("playToServer(ModPayloads.RequestSpiritStaffBuildPreview.TYPE"));
        assertTrue(network.contains("SpiritStaffBuildExecutor.preview("),
                "the server must calculate the same material-aware job used by placement");
        assertTrue(network.contains("SpiritStaffBuildExecutor.previewRemoval("),
                "the red removal outline must use the same server removal geometry as commit");
        assertTrue(payloads.contains("public static final int MAX_POSITIONS = 4096"));
        assertTrue(payloads.contains("if (size < 0 || size > MAX_POSITIONS)"),
                "oversized payloads must be rejected rather than truncated with unread bytes");
        assertTrue(client.contains("snapshot.requestId() != requestId"));
        assertTrue(client.contains("snapshot.pos() != requestedTarget.pos()"));
        assertTrue(client.contains("snapshot.hand() != requestedTarget.hand()"));
        assertTrue(client.contains("snapshot.removal() != requestedTarget.removal()"));
        assertTrue(client.contains("requestedTarget.removal() ? 1.0F : 0.15F"),
                "holding the special-operation key must render a red removal outline");
        assertTrue(client.contains("if (targetChanged)"),
                "refreshing an unchanged target must retain the previous full-frame outline");
        assertFalse(client.contains("SpiritStaffBuildPlan.create("),
                "the client must render the server job instead of recomputing storage and permissions");
    }

    @Test
    void wrenchModeLeavesForeignBlocksToTheirNormalWrenchPathAndDismantlesOwnedBlocksNormally()
            throws IOException {
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String wrench = methodBody(staff, "private static InteractionResult wrench(");
        String dismantle = methodBody(staff, "private static InteractionResult safelyDismantle(");

        assertTrue(wrench.contains("SpiritStaffWrenchCompat.interact(context, player)"),
                "foreign mod blocks must pass through the optional public-API wrench dispatcher");
        String dispatcher = source("compat", "SpiritStaffWrenchCompat.java");
        assertTrue(dispatcher.contains("return InteractionResult.PASS"),
                "unhandled foreign blocks must remain available to their normal interaction path");
        assertTrue(wrench.contains("isSafeWrenchTarget(blockEntity)"));
        assertTrue(dismantle.contains("ownsDismantleTarget(blockEntity, player)"));
        assertTrue(dismantle.contains("player.gameMode.destroyBlock(pos)"),
                "ImmortalStorage dismantling must preserve vanilla break events and block-entity drops");
        assertFalse(dismantle.contains("level.removeBlock("));
        assertFalse(dismantle.contains("setBlock("));
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
}
