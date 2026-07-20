package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Optional;

public record WorldShardMinerActivation(Optional<ResourceLocation> blockId,
                                        Optional<TagKey<Block>> blockTag) {
    public WorldShardMinerActivation {
        blockId = Objects.requireNonNull(blockId, "blockId");
        blockTag = Objects.requireNonNull(blockTag, "blockTag");
        if (blockId.isPresent() == blockTag.isPresent()) {
            throw new IllegalArgumentException("activation must define exactly one block or block tag");
        }
    }

    public static WorldShardMinerActivation forBlock(Block block) {
        return forBlockId(BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(block, "block")));
    }

    public static WorldShardMinerActivation forBlockId(ResourceLocation blockId) {
        return new WorldShardMinerActivation(Optional.of(Objects.requireNonNull(blockId, "blockId")), Optional.empty());
    }

    public static WorldShardMinerActivation forTag(ResourceLocation tagId) {
        return new WorldShardMinerActivation(Optional.empty(),
                Optional.of(TagKey.create(Registries.BLOCK, Objects.requireNonNull(tagId, "tagId"))));
    }

    public boolean matches(BlockState state) {
        if (state == null) return false;
        return blockId.map(id -> BuiltInRegistries.BLOCK.getOptional(id)
                        .map(state::is).orElse(false))
                .orElseGet(() -> state.is(blockTag.orElseThrow()));
    }
}
