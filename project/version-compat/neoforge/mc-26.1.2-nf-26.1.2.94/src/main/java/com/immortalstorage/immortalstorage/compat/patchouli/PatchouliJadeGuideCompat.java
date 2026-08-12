package com.immortalstorage.immortalstorage.compat.patchouli;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * 26.1-safe handbook bridge. Patchouli is optional on this target because no
 * target artifact was audited; the native book resources remain available to
 * any installed handbook viewer, while an absent or changed viewer cannot
 * break the core item use path.
 */
public final class PatchouliJadeGuideCompat {
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(
            ImmortalStorageMod.MODID, "jade_guide");

    public static void open(ServerPlayer player) {
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI", false,
                    PatchouliJadeGuideCompat.class.getClassLoader());
            Object api = apiClass.getMethod("get").invoke(null);
            for (Method method : apiClass.getMethods()) {
                if (!method.getName().equals("openBookGUI") || method.getParameterCount() != 2) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(player)
                        || !method.getParameterTypes()[1].isInstance(BOOK)) {
                    continue;
                }
                method.invoke(api, player, BOOK);
                return;
            }
            ImmortalStorageMod.LOG.warn("[Compat/Patchouli] No 26.1-compatible openBookGUI signature found");
        } catch (ReflectiveOperationException | LinkageError exception) {
            ImmortalStorageMod.LOG.debug("[Compat/Patchouli] Handbook viewer is not installed or has no compatible bridge", exception);
        }
    }

    private PatchouliJadeGuideCompat() {}
}
