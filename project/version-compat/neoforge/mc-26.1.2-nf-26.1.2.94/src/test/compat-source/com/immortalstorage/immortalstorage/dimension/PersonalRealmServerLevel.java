package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.LevelStorageSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/** A server level whose simulation, clock presentation and weather belong to one personal Xianqiao. */
final class PersonalRealmServerLevel extends ServerLevel {
    static final double MIN_TICK_SCALE = 0.0D;
    static final double MAX_TICK_SCALE = 32.0D;
    static final float BASE_TICKS_PER_SECOND = 20.0F;

    private final UUID ownerId;
    private final PersonalRealmLevelData realmLevelData;
    private final TickBudget tickBudget = new TickBudget();
    private WeatherData realmWeatherData;
    private boolean tickingRealm;

    PersonalRealmServerLevel(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storage,
            PersonalRealmLevelData levelData,
            ResourceKey<Level> levelKey,
            LevelStem stem,
            boolean debug,
            long seed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            UUID ownerId) {
        super(server, executor, storage, levelData, levelKey, stem, debug, seed, customSpawners, tickTime);
        if (!ImmortalStorageDimensions.isPersonalRealmFor(levelKey, ownerId)) {
            throw new IllegalArgumentException("Personal realm level key is not bound to its owner");
        }
        this.ownerId = ownerId;
        this.realmLevelData = levelData;
        levelData.bindPersistence(getDataStorage());
    }

    UUID ownerId() {
        return this.ownerId;
    }

    boolean isBoundTo(UUID playerId) {
        return this.ownerId.equals(playerId)
                && ImmortalStorageDimensions.isPersonalRealmFor(dimension(), playerId);
    }

    void activateTickScale(UUID playerId, double requestedScale) {
        if (!isBoundTo(playerId)) {
            throw new IllegalArgumentException("Only the bound owner may accelerate a personal realm");
        }
        this.tickBudget.activate(requestedScale);
    }

    void restoreNormalTickScale() {
        this.tickBudget.restore();
    }

    void refreshEnvironmentLock(UUID playerId, boolean daytime, int weatherMode) {
        if (!isBoundTo(playerId)) {
            throw new IllegalArgumentException("Only the bound owner may configure a personal realm");
        }
        realmLevelData.setEnvironmentLock(daytime, weatherMode);
        applyEnvironmentLock();
        syncOwnerEnvironment(RealmHelper.onlinePlayerForRealm(getServer(), this.ownerId));
    }

    boolean isTickScaleActive() {
        return this.tickBudget.active;
    }

    double effectiveTickScale() {
        return this.tickBudget.scale;
    }

    float effectiveTicksPerSecond() {
        return (float) (BASE_TICKS_PER_SECOND * this.tickBudget.scale);
    }

    /**
     * 26.1 moved weather out of ServerLevelData into WeatherData. Vanilla's
     * ServerLevel returns the server-global instance, so a personal realm must
     * provide its own instance or changing it also changes every dimension.
     * The lazy initialization is required because ServerLevel consults this
     * virtual method from its constructor.
     */
    @Override
    public WeatherData getWeatherData() {
        if (this.realmWeatherData == null) {
            this.realmWeatherData = new WeatherData();
        }
        return this.realmWeatherData;
    }

    /** Server-side time queries must use the realm lock, not the shared server clock. */
    @Override
    public long getDefaultClockTime() {
        return lockedClockTime();
    }

    @Override
    public long getOverworldClockTime() {
        return lockedClockTime();
    }

    @Override
    public void tick(BooleanSupplier hasTime) {
        if (this.tickingRealm) return;
        ServerPlayer owner = RealmHelper.onlinePlayerForRealm(getServer(), this.ownerId);
        // Only an offline owner restores 1x.  While the owner is online in a
        // different dimension the realm keeps its activated (possibly
        // accelerated) tick budget, so a time-flow adjustment made from
        // anywhere keeps ticking the realm.
        if (owner == null) {
            this.tickBudget.restore();
        }
        if (owner != null) {
            var data = com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(owner);
            realmLevelData.setEnvironmentLock(data.isRealmDaytime(), data.getRealmWeatherMode());
        }
        applyEnvironmentLock();
        int passes = this.tickBudget.consumePasses();
        if ((getGameTime() & 0x7F) == 0) {
            ImmortalStorageMod.LOG.info("[Realm] tick active={} scale={} passes={} ownerInRealm={}",
                    this.tickBudget.active, this.tickBudget.scale, passes,
                    owner != null && owner.level() == this);
        }
        this.tickingRealm = true;
        try {
            // First pass is the full dimension tick; extra accelerated passes
            // tick only block entities so realm machines run faster without
            // repeating the full dimension tick and stalling the main thread.
            super.tick(hasTime);
            applyEnvironmentLock();
            for (int pass = 1; pass < passes; pass++) {
                if (!this.tickBudget.active) break;
                this.tickBlockEntities();
                applyEnvironmentLock();
            }
        } finally {
            this.tickingRealm = false;
        }
        syncOwnerEnvironment(owner);
    }

    private long lockedClockTime() {
        return RealmEnvironmentPolicy.lockedDayTime(this.realmLevelData == null
                || this.realmLevelData.lockedDaytime());
    }

    private void applyEnvironmentLock() {
        int weather = realmLevelData.lockedWeatherMode();
        boolean raining = RealmEnvironmentPolicy.requiresRain(weather);
        boolean thundering = RealmEnvironmentPolicy.requiresThunder(weather);
        WeatherData weatherData = getWeatherData();
        if (weatherData.isRaining() != raining) weatherData.setRaining(raining);
        if (weatherData.isThundering() != thundering) weatherData.setThundering(thundering);
        if (raining) {
            if (weatherData.getRainTime() < 200) weatherData.setRainTime(12_000);
            if (weatherData.getClearWeatherTime() != 0) weatherData.setClearWeatherTime(0);
        } else {
            if (weatherData.getClearWeatherTime() < 200) weatherData.setClearWeatherTime(12_000);
            if (weatherData.getRainTime() != 0) weatherData.setRainTime(0);
        }
        if (thundering) {
            if (weatherData.getThunderTime() < 200) weatherData.setThunderTime(12_000);
        } else if (weatherData.getThunderTime() != 0) {
            weatherData.setThunderTime(0);
        }
        setRainLevel(raining ? 1.0F : 0.0F);
        setThunderLevel(thundering ? 1.0F : 0.0F);
        environmentAttributes().invalidateTickCache();
    }

    /**
     * World clocks are server-global in 26.1. Send the owner a realm-local
     * frozen state while they are inside this dimension, without mutating the
     * clock seen by the overworld or another player's realm.
     */
    private void syncOwnerEnvironment(@Nullable ServerPlayer owner) {
        if (owner == null || owner.level() != this) return;
        dimensionType().defaultClock().ifPresent(clock -> owner.connection.send(
                new ClientboundSetTimePacket(getGameTime(), Map.of(clock,
                        new ClockNetworkState(lockedClockTime(), 0.0F, 0.0F)))));
        owner.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, getRainLevel(1.0F)));
        owner.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, getThunderLevel(1.0F)));
    }

    static double clampTickScale(double requestedScale) {
        if (!Double.isFinite(requestedScale)) {
            return requestedScale == Double.POSITIVE_INFINITY ? MAX_TICK_SCALE : 1.0D;
        }
        return Math.max(MIN_TICK_SCALE, Math.min(MAX_TICK_SCALE, requestedScale));
    }

    static final class TickBudget {
        private boolean active;
        private double scale = 1.0D;
        private int scalePermille = RealmTimeScalePolicy.NORMAL_PERMILLE;
        private int carryPermille;

        void activate(double requestedScale) {
            double clampedScale = clampTickScale(requestedScale);
            int clampedPermille = (int) Math.round(
                    clampedScale * RealmTimeScalePolicy.NORMAL_PERMILLE);
            if (this.active && this.scalePermille == clampedPermille) return;
            this.active = true;
            this.scale = clampedScale;
            this.scalePermille = clampedPermille;
            this.carryPermille = 0;
        }

        void restore() {
            this.active = false;
            this.scale = 1.0D;
            this.scalePermille = RealmTimeScalePolicy.NORMAL_PERMILLE;
            this.carryPermille = 0;
        }

        int consumePasses() {
            this.carryPermille += this.scalePermille;
            int passes = Math.min(this.carryPermille / RealmTimeScalePolicy.NORMAL_PERMILLE,
                    (int) MAX_TICK_SCALE);
            this.carryPermille -= passes * RealmTimeScalePolicy.NORMAL_PERMILLE;
            return passes;
        }
    }
}
