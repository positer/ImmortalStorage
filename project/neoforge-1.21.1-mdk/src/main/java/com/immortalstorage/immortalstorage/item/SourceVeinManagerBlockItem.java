package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinManagerBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Manager item rendered by its normal block model plus the rotating source core BEWLR. */
public final class SourceVeinManagerBlockItem extends BlockItem {
    public SourceVeinManagerBlockItem(SourceVeinManagerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.immortalstorage.immortalstorage.client.render.SourceVeinManagerItemRenderer.INSTANCE;
            }
        });
    }
}
