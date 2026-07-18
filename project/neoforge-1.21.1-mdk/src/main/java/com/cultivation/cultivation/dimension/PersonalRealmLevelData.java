package com.cultivation.cultivation.dimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;

/**
 * Mutable time data for one personal realm.
 *
 * <p>Vanilla {@link DerivedLevelData} deliberately ignores time writes and
 * reads the wrapped overworld clock. That is correct for ordinary dimensions,
 * but it prevents a player-bound realm from owning an accelerated scheduled
 * tick timeline. This wrapper keeps only the realm clock and timer queue local;
 * game rules and the remaining world settings continue to come from the
 * server's normal world data.</p>
 */
final class PersonalRealmLevelData extends DerivedLevelData {
    private static final String CLOCK_DATA_ID = "cultivation_personal_realm_clock";

    private final TimerQueue<MinecraftServer> scheduledEvents =
            new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS);
    private long gameTime;
    private long dayTime;
    private ClockSavedData persistedClock;

    PersonalRealmLevelData(WorldData worldData, ServerLevelData wrapped) {
        super(worldData, wrapped);
        this.gameTime = wrapped.getGameTime();
        this.dayTime = wrapped.getDayTime();
    }

    void bindPersistence(DimensionDataStorage storage) {
        ClockSavedData clock = storage.computeIfAbsent(ClockSavedData.FACTORY, CLOCK_DATA_ID);
        if (clock.initialized) {
            this.gameTime = Math.max(0L, clock.gameTime);
            this.dayTime = Math.max(0L, clock.dayTime);
        } else {
            clock.update(this.gameTime, this.dayTime);
        }
        this.persistedClock = clock;
    }

    @Override
    public long getGameTime() {
        return this.gameTime;
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    @Override
    public void setGameTime(long gameTime) {
        this.gameTime = Math.max(0L, gameTime);
        persistClock();
    }

    @Override
    public void setDayTime(long dayTime) {
        this.dayTime = Math.max(0L, dayTime);
        persistClock();
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }

    private void persistClock() {
        if (this.persistedClock != null) {
            this.persistedClock.update(this.gameTime, this.dayTime);
        }
    }

    private static final class ClockSavedData extends SavedData {
        private static final SavedData.Factory<ClockSavedData> FACTORY =
                new SavedData.Factory<>(ClockSavedData::new, ClockSavedData::load);

        private boolean initialized;
        private long gameTime;
        private long dayTime;

        private static ClockSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            ClockSavedData data = new ClockSavedData();
            data.initialized = tag.getBoolean("initialized");
            data.gameTime = Math.max(0L, tag.getLong("gameTime"));
            data.dayTime = Math.max(0L, tag.getLong("dayTime"));
            return data;
        }

        private void update(long gameTime, long dayTime) {
            if (!this.initialized || this.gameTime != gameTime || this.dayTime != dayTime) {
                this.initialized = true;
                this.gameTime = gameTime;
                this.dayTime = dayTime;
                setDirty();
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("initialized", this.initialized);
            tag.putLong("gameTime", this.gameTime);
            tag.putLong("dayTime", this.dayTime);
            return tag;
        }
    }
}
