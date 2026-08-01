package com.immortalstorage.immortalstorage.spiritfield;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Data-pack extension point for seeds whose crop cannot be inferred from BlockItem. */
public final class SimulatedSpiritFieldCropCatalog {
    public static final String DIRECTORY = "simulated_spirit_field_crops";
    private static volatile Map<Item, Entry> entries = Map.of();

    public static Optional<Entry> find(Item seed) { return Optional.ofNullable(entries.get(seed)); }
    static void install(Map<Item, Entry> next) { entries = Map.copyOf(next); }

    public record Entry(BlockState crop, List<SubstrateRule> substrates) {
        public boolean accepts(BlockState substrate) {
            return substrates.isEmpty() || substrates.stream().anyMatch(rule -> rule.matches(substrate));
        }
    }
    public record SubstrateRule(Block block, TagKey<Block> tag) {
        boolean matches(BlockState state) {
            return block != null ? state.is(block) : tag != null && state.is(tag);
        }
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
        public ReloadListener() { super(GSON, DIRECTORY); }

        @Override protected void apply(Map<ResourceLocation, JsonElement> resources,
                                       ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<Item, Entry> loaded = new LinkedHashMap<>();
            resources.entrySet().stream().sorted(Map.Entry.comparingByKey(
                    Comparator.comparing(ResourceLocation::toString))).forEach(resource -> {
                try {
                    EntryDefinition definition = parse(resource.getValue().getAsJsonObject());
                    Item seed = BuiltInRegistries.ITEM.getOptional(definition.seed()).orElseThrow(
                            () -> new IllegalArgumentException("unknown seed " + definition.seed()));
                    Block crop = BuiltInRegistries.BLOCK.getOptional(definition.crop()).orElseThrow(
                            () -> new IllegalArgumentException("unknown crop " + definition.crop()));
                    loaded.put(seed, new Entry(crop.defaultBlockState(), definition.substrates()));
                } catch (RuntimeException error) {
                    ImmortalStorageMod.LOG.error("Ignoring invalid simulated spirit field crop {}: {}",
                            resource.getKey(), error.getMessage());
                }
            });
            install(loaded);
            ImmortalStorageMod.LOG.info("Loaded {} simulated spirit field crop mappings", loaded.size());
        }
    }

    static EntryDefinition parse(JsonObject json) {
        ResourceLocation seed = requiredId(json, "seed");
        ResourceLocation crop = requiredId(json, "crop");
        List<SubstrateRule> substrates = new ArrayList<>();
        if (json.has("substrates")) json.getAsJsonArray("substrates").forEach(value -> {
            String raw = value.getAsString();
            if (raw.startsWith("#")) {
                ResourceLocation id = requireId(raw.substring(1), "substrate tag");
                substrates.add(new SubstrateRule(null, BlockTags.create(id)));
            } else {
                ResourceLocation id = requireId(raw, "substrate block");
                Block block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(
                        () -> new IllegalArgumentException("unknown substrate " + id));
                substrates.add(new SubstrateRule(block, null));
            }
        });
        return new EntryDefinition(seed, crop, List.copyOf(substrates));
    }

    private static ResourceLocation requiredId(JsonObject json, String field) {
        if (!json.has(field)) throw new IllegalArgumentException("missing " + field);
        return requireId(json.get(field).getAsString(), field);
    }
    private static ResourceLocation requireId(String raw, String field) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) throw new IllegalArgumentException("invalid " + field + " " + raw);
        return id;
    }
    record EntryDefinition(ResourceLocation seed, ResourceLocation crop, List<SubstrateRule> substrates) {}
    private SimulatedSpiritFieldCropCatalog() {}
}
