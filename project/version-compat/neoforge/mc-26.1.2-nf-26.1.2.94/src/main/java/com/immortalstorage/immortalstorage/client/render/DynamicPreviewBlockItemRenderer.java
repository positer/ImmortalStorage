package com.immortalstorage.immortalstorage.client.render;

import java.util.function.Consumer;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compat.mc2612.SpecialModelGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** 26.1 special-model implementation for animated open-frame item previews. */
public final class DynamicPreviewBlockItemRenderer implements SpecialModelRenderer<ItemStack> {
    public static final DynamicPreviewBlockItemRenderer INSTANCE = new DynamicPreviewBlockItemRenderer();

    private DynamicPreviewBlockItemRenderer() {}

    @Override
    public void submit(ItemStack stack, PoseStack poses, SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean foil, int outlineColor) {
        SpecialModelGeometry.submitBlockBase(stack, poses, collector,
                packedLight, packedOverlay, outlineColor);
        Minecraft minecraft = Minecraft.getInstance();
        float ticks = minecraft.level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : minecraft.level.getGameTime()
                        + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (stack.is(ModBlocks.STABILIZED_MINIATURE_IMMORTAL_RUIN.get().asItem())) {
            poses.pushPose();
            poses.translate(0.5F, 0.5F, 0.72F);
            float radius = 0.24F + Mth.sin(ticks * 0.12F) * 0.025F;
            SpecialModelGeometry.submit(collector, poses,
                    buffers -> MiniatureImmortalRuinRenderer.drawDisc(
                            poses, buffers, radius, reversed(stack)));
            poses.popPose();
        } else if (stack.is(ModBlocks.XIANQIAO_MANAGER.get().asItem())) {
            SpecialModelGeometry.submit(collector, poses,
                    buffers -> FloatingCubeRenderer.render(poses, buffers, ticks,
                            0.5F, 0.34F, 0.035F, 0.075F, 0.72F, 0.43F,
                            0xF7FBFF, 104));
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
        consumer.accept(new Vector3f(0.5F, 0.5F, 0.5F));
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    private static boolean reversed(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag.getBooleanOr("Reversed", false);
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
