package com.immortalstorage.immortalstorage.compat.patchouli;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

/** Single authoritative Ancient Jade handbook entry point. */
public final class PatchouliJadeGuideCompat {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(
            ImmortalStorageMod.MODID, "jade_guide");

    public static void open(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK);
    }

    private PatchouliJadeGuideCompat() {}
}
