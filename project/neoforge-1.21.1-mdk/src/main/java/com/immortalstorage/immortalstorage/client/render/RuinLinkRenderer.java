package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class RuinLinkRenderer {
    static void render(PoseStack poses, MultiBufferSource buffers, BlockPos origin, BlockPos linked) {
        if (linked == null || !holdingWrench()) return;
        float dx = linked.getX() - origin.getX();
        float dy = linked.getY() - origin.getY();
        float dz = linked.getZ() - origin.getZ();
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001F) return;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poses.last();
        lines.addVertex(pose, 0.5F, 0.5F, 0.5F).setColor(255, 255, 255, 255)
                .setNormal(pose, dx / length, dy / length, dz / length);
        lines.addVertex(pose, dx + 0.5F, dy + 0.5F, dz + 0.5F).setColor(255, 255, 255, 255)
                .setNormal(pose, dx / length, dy / length, dz / length);
    }

    private static boolean holdingWrench() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }

    private static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof SpiritStaffItem && SpiritStaffItem.getMode(stack) == SpiritStaffItem.MODE_WRENCH;
    }

    private RuinLinkRenderer() {}
}
