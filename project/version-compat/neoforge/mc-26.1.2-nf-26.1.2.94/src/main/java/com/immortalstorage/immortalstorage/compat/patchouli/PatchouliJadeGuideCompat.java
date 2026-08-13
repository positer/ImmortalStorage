package com.immortalstorage.immortalstorage.compat.patchouli;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Direct 26.1 handbook bridge. Patchouli is bundled and required, matching the
 * 1.21.1 distribution contract, so the item always uses the official API.
 */
public final class PatchouliJadeGuideCompat {
    private static final Identifier BOOK = Identifier.fromNamespaceAndPath(
            ImmortalStorageMod.MODID, "jade_guide");

    public static void open(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK);
    }

    private PatchouliJadeGuideCompat() {}
}
