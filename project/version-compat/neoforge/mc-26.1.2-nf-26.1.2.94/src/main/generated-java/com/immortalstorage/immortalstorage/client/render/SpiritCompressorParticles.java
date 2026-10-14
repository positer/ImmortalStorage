package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.compressor.SpiritCompressorStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

/** Client-only: each chain independently emits at low frequency, with motion aimed at the controller. */
public final class SpiritCompressorParticles {
    public static void tick(MiniatureImmortalRuinBlockEntity ruin) {
        var level = ruin.getLevel();
        if (level == null || !level.isClientSide() || !ruin.compressor().formed()) return;
        var center = ruin.getBlockPos();
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            if (!SpiritCompressorStructure.blockId(x, y, z).equals("minecraft:chain") || level.getRandom().nextInt(160) != 0) continue;
            double dx = x + (level.getRandom().nextDouble() - .5) * .4;
            double dy = y + (level.getRandom().nextDouble() - .5) * .4;
            double dz = z + (level.getRandom().nextDouble() - .5) * .4;
            var particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.END_ROD,
                    center.getX() + .5 + dx, center.getY() + .5 + dy, center.getZ() + .5 + dz,
                    -dx / 24, -dy / 24, -dz / 24);
            if (particle != null) {
                if (particle instanceof net.minecraft.client.particle.SingleQuadParticle quad) quad.setColor(.76F, .97F, 1F); particle.setLifetime(24);
                if (particle instanceof net.minecraft.client.particle.SimpleAnimatedParticle animated) animated.setFadeColor(0xC2F7FF);
                var motion = (com.immortalstorage.immortalstorage.mixin.core.ParticleMotionAccessor) particle;
                motion.immortalstorage$setGravity(0); motion.immortalstorage$setFriction(1); motion.immortalstorage$setHasPhysics(false);
            }
        }
    }
    private SpiritCompressorParticles() {}
}
