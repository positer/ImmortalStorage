package com.immortalstorage.immortalstorage.compat.accessory;

import com.immortalstorage.immortalstorage.compat.CompatManager;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/** Keeps optional accessory APIs outside the always-loaded combat service. */
public final class AccessoryTalismanBridge {
    private static final Method CURIOS_QUERY = resolveCuriosQuery();

    private AccessoryTalismanBridge() {
    }

    public static boolean isEquipped(LivingEntity entity) {
        if (CURIOS_QUERY == null) return false;
        try {
            return Boolean.TRUE.equals(CURIOS_QUERY.invoke(null, entity));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Method resolveCuriosQuery() {
        if (!CompatManager.CURIOS_LOADED) return null;
        try {
            Class<?> bridge = Class.forName(
                    "com.immortalstorage.immortalstorage.compat.curios.CuriosTalismanCompat",
                    false, AccessoryTalismanBridge.class.getClassLoader());
            return bridge.getMethod("isEquipped", LivingEntity.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
