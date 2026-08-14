package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.dimension.DomainExpansionManager;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 26.1.2 target override: continuous per-frame outline for an active Domain
 * Expansion.  The volume is centered on the fixed position where the expansion
 * was toggled (the server's {@code worldCenter}), not on the player's live
 * position, so the border stays anchored to the swapped volume.
 */
public final class DomainExpansionHighlight {
    private static volatile boolean expanded;
    /** Fixed world position the expansion was toggled at; null when collapsed. */
    private static volatile BlockPos anchor;

    private DomainExpansionHighlight() {}

    public static boolean isExpanded() {
        return expanded;
    }

    public static void setExpanded(boolean value) {
        expanded = value;
        anchor = value ? captureAnchor() : null;
    }

    public static void toggle() {
        expanded = !expanded;
        anchor = expanded ? captureAnchor() : null;
    }

    private static BlockPos captureAnchor() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.blockPosition() : null;
    }

    private static void deactivate() {
        expanded = false;
        anchor = null;
    }

    public static void render(RenderLevelStageEvent.AfterTranslucentParticles event) {
        if (!expanded) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            deactivate();
            return;
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(mc.player);
        if (data.getStage() < 3) {
            deactivate();
            return;
        }
        int edge = DomainExpansionManager.edgeForStage(data.getStage());
        double radius = (edge - 1) / 2.0;
        // Anchor the border to the fixed expansion position instead of the
        // player's live position, so it does not follow the player.
        BlockPos center = anchor != null ? anchor : mc.player.blockPosition();

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        AABB area = new AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + 1 + radius, center.getY() + 1 + radius, center.getZ() + 1 + radius)
                .inflate(0.002D);
        VertexConsumer lines = buffers.getBuffer(
                com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes.lines());
        com.immortalstorage.immortalstorage.compat.mc2612.CompatRender.renderLineBox(
                poses, lines, area, 0.35F, 0.85F, 1.0F, 0.9F);
        poses.popPose();
        buffers.endBatch(com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes.lines());
    }
}
