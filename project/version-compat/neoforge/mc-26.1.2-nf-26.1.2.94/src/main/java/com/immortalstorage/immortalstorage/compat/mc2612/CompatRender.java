package com.immortalstorage.immortalstorage.compat.mc2612;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;

/** Small geometry helper for the line-box utility removed by the 26.1 renderer rewrite. */
public final class CompatRender {
    private CompatRender() {}

    public static void renderLineBox(PoseStack poseStack, VertexConsumer vertices, AABB box,
                                     float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        int r = (int) (red * 255.0F);
        int g = (int) (green * 255.0F);
        int b = (int) (blue * 255.0F);
        int a = (int) (alpha * 255.0F);
        line(vertices, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        line(vertices, pose, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);
        line(vertices, pose, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        line(vertices, pose, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(vertices, pose, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(vertices, pose, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(vertices, pose, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void line(VertexConsumer vertices, PoseStack.Pose pose,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             int red, int green, int blue, int alpha) {
        float dx = (float) (x1 - x0);
        float dy = (float) (y1 - y0);
        float dz = (float) (z1 - z0);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6F) return;
        dx /= length;
        dy /= length;
        dz /= length;
        vertices.addVertex(pose, (float) x0, (float) y0, (float) z0)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, dx, dy, dz);
        vertices.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, dx, dy, dz);
    }
}
