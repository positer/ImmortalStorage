package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

final class PersonalRealmLevelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ImmortalStorageMod.MODID + ".realm.factory");
    private static final ChunkProgressListener NOOP_PROGRESS = new ChunkProgressListener() {
        @Override public void updateSpawnPos(ChunkPos pos) {}
        @Override public void onStatusChange(ChunkPos pos, net.minecraft.world.level.chunk.status.ChunkStatus status) {}
        @Override public void start() {}
        @Override public void stop() {}
    };

    private PersonalRealmLevelFactory() {}

    static ServerLevel getOrCreate(MinecraftServer server, UUID playerId) {
        ResourceKey<Level> key = ImmortalStorageDimensions.personalRealmKey(playerId);
        ServerLevel existing = server.getLevel(key);
        if (existing != null) {
            return existing;
        }
        synchronized (server) {
            existing = server.getLevel(key);
            if (existing != null) {
                return existing;
            }
            return createAndRegister(server, key, playerId);
        }
    }

    private static ServerLevel createAndRegister(MinecraftServer server, ResourceKey<Level> key, UUID playerId) {
        try {
            Holder<DimensionType> dimensionType = server.registryAccess()
                    .lookupOrThrow(Registries.DIMENSION_TYPE)
                    .getOrThrow(ImmortalStorageDimensions.XIANQIAO_REALM_TYPE);
            HolderGetter<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
            LevelStem stem = new LevelStem(dimensionType, new XianqiaoRealmChunkGenerator(biomes));

            WorldData worldData = server.getWorldData();
            PersonalRealmLevelData levelData = new PersonalRealmLevelData(worldData, worldData.overworldData());
            Executor executor = field(server, MinecraftServer.class, "executor", Executor.class);
            LevelStorageSource.LevelStorageAccess storage = field(server, MinecraftServer.class, "storageSource", LevelStorageSource.LevelStorageAccess.class);
            long seed = worldData.worldGenOptions().seed() ^ playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
            RandomSequences randomSequences = new RandomSequences(seed);

            ServerLevel level = new PersonalRealmServerLevel(
                    server,
                    executor,
                    storage,
                    levelData,
                    key,
                    stem,
                    NOOP_PROGRESS,
                    worldData.isDebugWorld(),
                    seed,
                    List.of(),
                    true,
                    randomSequences,
                    playerId);

            Map<ResourceKey<Level>, ServerLevel> levels = server.forgeGetWorldMap();
            levels.put(key, level);
            server.markWorldsDirty();
            LOG.info("Registered personal Xianqiao realm {} for player {}", key.location(), playerId);
            return level;
        } catch (Exception e) {
            LOG.error("Failed to register personal Xianqiao realm {} for player {}", key.location(), playerId, e);
            return null;
        }
    }

    private static <T> T field(Object target, Class<?> owner, String name, Class<T> type) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
