package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import com.google.gson.JsonElement;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.NativeResourceKeyPayload;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Generic bridge for every addon key type registered through AE2's public registry. */
final class RegisteredAe2KeyBridge implements Ae2ExternalKeyBridge {
    static final RegisteredAe2KeyBridge INSTANCE = new RegisteredAe2KeyBridge();
    private static final String CHANNEL = "ae2_registered";
    private static final String NAMESPACE = "native";
    private final ConcurrentHashMap<ResourceChannelKey, AEKey> decoded = new ConcurrentHashMap<>();

    private RegisteredAe2KeyBridge() {}

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public @Nullable ResourceChannelKey toResourceKey(AEKey key) {
        if (!isAddonKey(key)) return null;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        JsonElement payload = AEKey.CODEC.encodeStart(ops, key).result().orElse(null);
        if (payload == null) return null;
        ResourceChannelKey encoded = NativeResourceKeyPayload.encode(CHANNEL, NAMESPACE, payload);
        decoded.put(encoded, key);
        return encoded;
    }

    @Override
    public @Nullable AEKey toAeKey(ResourceChannelKey key) {
        if (key == null || !CHANNEL.equals(key.channel())) return null;
        AEKey cached = decoded.get(key);
        if (cached != null && isAddonKey(cached)) return cached;
        JsonElement payload = NativeResourceKeyPayload.decode(key, CHANNEL, NAMESPACE);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (payload == null || server == null) return null;
        var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        AEKey restored = AEKey.CODEC.parse(ops, payload).result().orElse(null);
        if (!isAddonKey(restored)) return null;
        decoded.put(key, restored);
        return restored;
    }

    static Set<String> registeredAddonTypeIds() {
        return AEKeyTypes.getAll().stream()
                .filter(RegisteredAe2KeyBridge::isAddonType)
                .map(type -> type.getId().toString())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean isAddonKey(@Nullable AEKey key) {
        return key != null && isAddonType(key.getType());
    }

    private static boolean isAddonType(AEKeyType type) {
        return type != null
                && type != AEKeyType.items()
                && type != AEKeyType.fluids()
                && type != ImmortalStorageExternalResourceKeyType.TYPE
                && AEKeyTypes.getAll().contains(type);
    }
}
