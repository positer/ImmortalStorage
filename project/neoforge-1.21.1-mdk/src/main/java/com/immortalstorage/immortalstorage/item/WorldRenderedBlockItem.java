package com.immortalstorage.immortalstorage.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Uses the placed block model and renderer instead of a separate item preview. */
public class WorldRenderedBlockItem extends BlockItem {
    public WorldRenderedBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.immortalstorage.immortalstorage.client.render.WorldBlockItemRenderer.INSTANCE;
            }
        });
    }
}
