package com.immortalstorage.immortalstorage.client.render;

import java.util.function.Consumer;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerDisplayState;
import com.immortalstorage.immortalstorage.compat.mc2612.SpecialModelGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** 26.1 special-model implementation for the rotating source-manager core. */
public final class SourceVeinManagerItemRenderer implements SpecialModelRenderer<ItemStack> {
    public static final SourceVeinManagerItemRenderer INSTANCE = new SourceVeinManagerItemRenderer();
    private SourceVeinManagerItemRenderer() {}

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
        int state = itemDisplayState(stack);
        SpecialModelGeometry.submit(collector, poses,
                buffers -> SourceVeinManagerRenderer.drawCore(
                        poses, buffers, state, ticks * SourceVeinManagerRenderer.DEGREES_PER_TICK));
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

    private static int itemDisplayState(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return 0;
        CompoundTag tag = data.copyTag();
        if (tag.contains(SourceVeinManagerDisplayState.TAG)) {
            return Math.max(0, Math.min(SourceVeinManagerDisplayState.MAX_STATE,
                    tag.getIntOr(SourceVeinManagerDisplayState.TAG, 0)));
        }
        CompoundTag members = tag.getCompoundOrEmpty("Members");
        if (!members.isEmpty()) {
            ListTag items = members.getListOrEmpty("Items");
            return SourceVeinManagerDisplayState.stateForFilled(items.size());
        }
        return 0;
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
