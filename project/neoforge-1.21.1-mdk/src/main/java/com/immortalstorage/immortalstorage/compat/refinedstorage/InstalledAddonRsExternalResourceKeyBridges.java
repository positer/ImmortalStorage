package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Native-key adapters for installed RS resource-type addons, isolated by reflection. */
final class InstalledAddonRsExternalResourceKeyBridges {
    private static final String REFINED_TYPES = "refinedtypes";
    private static final String RS_MEKANISM = "refinedstorage_mekanism_integration";

    static void registerPresent() {
        if (loaded(REFINED_TYPES)) {
            registerSingleton(
                    "com.ultramega.refinedtypes.type.energy.EnergyResource",
                    "ENERGY_RESOURCE", ExternalResourceChannels.FE);
            if (loaded("ars_nouveau")) {
                registerSingleton(
                        "com.ultramega.refinedtypes.type.source.SourceResource",
                        "SOURCE_RESOURCE", ExternalResourceChannels.ARS_NOUVEAU_SOURCE);
            }
            if (loaded("industrialforegoingsouls")) {
                registerSingleton(
                        "com.ultramega.refinedtypes.type.soul.SoulResource",
                        "SOUL_RESOURCE", ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL);
            }
        }
        if (loaded(RS_MEKANISM) && loaded("mekanism")) {
            RsExternalResourceKeyBridges.register(new MekanismChemicalBridge());
        }
    }

    private static void registerSingleton(
            String className, String fieldName, ResourceChannelKey resource) {
        ResourceKey key = staticKey(className, fieldName);
        if (key != null) {
            RsExternalResourceKeyBridges.register(new SingletonBridge(className, key, resource));
        }
    }

    private static boolean loaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    private static @Nullable ResourceKey staticKey(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getField(fieldName);
            return field.get(null) instanceof ResourceKey key ? key : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private record SingletonBridge(
            String className, ResourceKey addonKey, ResourceChannelKey resource)
            implements RsExternalResourceKeyBridge {
        @Override public int priority() { return 100; }
        @Override public @Nullable ResourceChannelKey toResourceKey(ResourceKey key) {
            return key != null && className.equals(key.getClass().getName()) ? resource : null;
        }
        @Override public @Nullable ResourceKey toRsKey(ResourceChannelKey key) {
            return resource.equals(key) ? addonKey : null;
        }
    }

    private static final class MekanismChemicalBridge implements RsExternalResourceKeyBridge {
        private static final String KEY_CLASS =
                "com.refinedmods.refinedstorage.mekanism.ChemicalResource";

        @Override public int priority() { return 100; }

        @Override public @Nullable ResourceChannelKey toResourceKey(ResourceKey key) {
            if (key == null || !KEY_CLASS.equals(key.getClass().getName())) return null;
            try {
                Object chemical = key.getClass().getMethod("chemical").invoke(key);
                Object registry = chemicalRegistry();
                Method getKey = registry.getClass().getMethod("getKey", Object.class);
                Object id = getKey.invoke(registry, chemical);
                return id instanceof ResourceLocation location
                        ? ExternalResourceChannels.mekanismChemical(location.toString()) : null;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        @Override public @Nullable ResourceKey toRsKey(ResourceChannelKey key) {
            if (key == null || !ExternalResourceChannels.MEKANISM_CHEMICAL_CHANNEL.equals(
                    key.channel())) return null;
            try {
                Object registry = chemicalRegistry();
                Method get = registry.getClass().getMethod("get", ResourceLocation.class);
                Object chemical = get.invoke(registry, ResourceLocation.parse(key.resourceId()));
                if (chemical == null) return null;
                Class<?> keyClass = Class.forName(KEY_CLASS);
                Constructor<?> constructor = java.util.Arrays.stream(keyClass.getConstructors())
                        .filter(candidate -> candidate.getParameterCount() == 1
                                && candidate.getParameterTypes()[0].isInstance(chemical))
                        .findFirst().orElseThrow();
                return constructor.newInstance(chemical) instanceof ResourceKey result ? result : null;
            } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ignored) {
                return null;
            }
        }

        private static Object chemicalRegistry() throws ReflectiveOperationException {
            return Class.forName("mekanism." + "api.MekanismAPI")
                    .getField("CHEMICAL_REGISTRY").get(null);
        }
    }

    private InstalledAddonRsExternalResourceKeyBridges() {}
}
