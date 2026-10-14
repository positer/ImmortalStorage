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
        boolean activelyFlying = self instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().flying;
        if (activelyFlying && self.isFallFlying()) {
            ((net.minecraft.world.entity.player.Player) self).stopFallFlying();
        }
        if (self.onGround() || !self.isFallFlying() || activelyFlying) {
            self.getPersistentData().remove("ImmortalStorageVirtualElytra");
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void immortalstorage$auraGuardFlightBeforeTravel(CallbackInfo callback) {
        immortalstorage$auraGuardMaintainsVirtualElytra(callback);
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$auraGuardKeepsFlying(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof net.minecraft.world.entity.player.Player player
                && !player.getAbilities().flying
                && self.isFallFlying() && !self.isPassenger() && !self.isInWater()
                && player.getPersistentData().getBooleanOr("ImmortalStorageVirtualElytra", false)) {
            callback.cancel();
        }
    }
}
