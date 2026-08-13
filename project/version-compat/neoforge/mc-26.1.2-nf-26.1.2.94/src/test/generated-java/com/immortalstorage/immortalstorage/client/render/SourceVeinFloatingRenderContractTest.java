package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceVeinFloatingRenderContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void animationUsesContinuousClampedPartialTicksAndSmallRotationValues() {
        assertEquals(42.0D, SourceVeinAnimation.continuousTime(42L, -0.2F), 0.0D);
        assertEquals(42.5D, SourceVeinAnimation.continuousTime(42L, 0.5F), 0.0D);
        assertEquals(43.0D, SourceVeinAnimation.continuousTime(42L, 1.4F), 0.0D);

        double time = 42.5D;
        assertTrue(Math.abs(SourceVeinAnimation.bob(time + 0.05D)
                        - SourceVeinAnimation.bob(time)) < 0.001F,
                "a sub-tick frame must not create a visible bob jump");
        assertTrue(Math.abs(SourceVeinAnimation.rotationDegrees(time + 0.05D, 0.82D)
                        - SourceVeinAnimation.rotationDegrees(time, 0.82D)) < 0.1F,
                "a sub-tick frame must not create a visible rotation jump");
    }

    @Test
    void acceleratedClockAppliesTheLogicalMultiplierImmediatelyWithoutASmoothingTail() {
        SourceVeinAnimation.Clock clock = new SourceVeinAnimation.Clock();
        clock.sampleAt(0L, 0.0D);
        clock.sampleAt(50_000_000L, 1.0D);
        double beforeAcceleration = clock.sampleAt(100_000_000L, 5.0D);
        double afterAcceleration = clock.sampleAt(150_000_000L, 9.0D);

        assertEquals(4.0D, afterAcceleration - beforeAcceleration, 0.000001D,
                "four logical ticks in one wall-clock tick must render at 4x immediately");
        assertEquals(4.0D, clock.speed(), 0.000001D,
                "the clock must expose the actual accelerated multiplier without a slow tail");
    }

    @Test
    void randomAttitudeIsStablePerSourceAndDoesNotReRandomizeDuringRendering() {
        SourceVeinAnimation.Orientation first = SourceVeinAnimation.orientation(17L);
        SourceVeinAnimation.Orientation again = SourceVeinAnimation.orientation(17L);
        SourceVeinAnimation.Orientation other = SourceVeinAnimation.orientation(18L);

        assertEquals(first, again);
        assertNotEquals(first, other);
    }

    @Test
    void blockAndItemModelsAreRecenteredBeforeFloatingAnimation() throws IOException {
        String renderer = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "SourceVeinDisplayRenderer.java"));
        assertTrue(renderer.contains("applyCenteredModelTransform(poses, animationTime, 0.36F,"));
        assertTrue(renderer.contains("applyItemModelTransform(poses, animationTime, 0.48F,"));
        assertTrue(renderer.contains("AlphaVertexConsumer"));
        assertTrue(renderer.contains("ModelData.EMPTY"));
        assertTrue(renderer.contains("RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)"));
        assertTrue(renderer.contains("poses.scale(scale, scale, scale)"));
        assertTrue(renderer.contains("SourceVeinModelBounds.center(model, state)"));
        assertTrue(renderer.contains("SourceVeinModelBounds.center(model, null)"));
        assertTrue(renderer.contains("poses.translate(-center.x(), -center.y(), -center.z())"));
        assertTrue(renderer.contains("render" + "ModelLists(")
                        || renderer.contains("SpecialModelGeometry.submitBlockBase"),
                "item overlays must not receive ItemRenderer's second -0.5 translation");
        assertTrue(renderer.contains("double animationTime"));
        assertTrue(renderer.contains("SourceVeinAnimation.rotationDegrees"));
        assertTrue(renderer.contains("SourceVeinAnimation.orientation"));
        assertTrue(renderer.contains("Axis.ZP"));
        assertTrue(renderer.contains("BLOCK_CENTER_Y = 0.5F"));
        assertTrue(renderer.contains("ITEM_CENTER_X = 0.5F"));
        assertTrue(renderer.contains("ITEM_CENTER_Y = 0.5F"));
        assertTrue(renderer.contains("ITEM_CENTER_Z = 0.5F"));
        assertTrue(renderer.contains("BiomeColors.getAverageWaterColor(tintLevel, tintPos)"));
        assertTrue(renderer.contains("fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER"));
        assertTrue(renderer.contains("multiplyRgb(tint, biomeTint)"));
        assertTrue(renderer.contains("@Nullable BlockAndTintGetter tintLevel"));
        assertTrue(renderer.contains("@Nullable BlockPos tintPos"));
        assertTrue(renderer.contains("SOLID_ROTATION_DEGREES_PER_TICK"));
        assertTrue(renderer.contains("ITEM_ROTATION_DEGREES_PER_TICK"));
        assertFalse(renderer.contains("applyFloatingTransform"));
        assertFalse(renderer.contains("SourceVeinAnimation.bob"));

        String bounds = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "SourceVeinModelBounds.java"));
        assertTrue(bounds.contains("quad.getVertices()"));
        assertTrue(bounds.contains("DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.POSITION)"));

        String worldRenderer = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "SourceVeinRenderer.java"));
        assertTrue(worldRenderer.contains("SourceVeinAnimation.continuousTime"));
        assertTrue(worldRenderer.contains("SourceVeinAnimation.Clock"));
        assertTrue(worldRenderer.contains("getBlockPos().asLong()"));
        assertTrue(worldRenderer.contains("blockEntity.getLevel(), blockEntity.getBlockPos()"));

        String itemRenderer = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "SourceVeinItemRenderer.java"));
        assertTrue(itemRenderer.contains("SourceVeinAnimation.realTime()")
                        || itemRenderer.contains("System.currentTimeMillis()"));
        assertTrue(itemRenderer.contains("SourceVeinAnimation.continuousTime")
                        || itemRenderer.contains("getDeltaTracker().getGameTimeDeltaPartialTick"));
        assertTrue(itemRenderer.contains("SourceVeinDisplayRenderer.renderForItem"));
        assertTrue(itemRenderer.contains("orientationSeed(source, definition)"));

        String managerRenderer = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "SourceVeinManagerRenderer.java"));
        assertTrue(managerRenderer.contains("SourceVeinAnimation.rotationDegrees"));
        assertTrue(managerRenderer.contains("SourceVeinAnimation.Clock"));

        String floatingCube = Files.readString(source("main", "java", "com", "immortalstorage",
                "immortalstorage", "client", "render", "FloatingCubeRenderer.java"));
        assertTrue(floatingCube.contains("textures/block/white_concrete.png"));
        assertFalse(floatingCube.contains("textures/misc/white.png"));
    }

    private static Path source(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path project = current.resolve(Path.of("..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source"));
            if (Files.isDirectory(project)) {
                return current.resolve(Path.of("src", parts));
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
