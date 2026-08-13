package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.immortalstorage.immortalstorage.item.ModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Loader-neutral catalogue of external resources available in the current mod set. */
public final class ExternalResourceCatalog {
    private static final Map<ResourceLocation, Supplier<List<ResourceChannelKey>>> PROVIDERS =
            new LinkedHashMap<>();
    private static final Map<ResourceChannelKey, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Function<ResourceChannelKey, Definition>>
            DEFINITION_PROVIDERS = new LinkedHashMap<>();

    public record Definition(
            ResourceLocation icon, String unit, int color,
            Component displayName, boolean solidColor) {}

    public static synchronized void registerDefinition(
            ResourceChannelKey key, ResourceLocation icon, String unit, int color) {
        registerDefinition(key, icon, unit, color, null, false);
    }

    public static synchronized void registerDefinition(
            ResourceChannelKey key, ResourceLocation icon, String unit, int color,
            Component displayName, boolean solidColor) {
        DEFINITIONS.putIfAbsent(Objects.requireNonNull(key, "key"),
                new Definition(Objects.requireNonNull(icon, "icon"),
                        Objects.requireNonNull(unit, "unit"), color,
                        displayName, solidColor));
    }

    public static synchronized void register(
            ResourceLocation providerId, Supplier<List<ResourceChannelKey>> provider) {
        PROVIDERS.putIfAbsent(Objects.requireNonNull(providerId, "providerId"),
                Objects.requireNonNull(provider, "provider"));
    }

    public static synchronized void registerDefinitionProvider(
            ResourceLocation providerId, Function<ResourceChannelKey, Definition> provider) {
        DEFINITION_PROVIDERS.putIfAbsent(Objects.requireNonNull(providerId, "providerId"),
                Objects.requireNonNull(provider, "provider"));
    }

    public static synchronized List<ResourceChannelKey> available() {
        Map<ResourceChannelKey, ResourceChannelKey> unique = new LinkedHashMap<>();
        // FE is an ImmortalStorage built-in channel. Keep it visible even if
        // a screen is constructed before optional integration bootstrap.
        unique.putIfAbsent(ExternalResourceChannels.FE, ExternalResourceChannels.FE);
        for (Supplier<List<ResourceChannelKey>> provider : PROVIDERS.values()) {
            List<ResourceChannelKey> supplied = provider.get();
            if (supplied == null) continue;
            for (ResourceChannelKey key : supplied) {
                if (key != null) unique.putIfAbsent(key, key);
            }
        }
        List<ResourceChannelKey> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(ResourceChannelKey::channel)
                .thenComparing(ResourceChannelKey::resourceId));
        return List.copyOf(result);
    }

    public static boolean contains(ResourceChannelKey key) {
        return key != null && available().contains(key);
    }

    public static Component displayName(ResourceChannelKey key) {
        Definition definition = definition(key);
        if (definition != null && definition.displayName() != null) {
            return definition.displayName().copy();
        }
        return Component.translatable("resource.immortalstorage.external." + key.channel());
    }

    public static synchronized Definition definition(ResourceChannelKey key) {
        Definition definition = DEFINITIONS.get(key);
        if (definition == null && key != null) {
            for (Function<ResourceChannelKey, Definition> provider : DEFINITION_PROVIDERS.values()) {
                definition = provider.apply(key);
                if (definition != null) {
                    DEFINITIONS.put(key, definition);
                    break;
                }
            }
        }
        return definition != null ? definition : new Definition(
                ResourceLocation.fromNamespaceAndPath("immortalstorage", "textures/gui/external_resource/ae2_chemical.png"),
                "", 0xFF7F7F7F, Component.translatable("resource.immortalstorage.external.unknown"), true);
    }

    private ExternalResourceCatalog() {}
}
