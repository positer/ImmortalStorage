package com.immortalstorage.immortalstorage.locator;

import com.immortalstorage.immortalstorage.item.custom.ImmortalArtifactItem;
import com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One gate shared by server cache authorization and client rendering. */
public final class ToolLocatorPermissions {
    private ToolLocatorPermissions() {}
    public static int enabledMask(Player player) {
        if (!player.isAlive() || player.isSpectator()) return 0;
        int result = SpiritStaffItem.enabledExploreInstrument(player).isEmpty() ? 0 : 7;
        if (mining(player.getMainHandItem()) || mining(player.getOffhandItem())) result |= ToolLocatorRules.ORE;
        return result;
    }
    private static boolean mining(ItemStack stack) {
        return stack.getItem() instanceof ImmortalArtifactItem && SpiritStaffItem.getMode(stack) == SpiritStaffItem.MODE_PICK;
    }
}
