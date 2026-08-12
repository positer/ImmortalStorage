package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class RsExternalResourceKeyBridgesTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void nativeAddonKeyWinsForEmissionWhileBuiltinKeyRemainsReadable() {
        ResourceChannelKey logical = new ResourceChannelKey("test", "immortalstorage:native");
        TestKey nativeKey = new TestKey();
        RsExternalResourceKeyBridges.register(ImmortalStorageRsExternalResourceKeyBridge.INSTANCE);
        RsExternalResourceKeyBridges.register(new RsExternalResourceKeyBridge() {
            @Override public int priority() { return 100; }
            @Override public ResourceChannelKey toResourceKey(ResourceKey key) {
                return key == nativeKey ? logical : null;
            }
            @Override public ResourceKey toRsKey(ResourceChannelKey key) {
                return logical.equals(key) ? nativeKey : null;
            }
        });

        assertEquals(nativeKey, RsExternalResourceKeyBridges.toRsKey(logical));
        assertEquals(logical, RsExternalResourceKeyBridges.toResourceKey(nativeKey));
        assertEquals(ExternalResourceChannels.FE, RsExternalResourceKeyBridges.toResourceKey(
                new RsExternalResource(ExternalResourceChannels.FE)));
        assertInstanceOf(RsExternalResource.class,
                RsExternalResourceKeyBridges.toRsKey(ExternalResourceChannels.FE));
    }

    private static final class TestKey implements ResourceKey {
    }
}
