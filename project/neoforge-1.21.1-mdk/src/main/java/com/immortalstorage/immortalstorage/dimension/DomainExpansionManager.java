package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain Expansion ("领域展开"): a bounded volume centered on the player's
 * lower-body block is temporarily swapped with the owner's personal-realm
 * center (0, 56, 0).  It lets a not-yet-ascended cultivator (stage 3-5) use
 * the realm build-space without entering the realm dimension.
 *
 * The swap must not fire block updates and must preserve block-entity data
 * byte-for-byte.  Blocks are therefore written through
 * {@code Level.setBlock(pos, state, UPDATE_NONE | UPDATE_KNOWN_SHAPE)} (no
 * neighbour updates, no client packets) and block entities are transferred by
 * an equivalent serialization round-trip; a single client-only
 * {@code sendBlockUpdated} pass runs afterwards so the client sees the result
 * without any logic side effects.
 *
 * Mutual exclusion: "player inside the realm" and "player's domain expanded"
 * must never coexist.  {@link #collapse} is therefore invoked before any realm
 * entry via {@link RealmHelper#enterRealm}.
 */
public final class DomainExpansionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ImmortalStorageMod.MODID + ".domain");

    /** Realm center the 0.1.0 spec designates (0, 56, 0). */
    public static final int REALM_CENTER_X = 0;
    public static final int REALM_CENTER_Y = XianqiaoRealmChunkGenerator.TOP_Y; // 56
    public static final int REALM_CENTER_Z = 0;

    private static final Map<UUID, ActiveDomain> ACTIVE = new ConcurrentHashMap<>();
    private static final int FLAGS = Block.UPDATE_NONE | Block.UPDATE_KNOWN_SHAPE;

    private DomainExpansionManager() {}

    /** Edge length of the cube for a cultivation stage: 3 / 7 / 13. */
    public static int edgeForStage(int stage) {
        if (stage <= 3) return 3;
        if (stage == 4) return 7;
        return 13;
    }

    public static boolean isExpanded(UUID playerId) {
        return playerId != null && ACTIVE.containsKey(playerId);
    }

    /** Toggle the domain expansion for the triggering player (server side). */
    public static void toggle(ServerPlayer player) {
        if (player == null) return;
        if (RealmHelper.isInOwnRealm(player)) {
            player.sendSystemMessage(Component.literal("Domain Expansion cannot be used inside your realm."));
            return;
        }
        ActiveDomain existing = ACTIVE.get(player.getUUID());
        if (existing != null) {
            collapse(existing, player);
        } else {
            expand(player);
        }
    }

    /** Collapse any expanded domain for this player.  Safe idempotent no-op. */
    public static void collapseFor(ServerPlayer player) {
        if (player == null) return;
        ActiveDomain domain = ACTIVE.get(player.getUUID());
        if (domain != null) collapse(domain, player);
    }

    private static void expand(ServerPlayer player) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        int stage = data.getStage();
        if (stage < 3) {
            player.sendSystemMessage(Component.literal("Domain Expansion unlocks at stage 3."));
            return;
        }
        ServerLevel world = player.serverLevel();
        if (world == null || world.isClientSide) return;

        UUID realmId = RealmHelper.realmId(player);
        ServerLevel realm = PersonalRealmLevelFactory.getOrCreate(world.getServer(), realmId);
        if (realm == null) {
            player.sendSystemMessage(Component.literal("Your realm could not be prepared."));
            return;
        }

        int edge = edgeForStage(stage);
        int radius = (edge - 1) / 2;
        BlockPos worldCenter = player.blockPosition();
        BlockPos realmCenter = new BlockPos(REALM_CENTER_X, REALM_CENTER_Y, REALM_CENTER_Z);

        if (!boundsValid(worldCenter, radius) || !boundsValid(realmCenter, radius)) {
            player.sendSystemMessage(Component.literal("Domain Expansion area is outside the buildable world."));
            return;
        }

        forceLoad(realm, realmCenter);

        List<SavedCell> snapshot = capture(world, worldCenter, radius);
        BlockState barrier = com.immortalstorage.immortalstorage.block.ModBlocks.WORLD_BARRIER.get()
                .defaultBlockState();
        swap(realm, realmCenter, world, worldCenter, radius, barrier);

        ActiveDomain domain = new ActiveDomain(world.getServer(), player.getUUID(), realmId,
                world.dimension(), worldCenter, realmCenter, radius, snapshot, stage);
        ACTIVE.put(player.getUUID(), domain);

        player.sendSystemMessage(Component.literal("Domain Expansion opened (stage " + stage
                + ", " + edge + "×" + edge + "×" + edge + ")."));
    }

    private static void collapse(ActiveDomain domain, ServerPlayer trigger) {
        if (domain == null) return;
        ServerLevel world = domain.server().getLevel(domain.worldKey());
        ServerLevel realm = domain.server().getLevel(
                ImmortalStorageDimensions.personalRealmKey(domain.realmId()));
        if (world == null || realm == null) {
            // Cannot restore safely yet; keep the entry for a future retry.
            return;
        }
        forceLoad(realm, domain.realmCenter());

        swap(world, domain.worldCenter(), realm, domain.realmCenter(), domain.radius(),
                Blocks.AIR.defaultBlockState());
        restore(world, domain.snapshot());

        ACTIVE.remove(domain.owner());
        if (trigger != null) {
            trigger.sendSystemMessage(Component.literal("Domain Expansion closed."));
        }
        LOG.info("Collapsed domain expansion for {}", domain.owner());
    }

    private static boolean boundsValid(BlockPos center, int radius) {
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;
        return minY >= -64 && maxY < 320;
    }

    private static void forceLoad(ServerLevel level, BlockPos center) {
        try {
            level.getChunk(center.getX() >> 4, center.getZ() >> 4, ChunkStatus.FULL, true);
        } catch (Exception e) {
            LOG.debug("Force-load chunk for domain failed at {}", center, e);
        }
    }

    /** Serializes the volume in {@code level} so it can be restored exactly. */
    private static List<SavedCell> capture(ServerLevel level, BlockPos center, int radius) {
        List<SavedCell> cells = new ArrayList<>();
        forEach(center, radius, pos -> {
            BlockState state = level.getBlockState(pos);
            CompoundTag entity = null;
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                entity = be.saveWithFullMetadata(level.registryAccess());
            }
            cells.add(new SavedCell(pos, state, entity));
        });
        return cells;
    }

    /**
     * Move the {center,radius} volume from {@code from} into {@code to}.  The
     * source is left filled with {@code fillState} (world barrier while the
     * realm is being used by an expansion, air on the way back).  No block
     * updates are fired and no block-entity data changes; a client-only visual
     * notification is emitted at the end.
     */
    private static void swap(ServerLevel from, BlockPos fromCenter,
                             ServerLevel to, BlockPos toCenter, int radius,
                             BlockState fillState) {
        HolderLookup.Provider toRegistries = to.registryAccess();

        List<BlockPos> order = new ArrayList<>();
        forEach(fromCenter, radius, order::add);

        // Capture source block entities first (they hold the data to move).
        Map<BlockPos, CompoundTag> movedEntities = new java.util.HashMap<>();
        for (BlockPos pos : order) {
            BlockEntity be = from.getBlockEntity(pos);
            if (be != null) {
                movedEntities.put(pos, be.saveWithFullMetadata(from.registryAccess()));
            }
        }

        int offsetX = toCenter.getX() - fromCenter.getX();
        int offsetY = toCenter.getY() - fromCenter.getY();
        int offsetZ = toCenter.getZ() - fromCenter.getZ();

        for (BlockPos pos : order) {
            BlockPos target = new BlockPos(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);
            BlockState state = from.getBlockState(pos);

            from.removeBlockEntity(pos);
            to.removeBlockEntity(target);
            from.setBlock(pos, fillState, FLAGS);
            to.setBlock(target, state, FLAGS);

            CompoundTag beTag = movedEntities.get(pos);
            if (beTag != null) {
                BlockEntity restored = BlockEntity.loadStatic(target, state, beTag, toRegistries);
                if (restored != null) {
                    to.setBlockEntity(restored);
                }
            }
        }

        for (BlockPos pos : order) {
            BlockPos target = new BlockPos(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);
            from.sendBlockUpdated(pos, fillState, fillState, Block.UPDATE_CLIENTS);
            to.sendBlockUpdated(target, fillState, to.getBlockState(target), Block.UPDATE_CLIENTS);
        }
    }

    private static void restore(ServerLevel world, List<SavedCell> snapshot) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (SavedCell cell : snapshot) {
            world.removeBlockEntity(cell.pos());
            world.setBlock(cell.pos(), cell.state(), FLAGS);
            if (cell.entityTag() != null) {
                BlockEntity be = BlockEntity.loadStatic(cell.pos(), cell.state(), cell.entityTag(),
                        world.registryAccess());
                if (be != null) {
                    world.setBlockEntity(be);
                }
            }
            world.sendBlockUpdated(cell.pos(), air, cell.state(), Block.UPDATE_CLIENTS);
        }
    }

    private static void forEach(BlockPos center, int radius, java.util.function.Consumer<BlockPos> action) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    action.accept(center.offset(dx, dy, dz));
                }
            }
        }
    }

    /** One saved block: its absolute overworld position, state and optional entity NBT. */
    private record SavedCell(BlockPos pos, BlockState state, CompoundTag entityTag) {}

    /** A live expansion held in memory for one player. */
    private record ActiveDomain(net.minecraft.server.MinecraftServer server, UUID owner, UUID realmId,
                                ResourceKey<Level> worldKey, BlockPos worldCenter,
                                BlockPos realmCenter, int radius, List<SavedCell> snapshot, int stage) {}
}
