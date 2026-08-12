package com.immortalstorage.immortalstorage.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Pins an entity to one exact point and suppresses collision displacement for a bounded duration. */
public final class AbsoluteRestraint {
    private static final String END = "ImmortalStorageRestraintEnd";
    private static final String X = "ImmortalStorageRestraintX";
    private static final String Y = "ImmortalStorageRestraintY";
    private static final String Z = "ImmortalStorageRestraintZ";
    private static final String OLD_NO_PHYSICS = "ImmortalStorageRestraintOldNoPhysics";

    public static void apply(Entity entity, int ticks) {
        var data = entity.getPersistentData();
        data.putLong(END, entity.level().getGameTime() + Math.max(1, ticks));
        data.putDouble(X, entity.getX()); data.putDouble(Y, entity.getY()); data.putDouble(Z, entity.getZ());
        data.putBoolean(OLD_NO_PHYSICS, entity.noPhysics);
        entity.noPhysics = true;
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void tick(Entity entity) {
        var data = entity.getPersistentData();
        if (!data.contains(END)) return;
        if (entity.level().getGameTime() >= data.getLongOr(END, 0L)) {
            entity.noPhysics = data.getBooleanOr(OLD_NO_PHYSICS, false);
            data.remove(END); data.remove(X); data.remove(Y); data.remove(Z); data.remove(OLD_NO_PHYSICS);
            entity.setDeltaMovement(Vec3.ZERO);
            return;
        }
        entity.noPhysics = true;
        entity.setPos(data.getDoubleOr(X, 0.0D), data.getDoubleOr(Y, 0.0D), data.getDoubleOr(Z, 0.0D));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
    }

    private AbsoluteRestraint() {}
}
