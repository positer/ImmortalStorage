package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

/** Optional block tags avoid linking external accelerator classes or replacing their ordinary tick. */
public final class RealmSafeBlockTicker<T extends BlockEntity> implements BlockEntityTicker<T> {
    private static final TagKey<Block> DENY = TagKey.create(Registries.BLOCK, Identifier.parse("immortalstorage:time_acceleration_disallowed"));
    private static final TagKey<Block> COMMON_DENY = TagKey.create(Registries.BLOCK, Identifier.parse("c:tick_acceleration_disallowed"));
    private final BlockEntityTicker<T> original;
    private RealmSafeBlockTicker(BlockEntityTicker<T> original) { this.original = original; }
    public static <T extends BlockEntity> BlockEntityTicker<T> wrap(BlockEntityTicker<T> ticker) {
        return ticker == null || ticker instanceof RealmSafeBlockTicker<?> ? ticker : new RealmSafeBlockTicker<>(ticker);
    }
    public static void execute(Level level, BlockEntity entity, Runnable work) {
        if (!RealmTickSafety.scoped(level)) { work.run(); return; }
        var state = entity.getBlockState();
        RealmTickSafety.execute(level, entity, state.is(DENY) || state.is(COMMON_DENY), work,
                failure -> ImmortalStorageMod.LOG.error("[Realm] Disabled additional ticks for {} at {}; its normal tick remains enabled",
                        entity.getType(), entity.getBlockPos(), failure));
    }
    @Override public void tick(Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, T entity) {
        execute(level, entity, () -> original.tick(level, pos, state, entity));
    }
}
