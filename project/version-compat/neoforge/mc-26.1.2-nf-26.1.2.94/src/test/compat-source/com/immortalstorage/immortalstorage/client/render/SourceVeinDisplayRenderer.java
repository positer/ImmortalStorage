package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;
import com.immortalstorage.immortalstorage.compat.mc2612.SpecialModelGeometry;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 26.1 rule renderer for source definitions.
 *
 * <p>Fluid output samples the baked fluid material used by the target model
 * pipeline on every face. Item output submits the actual block/item model,
 * while unknown external definitions keep the same translucent fallback as
 * the canonical lane. No integration-specific switch is involved.</p>
 */
public final class SourceVeinDisplayRenderer {
    private static final RenderType FLUID_TYPE =
            CompatRenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final float CENTER_Y = 0.34F;
    private static final float SIZE = 0.42F;
    private static final int FLOATING_ALPHA = 166;

    public static void submit(SourceDefinition definition, float animationTime,
                              PoseStack poses, SubmitNodeCollector collector,
                              int packedLight, int packedOverlay) {
        if (definition == null) return;
        if (definition.fluid()) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(definition.outputId()).orElse(null);
            if (fluid != null && fluid != Fluids.EMPTY
                    && submitFluid(fluid, animationTime, poses, collector)) return;
        } else {
            Item item = BuiltInRegistries.ITEM.getOptional(definition.outputId()).orElse(null);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack output = new ItemStack(item);
                if (item instanceof BlockItem blockItem) {
                    submitBlock(blockItem, animationTime, poses, collector,
                            packedLight, packedOverlay);
                } else {
                    submitItem(output, animationTime, poses, collector,
                            packedLight, packedOverlay);
                }
                return;
            }
        }
        SpecialModelGeometry.submit(collector, poses,
                buffers -> FloatingCubeRenderer.render(poses, buffers, animationTime,
                        CENTER_Y, SIZE, 0.035F, 0.085F, 0.82F, 0.51F,
                        definition.coreColor(), FLOATING_ALPHA));
    }

    private static void submitBlock(BlockItem blockItem, float animationTime,
                                    PoseStack poses, SubmitNodeCollector collector,
                                    int packedLight, int packedOverlay) {
        if (blockItem.getBlock().defaultBlockState().getRenderShape() == RenderShape.INVISIBLE) return;
        poses.pushPose();
        animateTransform(poses, animationTime);
        poses.translate(-0.5F, -0.5F, -0.5F);
        poses.scale(0.48F, 0.48F, 0.48F);
        SpecialModelGeometry.submitBlockBase(new ItemStack(blockItem), poses, collector,
                packedLight, packedOverlay, 0);
        poses.popPose();
    }

    private static void submitItem(ItemStack output, float animationTime,
                                   PoseStack poses, SubmitNodeCollector collector,
                                   int packedLight, int packedOverlay) {
        poses.pushPose();
        animateTransform(poses, animationTime);
        SpecialModelGeometry.submitNestedItem(output, 0.0F, 0.0F, 0.0F, 0.34F,
                poses, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }

    private static void animateTransform(PoseStack poses, float animationTime) {
        float bob = Mth.sin(animationTime * 0.085F) * 0.035F;
        poses.translate(0.5F, CENTER_Y + bob, 0.5F);
        poses.mulPose(Axis.YP.rotationDegrees(animationTime * 0.82F));
        poses.mulPose(Axis.XP.rotationDegrees(animationTime * 0.51F));
    }

    private static boolean submitFluid(Fluid fluid, float animationTime,
                                       PoseStack poses, SubmitNodeCollector collector) {
        FluidModel model = Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet().get(fluid.defaultFluidState());
        if (model == null || model.stillMaterial() == null) return false;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tint = model.tintSource() == null
                ? 0xFFFFFFFF
                : model.tintSource().color(fluid.defaultFluidState().createLegacyBlock());
        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;
        SpecialModelGeometry.submit(collector, poses,
                buffers -> renderFluidCube(animationTime, poses, buffers,
                        sprite, red, green, blue));
        return true;
    }

    private static void renderFluidCube(float animationTime, PoseStack poses,
                                        MultiBufferSource buffers,
                                        TextureAtlasSprite sprite,
                                        int red, int green, int blue) {
        float bob = Mth.sin(animationTime * 0.085F) * 0.035F;
        poses.pushPose();
        poses.translate(0.5F, CENTER_Y + bob, 0.5F);
        poses.mulPose(Axis.YP.rotationDegrees(animationTime * 0.82F));
        poses.mulPose(Axis.XP.rotationDegrees(animationTime * 0.51F));
        VertexConsumer vertices = buffers.getBuffer(FLUID_TYPE);
        PoseStack.Pose pose = poses.last();
        float half = SIZE * 0.5F;
        face(vertices, pose, -half, -half, -half, half, -half, -half,
                half, -half, half, -half, -half, half, 0, -1, 0,
                sprite, red, green, blue);
        face(vertices, pose, -half, half, half, half, half, half,
                half, half, -half, -half, half, -half, 0, 1, 0,
                sprite, red, green, blue);
        face(vertices, pose, half, -half, -half, -half, -half, -half,
                -half, half, -half, half, half, -half, 0, 0, -1,
                sprite, red, green, blue);
        face(vertices, pose, -half, -half, half, half, -half, half,
                half, half, half, -half, half, half, 0, 0, 1,
                sprite, red, green, blue);
        face(vertices, pose, -half, -half, -half, -half, -half, half,
                -half, half, half, -half, half, -half, -1, 0, 0,
                sprite, red, green, blue);
        face(vertices, pose, half, -half, half, half, -half, -half,
                half, half, -half, half, half, half, 1, 0, 0,
                sprite, red, green, blue);
        poses.popPose();
    }

    private static void face(VertexConsumer vertices, PoseStack.Pose pose,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float normalX, float normalY, float normalZ,
                             TextureAtlasSprite sprite, int red, int green, int blue) {
        vertex(vertices, pose, x0, y0, z0, sprite.getU0(), sprite.getV1(),
                normalX, normalY, normalZ, red, green, blue);
        vertex(vertices, pose, x1, y1, z1, sprite.getU1(), sprite.getV1(),
                normalX, normalY, normalZ, red, green, blue);
        vertex(vertices, pose, x2, y2, z2, sprite.getU1(), sprite.getV0(),
                normalX, normalY, normalZ, red, green, blue);
        vertex(vertices, pose, x3, y3, z3, sprite.getU0(), sprite.getV0(),
                normalX, normalY, normalZ, red, green, blue);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ,
                               int red, int green, int blue) {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, FLOATING_ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private SourceVeinDisplayRenderer() {
    }
}
