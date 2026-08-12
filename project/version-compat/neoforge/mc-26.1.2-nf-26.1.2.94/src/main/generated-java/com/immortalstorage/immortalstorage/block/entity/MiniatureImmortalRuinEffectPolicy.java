package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Pure geometry and target-selection rules for a placed miniature ruin. */
final class MiniatureImmortalRuinEffectPolicy {
    private static final double HORIZONTAL_RADIUS = 6.0D;

    private MiniatureImmortalRuinEffectPolicy() {}

    /**
     * Returns the exact 13 x 1 x 13 collision box centered on the ruin's block
     * layer. The one-block Y extent deliberately excludes every other layer.
     */
    static AABB effectArea(BlockPos origin) {
        return new AABB(
                origin.getX() - HORIZONTAL_RADIUS, origin.getY(), origin.getZ() - HORIZONTAL_RADIUS,
                origin.getX() + 1.0D + HORIZONTAL_RADIUS, origin.getY() + 1.0D,
                origin.getZ() + 1.0D + HORIZONTAL_RADIUS);
    }

    static boolean shouldAffectLivingEntity(
            boolean isPlayer, boolean affectPlayers, boolean holdingMiniatureRuin) {
        return !isPlayer || (affectPlayers && !holdingMiniatureRuin);
    }
}
