package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class PillTooltips {
    static void add(List<Component> tooltip, String id) {
        tooltip.add(Component.translatable("tooltip.immortalstorage.pill." + id + ".effect")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.immortalstorage.pill." + id + ".detail")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private PillTooltips() {}
}
