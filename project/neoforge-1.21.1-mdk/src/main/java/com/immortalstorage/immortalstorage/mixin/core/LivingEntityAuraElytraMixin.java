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
        // Active flight always wins over the glide marker, so a player who
        // toggles creative or cultivation flight never keeps virtual-elytra
        // state (its boost and double-distance would otherwise still apply).
        boolean activelyFlying = self instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().flying;
        if (activelyFlying && self.isFallFlying()) {
            ((net.minecraft.world.entity.player.Player) self).stopFallFlying();
        }
        if (self.onGround() || !self.isFallFlying() || activelyFlying) {
            self.getPersistentData().remove("ImmortalStorageVirtualElytra");
        }
    }
    // LocalPlayer can enable ability flight inside aiStep, after tick HEAD.
    // Fold the vanilla glide flag before travel chooses its movement branch.
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
                && player.getPersistentData().getBoolean("ImmortalStorageVirtualElytra")) {
            callback.cancel();
        }
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 2), index = 1)
    private Vec3 immortalstorage$auraGuardDoubleFlightDistance(Vec3 movement) {
        LivingEntity self = (LivingEntity) (Object) this;
        return !(self instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().flying)
                && self.getPersistentData().getBoolean("ImmortalStorageVirtualElytra")
                ? movement.scale(2.0D) : movement;
    }
}
