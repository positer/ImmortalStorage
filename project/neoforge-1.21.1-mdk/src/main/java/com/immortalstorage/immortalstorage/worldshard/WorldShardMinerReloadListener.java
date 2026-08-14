package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.worldshard.WorldShardAddonRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorldShardMinerReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "world_shard_miner";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final RegistryAccess registryAccess;

    public WorldShardMinerReloadListener(RegistryAccess registryAccess) {
        super(GSON, DIRECTORY);
        this.registryAccess = registryAccess;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<WorldShardMinerMode> overrides = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        if (!entry.getValue().isJsonObject()) {
                            throw new IllegalArgumentException("root must be a JSON object");
                        }
                        overrides.add(WorldShardMinerModeParser.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
                    } catch (RuntimeException error) {
                        ImmortalStorageMod.LOG.error("Ignoring invalid world shard miner mode {}: {}",
                                entry.getKey(), error.getMessage());
                    }
                });
        List<WorldShardMinerMode> base = new ArrayList<>(WorldShardMinerModes.builtinModes());
        base.addAll(WorldShardAddonRegistry.minerModeOverrides());
        Map<ResourceLocation, WorldShardMinerMode> merged =
                WorldShardMinerModes.mergeDefinitions(base, overrides);
        WorldShardMinerModes.install(merged.values(), registryAccess);
        ImmortalStorageMod.LOG.info("Loaded {} world shard miner mode(s) ({} addon), generation {}",
                merged.size(), base.size() - WorldShardMinerModes.builtinModes().size(),
                WorldShardMinerModes.generation());
    }
}
