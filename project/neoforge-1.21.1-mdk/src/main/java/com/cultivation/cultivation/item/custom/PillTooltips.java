package com.cultivation.cultivation.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class PillTooltips {
    static void add(List<Component> tooltip, String id) {
        tooltip.add(Component.translatable("tooltip.cultivation.pill." + id + ".effect")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.cultivation.pill." + id + ".detail")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private PillTooltips() {}
}
