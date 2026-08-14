package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorldShardLootCatalog {
    private static volatile WorldShardLootCatalog active = of(builtinDefinitions());
    private final Map<ResourceLocation, List<WorldShardLootDefinition>> byMode;
    private final Map<ResourceLocation, LootTable> resolvedTables;
    private final long generation;

    private WorldShardLootCatalog(Map<ResourceLocation, List<WorldShardLootDefinition>> byMode,
                                  Map<ResourceLocation, LootTable> resolvedTables, long generation) {
        this.byMode = byMode;
        this.resolvedTables = resolvedTables;
        this.generation = generation;
    }

    public static WorldShardLootCatalog active() {
        return active;
    }

    /** Monotonic stamp incremented on every install; lets addons/UI detect a reload. */
    public long generation() {
        return generation;
    }

    public static synchronized void install(Collection<WorldShardLootDefinition> definitions) {
        install(definitions, null);
    }

    /**
     * Installs the catalog and eagerly resolves every referenced loot table from
     * the current datapack registry so a roll never performs a per-cycle
     * registry lookup. A {@code null} registry keeps the empty-lookup fallback
     * for tests that build a catalog without a live server registry.
     */
    public static synchronized void install(Collection<WorldShardLootDefinition> definitions,
                                            Registry<LootTable> lootTables) {
        active = of(definitions, resolveTables(definitions, lootTables), nextGeneration(active));
    }

    public static WorldShardLootCatalog of(Collection<WorldShardLootDefinition> definitions) {
        return of(definitions, Map.of(), 0L);
    }

    private static WorldShardLootCatalog of(Collection<WorldShardLootDefinition> definitions,
                                            Map<ResourceLocation, LootTable> resolvedTables,
                                            long generation) {
        Map<ResourceLocation, List<WorldShardLootDefinition>> grouped = new LinkedHashMap<>();
        definitions.stream().filter(definition -> definition.weight() > 0L)
                .sorted(Comparator.comparing(d -> d.id().toString())).forEach(definition ->
                        grouped.computeIfAbsent(definition.mode(), ignored -> new ArrayList<>()).add(definition));
        Map<ResourceLocation, List<WorldShardLootDefinition>> immutable = new LinkedHashMap<>();
        grouped.forEach((mode, entries) -> immutable.put(mode, List.copyOf(entries)));
        return new WorldShardLootCatalog(Map.copyOf(immutable),
                Map.copyOf(resolvedTables), generation);
    }

    /** Returns the eagerly-resolved table for {@code id}, or {@link LootTable#EMPTY}. */
    public LootTable resolveLootTable(ResourceLocation id) {
        LootTable table = resolvedTables.get(id);
        return table != null ? table : LootTable.EMPTY;
    }

    /** All selectable definitions registered for {@code mode}, in stable id order. */
    public List<WorldShardLootDefinition> definitions(ResourceLocation mode) {
        return byMode.getOrDefault(mode, List.of());
    }

    private static Map<ResourceLocation, LootTable> resolveTables(
            Collection<WorldShardLootDefinition> definitions, Registry<LootTable> lootTables) {
        if (lootTables == null) return Map.of();
        Map<ResourceLocation, LootTable> resolved = new HashMap<>();
        for (WorldShardLootDefinition definition : definitions) {
            ResourceLocation id = definition.lootTable();
            if (resolved.containsKey(id)) continue;
            LootTable table = lootTables.get(ResourceKey.create(Registries.LOOT_TABLE, id));
            if (table != null) resolved.put(id, table);
        }
        return resolved;
    }

    private static long nextGeneration(WorldShardLootCatalog previous) {
        return previous.generation() == Long.MAX_VALUE ? 0L : previous.generation() + 1L;
    }

    public Optional<WorldShardLootDefinition> select(ResourceLocation mode, long randomTicket,
                                                       WorldShardLootWeightProvider weights) {
        List<WorldShardLootDefinition> entries = byMode.getOrDefault(mode, List.of());
        long total = 0L;
        for (WorldShardLootDefinition entry : entries) {
            long weight = Math.max(0L, weights.weight(entry));
            if (Long.MAX_VALUE - total < weight) total = Long.MAX_VALUE;
            else total += weight;
        }
        if (total <= 0L) return Optional.empty();
        long ticket = Math.floorMod(randomTicket, total);
        for (WorldShardLootDefinition entry : entries) {
            long weight = Math.max(0L, weights.weight(entry));
            if (ticket < weight) return Optional.of(entry);
            ticket -= weight;
        }
        return Optional.empty();
    }

    public boolean hasSelectable(ResourceLocation mode, WorldShardLootWeightProvider weights) {
        if (mode == null || weights == null) return false;
        for (WorldShardLootDefinition entry : byMode.getOrDefault(mode, List.of())) {
            if (Math.max(0L, weights.weight(entry)) > 0L) return true;
        }
        return false;
    }

    public static Map<ResourceLocation, WorldShardLootDefinition> mergeDefinitions(
            Collection<WorldShardLootDefinition> base, Collection<WorldShardLootDefinition> overrides) {
        Map<ResourceLocation, WorldShardLootDefinition> merged = new LinkedHashMap<>();
        base.stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(d -> merged.put(d.id(), d));
        overrides.stream().sorted(Comparator.comparing(d -> d.id().toString())).forEach(d -> {
            if (d.weight() == 0L) merged.remove(d.id());
            else merged.put(d.id(), d);
        });
        return Map.copyOf(merged);
    }

    public static List<WorldShardLootDefinition> builtinDefinitions() {
        return List.of(
                builtin("overworld_simple_dungeon", WorldShardMinerModes.OVERWORLD,
                        "minecraft:chests/simple_dungeon", 100L),
                builtin("overworld_mineshaft", WorldShardMinerModes.OVERWORLD,
                        "minecraft:chests/abandoned_mineshaft", 80L),
                builtin("overworld_village", WorldShardMinerModes.OVERWORLD,
                        "minecraft:chests/village/village_plains_house", 40L),
                builtin("overworld_shipwreck", WorldShardMinerModes.OVERWORLD,
                        "minecraft:chests/shipwreck_treasure", 10L),
                builtin("nether_fortress", WorldShardMinerModes.NETHER,
                        "minecraft:chests/nether_bridge", 70L),
                builtin("nether_bastion_other", WorldShardMinerModes.NETHER,
                        "minecraft:chests/bastion_other", 20L),
                builtin("nether_bastion_treasure", WorldShardMinerModes.NETHER,
                        "minecraft:chests/bastion_treasure", 10L),
                builtin("end_city", WorldShardMinerModes.END,
                        "minecraft:chests/end_city_treasure", 100L));
    }

    private static WorldShardLootDefinition builtin(String id, ResourceLocation mode,
                                                     String lootTable, long weight) {
        return new WorldShardLootDefinition(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, id), mode,
                ResourceLocation.tryParse(lootTable), weight);
    }
}
