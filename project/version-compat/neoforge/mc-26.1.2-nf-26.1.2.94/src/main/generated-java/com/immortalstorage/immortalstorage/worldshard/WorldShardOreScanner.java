package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

public final class WorldShardOreScanner {
    static final long WEIGHT_SCALE = 1_000_000L;
    private static final int NOISE_INTEGRATION_STEPS = 512;
    private static final Set<Identifier> WARNED_UNMODELLED_MODIFIERS = ConcurrentHashMap.newKeySet();

    private WorldShardOreScanner() {
    }

    public static WorldShardOrePool buildPool(RegistryAccess registryAccess, WorldShardMinerMode mode) {
        return buildPool(registryAccess, null, mode);
    }

    public static WorldShardOrePool buildPool(MinecraftServer server, WorldShardMinerMode mode) {
        return buildPool(server.registryAccess(), server, mode);
    }

    private static WorldShardOrePool buildPool(RegistryAccess registryAccess, MinecraftServer server,
                                               WorldShardMinerMode mode) {
        try {
            CandidateFeatures candidates = candidatePlacedFeatures(registryAccess, server, mode);
            Map<Item, Long> detected = scanWeights(candidates.placedFeatures(), candidates.biomeCount());
            ImmortalStorageMod.LOG.info("World shard miner mode {} detected {} generated ore outputs: {}",
                    mode.id(), detected.size(), detected.keySet().stream()
                            .map(BuiltInRegistries.ITEM::getKey)
                            .sorted(java.util.Comparator.comparing(Identifier::toString))
                            .toList());
            return WorldShardOrePool.of(applyConfiguredWeights(
                    detected, mode.explicitOreWeights(), mode.replaceOreWeights()));
        } catch (RuntimeException error) {
            ImmortalStorageMod.LOG.error("Could not build dynamic ore pool for world shard miner mode {}", mode.id(), error);
            // A missing/opaque dynamic registry must not discard explicit
            // datapack weights.  world_first can remain CALIBRATING while its
            // configured additions (or a fixed_table replacement) stay usable.
            return WorldShardOrePool.of(applyConfiguredWeights(
                    Map.of(), mode.explicitOreWeights(), mode.replaceOreWeights()));
        }
    }


    static Map<Item, Long> applyConfiguredWeights(Map<Item, Long> detected,
                                                  Map<Identifier, Long> configured,
                                                  boolean replaceDetected) {
        Map<Item, Long> merged = new HashMap<>();
        if (!replaceDetected) merged.putAll(detected);
        configured.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Identifier::toString)))
                .forEach(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey()).ifPresent(item -> {
                    if (entry.getValue() == 0L) merged.remove(item);
                    else merged.put(item, entry.getValue());
                }));
        return Map.copyOf(merged);
    }

    static Map<Item, Long> scanWeights(Collection<PlacedFeature> placedFeatures, int candidateBiomeCount) {
        if (candidateBiomeCount <= 0) return Map.of();
        Map<Item, Long> weights = new HashMap<>();
        for (PlacedFeature placed : placedFeatures) {
            if (placed == null) continue;
            try {
                double expectedAttempts = expectedAttempts(placed.placement());
                if (!(expectedAttempts > 0.0D)) continue;
                Set<net.minecraft.core.Holder<ConfiguredFeature<?, ?>>> visitedConfigured =
                        Collections.newSetFromMap(new IdentityHashMap<>());
                placed.getFeatures().forEach(configured -> {
                    if (!visitedConfigured.add(configured)) return;
                    OreDescriptor ore = describeOreConfiguration(configured.value().config());
                    if (ore == null) return;
                    // The public worldgen API does not expose the fraction of
                    // terrain matching each RuleTest. Split one configured
                    // feature's expected yield across its distinct outputs so
                    // stone/deepslate targets do not double its total weight.
                    double contribution = expectedAttempts * ore.size() * WEIGHT_SCALE
                            / candidateBiomeCount / ore.outputs().size();
                    long perTargetWeight = scaledWeight(contribution);
                    if (perTargetWeight <= 0L) return;
                    for (Item output : ore.outputs()) {
                        weights.merge(output, perTargetWeight, WorldShardOreScanner::saturatingAdd);
                    }
                });
            } catch (RuntimeException opaqueFeatureFailure) {
                ImmortalStorageMod.LOG.warn("Skipping opaque placed feature while scanning world shard miner ores: {}",
                        placed, opaqueFeatureFailure);
            }
        }
        return Map.copyOf(weights);
    }

    static OreDescriptor describeOreConfiguration(FeatureConfiguration configuration) {
        if (configuration instanceof OreConfiguration ore) {
            return descriptor(ore.targetStates, ore.size);
        }

        // Some mods keep vanilla TargetBlockState entries but resolve vein size
        // from their live server config. Read that public structural contract so
        // the scanner follows the final worldgen object instead of a mod-id list.
        try {
            Object targets = configuration.getClass().getMethod("targetStates").invoke(configuration);
            Object size = configuration.getClass().getMethod("size").invoke(configuration);
            if (!(targets instanceof Iterable<?> iterable)) return null;
            int resolvedSize = switch (size) {
                case Number number -> number.intValue();
                case IntSupplier supplier -> supplier.getAsInt();
                case IntProvider provider -> (provider.minInclusive() + provider.maxInclusive()) / 2;
                default -> 0;
            };
            List<OreConfiguration.TargetBlockState> targetStates = new ArrayList<>();
            for (Object target : iterable) {
                if (!(target instanceof OreConfiguration.TargetBlockState state)) return null;
                targetStates.add(state);
            }
            return descriptor(targetStates, resolvedSize);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static OreDescriptor descriptor(Iterable<OreConfiguration.TargetBlockState> targets, int size) {
        if (size <= 0) return null;
        Set<Item> outputs = new LinkedHashSet<>();
        for (OreConfiguration.TargetBlockState target : targets) {
            Item output = target.state.getBlock().asItem();
            if (output != Items.AIR) outputs.add(output);
        }
        return outputs.isEmpty() ? null : new OreDescriptor(Set.copyOf(outputs), size);
    }

    private static double expectedAttempts(List<PlacementModifier> modifiers) {
        double attempts = 1.0D;
        for (PlacementModifier modifier : modifiers) {
            double factor = modifierFactor(modifier);
            if (!(factor > 0.0D)) return 0.0D;
            attempts *= factor;
            if (!Double.isFinite(attempts)) return Double.MAX_VALUE;
        }
        return attempts;
    }

    private static double modifierFactor(PlacementModifier modifier) {
        PlacementModifierType<?> type = modifier.type();
        JsonObject encoded = encode(modifier);
        if (encoded == null) return 1.0D;

        if (type == PlacementModifierType.COUNT || type == PlacementModifierType.COUNT_ON_EVERY_LAYER) {
            return expectedIntProvider(encoded.get("count"));
        }
        if (type == PlacementModifierType.RARITY_FILTER) {
            return 1.0D / Math.max(1, encoded.get("chance").getAsInt());
        }
        if (type == PlacementModifierType.NOISE_BASED_COUNT) {
            int ratio = encoded.get("noise_to_count_ratio").getAsInt();
            double offset = encoded.has("noise_offset") ? encoded.get("noise_offset").getAsDouble() : 0.0D;
            return expectedNoiseBasedCount(ratio, offset);
        }
        if (type == PlacementModifierType.NOISE_THRESHOLD_COUNT) {
            double threshold = encoded.get("noise_level").getAsDouble();
            int below = encoded.get("below_noise").getAsInt();
            int above = encoded.get("above_noise").getAsInt();
            double belowProbability = Mth.clamp((threshold + 1.0D) * 0.5D, 0.0D, 1.0D);
            return Math.max(0.0D, belowProbability * below + (1.0D - belowProbability) * above);
        }

        if (type == PlacementModifierType.HEIGHT_RANGE
                || type == PlacementModifierType.HEIGHTMAP
                || type == PlacementModifierType.IN_SQUARE
                || type == PlacementModifierType.RANDOM_OFFSET
                || type == PlacementModifierType.FIXED_PLACEMENT
                || type == PlacementModifierType.BIOME_FILTER
               
                || type == PlacementModifierType.ENVIRONMENT_SCAN) {
            return 1.0D;
        }

        Identifier typeId = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getKey(type);
        if (typeId != null && WARNED_UNMODELLED_MODIFIERS.add(typeId)) {
            ImmortalStorageMod.LOG.warn("World shard miner cannot estimate placement modifier {}; using factor 1. "
                    + "Set ore_weights in the mode JSON for an exact override", typeId);
        }
        return 1.0D;
    }

    private static JsonObject encode(PlacementModifier modifier) {
        try {
            JsonElement encoded = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier)
                    .result().orElse(null);
            if (encoded != null && encoded.isJsonObject()) return encoded.getAsJsonObject();
        } catch (RuntimeException error) {
            Identifier typeId = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.getKey(modifier.type());
            if (typeId != null && WARNED_UNMODELLED_MODIFIERS.add(typeId)) {
                ImmortalStorageMod.LOG.warn("Could not inspect world shard miner placement modifier {}; using factor 1",
                        typeId, error);
            }
        }
        return null;
    }

    private static double expectedIntProvider(JsonElement encoded) {
        if (encoded == null) return 1.0D;
        return net.minecraft.util.valueproviders.IntProviders.CODEC.parse(JsonOps.INSTANCE, encoded).result()
                .map(provider -> (provider.minInclusive() + (double) provider.maxInclusive()) * 0.5D)
                .orElse(1.0D);
    }

    private static double expectedNoiseBasedCount(int ratio, double offset) {
        if (ratio <= 0) return 0.0D;
        double total = 0.0D;
        for (int sample = 0; sample < NOISE_INTEGRATION_STEPS; sample++) {
            double noise = -1.0D + (sample + 0.5D) * 2.0D / NOISE_INTEGRATION_STEPS;
            total += Math.max(0, (int) Math.ceil((noise + offset) * ratio));
        }
        return total / NOISE_INTEGRATION_STEPS;
    }

    private static CandidateFeatures candidatePlacedFeatures(
            RegistryAccess registryAccess, MinecraftServer server, WorldShardMinerMode mode) {
        Registry<Biome> biomeRegistry = registryAccess.lookupOrThrow(Registries.BIOME);
        Set<Holder<Biome>> selected = null;

        if (mode.targetDimension().isPresent()) {
            Set<Holder<Biome>> fromDimension = new LinkedHashSet<>();
            if (server != null) {
                ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION,
                        mode.targetDimension().orElseThrow());
                ServerLevel targetLevel = server.getLevel(levelKey);
                if (targetLevel != null) {
                    fromDimension.addAll(targetLevel.getChunkSource().getGenerator()
                            .getBiomeSource().possibleBiomes());
                }
            }
            if (fromDimension.isEmpty()) {
                Registry<LevelStem> stems = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
                ResourceKey<LevelStem> key = ResourceKey.create(Registries.LEVEL_STEM,
                        mode.targetDimension().orElseThrow());
                stems.get(key).ifPresent(stem ->
                        fromDimension.addAll(stem.value().generator().getBiomeSource().possibleBiomes()));
            }
            selected = fromDimension;
        }

        if (mode.targetBiomeTag().isPresent()) {
            Set<Holder<Biome>> fromTag = new HashSet<>();
            biomeRegistry.getTagOrEmpty(mode.targetBiomeTag().orElseThrow()).forEach(fromTag::add);
            if (selected == null) selected = fromTag;
            else selected.retainAll(fromTag);
        }

        if (selected == null) selected = Collections.emptySet();
        List<PlacedFeature> occurrences = new ArrayList<>();
        for (Holder<Biome> biome : selected) {
            for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
                step.forEach(holder -> occurrences.add(holder.value()));
            }
        }
        occurrences.sort((left, right) -> left.toString().compareTo(right.toString()));
        return new CandidateFeatures(List.copyOf(occurrences), selected.size());
    }

    private static long scaledWeight(double contribution) {
        if (!(contribution > 0.0D)) return 0L;
        if (contribution >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(1L, Math.round(contribution));
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private record CandidateFeatures(List<PlacedFeature> placedFeatures, int biomeCount) {
    }

    record OreDescriptor(Set<Item> outputs, int size) {
    }
}
