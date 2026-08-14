package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
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

    /**
     * Resolve the durable realm id stored with the player's ImmortalStorage
     * data. Pre-0.0.9 saves are migrated once: a saved personal-realm respawn
     * or exit dimension wins, otherwise the legacy session UUID is retained.
     */
    public static UUID realmId(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        UUID established = data.getPersonalRealmId();
        if (established != null) return established;
        UUID legacy = ImmortalStorageDimensions.personalRealmOwner((player.getRespawnConfig() == null ? null : player.getRespawnConfig().respawnData().dimension())).orElse(null);
        if (legacy == null && data.hasExitPosition()) {
            Identifier exit = Identifier.tryParse(data.getLastExitDim());
            if (exit != null) {
                legacy = ImmortalStorageDimensions.personalRealmOwner(
                        ResourceKey.create(Registries.DIMENSION, exit)).orElse(null);
            }
        }
        if (legacy == null) legacy = legacyBoundItemOwner(player, data);
        UUID chosen = data.bindPersonalRealmOnce(legacy == null ? player.getUUID() : legacy);
        LOG.info("Bound player {} ({}) to persistent Xianqiao realm {}{}",
                player.getGameProfile().name(), player.getUUID(), chosen,
                chosen.equals(player.getUUID()) ? "" : " via legacy migration");
        return chosen;
    }

    private static UUID legacyBoundItemOwner(ServerPlayer player, ImmortalStoragePlayerData data) {
        Set<UUID> candidates = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            collectLegacyOwner(player.getInventory().getItem(slot), candidates);
        }
        for (net.minecraft.world.item.ItemStack stack : data.getXianqiaoStorageItems()) {
            collectLegacyOwner(stack, candidates);
        }
        if (candidates.size() == 1) return candidates.iterator().next();
        if (candidates.size() > 1) {
            LOG.warn("Cannot infer one legacy owner id for {}: bound items contain {}",
                    player.getGameProfile().name(), candidates);
        }
        return null;
    }

    private static void collectLegacyOwner(net.minecraft.world.item.ItemStack stack, Set<UUID> candidates) {
        if (stack == null || stack.isEmpty()) return;
        com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem.owner(stack).ifPresent(candidates::add);
        com.immortalstorage.immortalstorage.item.custom.SubstitutePuppetItem.owner(stack).ifPresent(candidates::add);
        com.immortalstorage.immortalstorage.item.custom.XianqiaoExchangeCellItem.owner(stack).ifPresent(candidates::add);
        com.immortalstorage.immortalstorage.item.custom.XianqiaoRsExchangeDiskItem.owner(stack).ifPresent(candidates::add);
    }

    public static boolean isInOwnRealm(ServerPlayer player) {
        return player != null && ImmortalStorageDimensions.isPersonalRealmFor(
                player.level().dimension(), realmId(player));
    }

    /** Find the current session player whose persisted data owns this realm. */
    public static ServerPlayer onlinePlayerForRealm(MinecraftServer server, UUID realmId) {
        if (server == null || realmId == null) return null;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (realmId(candidate).equals(realmId)) return candidate;
        }
        return null;
    }

    /** Restore a saved personal-realm respawn target before vanilla needs to resolve it. */
    public static void ensureRespawnRealmRegistered(ServerPlayer player) {
        if (player == null || com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()) == null) return;
        ResourceKey<Level> respawnDimension = (player.getRespawnConfig() == null ? null : player.getRespawnConfig().respawnData().dimension());
        java.util.Optional<UUID> owner = ImmortalStorageDimensions.personalRealmOwner(respawnDimension);
        if (owner.isPresent() && owner.get().equals(realmId(player))) {
            PersonalRealmLevelFactory.getOrCreate(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()), owner.get());
        }
    }

    public static ServerLevel resolveOwnedPersonalRealm(ServerPlayer player, ResourceKey<Level> target) {
        if (player == null || com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()) == null || target == null) return null;
        java.util.Optional<UUID> owner = ImmortalStorageDimensions.personalRealmOwner(target);
        if (owner.isEmpty() || !owner.get().equals(realmId(player))) return null;
        return PersonalRealmLevelFactory.getOrCreate(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()), owner.get());
    }

    /** Try to enter the realm.  Returns true on success. */
    public static boolean enterRealm(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (data.getStage() < 6) {
            player.sendSystemMessage(Component.literal("You are not ready to enter the realm (need stage 6+)."));
            return false;
        }
        // Mutual exclusion: a player whose domain is expanded must first collapse
        // it before entering the realm, so both states can never coexist.
        DomainExpansionManager.collapseFor(player);
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        UUID realmId = realmId(player);
        ServerLevel realm = PersonalRealmLevelFactory.getOrCreate(server, realmId);
        if (realm == null) {
            player.sendSystemMessage(Component.literal("Your personal realm could not be registered; check the server log."));
            return false;
        }
        if (!ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), realmId)) {
            player.sendSystemMessage(Component.literal("Refusing to enter a realm that is not bound to your player id."));
            return false;
        }
        if (!activateRealmTickRate(player, realm)) {
            player.sendSystemMessage(Component.literal("Your personal realm does not support an isolated tick rate."));
            return false;
        }
        // Save the "real" position so we can return.
        if (!isInOwnRealm(player)) {
            data.markExitPosition(
                    player.getX(), player.getY(), player.getZ(),
                    player.level().dimension().identifier().toString());
        }

        forceChunkIfNeeded(realm, realmId, CENTER_CHUNK_X, CENTER_CHUNK_Z, true);

        // Teleport to the realm center. grass tops out at y=55 (XianqiaoRealmChunkGenerator.TOP_Y);
        // the (0, 56, 0) center requested by the 0.1.0 spec refers to the air block above the grass.
        double centerY = XianqiaoRealmChunkGenerator.TOP_Y;
        Set<Relative> rel = Relative.ROTATION;
        player.teleportTo(realm, 0.5, centerY, 0.5,
                rel, player.getYRot(), player.getXRot(), false);
        player.sendSystemMessage(Component.literal("You have entered your Xianqiao (Realm)."));
        return true;
    }

    /**
     * Teleport the player to the fixed realm center (0, 56, 0).  Used by the
     * in-realm shift+V bind.  No-op unless the player is already inside their
     * own realm.
     */
    public static boolean teleportToRealmCenter(ServerPlayer player) {
        if (player == null || !isInOwnRealm(player)) return false;
        ServerLevel realm = (ServerLevel) player.level();
        double centerY = XianqiaoRealmChunkGenerator.TOP_Y;
        player.teleportTo(realm, 0.5, centerY, 0.5,
                Relative.ROTATION, player.getYRot(), player.getXRot(), false);
        return true;
    }

    /** Try to leave the realm and return to the previous real-world position. */
    public static boolean exitRealm(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!isInOwnRealm(player)) {
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
            Identifier dimId = Identifier.tryParse(data.getLastExitDim());
            if (dimId != null) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
                target = server.getLevel(key);
            }
        }
        if (target == null) {
            target = server.overworld();
        }

        // Reconcile the tick rate and realm force-load before leaving; the
        // dimension-change event re-applies it from the destination (the realm
        // stays loaded at 1x as well as accelerated).
        refreshRealmTickRate(player);

        BlockPos fallback = target.getRespawnData().pos();
        double x = data.hasExitPosition() ? data.getLastExitX() : fallback.getX() + 0.5;
        double y = data.hasExitPosition() ? data.getLastExitY() : fallback.getY() + 1.0;
        double z = data.hasExitPosition() ? data.getLastExitZ() : fallback.getZ() + 0.5;
        Set<Relative> rel = Relative.ROTATION;
        player.teleportTo(target, x, y, z, rel, player.getYRot(), player.getXRot(), false);
        data.clearExitPosition();
        player.sendSystemMessage(Component.literal("You have left your Xianqiao (Realm)."));
        return true;
    }

    /**
     * On server tick, force-load chunks inside the player's personal realm at the
     * current stage.  No-op if player is not in the realm.
     */
    public static void ensureChunksForced(ServerPlayer player) {
        UUID realmId = realmId(player);
        if (ADMIN_SUSPENDED_REALMS.contains(realmId)) return;
        if (!(player.level() instanceof ServerLevel realm)) return;
        if (!ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), realmId)) return;
        forceRealmChunksForced(player, realm);
    }

    /**
     * Force-load the center chunk plus every player-modified chunk for {@code realm}.
     * This is the authoritative "keep the realm ticking" set: only blocks the
     * player has actually changed are held loaded, never the whole dimension.
     */
    private static void forceRealmChunksForced(ServerPlayer player, ServerLevel realm) {
        UUID realmId = realmId(player);
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        forceChunkIfNeeded(realm, realmId, CENTER_CHUNK_X, CENTER_CHUNK_Z, false);
        for (long packed : data.getModifiedRealmChunks()) {
            forceChunkIfNeeded(realm, realmId,
                    net.minecraft.world.level.ChunkPos.getX(packed),
                    net.minecraft.world.level.ChunkPos.getZ(packed), false);
        }
    }

    /** Draw and enforce the finite plot only while its owner is inside this realm. */
    public static boolean enforcePlayerBoundary(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel realm)
                || !ImmortalStorageDimensions.isPersonalRealmFor(realm.dimension(), realmId(player))) return false;
        int stage = ImmortalStoragePlayerData.get(player).getStage();
        refreshVisibleBoundary(realm, realmId(player), stage,
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
                player.getYRot(), player.getXRot(), false);
        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        player.fallDistance = 0.0F;
        long gameTime = realm.getGameTime();
        long nextNotice = player.getPersistentData().getLongOr(NBT_BOUNDARY_NOTICE_TICK, 0L);
        if (gameTime >= nextNotice) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.realm_boundary_reached"), true);
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
        data.markRealmChunkModified(net.minecraft.world.level.ChunkPos.containing(pos));
    }

    public static int cachedForcedChunkCount(UUID owner) {
        Set<Long> forced = FORCED_CHUNK_CACHE.get(owner);
        return forced == null ? 0 : forced.size();
    }

    private static void forceChunkIfNeeded(ServerLevel realm, UUID owner, int chunkX, int chunkZ, boolean generate) {
        long packed = net.minecraft.world.level.ChunkPos.pack(chunkX, chunkZ);
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

    /**
     * Apply the stored scale and keep the owner's realm force-loaded.  Inside
     * the realm this behaves as before.  From another dimension it reuses the
     * center-chunk-plus-modified-chunks set (not the whole dimension) and stays
     * loaded at 1x as well as accelerated, so a time-flow adjustment made from
     * anywhere keeps affecting the realm.
     */
    public static boolean refreshRealmTickRate(ServerPlayer player) {
        UUID realmId = realmId(player);
        if (ADMIN_SUSPENDED_REALMS.contains(realmId)) return false;
        ServerLevel current = player.level() instanceof ServerLevel level ? level : null;
        boolean inRealm = current != null
                && ImmortalStorageDimensions.isPersonalRealmFor(current.dimension(), realmId);
        ServerLevel realm = inRealm ? current
                : PersonalRealmLevelFactory.getOrCreate(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()), realmId);
        if (!(realm instanceof PersonalRealmServerLevel personal) || !personal.isBoundTo(realmId)) {
            return false;
        }
        if (!inRealm) {
            // Hold the realm loaded from another dimension using the same
            // center-chunk-plus-modified-chunks set as ensureChunksForced.
            forceRealmChunksForced(player, realm);
        }
        boolean activated = activateRealmTickRate(player, realm);
        ImmortalStorageMod.LOG.info("[Realm] refreshTickRate inRealm={} permille={} activated={}",
                inRealm, ImmortalStoragePlayerData.get(player).getRealmTimeRatePermille(), activated);
        return activated;
    }

    /** Apply the owner's persisted day/weather selection immediately when its realm is loaded. */
    public static boolean refreshRealmEnvironment(ServerPlayer player) {
        if (player == null || com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()) == null) return false;
        UUID realmId = realmId(player);
        ServerLevel realm = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).getLevel(ImmortalStorageDimensions.personalRealmKey(realmId));
        if (!(realm instanceof PersonalRealmServerLevel personal) || !personal.isBoundTo(realmId)) {
            return false;
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        personal.refreshEnvironmentLock(realmId, data.isRealmDaytime(), data.getRealmWeatherMode());
        return true;
    }

    private static boolean activateRealmTickRate(ServerPlayer player, ServerLevel realm) {
        if (!(realm instanceof PersonalRealmServerLevel personal)
                || !personal.isBoundTo(realmId(player))) {
            return false;
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        int clampedPermille = data.isTribulationActive() ? RealmTimeScalePolicy.NORMAL_PERMILLE
                : RealmTimeScalePolicy.clampPermille(
                data.getStage(), data.getRealmTimeRatePermille());
        if (clampedPermille != data.getRealmTimeRatePermille()) {
            // Persist the safety clamp so old 16x+/invalid saves cannot keep
            // reapplying an unsafe value on every realm entry.
            data.setRealmTimeRatePermille(clampedPermille);
        }
        double requestedScale = clampedPermille / 1_000.0D;
        personal.activateTickScale(realmId(player), requestedScale);
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
        UUID realmId = realmId(owner);
        ADMIN_SUSPENDED_REALMS.remove(realmId);
        ServerLevel realm = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(owner.level()).getLevel(ImmortalStorageDimensions.personalRealmKey(realmId));
        if (realm == null) return true;
        if (isInOwnRealm(owner)) {
            ensureChunksForced(owner);
            return refreshRealmTickRate(owner);
        }
        forceChunkIfNeeded(realm, realmId, CENTER_CHUNK_X, CENTER_CHUNK_Z, false);
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
