package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/** Captures the live center of the first-person item after every target render transform. */
public final class OneQiHeldItemMuzzle {
    private static Vec3 position;
    private static long frameNanos;

    /** Retains the canonical center contract for callers that already own the fully transformed pose. */
    public static void capture(PoseStack poses, ItemDisplayContext context) {
        if (!context.firstPerson()) return;
        store(poses, new Vector4f(0.5F, 0.5F, 0.5F, 1.0F));
    }

    /**
     * Captures a center returned by {@code ItemStackRenderState#getModelBoundingBox()}.
     * In 26.1.2 that center already includes the item model's first-person and local transforms;
     * the supplied pose contributes the live hand, swing, bob and equip transforms.
     */
    public static void capture(PoseStack poses, ItemDisplayContext context, Vec3 modelCenter) {
        if (!context.firstPerson()) return;
        store(poses, new Vector4f(
                (float) modelCenter.x(), (float) modelCenter.y(), (float) modelCenter.z(), 1.0F));
    }

    private static void store(PoseStack poses, Vector4f center) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        center.mul(poses.last().pose());
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        position = camera.add(center.x(), center.y(), center.z());
        frameNanos = System.nanoTime();
    }

    public static Vec3 current() {
        return System.nanoTime() - frameNanos <= 100_000_000L ? position : null;
    }

    private OneQiHeldItemMuzzle() {}
}
