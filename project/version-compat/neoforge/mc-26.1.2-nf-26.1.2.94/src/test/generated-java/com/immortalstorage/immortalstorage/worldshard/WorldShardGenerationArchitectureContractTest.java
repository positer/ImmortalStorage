package com.immortalstorage.immortalstorage.worldshard;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Thin runtime-wiring checks for behavior that requires a live ServerLevel to execute. */
final class WorldShardGenerationArchitectureContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path BLOCK_ENTITIES = locateMainSources().resolve(Path.of("block", "entity"));

    @Test
    void oreAndTreasurePreflightIndependentlyBeforeExpensiveGenerationAndUseOneStatelessRouter() throws IOException {
        String minerTick = methodBody(source("WorldShardMinerBlockEntity.java"),
                "public static void serverTick(");
        String basinEntity = source("TreasureBasinBlockEntity.java");
        String basinTick = methodBody(basinEntity, "public static void serverTick(");
        String basinRoll = methodBody(basinEntity, "private boolean rollOnce(");

        assertOrdered(minerTick, "miner.canGenerateOutputs(level)", "sampleBatch(",
                "ore sampling must not run while the cache or owner storage blocks output");
        assertOrdered(basinTick, "basin.canGenerateOutputs(level)", "TreasureBasinSchedule.advance(",
                "schedule progress must not advance while the basin destination blocks output");
        assertOrdered(basinTick, "if (!basin.hasSelectableLoot()) return;",
                "basin.canGenerateOutputs(level)",
                "CALIBRATING must stop before output preflight and must not advance the cycle");
        assertOrdered(basinTick, "TreasureBasinSchedule.advance(",
                "basin.rollOnce(level, pos)",
                "an accelerated roll may only run after the schedule advance is recorded");
        assertFalse(basinRoll.contains("table.fill("),
                "container filling truncates a loot roll to 27 slots before the transaction sees it");
        assertFalse(basinEntity.contains("ReinforcementPluginHost.multiplyOutputs("),
                "the basin plugin must accelerate the schedule, never multiply one roll's drops");
        assertFalse(basinRoll.contains("SimpleContainer"),
                "the basin must route the complete loot-table result, not an inventory-shaped subset");
        assertTrue(basinRoll.contains("table.getRandomItemsRaw("),
                "the basin must roll the vanilla table through the raw context");
        assertTrue(basinRoll.contains("CommonHooks.modifyLoot("),
                "structure chests must honor the NeoForge global-loot-modifier chain");
        assertTrue(basinRoll.contains("basin.routeGenerated(level, generated)")
                        || basinRoll.contains("routeGenerated(level, generated)"),
                "treasure must use the basin-owned dimension-aware transaction");
        assertFalse(basinRoll.contains("miner.routeGenerated"),
                "loot may not publish through the miner cache or miner pause state");
        assertFalse(basinRoll.contains("WorldShardLootOutputRouter"),
                "the retired cache-only basin route would bypass Xianqiao direct storage");
        assertOrdered(basinRoll, "routeGenerated(level, generated)",
                "generationCycle =",
                "the basin cycle advances only after its own complete loot transaction succeeds");
    }

    @Test
    void pyramidActivationIsDimensionAgnosticAndOnlyOutputRoutingChecksTheOwnerRealm() throws IOException {
        String entity = source("WorldShardMinerBlockEntity.java");
        String minerTick = methodBody(entity, "public static void serverTick(");
        int activation = minerTick.indexOf("miner.updateActivation(");
        int generationGate = minerTick.indexOf("WorldShardMiningMath.shouldRun(");

        assertTrue(activation > 0, "the scanned pyramid must publish its activation state");
        assertTrue(generationGate > activation,
                "activation must refresh every tick before the 20-tick ore generation gate");
        assertFalse(minerTick.substring(0, activation).contains("ImmortalStorageDimensions"),
                "dimension checks must not prevent a valid pyramid from activating");
        assertTrue(methodBody(entity, "private boolean isExactOwnerRealm(")
                        .contains("ImmortalStorageDimensions.isPersonalRealmFor"),
                "the dimension is relevant only when selecting direct Xianqiao output");
    }

    @Test
    void eachMachinePausedStateIsSavedLoadedAndSentToTheClient() throws IOException {
        for (String file : new String[] {
                "WorldShardMinerBlockEntity.java", "TreasureBasinBlockEntity.java"
        }) {
            String entity = source(file);
            String save = methodBody(entity, "protected void saveAdditionalLegacy(");
            String load = methodBody(entity, "protected void loadAdditionalLegacy(");
            String client = methodBody(entity, "private CompoundTag writeClientState(");

            assertTrue(save.contains("writeClientState(tag)"), file);
            assertTrue(load.contains("tag.getListOrEmpty(PENDING_OUTPUT_TAG"), file);
            assertTrue(load.contains("cacheFull = !pendingOutput.isEmpty()"), file);
            assertTrue(load.contains("tag.getBooleanOr(STORAGE_UNAVAILABLE_TAG, false)"), file);
            assertTrue(client.contains("tag.putBoolean(CACHE_FULL_TAG, cacheFull)"), file);
            assertTrue(client.contains("tag.putBoolean(STORAGE_UNAVAILABLE_TAG, storageUnavailable)"), file);
        }
    }

    private static String source(String file) throws IOException {
        return Files.readString(BLOCK_ENTITIES.resolve(file));
    }

    private static void assertOrdered(String source, String first, String second, String message) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0 && secondIndex > firstIndex, message);
    }

    private static String methodBody(String source, String signature) {
        int method = source.indexOf(signature);
        if (method < 0) return "";
        int opening = source.indexOf('{', method);
        if (opening < 0) return "";
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
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
}
