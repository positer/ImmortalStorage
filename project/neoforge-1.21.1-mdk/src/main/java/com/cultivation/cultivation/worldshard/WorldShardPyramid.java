package com.cultivation.cultivation.worldshard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class WorldShardPyramid {
    private WorldShardPyramid() {
    }

    public static Result scan(BlockPos origin, Function<BlockPos, BlockState> stateAt,
                              Collection<WorldShardMinerMode> modes) {
        List<WorldShardMinerMode> allModes = new ArrayList<>(modes);
        allModes.sort(Comparator.comparing(mode -> mode.id().toString()));
        List<WorldShardMinerMode> candidates = new ArrayList<>(allModes);
        int completedLevel = 0;
        WorldShardMinerMode selected = null;
        Block expectedBlock = null;

        for (int layer = 1; layer <= WorldShardMiningMath.MAX_LEVEL; layer++) {
            boolean anyActivationBlock = false;
            List<WorldShardMinerMode> nextCandidates = new ArrayList<>(candidates);
            for (int x = -layer; x <= layer; x++) {
                for (int z = -layer; z <= layer; z++) {
                    BlockState state = stateAt.apply(origin.offset(x, -layer, z));
                    anyActivationBlock |= allModes.stream().anyMatch(mode -> mode.activation().matches(state));
                    if (expectedBlock == null && layer == 1 && x == -1 && z == -1) {
                        expectedBlock = state.getBlock();
                    }
                    if (state.getBlock() != expectedBlock) {
                        nextCandidates.clear();
                        continue;
                    }
                    nextCandidates.removeIf(mode -> !mode.activation().matches(state));
                }
            }

            if (nextCandidates.isEmpty()) {
                if (completedLevel == 0 && anyActivationBlock) return Result.inactive();
                break;
            }
            candidates = nextCandidates;
            completedLevel = layer;
            selected = candidates.getFirst();
        }
        return completedLevel == 0 || selected == null
                ? Result.inactive()
                : new Result(completedLevel, Optional.of(selected));
    }

    public record Result(int level, Optional<WorldShardMinerMode> mode) {
        public Result {
            if (level < 0 || level > WorldShardMiningMath.MAX_LEVEL) {
                throw new IllegalArgumentException("level must be in [0, 4]");
            }
            if ((level == 0) != mode.isEmpty()) {
                throw new IllegalArgumentException("inactive result and mode must agree");
            }
        }

        public static Result inactive() {
            return new Result(0, Optional.empty());
        }
    }
}
