package com.immortalstorage.immortalstorage.mixin.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
                && player.getPersistentData().getBoolean("ImmortalStorageVirtualElytra")) {
            callback.cancel();
        }
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 2), index = 1)
    private Vec3 immortalstorage$auraGuardDoubleFlightDistance(Vec3 movement) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.getPersistentData().getBoolean("ImmortalStorageVirtualElytra")
                ? movement.scale(2.0D) : movement;
    }
}
