package com.cultivation.cultivation.compat.patchouli;

import com.cultivation.cultivation.CultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

/** Patchouli-only entry point; the common item reaches it only after a mod-id gate. */
public final class PatchouliJadeGuideCompat {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(
            CultivationMod.MODID, "jade_guide");

    public static void open(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK);
    }

    private PatchouliJadeGuideCompat() {}
}
