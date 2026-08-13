package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression guards for the files that are intentionally target-only in 26.1.2. */
final class TargetSourceVeinRenderContractTest {
    @Test
    void targetRendererUsesActualBlockAndItemGeometryCenters() throws IOException {
        String renderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "SourceVeinDisplayRenderer.java"));

        assertTrue(renderer.contains("BlockStateModel model"));
        assertTrue(renderer.contains("model.collectParts(RandomSource.create(0L), parts)"));
        assertTrue(renderer.contains("part.getQuads(null)"));
        assertTrue(renderer.contains("BakedQuad.VERTEX_COUNT"));
        assertTrue(renderer.contains("SpecialModelGeometry.itemModelBounds(output)"));
        assertTrue(renderer.contains("poses.translate(-modelCenter.x(), -modelCenter.y(), -modelCenter.z())"));
        assertTrue(renderer.contains("poses.translate(0.5F, CENTER_Y + bob, 0.5F)"));
        assertTrue(renderer.contains("CENTER_Y = 0.5F"));

        String itemRenderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "SourceVeinItemRenderer.java"));
        assertTrue(itemRenderer.contains("SourceVeinDisplayRenderer.submitForItem"));

        String geometry = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "compat", "mc2612", "SpecialModelGeometry.java"));
        assertTrue(geometry.contains("getModelBoundingBox()"));
    }

    @Test
    void targetMatchesCanonicalBlockAndItemPresentationRules() throws IOException {
        String renderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "SourceVeinDisplayRenderer.java"));

        assertTrue(renderer.contains("applyBlockTransform(poses, animationTime, 0.36F"));
        assertTrue(renderer.contains("applyItemTransform(poses, animationTime, 0.48F"));
        assertTrue(renderer.contains("FLOATING_ALPHA << 24"));
        assertTrue(renderer.contains("vertices.putBakedQuad(poses.last(), quad, instance)"));

        int itemStart = renderer.indexOf("private static void applyItemTransform");
        int itemEnd = renderer.indexOf("private static void renderTranslucentBlock", itemStart);
        String itemTransform = renderer.substring(itemStart, itemEnd);
        assertTrue(itemTransform.contains("Axis.YP.rotationDegrees"));
        assertFalse(itemTransform.contains("Axis.XP.rotationDegrees"));
        assertFalse(itemTransform.contains("Axis.ZP.rotationDegrees"));
    }

    @Test
    void targetRendererUsesWorldFluidTintAndTheRealFluidSprite() throws IOException {
        String renderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "SourceVeinDisplayRenderer.java"));

        assertTrue(renderer.contains("model.stillMaterial().sprite()"));
        assertTrue(renderer.contains("BiomeColors.getAverageWaterColor(tintLevel, tintPos)"));
        assertTrue(renderer.contains("model.tintSource().colorInWorld(fluidState, tintLevel, tintPos)"));
        assertTrue(renderer.contains("@Nullable BlockAndTintGetter tintLevel"));
        assertTrue(renderer.contains("@Nullable BlockPos tintPos"));

        String worldRenderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "SourceVeinRenderer.java"));
        assertTrue(worldRenderer.contains("tintLevel"));
        assertTrue(worldRenderer.contains("blockEntity.getBlockPos()"));
    }

    @Test
    void targetManagerCoreUsesAnExistingVanillaTexture() throws IOException {
        String floatingCube = Files.readString(targetSource(
                "src", "main", "generated-java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "FloatingCubeRenderer.java"));
        assertTrue(floatingCube.contains("textures/block/white_concrete.png"));
        assertFalse(floatingCube.contains("textures/misc/white.png"));

        String manager = Files.readString(targetSource(
                "src", "main", "generated-java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "XianqiaoManagerRenderer.java"));
        assertTrue(manager.contains("FloatingCubeRenderer.render("));
    }

    @Test
    void targetMinerGlassDoesNotUseBlockPositionAsOutlineColour() throws IOException {
        String renderer = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "WorldShardMinerRenderer.java"));
        assertTrue(renderer.contains("glassModel.submitMultiLayer(poses, collector, state.lightCoords,"));
        assertTrue(renderer.contains("OverlayTexture.NO_OVERLAY, 0)"));
        assertFalse(renderer.contains("state.blockPos"));
    }

    private static Path targetSource(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path targetRoot = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94"));
            if (Files.isDirectory(targetRoot)) {
                Path resolved = targetRoot;
                for (String part : parts) resolved = resolved.resolve(part);
                return resolved;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate 26.1.2 compatibility project");
    }
}
