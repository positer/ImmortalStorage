package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

final class Ae2ExternalKeyBridgesTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void registeredBridgeRoundTripsAnAddonOwnedIdentity() {
        AEItemKey marker = AEItemKey.of(Items.DEBUG_STICK);
        Ae2ExternalKeyBridge bridge = new Ae2ExternalKeyBridge() {
            @Override
            public ResourceChannelKey toResourceKey(AEKey key) {
                return key == marker ? ExternalResourceChannels.FE : null;
            }

            @Override
            public AEKey toAeKey(ResourceChannelKey key) {
                return ExternalResourceChannels.FE.equals(key) ? marker : null;
            }
        };

        Ae2ExternalKeyBridges.register(bridge);
        Ae2ExternalKeyBridges.register(bridge);

        assertEquals(1L, Ae2ExternalKeyBridges.registered().stream()
                .filter(candidate -> candidate == bridge).count(),
                "one optional module must not register the same bridge twice");
        assertEquals(ExternalResourceChannels.FE, Ae2ExternalKeyBridges.toResourceKey(marker));
        assertSame(marker, Ae2ExternalKeyBridges.toAeKey(ExternalResourceChannels.FE));
    }

    @Test
    void addonCanonicalKeyWinsOverImmortalStorageFallbackWithoutRejectingEitherInput() {
        AEItemKey addonKey = AEItemKey.of(Items.REDSTONE);
        Ae2ExternalKeyBridge addon = new Ae2ExternalKeyBridge() {
            @Override public int priority() { return 100; }
            @Override public ResourceChannelKey toResourceKey(AEKey key) {
                return key == addonKey ? ExternalResourceChannels.BOTANIA_MANA : null;
            }
            @Override public AEKey toAeKey(ResourceChannelKey key) {
                return ExternalResourceChannels.BOTANIA_MANA.equals(key) ? addonKey : null;
            }
        };
        Ae2ExternalKeyBridges.register(ImmortalStorageExternalResourceKeyBridge.INSTANCE);
        Ae2ExternalKeyBridges.register(addon);

        ImmortalStorageExternalResourceKey fallback =
                new ImmortalStorageExternalResourceKey(ExternalResourceChannels.BOTANIA_MANA);
        assertSame(addonKey, Ae2ExternalKeyBridges.toAeKey(ExternalResourceChannels.BOTANIA_MANA),
                "one canonical addon identity must be emitted to prevent duplicate AE2 directory rows");
        assertEquals(ExternalResourceChannels.BOTANIA_MANA,
                Ae2ExternalKeyBridges.toResourceKey(addonKey));
        assertEquals(ExternalResourceChannels.BOTANIA_MANA,
                Ae2ExternalKeyBridges.toResourceKey(fallback),
                "legacy/fallback keys remain readable but share the same authoritative ledger key");
    }

    @Test
    void cultivationOwnedKeyRoundTripsWithoutTargetModClasses() {
        ResourceChannelKey chemical = ExternalResourceChannels.mekanismChemical("mekanism:hydrogen");
        ImmortalStorageExternalResourceKey key = new ImmortalStorageExternalResourceKey(chemical);

        assertEquals(chemical, key.resource());
        assertEquals(key, ImmortalStorageExternalResourceKey.fromTag(com.immortalstorage.immortalstorage.compat.CompatTestNbt.input(com.immortalstorage.immortalstorage.compat.CompatTestNbt.toTag(key))));
        assertNull(ImmortalStorageExternalResourceKey.fromTag(com.immortalstorage.immortalstorage.compat.CompatTestNbt.input(new net.minecraft.nbt.CompoundTag())));
        assertEquals("mekanism:hydrogen", key.getId().toString());
    }
}
