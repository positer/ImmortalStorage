package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.client.render.GuiItemPreviewOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsItemPreviewMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("TAIL"))
    private void immortalstorage$renderDynamicItemPreview(
            @Nullable LivingEntity entity, @Nullable Level level, ItemStack stack,
            int x, int y, int seed, int depth, CallbackInfo callback) {
        GuiItemPreviewOverlay.render((GuiGraphics) (Object) this, stack, x, y);
    }
}
