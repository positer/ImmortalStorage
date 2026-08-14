package com.immortalstorage.immortalstorage.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * World Barrier (世界胎壁): a translucent white wall crafted from a single
 * Primordial Qi.  It is only ever removed by a player breaking it; explosions,
 * mobs and other non-player destruction are rejected in {@code CommonEvents}.
 * Harvest hardness matches a hay bale (0.5).
 */
public final class WorldBarrierBlock extends Block {
    public WorldBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getExplosionResistance() {
        return 3_600_000.0F;
    }
}
