package com.immortalstorage.immortalstorage.timeshift;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 persistent bridge over {@link TimeShiftRecords}.
 *
 * <p>One store lives in each {@link ServerLevel}'s data storage, so a record is
 * bound to its dimension and survives a save/load cycle.  The store never
 * performs acceleration itself; {@link TimeShiftAcceleration} reads it once per
 * {@code tickBlockEntities} pass.</p>
 */
public final class TimeShiftStore extends SavedData {
    public static final String DATA_NAME = "immortalstorage_time_shift";

    private final TimeShiftRecords records = new TimeShiftRecords();

    public static TimeShiftStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TimeShiftStore::new, TimeShiftStore::load, null), DATA_NAME);
    }

    static TimeShiftStore load(CompoundTag tag, HolderLookup.Provider registries) {
        TimeShiftStore store = new TimeShiftStore();
        store.records.readTag(tag);
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return this.records.toTag();
    }

    public boolean isEmpty() {
        return this.records.isEmpty();
    }

    public int size() {
        return this.records.size();
    }

    public int revision() {
        return this.records.revision();
    }

    public @Nullable TimeShiftEntry get(BlockPos pos) {
        return this.records.get(pos);
    }

    public void put(BlockPos pos, TimeShiftEntry entry) {
        this.records.put(pos, entry);
        setDirty();
    }

    public @Nullable TimeShiftEntry remove(BlockPos pos) {
        TimeShiftEntry removed = this.records.remove(pos);
        if (removed != null) setDirty();
        return removed;
    }

    public boolean expireBoosts(long nowEpochMillis) {
        boolean changed = this.records.expireBoosts(nowEpochMillis);
        if (changed) setDirty();
        return changed;
    }

    public List<Map.Entry<BlockPos, TimeShiftEntry>> snapshot() {
        return this.records.snapshot();
    }
}
