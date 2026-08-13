package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Target-side block-item base with the official 26.1 constructor. */
public class CompatBlockItem extends BlockItem {
    public CompatBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

}
