package com.cultivation.cultivation.worldshard;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WorldShardMinerMode(ResourceLocation id,
                                  WorldShardMinerActivation activation,
                                  Optional<ResourceLocation> targetDimension,
                                  Optional<TagKey<Biome>> targetBiomeTag,
                                  int beamColor,
                                  double samplingMultiplier,
                                  boolean replaceOreWeights,
                                  Map<ResourceLocation, Long> explicitOreWeights) {
    public WorldShardMinerMode {
        id = Objects.requireNonNull(id, "id");
        activation = Objects.requireNonNull(activation, "activation");
        targetDimension = Objects.requireNonNull(targetDimension, "targetDimension");
        targetBiomeTag = Objects.requireNonNull(targetBiomeTag, "targetBiomeTag");
        explicitOreWeights = Map.copyOf(Objects.requireNonNull(explicitOreWeights, "explicitOreWeights"));
        if (targetDimension.isEmpty() && targetBiomeTag.isEmpty()) {
            throw new IllegalArgumentException("mode must select a target dimension or biome tag");
        }
        if (!Double.isFinite(samplingMultiplier) || samplingMultiplier <= 0.0D) {
            throw new IllegalArgumentException("sampling multiplier must be finite and positive");
        }
        if (explicitOreWeights.values().stream().anyMatch(weight -> weight == null || weight < 0L)) {
            throw new IllegalArgumentException("explicit ore weights must be non-negative");
        }
        beamColor |= 0xFF000000;
    }


    public WorldShardMinerMode(ResourceLocation id,
                               WorldShardMinerActivation activation,
                               Optional<ResourceLocation> targetDimension,
                               Optional<TagKey<Biome>> targetBiomeTag,
                               int beamColor,
                               double samplingMultiplier,
                               Map<ResourceLocation, Long> explicitOreWeights) {
        this(id, activation, targetDimension, targetBiomeTag, beamColor,
                samplingMultiplier, false, explicitOreWeights);
    }
}
