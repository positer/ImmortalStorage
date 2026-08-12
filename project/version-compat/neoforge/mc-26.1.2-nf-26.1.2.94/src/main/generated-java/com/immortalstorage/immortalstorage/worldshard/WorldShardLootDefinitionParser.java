package com.immortalstorage.immortalstorage.worldshard;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public final class WorldShardLootDefinitionParser {
    private WorldShardLootDefinitionParser() {
    }

    public static WorldShardLootDefinition parse(Identifier source, JsonObject json) {
        Identifier mode = parseId(source, json, "mode");
        Identifier lootTable = parseId(source, json, "loot_table");
        if (!json.has("weight") || !json.get("weight").isJsonPrimitive()) {
            throw new IllegalArgumentException("weight must be a non-negative integer");
        }
        long weight;
        try {
            weight = json.get("weight").getAsLong();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("weight must be a non-negative integer", error);
        }
        long sourceSeed = 0L;
        if (json.has("source_seed")) {
            if (!json.get("source_seed").isJsonPrimitive()) {
                throw new IllegalArgumentException("source_seed must be an integer");
            }
            try {
                sourceSeed = json.get("source_seed").getAsLong();
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("source_seed must be an integer", error);
            }
        }
        return new WorldShardLootDefinition(source, mode, lootTable, weight, sourceSeed);
    }

    private static Identifier parseId(Identifier source, JsonObject json, String member) {
        if (!json.has(member) || !json.get(member).isJsonPrimitive()) {
            throw new IllegalArgumentException(member + " must be a resource location in " + source);
        }
        Identifier id = Identifier.tryParse(json.get(member).getAsString());
        if (id == null) throw new IllegalArgumentException("invalid " + member + " in " + source);
        return id;
    }
}
