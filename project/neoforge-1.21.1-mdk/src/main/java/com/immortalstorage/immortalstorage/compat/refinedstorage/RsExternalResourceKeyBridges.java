package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-local registry for optional RS external-resource key adapters. */
final class RsExternalResourceKeyBridges {
    private static final CopyOnWriteArrayList<RsExternalResourceKeyBridge> BRIDGES =
            new CopyOnWriteArrayList<>();

    private RsExternalResourceKeyBridges() {}

    static void register(RsExternalResourceKeyBridge bridge) {
        if (bridge == null) throw new NullPointerException("bridge");
        if (!BRIDGES.contains(bridge)) {
            BRIDGES.add(bridge);
            BRIDGES.sort(Comparator.comparingInt(RsExternalResourceKeyBridge::priority).reversed());
        }
    }

    static List<RsExternalResourceKeyBridge> registered() {
        return List.copyOf(BRIDGES);
    }

    static @Nullable ResourceChannelKey toResourceKey(ResourceKey key) {
        if (key == null) return null;
        for (RsExternalResourceKeyBridge bridge : BRIDGES) {
            ResourceChannelKey converted = bridge.toResourceKey(key);
            if (converted != null) return converted;
        }
        return null;
    }

    static @Nullable ResourceKey toRsKey(ResourceChannelKey key) {
        if (key == null) return null;
        for (RsExternalResourceKeyBridge bridge : BRIDGES) {
            ResourceKey converted = bridge.toRsKey(key);
            if (converted != null) return converted;
        }
        return null;
    }
}
