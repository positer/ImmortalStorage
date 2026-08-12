package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Shared, texture-independent geometry for the luminous cubes rendered inside
 * ImmortalStorage's open machine frames. The only sampled texture is Minecraft's
 * runtime-provided white pixel, so the caller's RGB value defines the theme.
 */
public final class FloatingCubeRenderer {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType TRANSLUCENT_EMISSIVE =
            RenderType.entityTranslucentEmissive(WHITE_TEXTURE);

    /**
     * Renders one full-bright cube centered on the block's horizontal midpoint.
     * Animation values are expressed per game tick so renderers can interpolate
     * with partial ticks without keeping mutable client state.
     */
    public static void render(PoseStack poseStack,
                              MultiBufferSource buffers,
                              float animationTime,
                              float centerY,
                              float size,
                              float bobAmplitude,
                              float bobRadiansPerTick,
                              float yawDegreesPerTick,
                              float pitchDegreesPerTick,
                              int rgb,
                              int alpha) {
        render(poseStack, buffers, (double) animationTime, centerY, size, bobAmplitude,
                bobRadiansPerTick, yawDegreesPerTick, pitchDegreesPerTick, rgb, alpha);
    }

    public static void render(PoseStack poseStack,
                              MultiBufferSource buffers,
                              double animationTime,
                              float centerY,
                              float size,
                              float bobAmplitude,
                              float bobRadiansPerTick,
                              float yawDegreesPerTick,
                              float pitchDegreesPerTick,
                              int rgb,
                              int alpha) {
        float bob = (float) (Math.sin(animationTime * bobRadiansPerTick) * bobAmplitude);

        poseStack.pushPose();
        poseStack.translate(0.5F, centerY + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (float) ((animationTime * yawDegreesPerTick) % 360.0D)));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) ((animationTime * pitchDegreesPerTick) % 360.0D)));

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffers.getBuffer(TRANSLUCENT_EMISSIVE);
        float half = size * 0.5F;

        face(vertices, pose,
                -half, -half, -half,
                 half, -half, -half,
                 half, -half,  half,
                -half, -half,  half,
                0.0F, -1.0F, 0.0F, rgb, alpha, 0.82F);
        face(vertices, pose,
                -half,  half,  half,
                 half,  half,  half,
                 half,  half, -half,
                -half,  half, -half,
                0.0F, 1.0F, 0.0F, rgb, alpha, 1.0F);
        face(vertices, pose,
                 half, -half, -half,
                -half, -half, -half,
                -half,  half, -half,
                 half,  half, -half,
                0.0F, 0.0F, -1.0F, rgb, alpha, 0.86F);
        face(vertices, pose,
                -half, -half, half,
                 half, -half, half,
                 half,  half, half,
                -half,  half, half,
                0.0F, 0.0F, 1.0F, rgb, alpha, 0.94F);
        face(vertices, pose,
                -half, -half, -half,
                -half, -half,  half,
                -half,  half,  half,
                -half,  half, -half,
                -1.0F, 0.0F, 0.0F, rgb, alpha, 0.84F);
        face(vertices, pose,
                half, -half,  half,
                half, -half, -half,
                half,  half, -half,
                half,  half,  half,
                1.0F, 0.0F, 0.0F, rgb, alpha, 0.96F);

        poseStack.popPose();
    }

    /**
     * Renders an externally defined source with the same fixed-pivot rule as
     * the baked block/item paths.  The seed fixes its initial attitude; only
     * the slow yaw changes with animation time.
     */
    public static void renderAnchored(PoseStack poseStack,
                                      MultiBufferSource buffers,
                                      double animationTime,
                                      float centerX,
                                      float centerY,
                                      float centerZ,
                                      float size,
                                      long orientationSeed,
                                      int rgb,
                                      int alpha) {
        SourceVeinAnimation.Orientation orientation = SourceVeinAnimation.orientation(orientationSeed);
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(orientation.yaw()
                + SourceVeinAnimation.rotationDegrees(animationTime, 0.18D)));
        poseStack.mulPose(Axis.XP.rotationDegrees(orientation.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(orientation.roll()));

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffers.getBuffer(TRANSLUCENT_EMISSIVE);
        float half = size * 0.5F;
        face(vertices, pose,
                -half, -half, -half,
                 half, -half, -half,
                 half, -half,  half,
                -half, -half,  half,
                0.0F, -1.0F, 0.0F, rgb, alpha, 0.82F);
        face(vertices, pose,
                -half,  half,  half,
                 half,  half,  half,
                 half,  half, -half,
                -half,  half, -half,
                0.0F, 1.0F, 0.0F, rgb, alpha, 1.0F);
        face(vertices, pose,
                 half, -half, -half,
                -half, -half, -half,
                -half,  half, -half,
                 half,  half, -half,
                0.0F, 0.0F, -1.0F, rgb, alpha, 0.86F);
        face(vertices, pose,
                -half, -half, half,
                 half, -half, half,
                 half,  half, half,
                -half,  half, half,
                0.0F, 0.0F, 1.0F, rgb, alpha, 0.94F);
        face(vertices, pose,
                -half, -half, -half,
                -half, -half,  half,
                -half,  half,  half,
                -half,  half, -half,
                -1.0F, 0.0F, 0.0F, rgb, alpha, 0.84F);
        face(vertices, pose,
                 half, -half,  half,
                 half, -half, -half,
                 half,  half, -half,
                 half,  half,  half,
                1.0F, 0.0F, 0.0F, rgb, alpha, 0.96F);
        poseStack.popPose();
    }

    private static void face(VertexConsumer vertices,
                             PoseStack.Pose pose,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float normalX, float normalY, float normalZ,
                             int rgb, int alpha, float shade) {
        int red = shaded(rgb >> 16 & 0xFF, shade);
        int green = shaded(rgb >> 8 & 0xFF, shade);
        int blue = shaded(rgb & 0xFF, shade);

        vertex(vertices, pose, x0, y0, z0, 0.0F, 1.0F,
                normalX, normalY, normalZ, red, green, blue, alpha);
        vertex(vertices, pose, x1, y1, z1, 1.0F, 1.0F,
                normalX, normalY, normalZ, red, green, blue, alpha);
        vertex(vertices, pose, x2, y2, z2, 1.0F, 0.0F,
                normalX, normalY, normalZ, red, green, blue, alpha);
        vertex(vertices, pose, x3, y3, z3, 0.0F, 0.0F,
                normalX, normalY, normalZ, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices,
                               PoseStack.Pose pose,
                               float x, float y, float z,
                               float u, float v,
                               float normalX, float normalY, float normalZ,
                               int red, int green, int blue, int alpha) {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static int shaded(int channel, float shade) {
        return Mth.clamp(Math.round(channel * shade), 0, 255);
    }

    private FloatingCubeRenderer() {
    }
}
