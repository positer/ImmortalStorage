package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.item.custom.OneQiReturningOriginSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Renders the charged attack as continuous line geometry, never particles. */
public final class OneQiBeamRenderer {
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Player player : minecraft.level.players()) {
            int phase = OneQiReturningOriginSwordItem.beamPhase(player);
            if (phase != 0) renderPlayerBeam(poses, buffers, minecraft, player, camera, partialTick, phase);
        }
        poses.popPose();
        buffers.endBatch(RenderType.lightning());
    }

    private static void renderPlayerBeam(PoseStack poses, MultiBufferSource buffers, Minecraft minecraft,
                                         Player player, Vec3 camera, float partialTick, int phase) {
        Vec3 look = player.getViewVector(partialTick).normalize();
        boolean localFirstPerson = player == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson();
        Vec3 liveMuzzle = localFirstPerson ? OneQiHeldItemMuzzle.current() : null;
        Vec3 muzzle = liveMuzzle != null ? liveMuzzle : handPosition(player, look, partialTick);
        double visualRange = Math.max(256.0D,
                minecraft.options.renderDistance().get() * 16.0D * 1.5D);
        Vec3 end = player.getEyePosition(partialTick).add(look.scale(visualRange));
        Vec3 start = localFirstPerson
                ? muzzle.subtract(end.subtract(muzzle).normalize().scale(4.0D))
                : muzzle;
        if (phase == 3) {
            beam(poses, buffers, camera, start, end, 0.96F, 0.98F, 1.0F, 0.86F, 0.040D);
            beam(poses, buffers, camera, start, end, 0.12F, 0.48F, 1.0F, 1.0F, 0.013D);
        } else {
            beam(poses, buffers, camera, start, end, 0.95F, 1.0F, 1.0F, 0.95F,
                    phase == 1 ? 0.004D : 0.016D);
        }
    }

    private static Vec3 handPosition(Player player, Vec3 look, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        HumanoidArm usedArm = player.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        double side = usedArm == HumanoidArm.RIGHT ? 1.0D : -1.0D;
        return eye.add(right.scale(side * 0.34D))
                .add(0.0D, -0.42D, 0.0D)
                .add(look.scale(0.20D));
    }

    private static void beam(PoseStack poses, MultiBufferSource buffers, Vec3 camera,
                             Vec3 start, Vec3 end, float red, float green, float blue,
                             float alpha, double halfWidth) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 midpointToCamera = camera.subtract(start.add(end).scale(0.5D)).normalize();
        Vec3 side = direction.cross(midpointToCamera);
        if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
        side = side.normalize().scale(halfWidth);
        VertexConsumer beam = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poses.last();
        vertex(beam, pose, start.add(side), red, green, blue, alpha);
        vertex(beam, pose, end.add(side), red, green, blue, alpha);
        vertex(beam, pose, end.subtract(side), red, green, blue, alpha);
        vertex(beam, pose, start.subtract(side), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer beam, PoseStack.Pose pose, Vec3 position,
                               float red, float green, float blue, float alpha) {
        beam.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha);
    }

    private OneQiBeamRenderer() {}
}
