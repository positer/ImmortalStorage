package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimulatedMachineLocalCacheContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path SOURCE = locateProject().resolve(
            "src/main/java/com/immortalstorage/immortalstorage/block/entity");

    @Test
    void spiritFieldPersistsLocalRemainderAndBlocksUntilItDrains() throws Exception {
        String source = Files.readString(SOURCE.resolve("SimulatedSpiritFieldBlockEntity.java"));
        String route = methodBody(source, "private List<ItemStack> routeToLocalCache");
        String publish = methodBody(source, "private boolean publishCompletedHarvest");
        assertTrue(route.contains("items.set(slot"));
        assertTrue(route.contains("MachineOutputScheduler.pushItemToFaces"));
        assertTrue(route.contains("temporary.add(remaining.copy())"));
        assertFalse(route.contains("Block.popResource"));
        assertTrue(publish.contains("if (xianqiaoOutput)"));
        assertTrue(publish.contains("WorldShardOutputRouter.routeDirect(drops, endpoint)"));
        assertTrue(publish.contains("pendingHarvestDrops = temporary.isEmpty() ? null : temporary"));
        assertTrue(publish.contains("return pendingHarvestDrops == null"));
        assertTrue(publish.indexOf("WorldShardOutputRouter.routeDirect")
                < publish.indexOf("routeToLocalCache"));
        assertTrue(source.contains("PendingHarvestDrops"));
        assertTrue(source.contains("if (!settled)"));
        assertTrue(source.contains("drainPendingOutputForRemoval"));
        assertFalse(source.contains("OutputOverflowBlocked"));
        assertFalse(publish.contains("Block.popResource"));
        assertFalse(route.contains("endpoint.insert"));
    }

    @Test
    void reincarnationFurnacePersistsRemainderAndResumesOnlyAfterItClears() throws Exception {
        String source = Files.readString(SOURCE.resolve(
                "SimulatedReincarnationFurnaceBlockEntity.java"));
        String queue = methodBody(source, "private void queueOutput");
        String publish = methodBody(source, "private boolean publishPendingOutput");
        String flush = methodBody(source, "private boolean flushPendingOutputToCache");
        assertTrue(source.contains("private final List<ItemStack> pendingOutput"));
        assertTrue(source.contains("tag.put(\"PendingOutput\""));
        assertTrue(source.contains("\"PendingOutput\""));
        assertTrue(queue.contains("pendingOutput.add"));
        assertTrue(queue.contains("publishPendingOutput(level, endpoint)"));
        assertTrue(publish.contains("if (xianqiaoOutput)"));
        assertTrue(publish.contains("WorldShardOutputRouter.routeDirect(pendingOutput, endpoint)"));
        assertTrue(publish.contains("if (route.unaccepted() > 0L) return false"));
        assertTrue(publish.contains("return flushPendingOutputToCache(level)"));
        assertTrue(publish.indexOf("WorldShardOutputRouter.routeDirect")
                < publish.indexOf("flushPendingOutputToCache"));
        assertTrue(flush.contains("MachineOutputScheduler.insertIntoInternalSlots"));
        assertFalse(flush.contains("items.insertItem"),
                "machine-owned output must bypass the external insertion filter");
        assertTrue(flush.contains("MachineOutputScheduler.pushItemToFaces"));
        assertTrue(flush.contains("temporary.add(remaining.copy())"));
        assertTrue(flush.contains("pendingOutput.addAll(temporary)"));
        assertTrue(flush.contains("return pendingOutput.isEmpty()"));
        assertFalse(flush.contains("Block.popResource"));
        assertFalse(source.contains("OutputOverflowBlocked"));
        assertTrue(source.contains("if (!pendingSettled)"));
        assertFalse(queue.contains("Block.popResource"));
        assertFalse(publish.contains("Block.popResource"));
    }

    @Test
    void furnaceProductionBypassesItsExternalOutputInsertionFilter() {
        ItemStackHandler outputOnly = new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
        outputOnly.setStackInSlot(0, new ItemStack(Items.DIAMOND, 60));
        ItemStack offered = new ItemStack(Items.DIAMOND, 10);

        assertEquals(10, outputOnly.insertItem(0, offered.copy(), false).getCount(),
                "the public automation path must continue rejecting output insertion");

        ItemStack remainder = MachineOutputScheduler.insertIntoInternalSlots(
                outputOnly, 0, 2, offered);

        assertTrue(remainder.isEmpty());
        assertEquals(64, outputOnly.getStackInSlot(0).getCount());
        assertEquals(6, outputOnly.getStackInSlot(1).getCount());
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new AssertionError("Missing method: " + signature);
        int next = source.indexOf("\n    private ", start + signature.length());
        return next < 0 ? source.substring(start) : source.substring(start, next);
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
