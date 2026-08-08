package com.immortalstorage.immortalstorage.dimension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * A server level whose tick budget belongs only to one personal Xianqiao.
 *
 * <p>This deliberately does not read or mutate MinecraftServer's global
 * ServerTickRateManager (the object used by vanilla {@code /tick rate}). The
 * normal server loop still invokes this level once; this class grants bounded
 * additional passes to this level alone. The pattern is equivalent to local
 * tick accelerators such as Time in a Bottle, but the target is the owner's
 * isolated realm rather than an arbitrary block.</p>
 */
final class PersonalRealmServerLevel extends ServerLevel {
    static final double MIN_TICK_SCALE = 0.0D;
    static final double MAX_TICK_SCALE = 32.0D;
    static final float BASE_TICKS_PER_SECOND = 20.0F;

    private final UUID ownerId;
    private final PersonalRealmLevelData realmLevelData;
    private final TickBudget tickBudget = new TickBudget();
    private boolean tickingRealm;

    PersonalRealmServerLevel(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storage,
            PersonalRealmLevelData levelData,
            ResourceKey<Level> levelKey,
            LevelStem stem,
            ChunkProgressListener progressListener,
            boolean debug,
            long seed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            @Nullable RandomSequences randomSequences,
            UUID ownerId) {
        super(server, executor, storage, levelData, levelKey, stem, progressListener,
                debug, seed, customSpawners, tickTime, randomSequences);
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

    @Override
    public void tick(BooleanSupplier hasTime) {
        // A nested invocation would multiply the pass budget recursively. The
        // normal server loop is the sole permitted entry into this scheduler.
        if (this.tickingRealm) return;
        var owner = RealmHelper.onlinePlayerForRealm(getServer(), this.ownerId);
        if (owner == null || owner.level() != this) {
            this.tickBudget.restore();
        }
        if (owner != null) {
            var data = com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData.get(owner);
            realmLevelData.setEnvironmentLock(data.isRealmDaytime(), data.getRealmWeatherMode());
        }
        applyEnvironmentLock();
        int passes = this.tickBudget.consumePasses();
        this.tickingRealm = true;
        try {
            for (int pass = 0; pass < passes; pass++) {
                if (pass > 0 && !this.tickBudget.active) break;
                super.tick(hasTime);
                applyEnvironmentLock();
            }
        } finally {
            this.tickingRealm = false;
        }
    }

    private void applyEnvironmentLock() {
        setDayTime(RealmEnvironmentPolicy.lockedDayTime(realmLevelData.lockedDaytime()));
        int weather = realmLevelData.lockedWeatherMode();
        boolean raining = RealmEnvironmentPolicy.requiresRain(weather);
        boolean thundering = RealmEnvironmentPolicy.requiresThunder(weather);
        if (realmLevelData.isRaining() != raining) realmLevelData.setRaining(raining);
        if (realmLevelData.isThundering() != thundering) realmLevelData.setThundering(thundering);
        if (raining) {
            if (realmLevelData.getRainTime() < 200) realmLevelData.setRainTime(12_000);
            if (realmLevelData.getClearWeatherTime() != 0) realmLevelData.setClearWeatherTime(0);
        } else {
            if (realmLevelData.getClearWeatherTime() < 200) realmLevelData.setClearWeatherTime(12_000);
            if (realmLevelData.getRainTime() != 0) realmLevelData.setRainTime(0);
        }
        if (thundering) {
            if (realmLevelData.getThunderTime() < 200) realmLevelData.setThunderTime(12_000);
        } else if (realmLevelData.getThunderTime() != 0) {
            realmLevelData.setThunderTime(0);
        }
    }

    static double clampTickScale(double requestedScale) {
        if (!Double.isFinite(requestedScale)) {
            return requestedScale == Double.POSITIVE_INFINITY ? MAX_TICK_SCALE : 1.0D;
        }
        return Math.max(MIN_TICK_SCALE, Math.min(MAX_TICK_SCALE, requestedScale));
    }

    /** Fraction-preserving tick budget, kept package-visible for contract tests. */
    static final class TickBudget {
        private boolean active;
        private double scale = 1.0D;
        private int scalePermille = RealmTimeScalePolicy.NORMAL_PERMILLE;
        private int carryPermille;

        void activate(double requestedScale) {
            this.active = true;
            this.scale = clampTickScale(requestedScale);
            this.scalePermille = (int) Math.round(this.scale * RealmTimeScalePolicy.NORMAL_PERMILLE);
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
