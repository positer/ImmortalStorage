package com.immortalstorage.immortalstorage.mixin.core;

import com.immortalstorage.immortalstorage.timeshift.TimeShiftAcceleration;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the staff's per-block extra ticks after every block-entity pass.
 *
 * <p>The personal realm grants its own extra passes by calling
 * {@code tickBlockEntities()} again, so this hook runs once per pass and the
 * realm multiplier and the staff multiplier multiply instead of replacing each
 * other.</p>
 */
@Mixin(Level.class)
public abstract class TimeShiftBlockEntitiesMixin {
    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void immortalstorage$timeShiftBlocks(CallbackInfo callbackInfo) {
        TimeShiftAcceleration.tickBlockEntities((Level) (Object) this);
    }
}
