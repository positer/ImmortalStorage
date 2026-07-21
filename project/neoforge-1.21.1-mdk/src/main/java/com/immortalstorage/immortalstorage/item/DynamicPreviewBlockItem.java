package com.immortalstorage.immortalstorage.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item whose frame and animated core render as one model in every item viewer. */
public final class DynamicPreviewBlockItem extends BlockItem {
    public DynamicPreviewBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.immortalstorage.immortalstorage.client.render.DynamicPreviewBlockItemRenderer.INSTANCE;
            }
        });
    }
}
