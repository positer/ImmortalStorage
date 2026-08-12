package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Official 26.1 level-to-server access without relying on removed fields. */
public final class CompatLevel {
    private CompatLevel() {
    }

    public static MinecraftServer server(Level level) {
        return level instanceof ServerLevel serverLevel ? serverLevel.getServer() : null;
    }
}
