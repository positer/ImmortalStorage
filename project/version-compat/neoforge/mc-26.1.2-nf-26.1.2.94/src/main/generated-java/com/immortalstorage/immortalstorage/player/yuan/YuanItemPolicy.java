package com.immortalstorage.immortalstorage.player.yuan;

import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Single identity policy for the two ordinary, automation-visible yuan items. */
public final class YuanItemPolicy {
    private YuanItemPolicy() {}

    public static boolean isYuanItem(ItemStack stack) {
        return kindOf(stack) != null;
    }

    public static @Nullable YuanKind kindOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof TrueYuanItem) return YuanKind.TRUE;
        if (stack.getItem() instanceof ImmortalYuanItem) return YuanKind.IMMORTAL;
        return null;
    }
}
