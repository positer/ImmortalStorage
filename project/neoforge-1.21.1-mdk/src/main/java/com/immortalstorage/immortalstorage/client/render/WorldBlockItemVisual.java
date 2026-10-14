package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/** Detached, unticked item snapshots consumed by the real world renderer. */
public final class WorldBlockItemVisual {
    private static final Map<ItemStack, Cached> CACHE = new WeakHashMap<>();
    public record Visual(BlockState state, BlockEntity entity) {}
    private record Cached(ItemStack contents, Object level, Visual visual) {}

    public static Visual resolve(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Cached cached = CACHE.get(stack);
        if (cached != null && cached.level == minecraft.level
                && ItemStack.isSameItemSameComponents(cached.contents, stack)) return cached.visual;
        Block block = stack.getItem() instanceof BlockItem item ? item.getBlock()
                : stack.is(ModItems.MINIATURE_IMMORTAL_RUIN.get()) ? ModBlocks.MINIATURE_IMMORTAL_RUIN.get() : null;
        if (block == null || block instanceof SourceVeinBlock) return null;
        BlockState state = block.defaultBlockState();
        var properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties != null) state = properties.apply(state);
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag saved = data == null ? new CompoundTag() : data.copyTag();
        if (block instanceof MiniatureImmortalRuinBlock) {
            state = state.setValue(MiniatureImmortalRuinBlock.REVERSED, saved.getBoolean("Reversed"));
        }
        BlockEntity entity = block instanceof EntityBlock factory ? factory.newBlockEntity(BlockPos.ZERO, state) : null;
        if (entity != null) {
            entity.setLevel(minecraft.level);
            if (entity instanceof MiniatureImmortalRuinBlockEntity ruin) {
                ruin.restoreFromItem(saved);
            } else if (data != null && minecraft.level != null) {
                entity.loadWithComponents(saved, minecraft.level.registryAccess());
            }
            entity.applyComponentsFromItemStack(stack);
        }
        Visual visual = new Visual(state, entity);
        CACHE.put(stack, new Cached(stack.copy(), minecraft.level, visual));
        return visual;
    }

    private WorldBlockItemVisual() {}
}
