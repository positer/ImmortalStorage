package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldShardMinerOcclusionContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();

    @Test
    void registeredMinerStateCannotOccludeAdjacentFaces() {
        assertFalse(ModBlocks.WORLD_SHARD_MINER.get().defaultBlockState().canOcclude(),
                "the live registered state must expose noOcclusion semantics to Minecraft's face culler");
    }

    @Test
    void transparentMinerDoesNotOccludeFacesOfAdjacentBlocks() throws IOException {
        String registrations = source("block/ModBlocks.java");
        String minerRegistration = between(registrations,
                "WORLD_SHARD_MINER =", "TREASURE_BASIN =");

        assertTrue(minerRegistration.contains(".noOcclusion()"),
                "the glass-covered miner must not advertise a full occlusion shape; otherwise "
                        + "Minecraft culls the inward face of every block touching it");
    }

    @Test
    void occlusionFixKeepsNormalDepthTestingAndBlockGeometry() throws IOException {
        String block = source("block/custom/WorldShardMinerBlock.java");
        String renderer = source("client/render/WorldShardMinerRenderer.java");

        assertFalse(block.contains("skipRendering("),
                "noOcclusion is sufficient; do not add pair-specific face-culling exceptions");
        assertFalse(block.contains("getOcclusionShape("),
                "the miner keeps normal collision/selection geometry and only disables occlusion");
        assertFalse(renderer.contains("disableDepthTest"),
                "the transparent enclosure must remain depth-tested instead of rendering through blocks");
        assertFalse(renderer.contains("depthMask(false)"),
                "the transparent enclosure must not bypass normal depth writes as an occlusion workaround");
        assertFalse(renderer.contains("NO_DEPTH_TEST"),
                "the transparent enclosure must not use a through-wall render state");
    }

    private static String between(String source, String opening, String closing) {
        int start = source.indexOf(opening);
        int end = source.indexOf(closing, start + opening.length());
        assertTrue(start >= 0, "missing source marker " + opening);
        assertTrue(end > start, "missing source marker " + closing);
        return source.substring(start, end);
    }

    private static String source(String relative) throws IOException {
        return Files.readString(MAIN.resolve(relative));
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
