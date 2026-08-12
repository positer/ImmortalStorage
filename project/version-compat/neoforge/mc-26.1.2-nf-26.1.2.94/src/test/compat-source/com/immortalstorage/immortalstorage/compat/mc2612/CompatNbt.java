package com.immortalstorage.immortalstorage.compat.mc2612;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

/** Official 26.1 UUID codec bridge for legacy CompoundTag call sites. */
public final class CompatNbt {
    private CompatNbt() {
    }

    public static boolean hasUuid(CompoundTag tag, String key) {
        return tag != null && tag.getIntArray(key).map(value -> value.length == 4).orElse(false);
    }

    public static UUID getUuid(CompoundTag tag, String key) {
        if (!hasUuid(tag, key)) {
            throw new IllegalArgumentException("Missing UUID tag: " + key);
        }
        return UUIDUtil.uuidFromIntArray(tag.getIntArray(key).orElseThrow());
    }

    public static void putUuid(CompoundTag tag, String key, UUID value) {
        if (value != null) tag.putIntArray(key, UUIDUtil.uuidToIntArray(value));
    }

    public static boolean saveEntity(net.minecraft.world.entity.Entity entity, CompoundTag target) {
        if (entity == null || target == null) return false;
        net.minecraft.world.level.storage.TagValueOutput output =
                net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                        net.minecraft.util.ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(output);
        target.merge(output.buildResult());
        return true;
    }
}
