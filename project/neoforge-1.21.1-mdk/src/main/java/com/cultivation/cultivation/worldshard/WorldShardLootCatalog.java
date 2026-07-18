package com.cultivation.cultivation.worldshard;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorldShardLootCatalog {
    private static volatile WorldShardLootCatalog active = of(builtinDefinitions());
    private final Map<ResourceLocation, List<WorldShardLootDefinition>> byMode;

    private WorldShardLootCatalog(Map<ResourceLocation, List<WorldShardLootDefinition>> byMode) {
        this.byMode = byMode;
    }

    public static WorldShardLootCatalog active() {
        return active;
    }

    public static synchronized void install(Collection<WorldShardLootDefinition> definitions) {
        active = of(definitions);
    }

    public static WorldShardLootCatalog of(Collection<WorldShardLootDefinition> definitions) {
        Map<ResourceLocation, List<WorldShardLootDefinition>> grouped = new LinkedHashMap<>();
        definitions.stream().filter(definition -> definition.weight() > 0L)
                .sorted(Comparator.comparing(d -> d.id().toString())).forEach(definition ->
                        grouped.computeIfAbsent(definition.mode(), ignored -> new ArrayList<>()).add(definition));
        Map<ResourceLocation, List<WorldShardLootDefinition>> immutable = new LinkedHashMap<>();
        grouped.forEach((mode, entries) -> immutable.put(mode, List.copyOf(entries)));
        return new WorldShardLootCatalog(Map.copyOf(immutable));
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
                ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, id), mode,
                ResourceLocation.tryParse(lootTable), weight);
    }
}
