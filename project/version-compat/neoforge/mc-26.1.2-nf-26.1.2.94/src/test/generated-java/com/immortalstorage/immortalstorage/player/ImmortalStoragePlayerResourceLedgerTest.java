package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalStoragePlayerResourceLedgerTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final ResourceChannelKey ENERGY =
            new ResourceChannelKey("energy", "neoforge:fe");
    private static final ResourceChannelKey CHEMICAL =
            new ResourceChannelKey("mekanism_chemical", "mekanism:hydrogen");

    @Test
    void stableChannelResourceAmountAndRevisionRoundTripThroughPlayerNbt() {
        ImmortalStoragePlayerData original = new ImmortalStoragePlayerData();
        original.setStage(8);
        assertEquals(900L, original.insertExternalResource(
                ENERGY, 900L, ResourceTransferAction.EXECUTE));
        assertEquals(17L, original.insertExternalResource(
                CHEMICAL, 17L, ResourceTransferAction.EXECUTE));

        CompoundTag saved = original.serializeNBT(REGISTRIES);
        CompoundTag ledger = saved.getCompoundOrEmpty("externalResourceLedger");
        assertEquals(2L, ledger.getLongOr("revision", 0L));
        ListTag entries = ledger.getListOrEmpty("entries");
        assertEquals(2, entries.size());
        assertEquals("energy", entries.getCompoundOrEmpty(0).getStringOr("channel", ""));
        assertEquals("neoforge:fe", entries.getCompoundOrEmpty(0).getStringOr("resourceId", ""));
        assertEquals(900L, entries.getCompoundOrEmpty(0).getLongOr("amount", 0L));

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        assertEquals(900L, restored.getExternalResourceAmount(ENERGY));
        assertEquals(17L, restored.getExternalResourceAmount(CHEMICAL));
        assertEquals(2L, restored.getExternalResourceRevision());
    }

    @Test
    void loaderRetainsUnknownLegalKeysAndDropsMalformedOrNegativeRows() {
        CompoundTag saved = new CompoundTag();
        saved.putInt("stage", 8);
        CompoundTag ledger = new CompoundTag();
        ledger.putLong("revision", 41L);
        ListTag entries = new ListTag();
        entries.add(entry("future_magic", "pack:unknown_essence", Long.MAX_VALUE - 2L));
        entries.add(entry("future_magic", "pack:unknown_essence", 10L));
        entries.add(entry("Invalid Channel", "pack:ignored", 30L));
        entries.add(entry("future_magic", "missing_namespace", 40L));
        entries.add(entry("future_magic", "pack:negative", -1L));
        ledger.put("entries", entries);
        saved.put("externalResourceLedger", ledger);

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        ResourceChannelKey unknown =
                new ResourceChannelKey("future_magic", "pack:unknown_essence");
        assertEquals(Long.MAX_VALUE, restored.getExternalResourceAmount(unknown),
                "duplicate valid rows must merge without wrapping");
        assertEquals(0L, restored.getExternalResourceAmount(
                new ResourceChannelKey("future_magic", "pack:negative")));
        assertEquals(41L, restored.getExternalResourceRevision());

        CompoundTag resaved = restored.serializeNBT(REGISTRIES)
                .getCompoundOrEmpty("externalResourceLedger");
        ListTag sanitized = resaved.getListOrEmpty("entries");
        assertEquals(1, sanitized.size());
        assertEquals("future_magic", sanitized.getCompoundOrEmpty(0).getStringOr("channel", ""));
        assertEquals("pack:unknown_essence", sanitized.getCompoundOrEmpty(0).getStringOr("resourceId", ""));
        assertEquals(Long.MAX_VALUE, sanitized.getCompoundOrEmpty(0).getLongOr("amount", 0L));
    }

    @Test
    void cachedOwnerScopedStoreRechecksStageWithoutDeletingTheLedger() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(8);
        AtomicEnergyRefill.ResourceStore store = data.externalResourceStore(ENERGY);
        assertEquals(64L, store.insert(64L, ResourceTransferAction.EXECUTE));
        assertEquals(64L, store.amount());
        long revision = data.getExternalResourceRevision();

        data.setStage(7);
        assertEquals(0L, store.amount());
        assertEquals(0L, store.insert(20L, ResourceTransferAction.EXECUTE));
        assertEquals(0L, store.extract(20L, ResourceTransferAction.EXECUTE));
        assertEquals(revision, data.getExternalResourceRevision());
        ListTag persistedWhileLocked = data.serializeNBT(REGISTRIES)
                .getCompoundOrEmpty("externalResourceLedger")
                .getListOrEmpty("entries");
        assertEquals(64L, persistedWhileLocked.getCompoundOrEmpty(0).getLongOr("amount", 0L));

        data.setStage(8);
        assertEquals(64L, store.amount(), "the same cached view must reopen at stage eight");
        assertEquals(20L, store.extract(20L, ResourceTransferAction.SIMULATE));
        assertEquals(revision, data.getExternalResourceRevision(),
                "simulation must not advance the persisted revision");
        assertEquals(64L, store.amount());
    }

    @Test
    void longBoundaryAndSaturatedRevisionRemainStable() {
        CompoundTag saved = new CompoundTag();
        saved.putInt("stage", 8);
        CompoundTag ledger = new CompoundTag();
        ledger.putLong("revision", Long.MAX_VALUE);
        ListTag entries = new ListTag();
        entries.add(entry("energy", "neoforge:fe", Long.MAX_VALUE - 1L));
        ledger.put("entries", entries);
        saved.put("externalResourceLedger", ledger);

        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.deserializeNBT(REGISTRIES, saved);
        assertEquals(1L, data.insertExternalResource(
                ENERGY, Long.MAX_VALUE, ResourceTransferAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, data.getExternalResourceAmount(ENERGY));
        assertEquals(Long.MAX_VALUE, data.getExternalResourceRevision());
        assertEquals(0L, data.insertExternalResource(
                ENERGY, 1L, ResourceTransferAction.EXECUTE));
        assertEquals(Long.MAX_VALUE, data.getExternalResourceRevision());
        assertTrue(data.externalResourceStore(ENERGY) instanceof AtomicEnergyRefill.ResourceStore);
    }

    private static CompoundTag entry(String channel, String resourceId, long amount) {
        CompoundTag entry = new CompoundTag();
        entry.putString("channel", channel);
        entry.putString("resourceId", resourceId);
        entry.putLong("amount", amount);
        return entry;
    }
}
