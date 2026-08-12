package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinManagerDisplayState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Draws the source vein frame inherited by the manager model plus its
 * eight-segment rotating source core for the inventory and held item. The core
 * reuses the exact geometry of the world {@link SourceVeinManagerRenderer};
 * the occupancy state is derived from the item's persisted
 * {@code BLOCK_ENTITY_DATA} members when present.
 */
public final class SourceVeinManagerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final SourceVeinManagerItemRenderer INSTANCE = new SourceVeinManagerItemRenderer();
    private final SourceVeinAnimation.Clock animationClock = new SourceVeinAnimation.Clock();

    private SourceVeinManagerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        BakedModel base = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath())));

        var baseConsumer = ItemRenderer.getFoilBufferDirect(
                buffers, ItemBlockRenderTypes.getRenderType(stack, true), true, stack.hasFoil());
        minecraft.getItemRenderer().renderModelLists(
                base, stack, packedLight, packedOverlay, poseStack, baseConsumer);

        double logicalTime = minecraft.level == null
                ? SourceVeinAnimation.realTime()
                : SourceVeinAnimation.continuousTime(
                        minecraft.level.getGameTime(), minecraft.getTimer().getGameTimeDeltaPartialTick(true));
        double animationTime = animationClock.sample(logicalTime);
        SourceVeinManagerRenderer.drawCore(poseStack, buffers,
                itemDisplayState(stack), SourceVeinAnimation.rotationDegrees(
                        animationTime, SourceVeinManagerRenderer.DEGREES_PER_TICK));
    }

    private static int itemDisplayState(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return 0;
        CompoundTag tag = data.copyTag();
        if (tag.contains(SourceVeinManagerDisplayState.TAG)) {
            return Math.max(0, Math.min(SourceVeinManagerDisplayState.MAX_STATE,
                    tag.getInt(SourceVeinManagerDisplayState.TAG)));
        }
        CompoundTag members = tag.getCompound("Members");
        if (!members.isEmpty()) {
            ListTag items = members.getList("Items", Tag.TAG_COMPOUND);
            return SourceVeinManagerDisplayState.stateForFilled(items.size());
        }
        return 0;
    }
}
