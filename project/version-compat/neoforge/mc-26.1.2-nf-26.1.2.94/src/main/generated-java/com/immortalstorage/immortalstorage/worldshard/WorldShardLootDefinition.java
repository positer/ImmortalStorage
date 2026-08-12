package com.immortalstorage.immortalstorage.worldshard;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public record WorldShardLootDefinition(Identifier id, Identifier mode,
                                       Identifier lootTable, long weight, long sourceSeed) {
    public WorldShardLootDefinition(Identifier id, Identifier mode,
                                    Identifier lootTable, long weight) {
        this(id, mode, lootTable, weight, 0L);
    }

    public WorldShardLootDefinition {
        id = Objects.requireNonNull(id, "id");
        mode = Objects.requireNonNull(mode, "mode");
        lootTable = Objects.requireNonNull(lootTable, "lootTable");
        if (weight < 0L) throw new IllegalArgumentException("weight must be non-negative");
    }
}
