package com.cultivation.cultivation.source.definition;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.custom.VeinKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Atomic server definition snapshot. Reload never mutates a snapshot already in use. */
public final class SourceDefinitions {
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final Map<VeinKind, ResourceLocation> LEGACY_IDS = legacyIds();
    private static volatile Snapshot snapshot = buildSnapshot(builtinDefinitions().values());

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static long generation() {
        return snapshot.generation();
    }

    public static ResourceLocation legacyId(VeinKind kind) {
        return LEGACY_IDS.get(java.util.Objects.requireNonNull(kind, "kind"));
    }

    public static Optional<SourceDefinition> find(ResourceLocation idOrAlias) {
        if (idOrAlias == null) return Optional.empty();
        Snapshot current = snapshot;
        ResourceLocation canonical = current.aliases().getOrDefault(idOrAlias, idOrAlias);
        return Optional.ofNullable(current.definitions().get(canonical));
    }

    /** Installs a validated immutable snapshot. One bad entry cannot abort the remaining definitions. */
    public static synchronized void install(Collection<SourceDefinition> definitions) {
        snapshot = buildSnapshot(definitions);
    }

    public static Map<ResourceLocation, SourceDefinition> merge(
            Map<ResourceLocation, SourceDefinition> base,
            Collection<SourceDefinition> overrides) {
        LinkedHashMap<ResourceLocation, SourceDefinition> merged = new LinkedHashMap<>(base);
        LinkedHashMap<OutputKey, ResourceLocation> outputOwners = new LinkedHashMap<>();
        for (SourceDefinition definition : merged.values()) {
            ResourceLocation prior = outputOwners.putIfAbsent(outputKey(definition), definition.id());
            if (prior != null && !prior.equals(definition.id())) {
                throw duplicateOutput(definition, prior);
            }
        }
        java.util.HashSet<ResourceLocation> layerIds = new java.util.HashSet<>();
        for (SourceDefinition definition : overrides) {
            if (!layerIds.add(definition.id())) {
                throw new IllegalArgumentException("duplicate source definition id in one layer: " + definition.id());
            }
            OutputKey nextOutput = outputKey(definition);
            ResourceLocation outputOwner = outputOwners.get(nextOutput);
            if (outputOwner != null && !outputOwner.equals(definition.id())) {
                throw duplicateOutput(definition, outputOwner);
            }
            SourceDefinition previous = merged.get(definition.id());
            if (previous != null && !outputKey(previous).equals(nextOutput)) {
                outputOwners.remove(outputKey(previous), definition.id());
            }
            merged.put(definition.id(), definition);
            outputOwners.put(nextOutput, definition.id());
        }
        return Map.copyOf(merged);
    }

    public static Map<ResourceLocation, SourceDefinition> builtinDefinitions() {
        LinkedHashMap<ResourceLocation, SourceDefinition> result = new LinkedHashMap<>();
        for (VeinKind kind : VeinKind.values()) {
            ResourceLocation id = legacyId(kind);
            ResourceLocation output = outputId(kind);
            String blockPath = legacyBlockPath(kind);
            SourceDefinition.OutputType outputType = kind.fluid
                    ? SourceDefinition.OutputType.FLUID : SourceDefinition.OutputType.ITEM;
            long defaultRate = kind.yuanCostPerBatch <= 0L ? Integer.MAX_VALUE : kind.fluid ? 1000L : 64L;
            SourceDefinition definition = new SourceDefinition(
                    id, outputType, output, kind.yuanCostPerBatch, kind.outputsPerBatch,
                    kind.minStage, defaultRate, Long.MAX_VALUE,
                    "block.cultivation." + blockPath, 0xFFFFFF, blockPath,
                    List.of(ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, blockPath)), kind);
            result.put(id, definition);
        }
        return Map.copyOf(result);
    }

    public static boolean outputExists(SourceDefinition definition) {
        if (definition == null) return false;
        return definition.fluid()
                ? BuiltInRegistries.FLUID.containsKey(definition.outputId())
                : BuiltInRegistries.ITEM.containsKey(definition.outputId());
    }

    private static Snapshot buildSnapshot(Collection<SourceDefinition> definitions) {
        ArrayList<SourceDefinition> ordered = new ArrayList<>(definitions);
        ordered.sort(Comparator.comparing(definition -> definition.id().toString()));
        LinkedHashMap<ResourceLocation, SourceDefinition> accepted = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
        LinkedHashMap<OutputKey, ResourceLocation> outputOwners = new LinkedHashMap<>();
        for (SourceDefinition definition : ordered) {
            if (accepted.containsKey(definition.id()) || aliases.containsKey(definition.id())) {
                CultivationMod.LOG.error("Ignoring duplicate source definition id {}", definition.id());
                continue;
            }
            boolean aliasConflict = false;
            for (ResourceLocation alias : definition.aliases()) {
                if (accepted.containsKey(alias) || aliases.containsKey(alias)) {
                    CultivationMod.LOG.error("Ignoring source definition {} because alias {} is already bound",
                            definition.id(), alias);
                    aliasConflict = true;
                    break;
                }
            }
            if (aliasConflict) continue;
            OutputKey output = outputKey(definition);
            ResourceLocation outputOwner = outputOwners.get(output);
            if (outputOwner != null) {
                CultivationMod.LOG.error(
                        "Ignoring source definition {} because {} {} is already produced by {}",
                        definition.id(), definition.outputType().name().toLowerCase(java.util.Locale.ROOT),
                        definition.outputId(), outputOwner);
                continue;
            }
            accepted.put(definition.id(), definition);
            outputOwners.put(output, definition.id());
            definition.aliases().forEach(alias -> aliases.put(alias, definition.id()));
        }
        long next = GENERATION.updateAndGet(value -> value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L);
        return new Snapshot(next, Map.copyOf(accepted), Map.copyOf(aliases));
    }

    private static Map<VeinKind, ResourceLocation> legacyIds() {
        EnumMap<VeinKind, ResourceLocation> result = new EnumMap<>(VeinKind.class);
        for (VeinKind kind : VeinKind.values()) {
            result.put(kind, ResourceLocation.fromNamespaceAndPath(
                    CultivationMod.MODID, kind.name().toLowerCase(java.util.Locale.ROOT)));
        }
        return Map.copyOf(result);
    }

    static ResourceLocation conflictingOutputOwner(
            Map<ResourceLocation, SourceDefinition> definitions, SourceDefinition candidate) {
        OutputKey wanted = outputKey(candidate);
        for (SourceDefinition existing : definitions.values()) {
            if (!existing.id().equals(candidate.id()) && outputKey(existing).equals(wanted)) {
                return existing.id();
            }
        }
        return null;
    }

    private static OutputKey outputKey(SourceDefinition definition) {
        return new OutputKey(definition.outputType(), definition.outputId());
    }

    private static IllegalArgumentException duplicateOutput(
            SourceDefinition definition, ResourceLocation prior) {
        return new IllegalArgumentException("duplicate source output "
                + definition.outputType().name().toLowerCase(java.util.Locale.ROOT) + " "
                + definition.outputId() + " for " + definition.id() + "; already owned by " + prior);
    }

    private record OutputKey(SourceDefinition.OutputType type, ResourceLocation id) {}

    private static String legacyBlockPath(VeinKind kind) {
        return switch (kind) {
            case COBBLE -> "cobblestone_vein";
            default -> kind.name().toLowerCase(java.util.Locale.ROOT) + "_vein";
        };
    }

    private static ResourceLocation outputId(VeinKind kind) {
        String namespace = "minecraft";
        String path = switch (kind) {
            case WATER -> "water";
            case MILK -> "milk";
            case LAVA -> "lava";
            case COBBLE -> "cobblestone";
            case STONE -> "stone";
            case SMOOTH_STONE -> "smooth_stone";
            case WHITE_CONCRETE -> "white_concrete";
            case ORANGE_CONCRETE -> "orange_concrete";
            case MAGENTA_CONCRETE -> "magenta_concrete";
            case LIGHT_BLUE_CONCRETE -> "light_blue_concrete";
            case YELLOW_CONCRETE -> "yellow_concrete";
            case LIME_CONCRETE -> "lime_concrete";
            case PINK_CONCRETE -> "pink_concrete";
            case GRAY_CONCRETE -> "gray_concrete";
            case LIGHT_GRAY_CONCRETE -> "light_gray_concrete";
            case CYAN_CONCRETE -> "cyan_concrete";
            case PURPLE_CONCRETE -> "purple_concrete";
            case BLUE_CONCRETE -> "blue_concrete";
            case BROWN_CONCRETE -> "brown_concrete";
            case GREEN_CONCRETE -> "green_concrete";
            case RED_CONCRETE -> "red_concrete";
            case BLACK_CONCRETE -> "black_concrete";
            case DIRT -> "dirt";
            case OAK_LOG -> "oak_log";
            case COAL -> "coal";
            case RAW_COPPER -> "raw_copper";
            case RAW_IRON -> "raw_iron";
            case RAW_GOLD -> "raw_gold";
            case LAPIS -> "lapis_lazuli";
            case REDSTONE -> "redstone";
            case CRUDE_SPIRIT_IRON -> { namespace = CultivationMod.MODID; yield "crude_spirit_iron"; }
            case SPIRIT_CRYSTAL -> { namespace = CultivationMod.MODID; yield "spirit_crystal"; }
            case DIAMOND -> "diamond";
            case EMERALD -> "emerald";
            case ANCIENT_DEBRIS -> "ancient_debris";
            case NETHER_STAR -> "nether_star";
            case ENCHANTED_GOLDEN_APPLE -> "enchanted_golden_apple";
            case DRAGON_EGG -> "dragon_egg";
        };
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public record Snapshot(long generation,
                           Map<ResourceLocation, SourceDefinition> definitions,
                           Map<ResourceLocation, ResourceLocation> aliases) {}

    private SourceDefinitions() {}
}
