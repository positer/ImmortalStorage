package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.compat.mc2612.CompatRenderTypes;
import com.immortalstorage.immortalstorage.compat.mc2612.SpecialModelGeometry;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    private static final RenderType BLOCK_TYPE =
            CompatRenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    // Match the 1.21.1 block-space pivot exactly. The 26.1 submission API no
    // longer applies the old implicit item/block offset for custom geometry.
    private static final float CENTER_Y = 0.5F;
    private static final float SIZE = 0.42F;
    private static final int FLOATING_ALPHA = 166;

    public static void submit(SourceDefinition definition, float animationTime,
                              PoseStack poses, SubmitNodeCollector collector,
                              int packedLight, int packedOverlay) {
        submit(definition, animationTime, poses, collector, packedLight, packedOverlay,
                null, null);
    }

    /**
     * Submits the floating output from the special item-model pipeline. The
     * 26.1 item wrapper has already applied its vanilla -0.5 origin transform,
     * so the same geometric model centre is used with the item pivot at 0.5.
     */
    public static void submitForItem(SourceDefinition definition, float animationTime,
                                     PoseStack poses, SubmitNodeCollector collector,
                                     int packedLight, int packedOverlay) {
        submitAt(definition, animationTime, poses, collector, packedLight, packedOverlay,
                null, null);
    }

    public static void submit(SourceDefinition definition, float animationTime,
                              PoseStack poses, SubmitNodeCollector collector,
                              int packedLight, int packedOverlay,
                              @Nullable BlockAndTintGetter tintLevel,
                              @Nullable BlockPos tintPos) {
        submitAt(definition, animationTime, poses, collector, packedLight, packedOverlay,
                tintLevel, tintPos);
    }

    private static void submitAt(SourceDefinition definition, float animationTime,
                                 PoseStack poses, SubmitNodeCollector collector,
                                 int packedLight, int packedOverlay,
                                 @Nullable BlockAndTintGetter tintLevel,
                                 @Nullable BlockPos tintPos) {
        if (definition == null) return;
        if (definition.fluid()) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(definition.outputId()).orElse(null);
            if (fluid != null && fluid != Fluids.EMPTY
                    && submitFluid(fluid, animationTime, poses, collector, tintLevel, tintPos)) return;
        } else {
            Item item = BuiltInRegistries.ITEM.getOptional(definition.outputId()).orElse(null);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack output = new ItemStack(item);
                if (item instanceof BlockItem blockItem) {
                    submitBlock(blockItem, animationTime, poses, collector,
                            packedLight, packedOverlay, tintLevel, tintPos);
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
                                    int packedLight, int packedOverlay,
                                    @Nullable BlockAndTintGetter tintLevel,
                                    @Nullable BlockPos tintPos) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.getRenderShape() == RenderShape.INVISIBLE) return;
        poses.pushPose();
        applyBlockTransform(poses, animationTime, 0.36F, blockCenter(state),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).hashCode());
        SpecialModelGeometry.submit(collector, poses,
                buffers -> renderTranslucentBlock(state, poses, buffers,
                        packedLight, packedOverlay, tintLevel, tintPos));
        poses.popPose();
    }

    private static void submitItem(ItemStack output, float animationTime,
                                   PoseStack poses, SubmitNodeCollector collector,
                                   int packedLight, int packedOverlay) {
        AABB bounds = SpecialModelGeometry.itemModelBounds(output);
        poses.pushPose();
        applyItemTransform(poses, animationTime, 0.48F, center(bounds));
        SpecialModelGeometry.submitNestedItem(output, 0.0F, 0.0F, 0.0F, 1.0F,
                poses, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }

    private static void applyBlockTransform(PoseStack poses, float animationTime,
                                            float scale, Center modelCenter, long seed) {
        float bob = SourceVeinAnimation.bob(animationTime);
        SourceVeinAnimation.Orientation orientation = SourceVeinAnimation.orientation(seed);
        poses.translate(0.5F, CENTER_Y + bob, 0.5F);
        poses.mulPose(Axis.YP.rotationDegrees(orientation.yaw()
                + SourceVeinAnimation.rotationDegrees(animationTime, 0.18D)));
        poses.mulPose(Axis.XP.rotationDegrees(orientation.pitch()));
        poses.mulPose(Axis.ZP.rotationDegrees(orientation.roll()));
        poses.scale(scale, scale, scale);
        poses.translate(-modelCenter.x(), -modelCenter.y(), -modelCenter.z());
    }

    private static void applyItemTransform(PoseStack poses, float animationTime,
                                           float scale, Center modelCenter) {
        float bob = SourceVeinAnimation.bob(animationTime);
        poses.translate(0.5F, CENTER_Y + bob, 0.5F);
        // Items remain upright and rotate only around their geometric Y axis.
        poses.mulPose(Axis.YP.rotationDegrees(
                SourceVeinAnimation.rotationDegrees(animationTime, 0.82D)));
        poses.scale(scale, scale, scale);
        poses.translate(-modelCenter.x(), -modelCenter.y(), -modelCenter.z());
    }

    private static void renderTranslucentBlock(BlockState state, PoseStack poses,
                                               MultiBufferSource buffers,
                                               int packedLight, int packedOverlay,
                                               @Nullable BlockAndTintGetter tintLevel,
                                               @Nullable BlockPos tintPos) {
        BlockStateModel model = Minecraft.getInstance().getModelManager()
                .getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        VertexConsumer vertices = buffers.getBuffer(BLOCK_TYPE);
        for (BlockStateModelPart part : parts) {
            renderTranslucentQuads(state, part.getQuads(null), poses, vertices,
                    packedLight, packedOverlay, tintLevel, tintPos);
            for (Direction direction : Direction.values()) {
                renderTranslucentQuads(state, part.getQuads(direction), poses, vertices,
                        packedLight, packedOverlay, tintLevel, tintPos);
            }
        }
    }

    private static void renderTranslucentQuads(BlockState state, List<BakedQuad> quads,
                                               PoseStack poses, VertexConsumer vertices,
                                               int packedLight, int packedOverlay,
                                               @Nullable BlockAndTintGetter tintLevel,
                                               @Nullable BlockPos tintPos) {
        for (BakedQuad quad : quads) {
            int tint = 0xFFFFFF;
            if (quad.materialInfo().isTinted()) {
                var source = Minecraft.getInstance().getBlockColors()
                        .getTintSource(state, quad.materialInfo().tintIndex());
                if (source != null) {
                    tint = tintLevel != null && tintPos != null
                            ? source.colorInWorld(state, tintLevel, tintPos)
                            : source.color(state);
                }
            }
            QuadInstance instance = new QuadInstance();
            instance.setColor((FLOATING_ALPHA << 24) | (tint & 0xFFFFFF));
            instance.setLightCoords(packedLight);
            instance.setOverlayCoords(packedOverlay);
            vertices.putBakedQuad(poses.last(), quad, instance);
        }
    }

    private static boolean submitFluid(Fluid fluid, float animationTime,
                                       PoseStack poses, SubmitNodeCollector collector,
                                       @Nullable BlockAndTintGetter tintLevel,
                                       @Nullable BlockPos tintPos) {
        FluidModel model = Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet().get(fluid.defaultFluidState());
        if (model == null || model.stillMaterial() == null) return false;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        BlockState fluidState = fluid.defaultFluidState().createLegacyBlock();
        int tint = fluidTint(fluid, model, fluidState, tintLevel, tintPos);
        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;
        SpecialModelGeometry.submit(collector, poses,
                buffers -> renderFluidCube(animationTime, poses, buffers,
                        sprite, red, green, blue));
        return true;
    }

    private static int fluidTint(Fluid fluid, FluidModel model, BlockState fluidState,
                                 @Nullable BlockAndTintGetter tintLevel,
                                 @Nullable BlockPos tintPos) {
        if ((fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)
                && tintLevel != null && tintPos != null) {
            return BiomeColors.getAverageWaterColor(tintLevel, tintPos);
        }
        if (model.tintSource() == null) return 0xFFFFFFFF;
        return tintLevel != null && tintPos != null
                ? model.tintSource().colorInWorld(fluidState, tintLevel, tintPos)
                : model.tintSource().color(fluidState);
    }

    private static Center blockCenter(BlockState state) {
        BlockStateModel model = Minecraft.getInstance().getModelManager()
                .getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        Bounds bounds = null;
        for (BlockStateModelPart part : parts) {
            bounds = include(bounds, part.getQuads(null));
            for (Direction direction : Direction.values()) {
                bounds = include(bounds, part.getQuads(direction));
            }
        }
        return bounds == null ? new Center(0.5F, 0.5F, 0.5F) : bounds.center();
    }

    private static Center center(@Nullable AABB bounds) {
        if (bounds == null) return new Center(0.0F, 0.0F, 0.0F);
        return new Center((float) ((bounds.minX + bounds.maxX) * 0.5D),
                (float) ((bounds.minY + bounds.maxY) * 0.5D),
                (float) ((bounds.minZ + bounds.maxZ) * 0.5D));
    }

    private static @Nullable Bounds include(@Nullable Bounds current, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
                var position = quad.position(vertex);
                if (!Float.isFinite(position.x()) || !Float.isFinite(position.y())
                        || !Float.isFinite(position.z())) continue;
                current = current == null
                        ? new Bounds(position.x(), position.y(), position.z(),
                        position.x(), position.y(), position.z())
                        : current.include(position.x(), position.y(), position.z());
            }
        }
        return current;
    }

    private record Center(float x, float y, float z) {
    }

    private record Bounds(float minX, float minY, float minZ,
                          float maxX, float maxY, float maxZ) {
        private Bounds include(float x, float y, float z) {
            return new Bounds(Math.min(minX, x), Math.min(minY, y), Math.min(minZ, z),
                    Math.max(maxX, x), Math.max(maxY, y), Math.max(maxZ, z));
        }

        private Center center() {
            return new Center((minX + maxX) * 0.5F,
                    (minY + maxY) * 0.5F,
                    (minZ + maxZ) * 0.5F);
        }
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
