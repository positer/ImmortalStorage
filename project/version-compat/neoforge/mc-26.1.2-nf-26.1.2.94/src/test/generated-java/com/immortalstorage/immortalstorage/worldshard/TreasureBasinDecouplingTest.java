package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreasureBasinDecouplingTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Path TARGET_SOURCE_ROOT = PROJECT.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage")).normalize();
    private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath(
            "immortalstorage", "overworld");
    private static final Identifier NETHER = Identifier.fromNamespaceAndPath(
            "immortalstorage", "nether");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void onlyADirectlyBelowActiveMinerActivatesTheBasin() {
        UUID owner = UUID.randomUUID();

        TreasureBasinActivation adjacent = TreasureBasinActivation.resolve(
                true, true, OVERWORLD, owner);
        TreasureBasinActivation notDirectlyBelow = TreasureBasinActivation.resolve(
                false, true, OVERWORLD, owner);
        TreasureBasinActivation inactiveMiner = TreasureBasinActivation.resolve(
                true, false, OVERWORLD, owner);

        assertTrue(adjacent.active());
        assertEquals(OVERWORLD, adjacent.mode());
        assertEquals(owner, adjacent.owner());
        assertFalse(notDirectlyBelow.active(),
                "a miner beside or above the basin must not activate it");
        assertFalse(inactiveMiner.active(),
                "physical adjacency alone is insufficient while the miner is inactive");
    }

    @Test
    void modeChangesAreInheritedAndRemovingTheMinerClearsActivation() {
        UUID owner = UUID.randomUUID();
        TreasureBasinActivation overworld = TreasureBasinActivation.resolve(
                true, true, OVERWORLD, owner);
        TreasureBasinActivation nether = TreasureBasinActivation.resolve(
                true, true, NETHER, owner);
        TreasureBasinActivation removed = TreasureBasinActivation.resolve(
                false, false, null, null);

        assertNotEquals(overworld.mode(), nether.mode());
        assertEquals(NETHER, nether.mode());
        assertFalse(removed.active());
        assertEquals(null, removed.mode());
        assertEquals(null, removed.owner());
    }

    @Test
    void minerAndBasinCachesAreIndependentStatefulInstances() {
        WorldShardMinerCache minerCache = new WorldShardMinerCache(null);
        WorldShardMinerCache basinCache = new WorldShardMinerCache(null);

        WorldShardOutputRouter.RouteResult basinRoute = WorldShardOutputRouter.routeCache(
                List.of(new ItemStack(Items.DIAMOND, 17)), basinCache);

        assertEquals(17L, basinRoute.accepted());
        assertEquals(0, count(minerCache));
        assertEquals(17, count(basinCache));
    }

    @Test
    void productionMachinesShareOnlyTheStatelessRouterNotStateOrDuties() throws IOException {
        String miner = source("block/entity/WorldShardMinerBlockEntity.java");
        String basin = source("block/entity/TreasureBasinBlockEntity.java");
        String menu = source("menu/custom/TreasureBasinMenu.java");

        assertFalse(miner.contains("WorldShardLootCatalog"));
        assertFalse(miner.contains("TreasureBasinSchedule"));
        assertFalse(miner.contains("LootTable"), "the miner is responsible only for ores");

        assertTrue(basin.contains("private final WorldShardMinerCache cache"),
                "the basin must persist its own barrel-sized cache instance");
        assertTrue(basin.contains("private final List<ItemStack> pendingOutput"));
        assertTrue(basin.contains("WorldShardOutputRouter.routeCacheWithOverflow(generated, cache)"));
        assertTrue(basin.contains("MachineOutputScheduler.pushItemToFaces"));
        assertTrue(basin.contains("replacePendingOutput(temporary)"));
        assertFalse(basin.contains("Block.popResource(level, worldPosition, remainder)"));
        assertTrue(miner.contains("WorldShardOutputRouter.routeCacheWithOverflow(generated, cache)"));
        assertTrue(miner.contains("private final List<ItemStack> pendingOutput"));
        assertTrue(miner.contains("MachineOutputScheduler.pushItemToFaces"));
        assertTrue(miner.contains("replacePendingOutput(temporary)"));
        assertFalse(miner.contains("Block.popResource(level, worldPosition, remainder)"));
        assertTrue(basin.contains("if (!basin.settlePendingOutput(level)) return"));
        assertTrue(miner.contains("if (!miner.settlePendingOutput(level)) return"));
        assertFalse(basin.contains("miner.routeGenerated"));
        assertFalse(basin.contains("miner.canGenerateOutputs"));
        assertFalse(basin.contains("miner.getMinerCache"));
        assertFalse(basin.contains("getActiveLevel"),
                "the basin inherits only mode and owner, never the mining speed level");
        assertTrue(basin.contains("private long generationCycle"),
                "the basin owns its own loot timer/cycle");

        assertTrue(menu.contains("this(id, inventory, basin, liveStatus(basin), basin)"),
                "the basin UI must expose the basin cache, never the miner cache");
        assertFalse(menu.contains("this(id, inventory, miner, liveStatus(miner), basin, miner)"));
    }

    @Test
    void basinCapabilityRemovalComparatorAndPersistenceAllTargetItsOwnCache() throws IOException {
        String capabilities = source("block/entity/ModBlockEntities.java");
        String basinBlock = source("block/custom/TreasureBasinBlock.java");
        String minerBlock = source("block/custom/WorldShardMinerBlock.java");
        String basin = source("block/entity/TreasureBasinBlockEntity.java");

        assertTrue(capabilities.contains(
                "Capabilities.Item.BLOCK, TREASURE_BASIN.get()"));
        assertTrue(basinBlock.contains("basin.getCacheHandler().extractItem"),
                "breaking the basin must recover its visible cache");
        assertTrue(basinBlock.contains("basin.drainPendingOutputForRemoval()"),
                "breaking the basin must recover its temporary output cache");
        assertTrue(basinBlock.contains("basin.getCacheHandler().getStackInSlot"),
                "the basin comparator must inspect only the basin cache");
        assertFalse(basinBlock.contains("getAttachedMiner().getCacheHandler"));

        assertTrue(minerBlock.contains("miner.getCacheHandler().extractItem"));
        assertTrue(minerBlock.contains("miner.drainPendingOutputForRemoval()"));
        assertTrue(minerBlock.contains("miner.getCacheHandler().getStackInSlot"));
        assertFalse(minerBlock.contains("TreasureBasinBlockEntity"));

        assertTrue(basin.contains("tag.put(CACHE_TAG, com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.serialize(cache, registries))"));
        assertTrue(basin.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.deserialize(cache, registries, tag.getCompoundOrEmpty(CACHE_TAG))"));
        assertTrue(basin.contains("tag.put(PENDING_OUTPUT_TAG, pending)"));
        assertTrue(basin.contains("tag.getListOrEmpty(PENDING_OUTPUT_TAG)"));
        assertTrue(basin.contains("tag.putBoolean(CACHE_FULL_TAG, cacheFull)"));
        assertTrue(basin.contains("tag.putBoolean(STORAGE_UNAVAILABLE_TAG, storageUnavailable)"));
        assertTrue(basin.contains("ClientboundBlockEntityDataPacket.create"));
    }

    private static int count(WorldShardMinerCache cache) {
        int total = 0;
        for (int slot = 0; slot < cache.getSlots(); slot++) {
            total += cache.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static String source(String relative) throws IOException {
        return Files.readString(TARGET_SOURCE_ROOT
                .resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && current != null; i++, current = current.getParent()) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) {
                return current;
            }
        }
        throw new IllegalStateException("Cannot locate NeoForge project from "
                + Path.of("").toAbsolutePath());
    }
}
