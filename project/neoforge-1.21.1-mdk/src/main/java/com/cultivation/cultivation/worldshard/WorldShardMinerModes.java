package com.cultivation.cultivation.worldshard;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorldShardMinerModes {
    public static final ResourceLocation OVERWORLD = id("overworld");
    public static final ResourceLocation NETHER = id("nether");
    public static final ResourceLocation END = id("end");

    private static volatile Snapshot snapshot = initialSnapshot();

    private WorldShardMinerModes() {
    }

    public static List<WorldShardMinerMode> builtinModes() {
        return List.of(
                new WorldShardMinerMode(OVERWORLD, WorldShardMinerActivation.forBlock(Blocks.DIAMOND_BLOCK),
                        Optional.of(Level.OVERWORLD.location()), Optional.of(BiomeTags.IS_OVERWORLD),
                        0xFF55FF55, 1.0D, false, Map.of()),
                new WorldShardMinerMode(NETHER, WorldShardMinerActivation.forBlock(Blocks.ANCIENT_DEBRIS),
                        Optional.of(Level.NETHER.location()), Optional.of(BiomeTags.IS_NETHER),
                        0xFFFF5555, 1.0D, false, Map.of()),
                new WorldShardMinerMode(END, WorldShardMinerActivation.forBlock(Blocks.PURPUR_BLOCK),
                        Optional.of(Level.END.location()), Optional.of(BiomeTags.IS_END),
                        0xFFAA55FF, 1.0D, false, Map.of()));
    }

    public static Map<ResourceLocation, WorldShardMinerMode> mergeDefinitions(
            Collection<WorldShardMinerMode> base, Collection<WorldShardMinerMode> overrides) {
        List<WorldShardMinerMode> sortedBase = new ArrayList<>(base);
        List<WorldShardMinerMode> sortedOverrides = new ArrayList<>(overrides);
        Comparator<WorldShardMinerMode> byId = Comparator.comparing(mode -> mode.id().toString());
        sortedBase.sort(byId);
        sortedOverrides.sort(byId);
        Map<ResourceLocation, WorldShardMinerMode> merged = new LinkedHashMap<>();
        sortedBase.forEach(mode -> merged.put(mode.id(), mode));
        sortedOverrides.forEach(mode -> merged.put(mode.id(), mode));
        return Map.copyOf(merged);
    }

    public static Map<ResourceLocation, WorldShardMinerMode> definitions() {
        return snapshot.definitions();
    }

    public static Optional<ResolvedMode> resolved(ResourceLocation id) {
        return Optional.ofNullable(snapshot.resolved().get(id));
    }

    public static long generation() {
        return snapshot.generation();
    }

    static synchronized void install(Collection<WorldShardMinerMode> modes, RegistryAccess registryAccess) {
        Map<ResourceLocation, WorldShardMinerMode> definitions = mergeDefinitions(List.of(), modes);
        Snapshot previous = snapshot;
        snapshot = new Snapshot(definitions, resolve(definitions.values(),
                mode -> WorldShardOreScanner.buildPool(registryAccess, mode)), nextGeneration(previous));
    }

    static synchronized void rebuildPools(RegistryAccess registryAccess) {
        Snapshot previous = snapshot;
        snapshot = new Snapshot(previous.definitions(), resolve(previous.definitions().values(),
                mode -> WorldShardOreScanner.buildPool(registryAccess, mode)), nextGeneration(previous));
    }

    public static synchronized void rebuildPools(MinecraftServer server) {
        Snapshot previous = snapshot;
        snapshot = new Snapshot(previous.definitions(), resolve(previous.definitions().values(),
                mode -> WorldShardOreScanner.buildPool(server, mode)), nextGeneration(previous));
    }

    private static Map<ResourceLocation, ResolvedMode> resolve(Collection<WorldShardMinerMode> definitions,
                                                                java.util.function.Function<WorldShardMinerMode, WorldShardOrePool> scanner) {
        Map<ResourceLocation, ResolvedMode> resolved = new LinkedHashMap<>();
        definitions.stream()
                .sorted(Comparator.comparing(mode -> mode.id().toString()))
                .forEach(mode -> resolved.put(mode.id(), new ResolvedMode(mode, scanner.apply(mode))));
        return Map.copyOf(resolved);
    }

    private static long nextGeneration(Snapshot previous) {
        return previous.generation() == Long.MAX_VALUE ? 0L : previous.generation() + 1L;
    }

    private static Snapshot initialSnapshot() {
        Map<ResourceLocation, WorldShardMinerMode> definitions = mergeDefinitions(builtinModes(), List.of());
        Map<ResourceLocation, ResolvedMode> resolved = new LinkedHashMap<>();
        definitions.forEach((id, mode) -> resolved.put(id, new ResolvedMode(mode, WorldShardOrePool.of(Map.of()))));
        return new Snapshot(definitions, Map.copyOf(resolved), 0L);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, path);
    }

    public record ResolvedMode(WorldShardMinerMode mode, WorldShardOrePool orePool) {
    }

    private record Snapshot(Map<ResourceLocation, WorldShardMinerMode> definitions,
                            Map<ResourceLocation, ResolvedMode> resolved,
                            long generation) {
    }
}
