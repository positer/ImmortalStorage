package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Source item rendered by its normal item model with a client-side output decoration. */
public final class SourceVeinBlockItem extends BlockItem {
    public SourceVeinBlockItem(SourceVeinBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.immortalstorage.immortalstorage.client.render.SourceVeinItemRenderer.INSTANCE;
            }
        });
    }
}
