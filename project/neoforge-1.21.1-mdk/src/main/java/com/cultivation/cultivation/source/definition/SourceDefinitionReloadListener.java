package com.cultivation.cultivation.source.definition;

import com.cultivation.cultivation.CultivationMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datapack definitions are overlaid by config/cultivation/source_veins JSON files. */
public final class SourceDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "source_veins";
    public static final Path CONFIG_DIRECTORY =
            FMLPaths.CONFIGDIR.get().resolve("cultivation").resolve(DIRECTORY);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public SourceDefinitionReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, SourceDefinition> merged = new LinkedHashMap<>(SourceDefinitions.builtinDefinitions());
        List<SourceDefinition> datapack = parseDatapack(resources);
        mergeValidated(merged, datapack, "datapack");
        List<SourceDefinition> config = parseConfigDirectory(CONFIG_DIRECTORY);
        mergeValidated(merged, config, "config");
        SourceDefinitions.install(merged.values());
        CultivationMod.LOG.info("Loaded {} source definition(s), generation {} ({} datapack, {} config)",
                merged.size(), SourceDefinitions.generation(), datapack.size(), config.size());
    }

    static List<SourceDefinition> parseDatapack(Map<ResourceLocation, JsonElement> resources) {
        ArrayList<SourceDefinition> definitions = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        if (!entry.getValue().isJsonObject()) {
                            throw new IllegalArgumentException("root must be a JSON object");
                        }
                        definitions.add(SourceDefinitionParser.parse(entry.getKey(),
                                entry.getValue().getAsJsonObject()));
                    } catch (RuntimeException error) {
                        CultivationMod.LOG.error("Ignoring invalid source definition {}: {}",
                                entry.getKey(), error.getMessage());
                    }
                });
        return List.copyOf(definitions);
    }

    static List<SourceDefinition> parseConfigDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return List.of();
        ArrayList<SourceDefinition> definitions = new ArrayList<>();
        try (var paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> parseConfigFile(directory, path, definitions));
        } catch (java.io.IOException error) {
            CultivationMod.LOG.error("Unable to scan source definition config directory {}", directory, error);
        }
        return List.copyOf(definitions);
    }

    private static void parseConfigFile(Path directory, Path file, List<SourceDefinition> definitions) {
        ResourceLocation fallback = configFallbackId(directory, file);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) throw new IllegalArgumentException("root must be a JSON object");
            definitions.add(SourceDefinitionParser.parse(fallback, root.getAsJsonObject()));
        } catch (RuntimeException | java.io.IOException error) {
            CultivationMod.LOG.error("Ignoring invalid config source definition {}: {}", file, error.getMessage());
        }
    }

    private static ResourceLocation configFallbackId(Path directory, Path file) {
        String relative = directory.relativize(file).toString().replace('\\', '/');
        relative = relative.substring(0, relative.length() - ".json".length());
        ResourceLocation id = ResourceLocation.tryParse(CultivationMod.MODID + ":" + relative);
        if (id == null) throw new IllegalArgumentException("invalid config source definition path " + file);
        return id;
    }

    private static void mergeValidated(Map<ResourceLocation, SourceDefinition> destination,
                                       List<SourceDefinition> definitions, String layer) {
        java.util.HashSet<ResourceLocation> ids = new java.util.HashSet<>();
        for (SourceDefinition definition : definitions) {
            if (!ids.add(definition.id())) {
                CultivationMod.LOG.error("Ignoring duplicate {} source definition id {}",
                        layer, definition.id());
                continue;
            }
            if (!SourceDefinitions.outputExists(definition)) {
                CultivationMod.LOG.error("Ignoring {} source definition {}: unknown {} {}",
                        layer, definition.id(), definition.outputType().name().toLowerCase(java.util.Locale.ROOT),
                        definition.outputId());
                continue;
            }
            ResourceLocation conflict = SourceDefinitions.conflictingOutputOwner(destination, definition);
            if (conflict != null) {
                CultivationMod.LOG.error(
                        "Ignoring {} source definition {}: {} {} is already produced by {}",
                        layer, definition.id(),
                        definition.outputType().name().toLowerCase(java.util.Locale.ROOT),
                        definition.outputId(), conflict);
                continue;
            }
            destination.put(definition.id(), definition);
        }
    }
}
