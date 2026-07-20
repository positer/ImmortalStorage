package com.cultivation.cultivation.compat.ae2;

import appeng.api.stacks.AEKey;
import com.cultivation.core.resource.ResourceChannelKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-local registry for optional AE2 addon key bridges. */
public final class Ae2ExternalKeyBridges {
    private static final CopyOnWriteArrayList<Ae2ExternalKeyBridge> BRIDGES =
            new CopyOnWriteArrayList<>();

    private Ae2ExternalKeyBridges() {}

    public static void register(Ae2ExternalKeyBridge bridge) {
        if (bridge == null) throw new NullPointerException("bridge");
        if (!BRIDGES.contains(bridge)) {
            BRIDGES.add(bridge);
            BRIDGES.sort(Comparator.comparingInt(Ae2ExternalKeyBridge::priority).reversed());
        }
    }

    public static List<Ae2ExternalKeyBridge> registered() {
        return List.copyOf(BRIDGES);
    }

    static @Nullable ResourceChannelKey toResourceKey(AEKey key) {
        if (key == null) return null;
        for (Ae2ExternalKeyBridge bridge : BRIDGES) {
            ResourceChannelKey converted = bridge.toResourceKey(key);
            if (converted != null) return converted;
        }
        return null;
    }

    static @Nullable AEKey toAeKey(ResourceChannelKey key) {
        if (key == null) return null;
        for (Ae2ExternalKeyBridge bridge : BRIDGES) {
            AEKey converted = bridge.toAeKey(key);
            if (converted != null) return converted;
        }
        return null;
    }
}
