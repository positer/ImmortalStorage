package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.client.render.OneQiHeldItemMuzzle;
import com.immortalstorage.immortalstorage.item.custom.OneQiReturningOriginSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the removed 1.21.1 ItemRenderer muzzle hook in the 26.1.2 item-state pipeline. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererOneQiMuzzleMixin {
    @Unique
    private boolean immortalstorage$captureOneQiMuzzle;
    @Unique
    private ItemDisplayContext immortalstorage$oneQiDisplayContext = ItemDisplayContext.NONE;

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void immortalstorage$markOneQiMuzzle(
            LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
            CallbackInfo callback) {
        immortalstorage$captureOneQiMuzzle = stack.getItem() instanceof OneQiReturningOriginSwordItem
                && context.firstPerson();
        immortalstorage$oneQiDisplayContext = context;
    }

    @Redirect(method = "renderItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit"
                    + "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void immortalstorage$captureRenderedOneQiMuzzle(
            ItemStackRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            int lightCoords, int overlayCoords, int outlineColor) {
        if (immortalstorage$captureOneQiMuzzle) {
            Vec3 modelCenter = state.getModelBoundingBox().getCenter();
            OneQiHeldItemMuzzle.capture(poseStack, immortalstorage$oneQiDisplayContext, modelCenter);
        }
        immortalstorage$captureOneQiMuzzle = false;
        immortalstorage$oneQiDisplayContext = ItemDisplayContext.NONE;
        state.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
    }
}
