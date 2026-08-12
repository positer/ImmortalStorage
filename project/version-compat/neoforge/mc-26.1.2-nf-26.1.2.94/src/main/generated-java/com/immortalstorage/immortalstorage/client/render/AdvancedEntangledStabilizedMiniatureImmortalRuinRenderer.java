package com.immortalstorage.immortalstorage.client.render;
import com.immortalstorage.immortalstorage.compat.mc2612.LegacyBlockEntityRenderer;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;

import com.immortalstorage.immortalstorage.block.entity.AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Blue-framed advanced entangled ruin with two counter-rotating spheres and per-side preview boxes. */
public final class AdvancedEntangledStabilizedMiniatureImmortalRuinRenderer extends LegacyBlockEntityRenderer<AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity> {
    private static final float ORBIT_RADIUS = 0.16F;
    private static final float BASE_SCALE = 0.15F;

    public AdvancedEntangledStabilizedMiniatureImmortalRuinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void legacyRender(AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay, net.minecraft.world.phys.Vec3 cameraPosition) {
        float time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partialTick;
        float breath = Mth.sin(time * 0.12F) * 0.02F;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        poses.pushPose();
        poses.translate(0.5F, 0.5F, 0.5F);
        poses.mulPose(camera.rotation());
        float angleA = time * 0.30F;
        float angleB = -time * 0.30F;
        drawOrbiter(poses, buffers, angleA, BASE_SCALE + breath, false);
        drawOrbiter(poses, buffers, angleB, BASE_SCALE + breath, true);
        poses.popPose();

        if (entity.normalSide().preview()) renderPreview(poses, buffers, entity.normalSide(), entity.sideFaceMask(0));
        if (entity.reversedSide().preview()) renderPreview(poses, buffers, entity.reversedSide(), entity.sideFaceMask(1));
    }

    private void renderPreview(PoseStack poses, MultiBufferSource buffers,
                               AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity.SideConfig side,
                               int faceMask) {
        AABB area = new AABB(side.offsetX(), side.offsetY(), side.offsetZ(),
                side.offsetX() + side.sizeX(), side.offsetY() + side.sizeY(), side.offsetZ() + side.sizeZ())
                .inflate(0.01D);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox(poses, buffers.getBuffer(CompatRenderTypes.lines()), area,
                1.0F, 1.0F, 1.0F, 1.0F);
        RuinFaceHighlightRenderer.render(poses, buffers, area, faceMask);
    }

    private void drawOrbiter(PoseStack poses, MultiBufferSource buffers, float angle, float scale, boolean reversed) {
        poses.pushPose();
        poses.mulPose(Axis.ZP.rotation(angle));
        poses.translate(ORBIT_RADIUS, 0.0F, 0.0F);
        MiniatureImmortalRuinRenderer.drawDisc(poses, buffers, scale, reversed);
        poses.popPose();
    }

    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 96; }
}
