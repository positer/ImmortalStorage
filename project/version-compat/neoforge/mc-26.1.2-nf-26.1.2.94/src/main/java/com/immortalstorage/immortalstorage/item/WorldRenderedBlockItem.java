package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockItem;
import net.minecraft.world.level.block.Block;

/** The 26.1 item-definition special model selects the shared world renderer. */
public class WorldRenderedBlockItem extends CompatBlockItem {
    public WorldRenderedBlockItem(Block block, Properties properties) { super(block, properties); }
}
