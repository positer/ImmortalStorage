package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.worldshard.WorldShardAddonRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
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

    private final net.minecraft.core.RegistryAccess registryAccess;

    public WorldShardLootReloadListener(net.minecraft.core.RegistryAccess registryAccess) {
        super(GSON, DIRECTORY);
        this.registryAccess = registryAccess;
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
        // Native discovery of every structure chest loot table (instead of a
        // hard-coded whitelist); datapack entries override weights on top, and
        // addon programmatic definitions sit beneath the datapack overrides.
        List<WorldShardLootDefinition> discovered = WorldShardStructureLootScanner.discover(registryAccess);
        List<WorldShardLootDefinition> base = new ArrayList<>(discovered);
        base.addAll(WorldShardAddonRegistry.lootOverrides());
        Map<ResourceLocation, WorldShardLootDefinition> merged = WorldShardLootCatalog.mergeDefinitions(
                base, overrides);
        // Eagerly resolve every loot table once so the basin's per-cycle roll
        // is a catalog lookup instead of a reloadable-registry lookup.
        Registry<LootTable> lootTables = registryAccess.registryOrThrow(Registries.LOOT_TABLE);
        WorldShardLootCatalog.install(merged.values(), lootTables);
        ImmortalStorageMod.LOG.info("Loaded {} world shard loot-table entries ({} discovered, {} addon, {} datapack override)",
                merged.size(), discovered.size(), base.size() - discovered.size(), resources.size());
    }
}
