package com.immortalstorage.immortalstorage.timeshift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic time-shift record table.
 *
 * <p>This class owns the whole behaviour (mutation, expiry, NBT shape) so the
 * per-generation {@link TimeShiftStore} is only a thin {@code SavedData} bridge.
 * Minecraft 26.1 replaced {@code SavedData#save}/{@code load} with
 * {@code SavedDataType}, and keeping the data handling here means both
 * generations persist the exact same tag layout.</p>
 */
public final class TimeShiftRecords {
    private static final String ENTRIES = "entries";
    private static final String POS = "pos";
    private static final String TIER = "tier";
    private static final String BOOSTED = "boosted";
    private static final String BOOST_END = "boostEnd";

    private final Map<BlockPos, TimeShiftEntry> entries = new LinkedHashMap<>();
    private int revision;

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public int revision() {
        return this.revision;
    }

    public @Nullable TimeShiftEntry get(BlockPos pos) {
        return this.entries.get(pos);
    }

    public void put(BlockPos pos, TimeShiftEntry entry) {
        this.entries.put(pos.immutable(), entry);
        this.revision++;
    }

    public @Nullable TimeShiftEntry remove(BlockPos pos) {
        TimeShiftEntry removed = this.entries.remove(pos);
        if (removed != null) this.revision++;
        return removed;
    }

    /** Drops every expired boost flag while keeping its permanent tier. */
    public boolean expireBoosts(long nowEpochMillis) {
        boolean changed = false;
        for (Map.Entry<BlockPos, TimeShiftEntry> entry : this.entries.entrySet()) {
            TimeShiftEntry value = entry.getValue();
            if (value.expired(nowEpochMillis)) {
                entry.setValue(value.withoutBoost());
                changed = true;
            }
        }
        if (changed) this.revision++;
        return changed;
    }

    /** Immutable snapshot used by the tick loop; safe against concurrent record changes. */
    public List<Map.Entry<BlockPos, TimeShiftEntry>> snapshot() {
        return new ArrayList<>(this.entries.entrySet());
    }

    public CompoundTag toTag() {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, TimeShiftEntry> entry : this.entries.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(POS, entry.getKey().asLong());
            item.putInt(TIER, entry.getValue().tier());
            item.putBoolean(BOOSTED, entry.getValue().boosted());
            item.putLong(BOOST_END, entry.getValue().boostEndEpochMillis());
            list.add(item);
        }
        CompoundTag tag = new CompoundTag();
        tag.put(ENTRIES, list);
        return tag;
    }

    public void readTag(CompoundTag tag) {
        this.entries.clear();
        this.revision = 0;
        ListTag list = tag.getList(ENTRIES, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag item = list.getCompound(index);
            if (!item.contains(POS, Tag.TAG_LONG)) continue;
            this.entries.put(BlockPos.of(item.getLong(POS)), new TimeShiftEntry(
                    item.getInt(TIER), item.getBoolean(BOOSTED), item.getLong(BOOST_END)));
        }
    }

    public static TimeShiftRecords fromTag(CompoundTag tag) {
        TimeShiftRecords records = new TimeShiftRecords();
        records.readTag(tag);
        return records;
    }
}
