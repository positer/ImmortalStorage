package com.cultivation.cultivation.client.render;

import com.cultivation.cultivation.block.custom.VeinKind;
import com.cultivation.cultivation.block.entity.SourceVeinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Renders a compact source-colored core for every source-vein variant. */
public final class SourceVeinRenderer implements BlockEntityRenderer<SourceVeinBlockEntity> {
    public SourceVeinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SourceVeinBlockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {
        float worldTime = blockEntity.getLevel() == null
                ? partialTick
                : blockEntity.getLevel().getGameTime() + partialTick;
        float positionPhase = (blockEntity.getBlockPos().asLong() & 0xFFL) * 0.125F;

        FloatingCubeRenderer.render(
                poseStack, buffers, worldTime + positionPhase,
                0.5F, 0.34F,
                0.035F, 0.085F,
                0.82F, 0.51F,
                themeColor(blockEntity), 112);
    }

    private static int themeColor(SourceVeinBlockEntity blockEntity) {
        ItemStack sample = blockEntity.sampleOutput();
        if (!sample.isEmpty()
                && sample.getItem() instanceof BlockItem blockItem
                && blockEntity.getLevel() != null) {
            int mapColor = blockItem.getBlock()
                    .defaultBlockState()
                    .getMapColor(blockEntity.getLevel(), blockEntity.getBlockPos())
                    .col;
            if (mapColor != 0) {
                return liftTowardWhite(mapColor, 0.18F);
            }
        }

        VeinKind kind = blockEntity.getKind();
        return switch (kind) {
            case WATER -> 0x4C8DFF;
            case MILK -> 0xF5F1E8;
            case LAVA -> 0xFF6A1A;
            case COAL -> 0x3C3F48;
            case RAW_COPPER -> 0xD47B55;
            case RAW_IRON -> 0xD8B8A7;
            case RAW_GOLD -> 0xF0C14F;
            case LAPIS -> 0x3F63D8;
            case REDSTONE -> 0xE13A31;
            case CRUDE_SPIRIT_IRON -> 0x83B6C8;
            case SPIRIT_CRYSTAL -> 0x77E5F2;
            case DIAMOND -> 0x63E4D8;
            case EMERALD -> 0x48D978;
            case NETHER_STAR -> 0xE3EDF0;
            case ENCHANTED_GOLDEN_APPLE -> 0xF3CE57;
            default -> 0xA7D9F2;
        };
    }

    private static int liftTowardWhite(int rgb, float amount) {
        int red = lift(rgb >> 16 & 0xFF, amount);
        int green = lift(rgb >> 8 & 0xFF, amount);
        int blue = lift(rgb & 0xFF, amount);
        return red << 16 | green << 8 | blue;
    }

    private static int lift(int channel, float amount) {
        return Math.min(255, Math.round(channel + (255 - channel) * amount));
    }
}
