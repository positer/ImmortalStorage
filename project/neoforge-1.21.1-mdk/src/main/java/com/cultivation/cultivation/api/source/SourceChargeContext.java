package com.cultivation.cultivation.api.source;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.UUID;

/** Server-owned identity passed to registered charge providers. */
public record SourceChargeContext(ServerLevel level, BlockPos sourcePos, UUID owner) {
    public SourceChargeContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(sourcePos, "sourcePos");
        Objects.requireNonNull(owner, "owner");
        sourcePos = sourcePos.immutable();
    }
}
