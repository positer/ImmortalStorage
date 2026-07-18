package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.block.entity.SourceVeinManagerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the manager's fixed six-cell occupancy indicator in its bookshelf
 * bays.  The inventory item deliberately renders only the body.
 */
public final class SourceVeinManagerRenderer implements BlockEntityRenderer<SourceVeinManagerBlockEntity> {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType INDICATORS = RenderType.entityTranslucentEmissive(WHITE_TEXTURE);

    public SourceVeinManagerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SourceVeinManagerBlockEntity manager,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {
        if (manager.getLevel() != null) {
            BlockPos coveredPos = manager.getBlockPos().relative(Direction.NORTH);
            if (manager.getLevel().getBlockState(coveredPos)
                    .isSolidRender(manager.getLevel(), coveredPos)) return;
        }

        VertexConsumer vertices = buffers.getBuffer(INDICATORS);
        PoseStack.Pose pose = poseStack.last();
        int state = manager.displayState();
        for (int index = 0; index < SourceVeinManagerIndicatorLayout.INDICATORS; index++) {
            float half = SourceVeinManagerIndicatorLayout.INDICATOR_SIZE * 0.5F;
            float centerX = SourceVeinManagerIndicatorLayout.centerX(index);
            float centerY = SourceVeinManagerIndicatorLayout.centerY(index);
            indicator(vertices, pose, centerX - half, centerY - half,
                    centerX + half, centerY + half,
                    SourceVeinManagerIndicatorLayout.colorFor(state, index));
        }
    }

    @Override
    public boolean shouldRender(SourceVeinManagerBlockEntity manager, Vec3 cameraPosition) {
        BlockPos pos = manager.getBlockPos();
        // The six bays exist only on the north face.  Default BER frustum
        // culling still applies; this additional face test avoids submitting
        // indicator geometry while the face itself cannot be seen.
        return cameraPosition.z < pos.getZ() + 0.5D
                && Vec3.atCenterOf(pos).closerThan(cameraPosition, getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(SourceVeinManagerBlockEntity manager) {
        return new AABB(manager.getBlockPos()).inflate(0.01D);
    }

    private static void indicator(VertexConsumer vertices, PoseStack.Pose pose,
                                  float minX, float minY, float maxX, float maxY, int rgb) {
        vertex(vertices, pose, maxX, minY, 0.0F, 0.0F, 1.0F, rgb);
        vertex(vertices, pose, minX, minY, 0.0F, 1.0F, 1.0F, rgb);
        vertex(vertices, pose, minX, maxY, 0.0F, 1.0F, 0.0F, rgb);
        vertex(vertices, pose, maxX, maxY, 0.0F, 0.0F, 0.0F, rgb);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int rgb) {
        vertices.addVertex(pose, x, y, z - 0.002F)
                .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
