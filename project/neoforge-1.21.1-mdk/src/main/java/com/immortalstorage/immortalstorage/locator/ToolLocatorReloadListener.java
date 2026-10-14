package com.immortalstorage.immortalstorage.locator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Map;

public final class ToolLocatorReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "tool_locator";
    public ToolLocatorReloadListener() { super(GSON, DIRECTORY); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        int radius = 0, miningRadius = 0;
        var data = resources.get(ResourceLocation.fromNamespaceAndPath("immortalstorage", "settings"));
        if (data != null) {
            try {
                var settings = data.getAsJsonObject();
                int parsedRadius = ToolLocatorSettings.parseRadius(settings);
                int parsedMining = ToolLocatorSettings.parseMiningRadius(settings);
                radius = parsedRadius; miningRadius = parsedMining;
            }
            catch (RuntimeException error) { ImmortalStorageMod.LOG.error("Ignoring invalid tool locator settings: {}", error.getMessage()); }
        }
        ToolLocatorSettings.install(radius, miningRadius);
    }
}
