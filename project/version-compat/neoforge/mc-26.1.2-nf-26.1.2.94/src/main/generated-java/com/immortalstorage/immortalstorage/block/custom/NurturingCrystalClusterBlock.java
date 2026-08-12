package com.immortalstorage.immortalstorage.block.custom;

import net.minecraft.world.level.block.AmethystClusterBlock;

/** Independent white-crystal cluster block sharing only vanilla's public geometry contract. */
public final class NurturingCrystalClusterBlock extends AmethystClusterBlock {
    public NurturingCrystalClusterBlock(int height, int aabbOffset, Properties properties) {
        super(height, aabbOffset, properties);
    }
}
