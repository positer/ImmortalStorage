package com.immortalstorage.immortalstorage.machine;

import com.google.gson.*;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

/** data/<namespace>/machine_policies/<id>.json; pack ids override addon defaults. */
public final class MachinePolicyReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "machine_policies";
    public MachinePolicyReloadListener() { super(GSON, DIRECTORY); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, MachinePolicyApi.Rule> rules = new LinkedHashMap<>(MachinePolicyApi.registeredDefaults());
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        JsonObject json = entry.getValue().getAsJsonObject();
                        if (json.has("enabled") && !json.get("enabled").getAsBoolean()) rules.remove(entry.getKey());
                        else rules.put(entry.getKey(), parse(json));
                    } catch (RuntimeException error) {
                        ImmortalStorageMod.LOG.error("Invalid machine policy {}: {}", entry.getKey(), error.getMessage());
                    }
                });
        MachinePolicyApi.install(rules);
        ImmortalStorageMod.LOG.info("Loaded {} machine policies", rules.size());
    }
    public static MachinePolicyApi.Rule parse(JsonObject json) {
        return new MachinePolicyApi.Rule(
                MachinePolicyApi.Machine.valueOf(json.get("machine").getAsString().toUpperCase(Locale.ROOT)),
                json.has("min_level") ? json.get("min_level").getAsBigDecimal().intValueExact() : 1,
                json.has("max_level") ? json.get("max_level").getAsBigDecimal().intValueExact() : Integer.MAX_VALUE,
                json.has("mode") ? ResourceLocation.parse(json.get("mode").getAsString()) : null,
                MachinePolicyApi.Target.valueOf(json.get("target").getAsString().toUpperCase(Locale.ROOT)),
                selectors(json, "whitelist"), selectors(json, "blacklist"));
    }
    private static List<String> selectors(JsonObject json, String name) {
        if (!json.has(name)) return List.of();
        List<String> result = new ArrayList<>();
        json.getAsJsonArray(name).forEach(value -> result.add(value.getAsString()));
        return result;
    }
}
