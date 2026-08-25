package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAuraElytraMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$auraGuardStartsFlying(CallbackInfoReturnable<Boolean> result) {
        Player self = (Player) (Object) this;
        if (!self.isFallFlying() && !self.onGround() && !self.isPassenger() && !self.isInWater()
                && !self.hasEffect(MobEffects.LEVITATION)
                && ImmortalMasterTalismanService.hasAuraGuard(self)) {
            self.startFallFlying();
            self.getPersistentData().putBoolean("ImmortalStorageVirtualElytra", true);
            result.setReturnValue(true);
        }
    }
}
