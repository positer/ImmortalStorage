package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.world.item.ItemStack;

/** Test-source access to package-private terminal helpers. */
public final class TerminalMenuSupportTestAccess {
    public static ItemStack insertKongqiao(CultivationPlayerData data, ItemStack stack) {
        return TerminalMenuSupport.insertKongqiao(data, stack);
    }

    private TerminalMenuSupportTestAccess() {}
}
