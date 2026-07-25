package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.client.render.OneQiHeldItemMuzzle;
import com.immortalstorage.immortalstorage.item.custom.OneQiReturningOriginSwordItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the sword model center after vanilla has applied the complete live hand transform. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererOneQiMuzzleMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", shift = At.Shift.AFTER))
    private void immortalstorage$captureOneQiMuzzle(
            ItemStack stack, ItemDisplayContext context, boolean leftHand,
            PoseStack poses, MultiBufferSource buffers, int light, int overlay,
            BakedModel model, CallbackInfo ci) {
        if (stack.getItem() instanceof OneQiReturningOriginSwordItem && context.firstPerson()) {
            OneQiHeldItemMuzzle.capture(poses, context);
        }
    }
}
