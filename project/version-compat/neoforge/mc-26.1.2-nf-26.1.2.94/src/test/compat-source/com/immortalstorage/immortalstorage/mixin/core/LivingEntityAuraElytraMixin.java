package com.immortalstorage.immortalstorage.mixin.core;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAuraElytraMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void immortalstorage$auraGuardMaintainsVirtualElytra(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.onGround() || !self.isFallFlying()) {
            self.getPersistentData().remove("ImmortalStorageVirtualElytra");
        }
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$auraGuardKeepsFlying(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof net.minecraft.world.entity.player.Player player
                && self.isFallFlying() && !self.isPassenger() && !self.isInWater()
                && player.getPersistentData().getBooleanOr("ImmortalStorageVirtualElytra", false)) {
            callback.cancel();
        }
    }
}
