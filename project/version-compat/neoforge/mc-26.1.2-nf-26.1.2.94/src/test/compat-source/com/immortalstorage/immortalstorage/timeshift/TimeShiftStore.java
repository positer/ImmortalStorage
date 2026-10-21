package com.immortalstorage.immortalstorage.timeshift;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

/**
 * 26.1.2 persistent bridge over {@link TimeShiftRecords}.
 *
 * <p>26.1 replaced {@code SavedData#save}/{@code load} with {@link SavedDataType},
 * so this generation declares a {@code SavedDataType} instead of overriding a
 * save method.  The codec is a passthrough over the exact same compound tag the
 * 1.21.1 lane writes, which keeps one on-disk layout for both generations.
 * 1.21.1 keeps its own copy of this file in the canonical tree.</p>
 */
public final class TimeShiftStore extends SavedData {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("immortalstorage", "time_shift");

    public static final Codec<TimeShiftStore> CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> fromTag((CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue()),
            store -> new Dynamic<>(NbtOps.INSTANCE, store.records.toTag()));

    public static final SavedDataType<TimeShiftStore> TYPE =
            new SavedDataType<>(ID, TimeShiftStore::new, CODEC, null);

    private final TimeShiftRecords records = new TimeShiftRecords();

    public static TimeShiftStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    static TimeShiftStore fromTag(CompoundTag tag) {
        TimeShiftStore store = new TimeShiftStore();
        store.records.readTag(tag);
        return store;
    }

    /**
     * Shared-test bridge. 26.1.2 persists through {@link #CODEC}; this method keeps the
     * canonical store tests compiling against both generations. The provider is unused.
     */
    static TimeShiftStore load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return fromTag(tag);
    }

    /** Shared-test bridge; see {@link #load}. The provider is unused. */
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
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
