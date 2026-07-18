package com.cultivation.cultivation.worldshard;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record WorldShardLootDefinition(ResourceLocation id, ResourceLocation mode,
                                       ResourceLocation lootTable, long weight, long sourceSeed) {
    public WorldShardLootDefinition(ResourceLocation id, ResourceLocation mode,
                                    ResourceLocation lootTable, long weight) {
        this(id, mode, lootTable, weight, 0L);
    }

    public WorldShardLootDefinition {
        id = Objects.requireNonNull(id, "id");
        mode = Objects.requireNonNull(mode, "mode");
        lootTable = Objects.requireNonNull(lootTable, "lootTable");
        if (weight < 0L) throw new IllegalArgumentException("weight must be non-negative");
    }
}
