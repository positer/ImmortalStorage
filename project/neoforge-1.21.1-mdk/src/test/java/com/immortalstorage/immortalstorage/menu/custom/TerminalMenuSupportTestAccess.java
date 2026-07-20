package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.world.item.ItemStack;

/** Test-source access to package-private terminal helpers. */
public final class TerminalMenuSupportTestAccess {
    public static ItemStack insertKongqiao(ImmortalStoragePlayerData data, ItemStack stack) {
        return TerminalMenuSupport.insertKongqiao(data, stack);
    }

    private TerminalMenuSupportTestAccess() {}
}
