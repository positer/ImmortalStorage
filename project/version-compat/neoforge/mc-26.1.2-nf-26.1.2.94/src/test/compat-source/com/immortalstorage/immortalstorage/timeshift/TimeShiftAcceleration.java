package com.immortalstorage.immortalstorage.timeshift;

import java.util.Map;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.dimension.RealmSafeBlockTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * JDT-style per-block acceleration: after every block-entity pass, each shifted
 * block receives {@code effective - 1} extra ticks because the vanilla pass has
 * already ticked it once.
 *
 * <p>This runs from the tail of {@code Level#tickBlockEntities()}.  The personal
 * realm accelerates itself by invoking {@code tickBlockEntities()} once per
 * extra pass, so realm speed and a staff tier multiply naturally instead of
 * replacing each other.  Unlike JDT's entity-driven wand no entity is spawned,
 * which also keeps the record valid for unloaded chunks.</p>
 */
public final class TimeShiftAcceleration {
    /** Vanilla random-tick expectation: one pass over a 16x16x16 section has ~1/1365 chance per block. */
    public static final int RANDOM_TICK_DENOMINATOR = 1365;

    private static final TagKey<Block> DENY = TagKey.create(Registries.BLOCK,
            Identifier.parse("immortalstorage:time_acceleration_disallowed"));
    private static final TagKey<Block> COMMON_DENY = TagKey.create(Registries.BLOCK,
            Identifier.parse("c:tick_acceleration_disallowed"));

    /** Diagnostic counter read by the GameTest; gameplay never reads it. */
    public static volatile long debugExtraTicks;
    /** Extra ticks whose ticker body actually ran (a wrapped ticker can still be skipped). */
    public static volatile long debugExecutedTicks;

    private static long lastFailureReportGameTime = Long.MIN_VALUE;
    private static BlockPos lastFailureReportPos = BlockPos.ZERO;

    /** Called from the mixin at the tail of every {@code tickBlockEntities} pass. */
    public static void tickBlockEntities(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        TimeShiftStore store = TimeShiftStore.get(serverLevel);
        if (store.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (store.expireBoosts(now)) TimeShiftService.broadcast(serverLevel);
        int boostMultiplier = TimeShiftTiers.boostMultiplier(ImmortalStorageConfig.TIME_SHIFT_BOOST_LOG2.get());
        long extraTicksBefore = debugExtraTicks;
        for (Map.Entry<BlockPos, TimeShiftEntry> entry : store.snapshot()) {
            BlockPos pos = entry.getKey();
            // The vanilla pass already filtered this block with shouldTickBlocksAt
            // immediately before this hook ran, so repeating the same test can only
            // remove ticks vanilla granted (JDT does not apply it either). "Loaded" is
            // the useful guard here: an unloaded chunk must not resolve a block entity.
            if (!serverLevel.isLoaded(pos)) continue;
            TimeShiftEntry record = entry.getValue();
            int multiplier = record.effectiveMultiplier(now, boostMultiplier);
            int extra = TimeShiftTiers.extraTicks(multiplier);
            if (extra <= 0) continue;
            accelerate(serverLevel, pos, multiplier);
        }
        // Throttled evidence line: proves the hook actually runs and how many extra
        // ticks this pass issued. One line per dimension per 10 seconds.
        if (serverLevel.getGameTime() % 200L == 0L) {
            ImmortalStorageMod.LOG.info("[TimeShift] {} record(s) in {} -> {} extra tick(s) this pass",
                    store.size(), serverLevel.dimension().identifier(), debugExtraTicks - extraTicksBefore);
        }
    }

    /**
     * Whether the staff may record a block at all.
     *
     * <p>2026-10-20 user ruling: while the staff is held, acceleration takes
     * priority over the block's own interaction, and the staff must not add a
     * "can this block tick" filter on top of the acceleration blacklist — every
     * block the blacklist does not forbid is accepted. A block with nothing to
     * tick (plain stone, a chest) still receives its box and its multiplier
     * label; the tick loop simply has no work for it, which is a physical fact
     * about that block rather than a refusal by the staff.</p>
     */
    public static boolean isAccelerable(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (state.isAir()) return false;
        return !state.is(DENY) && !state.is(COMMON_DENY);
    }

    private static void accelerate(ServerLevel level, BlockPos pos, int multiplier) {
        int extra = TimeShiftTiers.extraTicks(multiplier);
        if (extra <= 0) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            accelerateRandomTicks(level, pos, state, extra);
            return;
        }
        if (state.is(DENY) || state.is(COMMON_DENY)) return;
        BlockEntityTicker<BlockEntity> ticker = TimeShiftTickers.tickerFor(level, state, blockEntity);
        if (ticker == null) return;
        for (int index = 0; index < extra; index++) {
            if (blockEntity.isRemoved()) return;
            BlockState live = level.getBlockState(pos);
            if (live != state) return;
            try {
                debugExtraTicks++;
                runTicker(level, pos, live, blockEntity, ticker);
            } catch (RuntimeException | StackOverflowError failure) {
                reportFailure(level, pos, blockEntity, failure);
                return;
            }
        }
    }

    /**
     * Runs one extra tick without nesting the realm safety guard twice.
     *
     * <p>A ticker obtained through the block factory is already wrapped in
     * {@link RealmSafeBlockTicker} (the factory mixin does that), and that wrapper
     * enters {@code RealmTickSafety} before delegating. Wrapping it a second time
     * here made the inner call find the target already in the frame's running set,
     * so inside a personal realm — where that set is active — the whole extra tick
     * was silently dropped. That is exactly why the staff multiplier disappeared
     * whenever the realm's own acceleration was running. Only a bare ticker, which
     * has no guard of its own, is wrapped here.</p>
     */
    private static void runTicker(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity,
                                  BlockEntityTicker<BlockEntity> ticker) {
        if (ticker instanceof RealmSafeBlockTicker<?>) {
            ticker.tick(level, pos, state, blockEntity);
            debugExecutedTicks++;
            return;
        }
        RealmSafeBlockTicker.execute(level, blockEntity, () -> ticker.tick(level, pos, state, blockEntity));
    }

    private static void accelerateRandomTicks(ServerLevel level, BlockPos pos, BlockState state, int extra) {
        if (!state.isRandomlyTicking()) return;
        if (!ImmortalStorageConfig.TIME_SHIFT_RANDOM_TICK_BLOCKS.get()) return;
        for (int index = 0; index < extra; index++) {
            if (level.getRandom().nextInt(RANDOM_TICK_DENOMINATOR) == 0) {
                try {
                    state.randomTick(level, pos, level.getRandom());
                } catch (RuntimeException | StackOverflowError failure) {
                    reportFailure(level, pos, null, failure);
                    return;
                }
            }
        }
    }

    /** Logs the first failure and then at most once per ten seconds, so a broken block cannot flood the log 1024 times a tick. */
    private static void reportFailure(ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, Throwable failure) {
        long now = level.getGameTime();
        if (now - lastFailureReportGameTime < 200 && lastFailureReportPos.equals(pos)) return;
        lastFailureReportGameTime = now;
        lastFailureReportPos = pos.immutable();
        ImmortalStorageMod.LOG.error("[TimeShift] Disabled extra ticks for {} at {} after a failure; the block keeps its normal tick",
                blockEntity == null ? level.getBlockState(pos) : blockEntity.getType(), pos, failure);
    }

    private TimeShiftAcceleration() {}
}
