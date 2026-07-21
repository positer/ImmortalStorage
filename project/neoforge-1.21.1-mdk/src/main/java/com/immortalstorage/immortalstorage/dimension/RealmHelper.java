package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Teleports a player into and out of the personal    (      ).
 *
 * Per-player isolation is achieved by registering a distinct runtime
 * dimension key for each player UUID: immortalstorage:xianqiao_realm/<uuid>.
 * The old shared-dimension coordinate partition is intentionally not used.
 */
public final class RealmHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ImmortalStorageMod.MODID + ".realm");

    private static final int CENTER_CHUNK_X = 0;
    private static final int CENTER_CHUNK_Z = 0;
    private static final int MAX_RUNTIME_FORCED_RADIUS_CHUNKS = 3;
    private static final Map<UUID, Set<Long>> FORCED_CHUNK_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> ADMIN_SUSPENDED_REALMS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Double> APPLIED_BORDER_SIZE = new ConcurrentHashMap<>();
    private static final double UNBOUNDED_BORDER_SIZE = 59_999_968.0D;
    private static final String NBT_BOUNDARY_NOTICE_TICK = "immortalstorageBoundaryNoticeTick";
    private static final long BOUNDARY_NOTICE_COOLDOWN_TICKS = 40L;

    private RealmHelper() {}

    /** Restore a saved personal-realm respawn target before vanilla needs to resolve it. */
    public static void ensureRespawnRealmRegistered(ServerPlayer player) {
        if (player == null || player.server == null) return;
        ResourceKey<Level> respawnDimension = player.getRespawnDimension();
        java.util.Optional<UUID> owner = ImmortalStorageDimensions.personalRealmOwner(respawnDimension);
        if (owner.isPresent() && owner.get().equals(player.getUUID())) {
            PersonalRealmLevelFactory.getOrCreate(player.server, owner.get());
        }
    }

    public static ServerLevel resolveOwnedPersonalRealm(ServerPlayer player, ResourceKey<Level> target) {
        if (player == null || player.server == null || target == null) return null;
        java.util.Optional<UUID> owner = ImmortalStorageDimensions.personalRealmOwner(target);
        if (owner.isEmpty() || !owner.get().equals(player.getUUID())) return null;
        return PersonalRealmLevelFactory.getOrCreate(player.server, owner.get());
    }

    /** Try to enter the realm.  Returns true on success. */
    public static boolean enterRealm(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) {
            player.sendSystemMessage(Component.literal("You are not ready to enter the realm (need stage 6+)."));
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel realm = PersonalRealmLevelFactory.getOrCreate(server, player.getUUID());
        if (realm == null) {
            player.sendSystemMessage(Component.literal("Your personal realm could not be registered; check the server log."));
            return false;
        }
        if (!ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), player.getUUID())) {
            player.sendSystemMessage(Component.literal("Refusing to enter a realm that is not bound to your player id."));
            return false;
        }
        if (!activateRealmTickRate(player, realm)) {
            player.sendSystemMessage(Component.literal("Your personal realm does not support an isolated tick rate."));
            return false;
        }
        // Save the "real" position so we can return.
        if (!ImmortalStorageDimensions.isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
            data.markExitPosition(
                    player.getX(), player.getY(), player.getZ(),
                    player.level().dimension().location().toString());
        }

        forceChunkIfNeeded(realm, player.getUUID(), CENTER_CHUNK_X, CENTER_CHUNK_Z, true);

        // Teleport to the surface of this player's independent dimension origin.
        double surfaceY = XianqiaoRealmChunkGenerator.TOP_Y + 1.0;
        Set<RelativeMovement> rel = RelativeMovement.ALL;
        player.teleportTo(realm, 8.0, surfaceY, 8.0,
                rel, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("You have entered your Xianqiao (Realm)."));
        return true;
    }

    /** Try to leave the realm and return to the previous real-world position. */
    public static boolean exitRealm(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!ImmortalStorageDimensions.isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
            player.sendSystemMessage(Component.literal("You are not in the realm."));
            return false;
        }
        if (!data.hasExitPosition()) {
            player.sendSystemMessage(Component.literal("No return point known; defaulting to overworld spawn."));
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        ServerLevel target = null;
        if (data.hasExitPosition()) {
            ResourceLocation dimId = ResourceLocation.tryParse(data.getLastExitDim());
            if (dimId != null) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
                target = server.getLevel(key);
            }
        }
        if (target == null) {
            target = server.overworld();
        }

        // Restore normal time flow before leaving and release all owner tickets.
        releaseRealmTickRate(server, player.getUUID());

        BlockPos fallback = target.getSharedSpawnPos();
        double x = data.hasExitPosition() ? data.getLastExitX() : fallback.getX() + 0.5;
        double y = data.hasExitPosition() ? data.getLastExitY() : fallback.getY() + 1.0;
        double z = data.hasExitPosition() ? data.getLastExitZ() : fallback.getZ() + 0.5;
        Set<RelativeMovement> rel = RelativeMovement.ALL;
        player.teleportTo(target, x, y, z, rel, player.getYRot(), player.getXRot());
        data.clearExitPosition();
        player.sendSystemMessage(Component.literal("You have left your Xianqiao (Realm)."));
        return true;
    }

    /**
     * On server tick, force-load chunks inside the player's personal realm at the
     * current stage.  No-op if player is not in the realm.
     */
    public static void ensureChunksForced(ServerPlayer player) {
        if (ADMIN_SUSPENDED_REALMS.contains(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel realm)) return;
        if (!ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), player.getUUID())) return;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        forceChunkIfNeeded(realm, player.getUUID(), CENTER_CHUNK_X, CENTER_CHUNK_Z, false);
        for (long packed : data.getModifiedRealmChunks()) {
            forceChunkIfNeeded(realm, player.getUUID(),
                    net.minecraft.world.level.ChunkPos.getX(packed),
                    net.minecraft.world.level.ChunkPos.getZ(packed), false);
        }
    }

    /** Draw and enforce the finite plot only while its owner is inside this realm. */
    public static boolean enforcePlayerBoundary(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel realm)
                || !ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), player.getUUID())) return false;
        int stage = ImmortalStoragePlayerData.get(player).getStage();
        refreshVisibleBoundary(realm, player.getUUID(), stage,
                ImmortalStoragePlayerData.get(player).getRealmRadiusChunks());
        if (stage < 6 || stage >= 9) return false;
        int radius = Math.max(1, ImmortalStoragePlayerData.get(player).getRealmRadiusChunks());
        double min = -radius * 16.0D;
        double max = (radius + 1) * 16.0D - 0.01D;
        double x = net.minecraft.util.Mth.clamp(player.getX(), min, max);
        double z = net.minecraft.util.Mth.clamp(player.getZ(), min, max);
        if (x == player.getX() && z == player.getZ()) return false;
        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(realm, x, player.getY(), z, Set.of(),
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        player.fallDistance = 0.0F;
        long gameTime = realm.getGameTime();
        long nextNotice = player.getPersistentData().getLong(NBT_BOUNDARY_NOTICE_TICK);
        if (gameTime >= nextNotice) {
            player.displayClientMessage(Component.translatable("message.immortalstorage.realm_boundary_reached"), true);
            player.getPersistentData().putLong(NBT_BOUNDARY_NOTICE_TICK,
                    gameTime + BOUNDARY_NOTICE_COOLDOWN_TICKS);
        }
        return true;
    }

    private static void refreshVisibleBoundary(ServerLevel realm, UUID owner, int stage, int radiusChunks) {
        double size = stage >= 6 && stage < 9
                ? (Math.max(1, radiusChunks) * 2.0D + 1.0D) * 16.0D
                : UNBOUNDED_BORDER_SIZE;
        Double previous = APPLIED_BORDER_SIZE.put(owner, size);
        if (previous != null && Double.compare(previous, size) == 0) return;
        realm.getWorldBorder().setCenter(8.0D, 8.0D);
        realm.getWorldBorder().setSize(size);
        realm.getWorldBorder().setWarningBlocks(0);
    }

    public static void markModifiedChunk(ServerPlayer owner, BlockPos pos) {
        if (owner == null || pos == null) return;
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(owner);
        data.markRealmChunkModified(new net.minecraft.world.level.ChunkPos(pos));
    }

    public static int cachedForcedChunkCount(UUID owner) {
        Set<Long> forced = FORCED_CHUNK_CACHE.get(owner);
        return forced == null ? 0 : forced.size();
    }

    private static void forceChunkIfNeeded(ServerLevel realm, UUID owner, int chunkX, int chunkZ, boolean generate) {
        long packed = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        Set<Long> forced = FORCED_CHUNK_CACHE.computeIfAbsent(owner, ignored -> ConcurrentHashMap.newKeySet());
        if (!forced.add(packed)) return;
        realm.setChunkForced(chunkX, chunkZ, true);
        if (generate) {
            try {
                realm.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            } catch (Exception e) {
                LOG.debug("Pre-generate chunk failed for {},{}", chunkX, chunkZ, e);
            }
        }
    }

    /** Apply the stored scale only when the owner is inside their bound realm. */
    public static boolean refreshRealmTickRate(ServerPlayer player) {
        if (ADMIN_SUSPENDED_REALMS.contains(player.getUUID())) return false;
        if (!(player.level() instanceof ServerLevel realm)
                || !ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), player.getUUID())) {
            releaseRealmTickRate(player.server, player.getUUID());
            return false;
        }
        return activateRealmTickRate(player, realm);
    }

    private static boolean activateRealmTickRate(ServerPlayer player, ServerLevel realm) {
        if (!(realm instanceof PersonalRealmServerLevel personal)
                || !personal.isBoundTo(player.getUUID())) {
            return false;
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        int clampedPermille = RealmTimeScalePolicy.clampPermille(
                data.getStage(), data.getRealmTimeRatePermille());
        if (clampedPermille != data.getRealmTimeRatePermille()) {
            // Persist the safety clamp so old 16x+/invalid saves cannot keep
            // reapplying an unsafe value on every realm entry.
            data.setRealmTimeRatePermille(clampedPermille);
        }
        double requestedScale = clampedPermille / 1_000.0D;
        personal.activateTickScale(player.getUUID(), requestedScale);
        return true;
    }

    /**
     * Restore one realm to 1x and release every forced chunk ticket owned by
     * that player. Safe to call repeatedly during exit, logout and reconnect.
     */
    public static void releaseRealmTickRate(MinecraftServer server, UUID owner) {
        if (server == null || owner == null) return;
        ServerLevel realm = server.getLevel(ImmortalStorageDimensions.personalRealmKey(owner));
        if (realm instanceof PersonalRealmServerLevel personal && personal.isBoundTo(owner)) {
            personal.restoreNormalTickScale();
        }
        releaseForcedChunks(realm, owner);
    }

    /** Restore all dynamic realm rates before an orderly server shutdown. */
    public static void releaseAllRealmTickRates(MinecraftServer server) {
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level instanceof PersonalRealmServerLevel personal) {
                personal.restoreNormalTickScale();
                releaseForcedChunks(level, personal.ownerId());
            }
        }
        FORCED_CHUNK_CACHE.clear();
        ADMIN_SUSPENDED_REALMS.clear();
        APPLIED_BORDER_SIZE.clear();
    }

    public static void suspendRealmLoading(MinecraftServer server, UUID owner) {
        if (server == null || owner == null) return;
        ADMIN_SUSPENDED_REALMS.add(owner);
        releaseRealmTickRate(server, owner);
    }

    public static boolean resumeRealmLoading(ServerPlayer owner) {
        if (owner == null) return false;
        ADMIN_SUSPENDED_REALMS.remove(owner.getUUID());
        ServerLevel realm = owner.server.getLevel(ImmortalStorageDimensions.personalRealmKey(owner.getUUID()));
        if (realm == null) return true;
        if (ImmortalStorageDimensions.isPersonalRealmFor(owner.level().dimension(), owner.getUUID())) {
            ensureChunksForced(owner);
            return refreshRealmTickRate(owner);
        }
        forceChunkIfNeeded(realm, owner.getUUID(), CENTER_CHUNK_X, CENTER_CHUNK_Z, false);
        return true;
    }

    public static boolean isRealmLoadingSuspended(UUID owner) {
        return owner != null && ADMIN_SUSPENDED_REALMS.contains(owner);
    }

    /** Restore an unloading personal level without consulting any global rate. */
    public static void releaseUnloadingRealm(ServerLevel level) {
        if (level instanceof PersonalRealmServerLevel personal) {
            personal.restoreNormalTickScale();
            releaseForcedChunks(level, personal.ownerId());
            APPLIED_BORDER_SIZE.remove(personal.ownerId());
        }
    }

    private static void releaseForcedChunks(ServerLevel realm, UUID owner) {
        Set<Long> forced = FORCED_CHUNK_CACHE.remove(owner);
        if (realm == null || forced == null) return;
        for (long packed : forced) {
            realm.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed),
                    net.minecraft.world.level.ChunkPos.getZ(packed), false);
        }
    }

    public static int runtimeForcedRadiusChunks(ImmortalStoragePlayerData data) {
        return Math.min(MAX_RUNTIME_FORCED_RADIUS_CHUNKS, Math.max(1, data.getRealmRadiusChunks()));
    }

}
