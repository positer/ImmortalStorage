package com.immortalstorage.immortalstorage.locator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Server-side classification, extended by normal synchronized datapack block tags. */
public final class ToolLocatorRules {
    public static final int STORAGE = 1, SPAWNER = 2, ARCHAEOLOGY = 4, ORE = 8;
    public static final TagKey<Block> STORAGE_TAG = tag("storage");
    public static final TagKey<Block> SPAWNER_TAG = tag("spawners");
    public static final TagKey<Block> ARCHAEOLOGY_TAG = tag("archaeology");
    public static final TagKey<Block> ORE_TAG = tag("ores");
    private ToolLocatorRules() {}
    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("immortalstorage", "tool_locator/" + path));
    }
    public static int stateMask(BlockState state) {
        return (state.is(STORAGE_TAG) ? STORAGE : 0) | (state.is(SPAWNER_TAG) ? SPAWNER : 0)
                | (state.is(ARCHAEOLOGY_TAG) ? ARCHAEOLOGY : 0) | (state.is(ORE_TAG) ? ORE : 0);
    }
    public static int withEntity(int mask, BlockEntity entity) {
        if (entity instanceof RandomizableContainerBlockEntity || entity != null
                && entity.getClass().getName().startsWith("noobanidus.mods.lootr.")) mask |= STORAGE;
        return mask;
    }
    public static int visibleCategory(int mask, boolean explore, boolean mine) {
        if (explore) {
            if ((mask & ARCHAEOLOGY) != 0) return ARCHAEOLOGY;
            if ((mask & SPAWNER) != 0) return SPAWNER;
            if ((mask & STORAGE) != 0) return STORAGE;
        }
        return mine && (mask & ORE) != 0 ? ORE : 0;
    }
}
