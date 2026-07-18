package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.item.custom.SpiritStaffItem;
import com.cultivation.cultivation.item.custom.SpiritStaffBuildExecutor;
import com.cultivation.cultivation.network.ModPayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Full-frame build-mode outline generated from the same geometry as the server commit. */
public final class SpiritStaffBuildPreview {
    private static int previewCount = -1;
    private static SpiritStaffBuildExecutor.Failure previewFailure;
    private static PreviewTarget requestedTarget;
    private static int requestId;
    private static long lastRequestTick = Long.MIN_VALUE;
    private static List<BlockPos> serverPositions = List.of();

    public static void init(IEventBus forgeBus) {
        forgeBus.addListener(SpiritStaffBuildPreview::renderWorldPreview);
        forgeBus.addListener(SpiritStaffBuildPreview::renderHudCount);
        forgeBus.addListener(SpiritStaffBuildPreview::removeLayerOnControlUse);
    }

    private static void removeLayerOnControlUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || !net.minecraft.client.gui.screens.Screen.hasControlDown()
                || minecraft.player == null || minecraft.screen != null
                || !(minecraft.hitResult instanceof BlockHitResult hit)) return;
        ItemStack held = minecraft.player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof SpiritStaffItem)
                || SpiritStaffItem.getMode(held) != SpiritStaffItem.MODE_BUILD) return;
        PacketDistributor.sendToServer(new ModPayloads.RemoveSpiritStaffBuildLayer(
                hit.getBlockPos(), hit.getDirection().ordinal(), event.getHand().ordinal()));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private static void renderWorldPreview(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            reset();
            return;
        }
        InteractionHand hand = buildHand(minecraft.player.getMainHandItem(),
                minecraft.player.getOffhandItem());
        if (hand == null) {
            reset();
            return;
        }

        BlockHitResult hit = event.getTarget();
        PreviewTarget current = new PreviewTarget(
                hit.getBlockPos().asLong(), hit.getDirection().ordinal(), hand.ordinal());
        long gameTick = minecraft.level.getGameTime();
        boolean targetChanged = !current.equals(requestedTarget);
        if (targetChanged || gameTick - lastRequestTick >= 5L) {
            requestedTarget = current;
            lastRequestTick = gameTick;
            requestId = requestId == Integer.MAX_VALUE ? 1 : requestId + 1;
            if (targetChanged) {
                serverPositions = List.of();
                previewFailure = null;
            }
            PacketDistributor.sendToServer(new ModPayloads.RequestSpiritStaffBuildPreview(
                    requestId, hit.getBlockPos(), hit.getDirection().ordinal(), hand.ordinal()));
        }
        if (previewFailure == null) return;
        previewCount = serverPositions.size();
        if (serverPositions.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (BlockPos pos : serverPositions) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(pos).inflate(0.002D),
                    0.15F, 0.95F, 0.85F, 0.9F);
        }
        poseStack.popPose();
    }

    private static void renderHudCount(RenderGuiEvent.Post event) {
        if (previewCount < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        String key;
        if (previewCount > 0) {
            key = "message.cultivation.spirit_staff.build.preview";
        } else if (previewFailure == SpiritStaffBuildExecutor.Failure.NO_MATERIALS) {
            key = "message.cultivation.spirit_staff.build.preview_no_materials";
        } else if (previewFailure == SpiritStaffBuildExecutor.Failure.NOT_A_BLOCK_ITEM) {
            key = "message.cultivation.spirit_staff.build.preview_not_block";
        } else {
            key = "message.cultivation.spirit_staff.build.preview_empty";
        }
        Component label = previewCount > 0
                ? Component.translatable(key, previewCount)
                : Component.translatable(key);
        event.getGuiGraphics().drawCenteredString(
                minecraft.font, label,
                event.getGuiGraphics().guiWidth() / 2,
                event.getGuiGraphics().guiHeight() / 2 + 24,
                previewCount == 0 ? 0xFFFF5555 : 0xFF55FFFF);
        previewCount = -1;
    }

    public static void applyServerSnapshot(ModPayloads.SpiritStaffBuildPreviewSnapshot snapshot) {
        if (snapshot == null || snapshot.requestId() != requestId || requestedTarget == null
                || snapshot.pos() != requestedTarget.pos() || snapshot.face() != requestedTarget.face()
                || snapshot.hand() != requestedTarget.hand()) {
            return;
        }
        SpiritStaffBuildExecutor.Failure[] failures = SpiritStaffBuildExecutor.Failure.values();
        previewFailure = snapshot.failure() >= 0 && snapshot.failure() < failures.length
                ? failures[snapshot.failure()]
                : SpiritStaffBuildExecutor.Failure.INVALID_CONTEXT;
        serverPositions = snapshot.positions().stream().map(BlockPos::of).toList();
    }

    private static void reset() {
        requestedTarget = null;
        serverPositions = List.of();
        previewCount = -1;
        previewFailure = null;
        lastRequestTick = Long.MIN_VALUE;
    }

    private static InteractionHand buildHand(ItemStack mainHand, ItemStack offHand) {
        if (mainHand.getItem() instanceof SpiritStaffItem
                && SpiritStaffItem.getMode(mainHand) == SpiritStaffItem.MODE_BUILD) {
            return InteractionHand.MAIN_HAND;
        }
        if (offHand.getItem() instanceof SpiritStaffItem
                && SpiritStaffItem.getMode(offHand) == SpiritStaffItem.MODE_BUILD) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private record PreviewTarget(long pos, int face, int hand) {}

    private SpiritStaffBuildPreview() {}
}
