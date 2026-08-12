package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

/** Official 26.1 permission-level bridge for migrated command call sites. */
public final class CompatCommands {
    private CompatCommands() {
    }

    public static boolean hasPermission(CommandSourceStack source, int requiredLevel) {
        return source.permissions() instanceof LevelBasedPermissionSet level
                && level.level().id() >= requiredLevel;
    }
}
