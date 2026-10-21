package com.immortalstorage.immortalstorage.timeshift;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Persistence and expiry contract of the per-dimension time-shift store. */
final class TimeShiftStoreTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void savedRecordsSurviveACompoundRoundTrip() {
        TimeShiftStore store = new TimeShiftStore();
        BlockPos furnace = new BlockPos(12, 70, -34);
        BlockPos crop = new BlockPos(-5, 64, 9);
        store.put(furnace, TimeShiftEntry.permanent(4));
        store.put(crop, new TimeShiftEntry(1, true, 123_456_789L));

        CompoundTag saved = store.save(new CompoundTag(), null);
        TimeShiftStore loaded = TimeShiftStore.load(saved, null);

        assertEquals(2, loaded.size());
        assertEquals(TimeShiftEntry.permanent(4), loaded.get(furnace));
        TimeShiftEntry boosted = loaded.get(crop);
        assertNotNull(boosted);
        assertEquals(1, boosted.tier());
        assertTrue(boosted.boosted());
        assertEquals(123_456_789L, boosted.boostEndEpochMillis());
    }

    @Test
    void anExpiredBoostOnlyClearsTheFlagAndKeepsTheTier() {
        TimeShiftStore store = new TimeShiftStore();
        BlockPos pos = new BlockPos(0, 64, 0);
        store.put(pos, new TimeShiftEntry(2, true, 1_000L));

        assertFalse(store.expireBoosts(500L));
        assertTrue(store.get(pos).boosted());
        assertTrue(store.expireBoosts(1_000L));
        assertNotNull(store.get(pos));
        assertFalse(store.get(pos).boosted());
        assertEquals(2, store.get(pos).tier());
        assertFalse(store.expireBoosts(2_000L));
    }

    @Test
    void removingARecordReportsWhetherSomethingWasRemoved() {
        TimeShiftStore store = new TimeShiftStore();
        BlockPos pos = new BlockPos(3, 3, 3);
        store.put(pos, TimeShiftEntry.permanent(0));
        assertEquals(1, store.size());
        assertNotNull(store.remove(pos));
        assertNull(store.remove(pos));
        assertTrue(store.isEmpty());
    }

    @Test
    void anEmptyRecordRoundTripsThroughTheSavedTag() {
        TimeShiftStore store = new TimeShiftStore();
        // Only our own save/load touch the tag: 26.1.2 changed CompoundTag's typed
        // contains/getList signatures, so the test treats the tag as an opaque value.
        CompoundTag saved = store.save(new CompoundTag(), null);
        TimeShiftStore reloaded = TimeShiftStore.load(saved, null);
        assertTrue(reloaded.isEmpty());
        assertEquals(0, reloaded.size());
    }

    @Test
    void aStaleSnapshotDoesNotFollowLaterMutations() {
        TimeShiftStore store = new TimeShiftStore();
        BlockPos first = new BlockPos(1, 1, 1);
        store.put(first, TimeShiftEntry.permanent(1));
        var snapshot = store.snapshot();
        store.remove(first);
        assertEquals(1, snapshot.size());
        assertEquals(0, store.size());
        Map.Entry<BlockPos, TimeShiftEntry> only = snapshot.get(0);
        assertEquals(first, only.getKey());
    }

    @Test
    void everyRevisionCounterAdvancesOnARealMutation() {
        TimeShiftStore store = new TimeShiftStore();
        int start = store.revision();
        BlockPos pos = new BlockPos(9, 9, 9);
        store.put(pos, TimeShiftEntry.permanent(2));
        assertTrue(store.revision() > start);
        int afterPut = store.revision();
        store.remove(new BlockPos(0, 0, 0));
        assertEquals(afterPut, store.revision());
        store.remove(pos);
        assertTrue(store.revision() > afterPut);
    }

}
