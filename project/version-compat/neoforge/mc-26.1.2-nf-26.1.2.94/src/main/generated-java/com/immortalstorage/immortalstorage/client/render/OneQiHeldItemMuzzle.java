package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector4f;

/** Captures the live center of the first-person item after every vanilla hand transform. */
public final class OneQiHeldItemMuzzle {
    private static net.minecraft.world.phys.Vec3 position;
    private static long frameNanos;

    public static void capture(PoseStack poses, ItemDisplayContext context) {
        if (!context.firstPerson()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // ItemRenderer has already translated model coordinates by -0.5 on every axis.
        Vector4f center = new Vector4f(0.5F, 0.5F, 0.5F, 1.0F).mul(poses.last().pose());
        net.minecraft.world.phys.Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        position = camera.add(center.x(), center.y(), center.z());
        frameNanos = System.nanoTime();
    }

    public static net.minecraft.world.phys.Vec3 current() {
        return System.nanoTime() - frameNanos <= 100_000_000L ? position : null;
    }

    private OneQiHeldItemMuzzle() {}
}
