package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorldShardLootReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "world_shard_loot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public WorldShardLootReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<WorldShardLootDefinition> overrides = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        if (!entry.getValue().isJsonObject()) {
                            throw new IllegalArgumentException("root must be a JSON object");
                        }
                        overrides.add(WorldShardLootDefinitionParser.parse(
                                entry.getKey(), entry.getValue().getAsJsonObject()));
                    } catch (RuntimeException error) {
                        ImmortalStorageMod.LOG.error("Ignoring invalid world shard loot entry {}: {}",
                                entry.getKey(), error.getMessage());
                    }
                });
        Map<ResourceLocation, WorldShardLootDefinition> merged = WorldShardLootCatalog.mergeDefinitions(
                WorldShardLootCatalog.builtinDefinitions(), overrides);
        WorldShardLootCatalog.install(merged.values());
        ImmortalStorageMod.LOG.info("Loaded {} world shard loot-table entries from {} definitions",
                merged.size(), resources.size());
    }
}
