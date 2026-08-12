package com.immortalstorage.immortalstorage.client.render;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a white semi-transparent quad on the interaction face of the preview
 * box, so players can see at a glance which container face a ruin targets.
 * Shares the full-bright white-texture render type used by
 * {@link FloatingCubeRenderer}; null face renders nothing.
 */
public final class RuinFaceHighlightRenderer {
    private static final Identifier WHITE_TEXTURE =
            Identifier.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType TRANSLUCENT =
            CompatRenderTypes.entityTranslucentEmissive(WHITE_TEXTURE);

    private RuinFaceHighlightRenderer() {
    }

    /** Renders one quad per enabled {@link Direction} bit in the mask; all-off renders nothing. */
    public static void render(PoseStack poses, MultiBufferSource buffers, AABB box, int faceMask) {
        if (faceMask == 0) return;
        for (Direction face : Direction.values()) {
            if ((faceMask & (1 << face.ordinal())) != 0) render(poses, buffers, box, face);
        }
    }

    /** Renders white on enabled faces, then green for pull and red for push. */
    public static void render(PoseStack poses, MultiBufferSource buffers, AABB box,
                              int[] faceModes) {
        for (Direction face : Direction.values()) {
            int mode = faceModes[face.ordinal()];
            if (mode == 0) continue;
            render(poses, buffers, box, face, 1.0F, 1.0F, 1.0F, 0.35F);
            if (mode == 1) {
                render(poses, buffers, box, face, 0.0F, 1.0F, 0.0F, 0.35F);
            } else if (mode == 2) {
                render(poses, buffers, box, face, 1.0F, 0.0F, 0.0F, 0.35F);
            }
        }
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, AABB box,
                              @Nullable Direction face) {
        render(poses, buffers, box, face, 1.0F, 1.0F, 1.0F, 0.35F);
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, AABB box,
                              @Nullable Direction face, float r, float g, float b, float a) {
        if (face == null) return;
        VertexConsumer vertices = buffers.getBuffer(TRANSLUCENT);
        PoseStack.Pose pose = poses.last();
        switch (face) {
            case DOWN -> quad(vertices, pose,
                    box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                    box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                    0.0F, -1.0F, 0.0F, r, g, b, a);
            case UP -> quad(vertices, pose,
                    box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                    box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ,
                    0.0F, 1.0F, 0.0F, r, g, b, a);
            case NORTH -> quad(vertices, pose,
                    box.maxX, box.minY, box.minZ, box.minX, box.minY, box.minZ,
                    box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                    0.0F, 0.0F, -1.0F, r, g, b, a);
            case SOUTH -> quad(vertices, pose,
                    box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ,
                    box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                    0.0F, 0.0F, 1.0F, r, g, b, a);
            case WEST -> quad(vertices, pose,
                    box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ,
                    box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                    -1.0F, 0.0F, 0.0F, r, g, b, a);
            case EAST -> quad(vertices, pose,
                    box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    1.0F, 0.0F, 0.0F, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             float nx, float ny, float nz, float r, float g, float b, float a) {
        vertex(vertices, pose, x0, y0, z0, 0.0F, 1.0F, nx, ny, nz, r, g, b, a);
        vertex(vertices, pose, x1, y1, z1, 1.0F, 1.0F, nx, ny, nz, r, g, b, a);
        vertex(vertices, pose, x2, y2, z2, 1.0F, 0.0F, nx, ny, nz, r, g, b, a);
        vertex(vertices, pose, x3, y3, z3, 0.0F, 0.0F, nx, ny, nz, r, g, b, a);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               double x, double y, double z, float u, float v,
                               float nx, float ny, float nz, float r, float g, float b, float a) {
        vertices.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(pose, nx, ny, nz);
    }
}
