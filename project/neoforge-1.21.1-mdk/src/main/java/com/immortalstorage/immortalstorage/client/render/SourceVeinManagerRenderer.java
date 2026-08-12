package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders the manager's eight-segment source core as one rigid body that
 * slowly spins around the block centre [8,8,8].  Each 3x3x3 segment samples
 * the solid empty/used/full textures selected by the eight-state ladder.  The
 * static block model inherits the source vein frame directly; this renderer
 * owns only the manager's eight-segment core. The same core geometry is shared
 * with the inventory/hand BEWLR through {@link #drawCore}.
 */
public final class SourceVeinManagerRenderer implements BlockEntityRenderer<SourceVeinManagerBlockEntity> {
    static final float DEGREES_PER_TICK = 4.0F;
    /** Keep the eight core cubes translucent while preserving their 3x3x3 layout. */
    private static final int CORE_ALPHA = 208;
    private static final RenderType TYPE_EMPTY = materialType("source_vein_manager_core_empty");
    private static final RenderType TYPE_USED = materialType("source_vein_manager_core_used");
    private static final RenderType TYPE_FULL = materialType("source_vein_manager_core_full");
    private final Map<Long, SourceVeinAnimation.Clock> animationClocks = new HashMap<>();

    public SourceVeinManagerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SourceVeinManagerBlockEntity manager,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {
        double logicalTime = manager.getLevel() == null
                ? SourceVeinAnimation.continuousTime(0L, partialTick)
                : SourceVeinAnimation.continuousTime(manager.getLevel().getGameTime(), partialTick);
        double animationTime = clockFor(manager.getBlockPos().asLong()).sample(logicalTime);

        drawCore(poseStack, buffers, manager.displayState(),
                SourceVeinAnimation.rotationDegrees(animationTime, DEGREES_PER_TICK));
    }

    /**
     * Draws the eight-segment core as one rigid body spinning around the block
     * centre.  Shared by the world renderer and the inventory/hand BEWLR so
     * both views stay pixel-identical.
     */
    static void drawCore(PoseStack poseStack, MultiBufferSource buffers,
                         int state, float rotationDegrees) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        for (int segment = 0; segment < SourceVeinCoreLayout.SEGMENTS; segment++) {
            RenderType type = renderTypeFor(SourceVeinCoreLayout.materialFor(state, segment));
            PoseStack.Pose pose = poseStack.last();
            cube(buffers.getBuffer(type), pose,
                    SourceVeinCoreLayout.centerX(segment),
                    SourceVeinCoreLayout.centerY(segment),
                    SourceVeinCoreLayout.centerZ(segment));
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(SourceVeinManagerBlockEntity manager, Vec3 cameraPosition) {
        return Vec3.atCenterOf(manager.getBlockPos()).closerThan(cameraPosition, getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(SourceVeinManagerBlockEntity manager) {
        return new AABB(manager.getBlockPos()).inflate(0.01D);
    }

    private SourceVeinAnimation.Clock clockFor(long key) {
        if (animationClocks.size() >= 512 && !animationClocks.containsKey(key)) {
            animationClocks.clear();
        }
        return animationClocks.computeIfAbsent(key, ignored -> new SourceVeinAnimation.Clock());
    }

    private static RenderType renderTypeFor(SourceVeinCoreLayout.Material material) {
        return switch (material) {
            case EMPTY -> TYPE_EMPTY;
            case USED -> TYPE_USED;
            case FULL -> TYPE_FULL;
        };
    }

    private static RenderType materialType(String texture) {
        return RenderType.entityTranslucentEmissive(ResourceLocation
                .fromNamespaceAndPath("immortalstorage", "textures/block/" + texture + ".png"));
    }

    private static void cube(VertexConsumer vertices, PoseStack.Pose pose,
                             float cx, float cy, float cz) {
        float h = SourceVeinCoreLayout.HALF_SIZE;
        face(vertices, pose,
                cx - h, cy - h, cz - h,
                cx + h, cy - h, cz - h,
                cx + h, cy - h, cz + h,
                cx - h, cy - h, cz + h,
                0.0F, -1.0F, 0.0F, 12.0F, 6.0F, 16.0F, 12.0F, 0.82F);
        face(vertices, pose,
                cx - h, cy + h, cz + h,
                cx + h, cy + h, cz + h,
                cx + h, cy + h, cz - h,
                cx - h, cy + h, cz - h,
                0.0F, 1.0F, 0.0F, 12.0F, 0.0F, 16.0F, 6.0F, 1.0F);
        face(vertices, pose,
                cx + h, cy - h, cz - h,
                cx - h, cy - h, cz - h,
                cx - h, cy + h, cz - h,
                cx + h, cy + h, cz - h,
                0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 6.0F, 6.0F, 0.86F);
        face(vertices, pose,
                cx - h, cy - h, cz + h,
                cx + h, cy - h, cz + h,
                cx + h, cy + h, cz + h,
                cx - h, cy + h, cz + h,
                0.0F, 0.0F, 1.0F, 0.0F, 6.0F, 6.0F, 12.0F, 0.94F);
        face(vertices, pose,
                cx - h, cy - h, cz - h,
                cx - h, cy - h, cz + h,
                cx - h, cy + h, cz + h,
                cx - h, cy + h, cz - h,
                -1.0F, 0.0F, 0.0F, 6.0F, 6.0F, 12.0F, 12.0F, 0.84F);
        face(vertices, pose,
                cx + h, cy - h, cz + h,
                cx + h, cy - h, cz - h,
                cx + h, cy + h, cz - h,
                cx + h, cy + h, cz + h,
                1.0F, 0.0F, 0.0F, 6.0F, 0.0F, 12.0F, 6.0F, 0.96F);
    }

    private static void face(VertexConsumer vertices,
                             PoseStack.Pose pose,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float normalX, float normalY, float normalZ,
                             float u1, float v1, float u2, float v2,
                             float shade) {
        int light = Math.round(shade * 255.0F);
        vertex(vertices, pose, x0, y0, z0, u1, v2, normalX, normalY, normalZ, light);
        vertex(vertices, pose, x1, y1, z1, u2, v2, normalX, normalY, normalZ, light);
        vertex(vertices, pose, x2, y2, z2, u2, v1, normalX, normalY, normalZ, light);
        vertex(vertices, pose, x3, y3, z3, u1, v1, normalX, normalY, normalZ, light);
    }

    private static void vertex(VertexConsumer vertices,
                               PoseStack.Pose pose,
                               float x, float y, float z,
                               float u, float v,
                               float normalX, float normalY, float normalZ,
                               int light) {
        vertices.addVertex(pose, x, y, z)
                .setColor(light, light, light, CORE_ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
