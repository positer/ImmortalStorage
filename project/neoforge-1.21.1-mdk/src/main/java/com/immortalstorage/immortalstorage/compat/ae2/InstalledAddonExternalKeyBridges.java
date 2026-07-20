package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEKey;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Canonical-key adapters for installed AE2 resource addons, isolated by reflection. */
final class InstalledAddonExternalKeyBridges {
    static void registerPresent() {
        registerSingleton("appbot", "appbot.ae2.ManaKey", "KEY",
                ExternalResourceChannels.BOTANIA_MANA);
        registerSingleton("arseng", "gripe._90.arseng.me.key.SourceKey", "KEY",
                ExternalResourceChannels.ARS_NOUVEAU_SOURCE);
        if (loaded("appflux")) Ae2ExternalKeyBridges.register(new AppliedFluxBridge());
        if (loaded("appmek")) Ae2ExternalKeyBridges.register(new AppliedMekanisticsBridge());
    }

    private static void registerSingleton(
            String modId, String className, String fieldName, ResourceChannelKey resource) {
        if (!loaded(modId)) return;
        AEKey key = staticKey(className, fieldName);
        if (key != null) Ae2ExternalKeyBridges.register(new SingletonBridge(className, key, resource));
    }

    private static boolean loaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    private static @Nullable AEKey staticKey(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getField(fieldName);
            return field.get(null) instanceof AEKey key ? key : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private record SingletonBridge(
            String className, AEKey addonKey, ResourceChannelKey resource)
            implements Ae2ExternalKeyBridge {
        @Override public int priority() { return 100; }
        @Override public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
            return key != null && className.equals(key.getClass().getName()) ? resource : null;
        }
        @Override public @Nullable AEKey toAeKey(ResourceChannelKey key) {
            return resource.equals(key) ? addonKey : null;
        }
    }

    private static final class AppliedFluxBridge implements Ae2ExternalKeyBridge {
        private static final String KEY_CLASS =
                "com.glodblock.github.appflux.common.me.key.FluxKey";
        private static final String TYPE_CLASS =
                "com.glodblock.github.appflux.common.me.key.type.EnergyType";

        @Override public int priority() { return 100; }
        @Override public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
            if (key == null || !KEY_CLASS.equals(key.getClass().getName())) return null;
            try {
                Object type = key.getClass().getMethod("getEnergyType").invoke(key);
                return type instanceof Enum<?> value && "FE".equals(value.name())
                        ? ExternalResourceChannels.FE : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        @Override public @Nullable AEKey toAeKey(ResourceChannelKey key) {
            if (!ExternalResourceChannels.FE.equals(key)) return null;
            try {
                Class<?> typeClass = Class.forName(TYPE_CLASS);
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object fe = Enum.valueOf((Class) typeClass.asSubclass(Enum.class), "FE");
                Method of = Class.forName(KEY_CLASS).getMethod("of", typeClass);
                return of.invoke(null, fe) instanceof AEKey result ? result : null;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }
    }

    private static final class AppliedMekanisticsBridge implements Ae2ExternalKeyBridge {
        private static final String KEY_CLASS = "me.ramidzkh.mekae2.ae2.MekanismKey";

        @Override public int priority() { return 100; }
        @Override public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
            if (key == null || !KEY_CLASS.equals(key.getClass().getName())) return null;
            return ExternalResourceChannels.mekanismChemical(key.getId().toString());
        }
        @Override public @Nullable AEKey toAeKey(ResourceChannelKey key) {
            if (key == null || !ExternalResourceChannels.MEKANISM_CHEMICAL_CHANNEL.equals(
                    key.channel())) {
                return null;
            }
            try {
                Class<?> api = Class.forName("mekanism." + "api.MekanismAPI");
                Object registry = api.getField("CHEMICAL_REGISTRY").get(null);
                Method get = registry.getClass().getMethod(
                        "get", net.minecraft.resources.ResourceLocation.class);
                Object chemical = get.invoke(registry,
                        net.minecraft.resources.ResourceLocation.parse(key.resourceId()));
                if (chemical == null) return null;
                Class<?> stackClass = Class.forName(
                        "mekanism." + "api.chemical.ChemicalStack");
                Method wrap = java.util.Arrays.stream(registry.getClass().getMethods())
                        .filter(method -> method.getName().equals("wrapAsHolder")
                                && method.getParameterCount() == 1)
                        .findFirst().orElseThrow();
                Object holder = wrap.invoke(registry, chemical);
                var constructor = java.util.Arrays.stream(stackClass.getConstructors())
                        .filter(candidate -> candidate.getParameterCount() == 2
                                && candidate.getParameterTypes()[1] == long.class)
                        .findFirst().orElseThrow();
                Object stack = constructor.newInstance(holder, 1L);
                Method of = Class.forName(KEY_CLASS).getMethod("of", stackClass);
                return of.invoke(null, stack) instanceof AEKey result ? result : null;
            } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private InstalledAddonExternalKeyBridges() {}
}
