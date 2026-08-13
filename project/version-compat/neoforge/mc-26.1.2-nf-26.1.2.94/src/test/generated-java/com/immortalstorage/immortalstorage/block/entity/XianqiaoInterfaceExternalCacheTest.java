package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceExternalCacheTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static RegistryAccess.Frozen registries;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void directionlessBlockInteractionUsesOnlyConfiguredRealCache() {
        FakeExternalStorage owner = new FakeExternalStorage();
        ResourceChannelKey mana = ExternalResourceChannels.BOTANIA_MANA;
        owner.amounts.put(mana, 500L);
        XianqiaoInterfaceInventory inventory = inventory(owner);

        AtomicEnergyRefill.ResourceStore unconfigured = inventory.externalCacheStore(mana);
        assertEquals(0L, unconfigured.insert(10L, ResourceTransferAction.EXECUTE));
        assertFalse(inventory.hasExternalTarget(mana));

        assertTrue(inventory.setExternalTarget(0, mana, 100L));
        assertTrue(inventory.setExternalTarget(1, mana, 200L));
        assertTrue(inventory.hasExternalTarget(mana));
        assertEquals(300L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));
        assertEquals(200L, owner.amount(mana));

        AtomicEnergyRefill.ResourceStore cache = inventory.externalCacheStore(mana);
        assertEquals(300L, cache.amount());
        assertEquals(250L, cache.extract(250L, ResourceTransferAction.SIMULATE));
        assertEquals(300L, cache.amount());
        assertEquals(250L, cache.extract(250L, ResourceTransferAction.EXECUTE));
        assertEquals(50L, cache.amount());
        assertEquals(200L, owner.amount(mana),
                "direct target-mod extraction must not bypass the block cache");

        assertEquals(125L, cache.insert(125L, ResourceTransferAction.EXECUTE));
        assertEquals(175L, cache.amount());
        assertEquals(200L, owner.amount(mana),
                "direct target-mod insertion must remain in the block cache until scheduling");
        assertEquals(125L, inventory.replenishAllSlots(TerminalStorageAction.EXECUTE));
        assertEquals(300L, cache.amount());
        assertEquals(75L, owner.amount(mana));
    }

    @Test
    void faceMasksDoNotGateDirectionlessCacheAndClearReturnsItAtomically() {
        FakeExternalStorage owner = new FakeExternalStorage();
        ResourceChannelKey source = ExternalResourceChannels.ARS_NOUVEAU_SOURCE;
        owner.amounts.put(source, 80L);
        XianqiaoInterfaceInventory inventory = inventory(owner);
        assertTrue(inventory.setExternalTarget(4, source, 80L));
        assertEquals(0, inventory.getOutputFaceMask(4));
        assertEquals(80L, inventory.replenishSlot(4, TerminalStorageAction.EXECUTE));

        AtomicEnergyRefill.ResourceStore cache = inventory.externalCacheStore(source);
        assertEquals(20L, cache.extract(20L, ResourceTransferAction.EXECUTE));
        assertEquals(60L, cache.amount());
        assertEquals(0L, owner.amount(source));

        assertTrue(inventory.clearSlot(4));
        assertFalse(inventory.hasExternalTarget(source));
        assertEquals(60L, owner.amount(source));
        assertEquals(0L, cache.amount());
    }

    @Test
    void longTargetCacheAndPerSlotMaskRoundTripExactly() {
        FakeExternalStorage owner = new FakeExternalStorage();
        ResourceChannelKey mana = ExternalResourceChannels.BOTANIA_MANA;
        long amount = ExternalResourceChannels.cacheLimit(mana);
        owner.amounts.put(mana, amount);
        XianqiaoInterfaceInventory original = inventory(owner);
        assertTrue(original.setExternalTarget(7, mana, amount));
        assertTrue(original.setOutputFaceEnabled(7, Direction.EAST, true));
        assertEquals(amount, original.replenishSlot(7, TerminalStorageAction.EXECUTE));

        CompoundTag saved = new CompoundTag();
        original.saveState(saved, registries);
        XianqiaoInterfaceInventory restored = inventory(new FakeExternalStorage());
        restored.loadState(saved, registries);

        assertEquals(mana, restored.getExternalTarget(7));
        assertEquals(amount, restored.getExternalDesiredAmount(7));
        assertEquals(amount, restored.getExternalCachedAmount(7));
        assertTrue(restored.isOutputFaceEnabled(7, Direction.EAST));
        assertFalse(restored.isOutputFaceEnabled(7, Direction.WEST));
    }

    @Test
    void resourceSpecificLimitsClampDesiredButPreserveLegacyCachedExcess() {
        FakeExternalStorage owner = new FakeExternalStorage();
        XianqiaoInterfaceInventory inventory = inventory(owner);
        assertTrue(inventory.setExternalTarget(
                0, ExternalResourceChannels.ARS_NOUVEAU_SOURCE, Long.MAX_VALUE));
        assertEquals(10_000L, inventory.getExternalDesiredAmount(0));
    }

    @Test
    void pipeViewsInsertOnAnyFaceButExtractOnlyThroughEnabledSlotFaces() {
        FakeExternalStorage owner = new FakeExternalStorage();
        ResourceChannelKey energy = ExternalResourceChannels.FE;
        owner.amounts.put(energy, 100L);
        XianqiaoInterfaceInventory inventory = inventory(owner);
        assertTrue(inventory.setExternalTarget(2, energy, 100L));
        assertTrue(inventory.setOutputFaceEnabled(2, Direction.EAST, true));
        assertEquals(100L, inventory.replenishSlot(2, TerminalStorageAction.EXECUTE));

        AtomicEnergyRefill.ResourceStore east = inventory.externalCacheStore(
                energy, Direction.EAST);
        AtomicEnergyRefill.ResourceStore west = inventory.externalCacheStore(
                energy, Direction.WEST);
        assertEquals(0L, west.insert(10L, ResourceTransferAction.EXECUTE),
                "raw directional cache remains extraction-mask scoped");
        assertEquals(100L, east.amount());
        assertEquals(0L, west.amount());
        assertEquals(0L, west.extract(100L, ResourceTransferAction.EXECUTE));
        assertEquals(100L, east.amount());
    }

    private static XianqiaoInterfaceInventory inventory(ExternalResourceStorage external) {
        return new XianqiaoInterfaceInventory(
                new EmptyItemStorage(), null, external, () -> true, () -> {}, () -> {});
    }

    private static final class EmptyItemStorage implements TerminalItemStorage {
        @Override public long revision() { return 0L; }
        @Override public List<StorageItemSummary> snapshot() { return List.of(); }
        @Override public long insert(
                TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
        @Override public long extract(
                TerminalEntryKey key, long amount, TerminalStorageAction action) { return 0L; }
    }

    private static final class FakeExternalStorage implements ExternalResourceStorage {
        private final Map<ResourceChannelKey, Long> amounts = new HashMap<>();
        private long revision;

        private long amount(ResourceChannelKey key) { return amounts.getOrDefault(key, 0L); }
        @Override public long revision() { return revision; }
        @Override public List<ResourceChannelEntry> snapshot() {
            return amounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0L)
                    .map(entry -> new ResourceChannelEntry(entry.getKey(), entry.getValue()))
                    .toList();
        }
        @Override public long insert(
                ResourceChannelKey key, long amount, ResourceTransferAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            long current = amount(key);
            long accepted = Math.min(amount, Long.MAX_VALUE - current);
            if (action.executes() && accepted > 0L) {
                amounts.put(key, current + accepted);
                revision++;
            }
            return accepted;
        }
        @Override public long extract(
                ResourceChannelKey key, long amount, ResourceTransferAction action) {
            if (key == null || amount <= 0L || action == null) return 0L;
            long current = amount(key);
            long extracted = Math.min(amount, current);
            if (action.executes() && extracted > 0L) {
                amounts.put(key, current - extracted);
                revision++;
            }
            return extracted;
        }
    }
}
