package com.immortalstorage.immortalstorage.mixin.core;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Removes vanilla end-rod gravity/damping only for particles created by the compressor. */
@Mixin(Particle.class)
public interface ParticleMotionAccessor {
    @Accessor("gravity") void immortalstorage$setGravity(float value);
    @Accessor("friction") void immortalstorage$setFriction(float value);
    @Accessor("hasPhysics") void immortalstorage$setHasPhysics(boolean value);
}
