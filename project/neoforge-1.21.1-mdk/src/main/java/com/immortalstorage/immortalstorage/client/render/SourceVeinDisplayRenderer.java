package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Rule renderer for source definitions.  It intentionally resolves registry
 * objects from the definition rather than a built-in VeinKind switch, so a
 * datapack/integration-defined source receives the same visual contract.
 */
public final class SourceVeinDisplayRenderer {
    private static final RenderType FLUID_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType BLOCK_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final float BLOCK_CENTER_X = 0.5F;
    private static final float BLOCK_CENTER_Y = 0.5F;
    private static final float BLOCK_CENTER_Z = 0.5F;
    private static final float ITEM_CENTER_X = 0.0F;
    private static final float ITEM_CENTER_Y = 0.0F;
    private static final float ITEM_CENTER_Z = 0.0F;
    private static final float SIZE = 0.42F;
    private static final int FLOATING_ALPHA = 166;
    private static final double SOLID_ROTATION_DEGREES_PER_TICK = 0.18D;
    private static final double ITEM_ROTATION_DEGREES_PER_TICK = 0.82D;

    public static void render(SourceDefinition definition, double animationTime,
                              PoseStack poses, MultiBufferSource buffers,
                              int packedLight, int packedOverlay) {
        render(definition, animationTime, poses, buffers, packedLight, packedOverlay, 0L);
    }

    /** Renders a source in block-world coordinates around the block centre. */
    public static void render(SourceDefinition definition, double animationTime,
                              PoseStack poses, MultiBufferSource buffers,
                              int packedLight, int packedOverlay,
                              long orientationSeed) {
        renderAt(definition, animationTime, poses, buffers, packedLight, packedOverlay,
                BLOCK_CENTER_X, BLOCK_CENTER_Y, BLOCK_CENTER_Z, orientationSeed);
    }

    /** Renders a source inside an item transform around the item origin. */
    public static void renderForItem(SourceDefinition definition, double animationTime,
                                     PoseStack poses, MultiBufferSource buffers,
                                     int packedLight, int packedOverlay,
                                     long orientationSeed) {
        renderAt(definition, animationTime, poses, buffers, packedLight, packedOverlay,
                ITEM_CENTER_X, ITEM_CENTER_Y, ITEM_CENTER_Z, orientationSeed);
    }

    private static void renderAt(SourceDefinition definition, double animationTime,
                                 PoseStack poses, MultiBufferSource buffers,
                                 int packedLight, int packedOverlay,
                                 float centerX, float centerY, float centerZ,
                                 long orientationSeed) {
        if (definition == null) return;
        if (definition.fluid()) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(definition.outputId()).orElse(null);
            if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY
                    && renderFluid(fluid, animationTime, poses, buffers,
                    centerX, centerY, centerZ, orientationSeed)) return;
        } else {
            Item item = BuiltInRegistries.ITEM.getOptional(definition.outputId()).orElse(null);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack output = new ItemStack(item);
                if (item instanceof BlockItem blockItem) {
                    renderBlock(blockItem, animationTime, poses, buffers,
                            packedLight, packedOverlay, centerX, centerY, centerZ,
                            orientationSeed);
                } else {
                    renderOutputItem(output, animationTime, poses, buffers,
                            packedLight, packedOverlay, centerX, centerY, centerZ,
                            orientationSeed);
                }
                return;
            }
        }
        // Unknown external definitions use the same fixed-pivot, deterministic
        // orientation rule instead of disappearing or using a special case.
        FloatingCubeRenderer.renderAnchored(poses, buffers, animationTime,
                centerX, centerY, centerZ, SIZE, orientationSeed,
                definition.coreColor(), FLOATING_ALPHA);
    }

    private static void renderBlock(BlockItem blockItem, double animationTime,
                                    PoseStack poses, MultiBufferSource buffers,
                                    int packedLight, int packedOverlay,
                                    float centerX, float centerY, float centerZ,
                                    long orientationSeed) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.getRenderShape() == RenderShape.INVISIBLE) return;
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        poses.pushPose();
        applyCenteredModelTransform(poses, animationTime, 0.36F,
                SourceVeinModelBounds.center(model, state), centerX, centerY, centerZ,
                orientationSeed);
        // A source block is a translucent preview of the linked block, not a
        // second opaque block.  Keep the model's real UV/quad geometry and
        // only wrap its vertex alpha at the final buffer boundary.
        MultiBufferSource translucentBuffers = renderType -> new AlphaVertexConsumer(
                buffers.getBuffer(renderType), FLOATING_ALPHA / 255.0F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, poses, translucentBuffers, packedLight, packedOverlay,
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, BLOCK_TYPE);
        poses.popPose();
    }

    private static void renderOutputItem(ItemStack output, double animationTime,
                                          PoseStack poses, MultiBufferSource buffers,
                                          int packedLight, int packedOverlay,
                                          float centerX, float centerY, float centerZ,
                                          long orientationSeed) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(output, minecraft.level, null, 0);
        poses.pushPose();
        applyItemModelTransform(poses, animationTime, 0.48F,
                SourceVeinModelBounds.center(model, null), centerX, centerY, centerZ);
        // ItemRenderer.render adds its own camera transform and a
        // translate(-0.5,-0.5,-0.5).  The floating display already applies an
        // explicit baked-model-centre pivot, so using renderModelLists avoids
        // translating the target item a second time and keeps its geometric
        // centre on the source pivot.
        var consumer = ItemRenderer.getFoilBufferDirect(
                buffers, ItemBlockRenderTypes.getRenderType(output, true), true, output.hasFoil());
        minecraft.getItemRenderer().renderModelLists(
                model, output, packedLight, OverlayTexture.NO_OVERLAY, poses, consumer);
        poses.popPose();
    }

    /** Applies one uniform alpha multiplier without changing item/block UVs. */
    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaMultiplier;

        private AlphaVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
            this.delegate = delegate;
            this.alphaMultiplier = alphaMultiplier;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue,
                    Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier))));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad,
                                float red, float green, float blue, float alpha,
                                int packedLight, int packedOverlay) {
            delegate.putBulkData(pose, quad, red, green, blue,
                    alpha * alphaMultiplier, packedLight, packedOverlay);
        }
    }

    private static void applyCenteredModelTransform(PoseStack poses, double animationTime,
                                                   float scale, SourceVeinModelBounds.Center center,
                                                   float centerX, float centerY, float centerZ,
                                                   long orientationSeed) {
        applySolidTransform(poses, animationTime, centerX, centerY, centerZ, orientationSeed);
        poses.scale(scale, scale, scale);
        // Read the actual baked model geometry instead of assuming a full
        // [0,1] cube.  Slab, plant, custom item and injected source models
        // therefore rotate around their true geometric centre.
        poses.translate(-center.x(), -center.y(), -center.z());
    }

    private static void applyItemModelTransform(PoseStack poses, double animationTime,
                                                float scale, SourceVeinModelBounds.Center center,
                                                float centerX, float centerY, float centerZ) {
        poses.translate(centerX, centerY, centerZ);
        poses.mulPose(Axis.YP.rotationDegrees(
                SourceVeinAnimation.rotationDegrees(animationTime, ITEM_ROTATION_DEGREES_PER_TICK)));
        poses.scale(scale, scale, scale);
        poses.translate(-center.x(), -center.y(), -center.z());
    }

    private static void applySolidTransform(PoseStack poses, double animationTime,
                                            float centerX, float centerY, float centerZ,
                                            long orientationSeed) {
        SourceVeinAnimation.Orientation orientation = SourceVeinAnimation.orientation(orientationSeed);
        poses.translate(centerX, centerY, centerZ);
        poses.mulPose(Axis.YP.rotationDegrees(orientation.yaw()
                + SourceVeinAnimation.rotationDegrees(
                animationTime, SOLID_ROTATION_DEGREES_PER_TICK)));
        poses.mulPose(Axis.XP.rotationDegrees(orientation.pitch()));
        poses.mulPose(Axis.ZP.rotationDegrees(orientation.roll()));
    }

    private static boolean renderFluid(Fluid fluid, double animationTime,
                                       PoseStack poses, MultiBufferSource buffers,
                                       float centerX, float centerY, float centerZ,
                                       long orientationSeed) {
        FluidStack stack = new FluidStack(fluid, 1_000);
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluidType());
        ResourceLocation still = extensions.getStillTexture(stack);
        if (still == null) return false;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);
        int tint = extensions.getTintColor(stack);
        int tintAlpha = Math.max(1, tint >>> 24);
        int alpha = Math.max(48, Math.min(FLOATING_ALPHA, Math.round(tintAlpha * (FLOATING_ALPHA / 255.0F))));
        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;

        poses.pushPose();
        applySolidTransform(poses, animationTime, centerX, centerY, centerZ, orientationSeed);
        VertexConsumer vertices = buffers.getBuffer(FLUID_TYPE);
        PoseStack.Pose pose = poses.last();
        float half = SIZE * 0.5F;
        face(vertices, pose, -half, -half, -half, half, -half, -half,
                half, -half, half, -half, -half, half, 0, -1, 0,
                sprite, red, green, blue, alpha);
        face(vertices, pose, -half, half, half, half, half, half,
                half, half, -half, -half, half, -half, 0, 1, 0,
                sprite, red, green, blue, alpha);
        face(vertices, pose, half, -half, -half, -half, -half, -half,
                -half, half, -half, half, half, -half, 0, 0, -1,
                sprite, red, green, blue, alpha);
        face(vertices, pose, -half, -half, half, half, -half, half,
                half, half, half, -half, half, half, 0, 0, 1,
                sprite, red, green, blue, alpha);
        face(vertices, pose, -half, -half, -half, -half, -half, half,
                -half, half, half, -half, half, -half, -1, 0, 0,
                sprite, red, green, blue, alpha);
        face(vertices, pose, half, -half, half, half, -half, -half,
                half, half, -half, half, half, half, 1, 0, 0,
                sprite, red, green, blue, alpha);
        poses.popPose();
        return true;
    }

    private static void face(VertexConsumer vertices, PoseStack.Pose pose,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float normalX, float normalY, float normalZ,
                             TextureAtlasSprite sprite, int red, int green, int blue, int alpha) {
        vertex(vertices, pose, x0, y0, z0, sprite.getU0(), sprite.getV1(), normalX, normalY, normalZ,
                red, green, blue, alpha);
        vertex(vertices, pose, x1, y1, z1, sprite.getU1(), sprite.getV1(), normalX, normalY, normalZ,
                red, green, blue, alpha);
        vertex(vertices, pose, x2, y2, z2, sprite.getU1(), sprite.getV0(), normalX, normalY, normalZ,
                red, green, blue, alpha);
        vertex(vertices, pose, x3, y3, z3, sprite.getU0(), sprite.getV0(), normalX, normalY, normalZ,
                red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ,
                               int red, int green, int blue, int alpha) {
        vertices.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private SourceVeinDisplayRenderer() {}
}
