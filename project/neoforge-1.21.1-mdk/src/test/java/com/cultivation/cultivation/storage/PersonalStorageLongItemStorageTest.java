package com.cultivation.cultivation.storage;

import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.network.storage.PersonalStorageLongItemStorage;
import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PersonalStorageLongItemStorageTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void guardedLongSurfaceSharesTheDataSummaryAndPublishesOnlyCommittedChanges() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(6);
        AtomicReference<CultivationPlayerData> live = new AtomicReference<>(data);
        AtomicInteger changed = new AtomicInteger();
        PersonalStorageLongItemStorage storage = new PersonalStorageLongItemStorage(
                data, changed::incrementAndGet, () -> live.get() == data);
        TerminalEntryKey diamond = TerminalEntryKey.of(new ItemStack(Items.DIAMOND));

        assertEquals(12L, storage.insert(diamond, 12L, TerminalStorageAction.SIMULATE));
        assertTrue(storage.snapshot().isEmpty());
        assertEquals(0, changed.get());

        assertEquals(12L, storage.insert(diamond, 12L, TerminalStorageAction.EXECUTE));
        assertEquals(1, changed.get());
        assertEquals(12L, storage.snapshot().getFirst().amount());
        assertEquals(data.getXianqiaoStorageRevision(), storage.revision());

        assertEquals(5L, storage.extract(diamond, 5L, TerminalStorageAction.SIMULATE));
        assertEquals(1, changed.get());
        assertEquals(5L, storage.extract(diamond, 5L, TerminalStorageAction.EXECUTE));
        assertEquals(2, changed.get());
        assertEquals(7L, storage.snapshot().getFirst().amount());

        live.set(new CultivationPlayerData());
        assertTrue(storage.snapshot().isEmpty());
        assertEquals(0L, storage.insert(diamond, 1L, TerminalStorageAction.EXECUTE));
        assertEquals(0L, storage.extract(diamond, 1L, TerminalStorageAction.EXECUTE));
        assertEquals(7L, data.getXianqiaoItemSummary().getFirst().amount());
    }

    @Test
    void aPreviouslyAcquiredSurfaceRetiresImmediatelyBelowTheXianqiaoBoundary() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(6);
        PersonalStorageLongItemStorage storage = new PersonalStorageLongItemStorage(
                data, () -> {}, () -> true);
        TerminalEntryKey iron = TerminalEntryKey.of(new ItemStack(Items.IRON_INGOT));
        assertEquals(3L, storage.insert(iron, 3L, TerminalStorageAction.EXECUTE));

        data.setStage(5);

        assertEquals(0L, storage.revision());
        assertTrue(storage.snapshot().isEmpty());
        assertEquals(0L, storage.extract(iron, 3L, TerminalStorageAction.EXECUTE));
    }

    @Test
    void endpointAddsANativeLongItemViewWithoutChangingLegacyFluidOptIn() {
        RegistryAccess.Frozen registries =
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(6);
        PersonalStorageNetwork.Endpoint endpoint = new PersonalStorageNetwork.Endpoint(
                UUID.randomUUID(), data, registries, () -> {});
        TerminalEntryKey emerald = TerminalEntryKey.of(new ItemStack(Items.EMERALD));

        assertNotNull(endpoint.itemStorage());
        assertEquals(80L, endpoint.itemStorage().insert(
                emerald, 80L, TerminalStorageAction.EXECUTE));
        assertEquals(80L, endpoint.itemStorage().snapshot().getFirst().amount());
        assertNotNull(endpoint.itemHandler(), "the standard int bridge remains available");
        assertNull(endpoint.fluidStorage(), "legacy constructors remain item-only");
        assertNull(endpoint.fluidHandler(), "legacy constructors remain item-only");
    }
}
