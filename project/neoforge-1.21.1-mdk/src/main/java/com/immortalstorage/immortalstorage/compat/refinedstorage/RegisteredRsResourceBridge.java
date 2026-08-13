package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.google.gson.JsonElement;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.NativeResourceKeyPayload;
import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Generic bridge for addon resource and storage types registered through RS's public registries. */
final class RegisteredRsResourceBridge implements RsExternalResourceKeyBridge {
    static final RegisteredRsResourceBridge INSTANCE = new RegisteredRsResourceBridge();
    private static final String CHANNEL = "rs_registered";
    private static final String NAMESPACE = "native";
    private final ConcurrentHashMap<ResourceChannelKey, ResourceKey> decoded = new ConcurrentHashMap<>();

    private RegisteredRsResourceBridge() {}

    @Override public int priority() {
        return 10;
    }

    @Override public @Nullable ResourceChannelKey toResourceKey(ResourceKey key) {
        if (!isRegisteredAddonResource(key)) return null;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        JsonElement payload = ResourceCodecs.CODEC.encodeStart(ops, (PlatformResourceKey) key)
                .result().orElse(null);
        if (payload == null) return null;
        ResourceChannelKey encoded = NativeResourceKeyPayload.encode(CHANNEL, NAMESPACE, payload);
        decoded.put(encoded, key);
        return encoded;
    }

    @Override public @Nullable ResourceKey toRsKey(ResourceChannelKey key) {
        if (key == null || !CHANNEL.equals(key.channel())) return null;
        ResourceKey cached = decoded.get(key);
        if (isRegisteredAddonResource(cached)) return cached;
        JsonElement payload = NativeResourceKeyPayload.decode(key, CHANNEL, NAMESPACE);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (payload == null || server == null) return null;
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        ResourceKey restored = ResourceCodecs.CODEC.parse(ops, payload).result().orElse(null);
        if (!isRegisteredAddonResource(restored)) return null;
        decoded.put(key, restored);
        return restored;
    }

    static boolean isRegisteredAddonResource(@Nullable ResourceKey key) {
        if (!(key instanceof PlatformResourceKey platform)
                || key instanceof ItemResource
                || key instanceof FluidResource
                || key instanceof RsExternalResource) return false;
        return RefinedStorageApi.INSTANCE.getResourceTypeRegistry()
                .getId(platform.getResourceType()).isPresent();
    }

    static boolean isAllowedByRegisteredAddonStorage(ResourceKey key) {
        if (!isRegisteredAddonResource(key)) return false;
        for (StorageType type : RefinedStorageApi.INSTANCE.getStorageTypeRegistry().getAll()) {
            if (type == RsXianqiaoStorageType.INSTANCE) continue;
            try {
                if (type.isAllowed(key)) return true;
            } catch (RuntimeException ignored) {
                // One broken addon registration must not disable all other registered types.
            }
        }
        return false;
    }

    static Set<String> registeredAddonResourceTypeIds() {
        var registry = RefinedStorageApi.INSTANCE.getResourceTypeRegistry();
        return registry.getAll().stream()
                .filter(type -> type != RsExternalResourceType.INSTANCE)
                .map(type -> registry.getId(type).map(Object::toString).orElse("unregistered"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    static Set<String> registeredAddonStorageTypeIds() {
        var registry = RefinedStorageApi.INSTANCE.getStorageTypeRegistry();
        return registry.getAll().stream()
                .filter(type -> type != RsXianqiaoStorageType.INSTANCE)
                .map(type -> registry.getId(type).map(Object::toString).orElse("unregistered"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
