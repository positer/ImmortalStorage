package com.cultivation.cultivation.block.entity;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** First-claim-wins UUID binding used by owner-scoped block capabilities. */
final class OwnerBinding {
    private UUID owner;

    public @Nullable UUID owner() {
        return owner;
    }

    /** Claims an unowned binding or validates the existing owner without replacing it. */
    public boolean claim(@Nullable UUID candidate) {
        if (candidate == null) return false;
        if (owner == null) owner = candidate;
        return owner.equals(candidate);
    }

    public boolean isOwner(@Nullable UUID candidate) {
        return candidate != null && candidate.equals(owner);
    }

    public void save(CompoundTag tag, String key) {
        if (owner != null) tag.putUUID(key, owner);
    }

    public void load(CompoundTag tag, String key) {
        owner = tag.hasUUID(key) ? tag.getUUID(key) : null;
    }
}
