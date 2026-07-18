package com.cultivation.cultivation.worldshard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biome;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WorldShardMinerModeParser {
    private static final Set<String> FIELDS = Set.of(
            "id", "activation", "target_dimension", "target_biome_tag",
            "beam_color", "sampling_multiplier", "generation_source", "replace_ore_weights", "ore_weights");
    private static final Set<String> ACTIVATION_FIELDS = Set.of("block", "tag");

    private WorldShardMinerModeParser() {
    }

    public static WorldShardMinerMode parse(ResourceLocation source, JsonObject json) {
        requireKnownFields(source, json, FIELDS, "mode");
        ResourceLocation id = parseId(source, GsonHelper.getAsString(json, "id"), "id");
        JsonObject activationJson = GsonHelper.getAsJsonObject(json, "activation");
        requireKnownFields(source, activationJson, ACTIVATION_FIELDS, "activation");
        boolean hasBlock = activationJson.has("block");
        boolean hasTag = activationJson.has("tag");
        if (hasBlock == hasTag) {
            throw failure(source, "activation must contain exactly one of block or tag");
        }
        WorldShardMinerActivation activation;
        if (hasBlock) {
            ResourceLocation blockId = parseId(source,
                    GsonHelper.getAsString(activationJson, "block"), "activation.block");
            if (BuiltInRegistries.BLOCK.getOptional(blockId).isEmpty()) {
                throw failure(source, "unknown activation block " + blockId);
            }
            activation = WorldShardMinerActivation.forBlockId(blockId);
        } else {
            activation = WorldShardMinerActivation.forTag(parseId(source,
                    GsonHelper.getAsString(activationJson, "tag"), "activation.tag"));
        }

        Optional<ResourceLocation> targetDimension = json.has("target_dimension")
                ? Optional.of(parseId(source, GsonHelper.getAsString(json, "target_dimension"), "target_dimension"))
                : Optional.empty();
        Optional<TagKey<Biome>> targetBiomeTag = json.has("target_biome_tag")
                ? Optional.of(TagKey.create(Registries.BIOME, parseId(source,
                        GsonHelper.getAsString(json, "target_biome_tag"), "target_biome_tag")))
                : Optional.empty();
        if (targetDimension.isEmpty() && targetBiomeTag.isEmpty()) {
            throw failure(source, "target_dimension or target_biome_tag is required");
        }

        int beamColor = parseColor(source, json.get("beam_color"));
        double multiplier = GsonHelper.getAsDouble(json, "sampling_multiplier", 1.0D);
        if (!Double.isFinite(multiplier) || multiplier <= 0.0D || multiplier > 1024.0D) {
            throw failure(source, "sampling_multiplier must be finite and in (0, 1024]");
        }
        boolean replaceOreWeights = GsonHelper.getAsBoolean(json, "replace_ore_weights", false);
        if (json.has("generation_source")) {
            String generationSource = GsonHelper.getAsString(json, "generation_source");
            if ("fixed_table".equals(generationSource)) {
                replaceOreWeights = true;
            } else if (!"world_first".equals(generationSource)) {
                throw failure(source, "generation_source must be world_first or fixed_table");
            } else if (replaceOreWeights) {
                throw failure(source, "world_first conflicts with replace_ore_weights=true");
            }
        }
        Map<ResourceLocation, Long> weights = parseWeights(source, json);
        return new WorldShardMinerMode(id, activation, targetDimension, targetBiomeTag,
                beamColor, multiplier, replaceOreWeights, weights);
    }

    private static Map<ResourceLocation, Long> parseWeights(ResourceLocation source, JsonObject json) {
        if (!json.has("ore_weights")) return Map.of();
        JsonObject weightsJson = GsonHelper.getAsJsonObject(json, "ore_weights");
        Map<ResourceLocation, Long> weights = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : weightsJson.entrySet()) {
            ResourceLocation itemId = parseId(source, entry.getKey(), "ore_weights key");
            if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty()) {
                throw failure(source, "unknown ore weight item " + itemId);
            }
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw failure(source, "ore weight for " + itemId + " must be an integer");
            }
            long weight;
            try {
                weight = entry.getValue().getAsLong();
            } catch (RuntimeException error) {
                throw failure(source, "invalid ore weight for " + itemId);
            }
            if (weight < 0L || weight > 1_000_000_000L) {
                throw failure(source, "ore weight for " + itemId + " must be in [0, 1000000000]");
            }
            weights.put(itemId, weight);
        }
        return Map.copyOf(weights);
    }

    private static int parseColor(ResourceLocation source, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw failure(source, "beam_color is required");
        }
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            String raw = primitive.getAsString();
            if (!raw.matches("#[0-9A-Fa-f]{6}")) {
                throw failure(source, "beam_color must be #RRGGBB or an integer RGB value");
            }
            return 0xFF000000 | Integer.parseInt(raw.substring(1), 16);
        }
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            long raw = primitive.getAsLong();
            if (raw < 0L || raw > 0xFFFFFFL) {
                throw failure(source, "integer beam_color must be in [0, 16777215]");
            }
            return 0xFF000000 | (int) raw;
        }
        throw failure(source, "beam_color must be #RRGGBB or an integer RGB value");
    }

    private static ResourceLocation parseId(ResourceLocation source, String raw, String field) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) throw failure(source, field + " is not a valid resource location: " + raw);
        return id;
    }

    private static void requireKnownFields(ResourceLocation source, JsonObject json,
                                           Set<String> allowed, String objectName) {
        Set<String> unknown = new HashSet<>(json.keySet());
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw failure(source, "unknown " + objectName + " field(s): " + unknown.stream().sorted().toList());
        }
    }

    private static IllegalArgumentException failure(ResourceLocation source, String message) {
        return new IllegalArgumentException("World shard miner mode " + source + ": " + message);
    }
}
