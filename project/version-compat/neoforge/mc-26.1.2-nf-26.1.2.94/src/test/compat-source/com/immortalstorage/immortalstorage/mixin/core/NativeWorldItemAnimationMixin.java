package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.client.render.WorldBlockItemRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native GUI caching must not freeze a world block entity's animated model. */
@Mixin(SpecialModelWrapper.class)
public abstract class NativeWorldItemAnimationMixin {
    @Shadow @Final private SpecialModelRenderer<?> specialRenderer;
    @Inject(method = "update", at = @At("RETURN"))
    private void immortalstorage$animateWorldModel(ItemStackRenderState state, ItemStack item,
            ItemModelResolver resolver, ItemDisplayContext context, ClientLevel level,
            ItemOwner owner, int seed, CallbackInfo callback) {
        if (specialRenderer instanceof WorldBlockItemRenderer) state.setAnimated();
    }
}
