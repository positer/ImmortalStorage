package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.core.BlockPos;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/** Official 26.1 player interaction accessors used by migrated call sites. */
public final class CompatPlayer {
    private CompatPlayer() {
    }

    public static EquipmentSlot slotForHand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    public static boolean canInteractWithBlock(Player player, BlockPos pos, double distance) {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.isWithinBlockInteractionRange(pos, distance);
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return player.distanceToSqr(x, y, z) <= distance * distance;
    }

    public static boolean hasPermissions(Player player, int requiredLevel) {
        return player instanceof ServerPlayer serverPlayer
                && serverPlayer.createCommandSourceStack().permissions() instanceof LevelBasedPermissionSet level
                && level.level().id() >= requiredLevel;
    }
}
