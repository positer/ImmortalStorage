package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmortalArtifactUnboundedBuildContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();

    @Test
    void artifactBuildIsTickBatchedWithoutAUserVisibleTotalCountCap() throws IOException {
        String executor = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffBuildExecutor.java"));
        assertTrue(executor.contains("ARTIFACT_PLACEMENTS_PER_TICK"));
        assertTrue(executor.contains("ARTIFACT_SCANS_PER_TICK"));
        assertTrue(executor.contains("UnboundedArtifactJob"));
        assertTrue(executor.contains("ARTIFACT_PLACEMENTS_PER_TICK = 64"));
        String jobTick = executor.substring(executor.indexOf("private boolean tick(ServerPlayer player)"));
        assertFalse(jobTick.contains("com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.canInteractWithBlock(player, target, 1.0D)"));
        String preview = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/render/SpiritStaffBuildPreview.java"));
        assertTrue(preview.contains("immortal_artifact.build.preview_unbounded"));
        assertTrue(executor.contains("SpiritStaffBuildExecutor.tick".replace("SpiritStaffBuildExecutor.", "")));
        assertFalse(executor.contains("instanceof ImmortalArtifactItem\n                ? 4096"));
        assertTrue(executor.contains("offhand.getItem() instanceof BlockItem"));
        assertTrue(executor.indexOf("int fromStorage = extractFromStorage(endpoint, template, requested)")
                < executor.indexOf("int fromInventory = remaining <= 0 ? 0 : removeFromPlayer"));
        assertTrue(executor.indexOf("if (storageRemaining > 0)")
                < executor.indexOf("else if (inventoryRemaining > 0)"));
    }

    @Test
    void playerTickDrivesAndLogoutClearsTheQueuedJob() throws IOException {
        String events = Files.readString(PROJECT.resolve(
                "src/main/java/com/immortalstorage/immortalstorage/event/CommonEvents.java"));
        assertTrue(events.contains("SpiritStaffBuildExecutor.tick(p)"));
        assertTrue(events.contains("SpiritStaffBuildExecutor.clear(player)"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
