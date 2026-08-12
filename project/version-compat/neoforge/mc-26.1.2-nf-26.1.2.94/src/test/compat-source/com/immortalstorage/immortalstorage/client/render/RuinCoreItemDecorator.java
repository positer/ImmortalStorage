package com.immortalstorage.immortalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.IItemDecorator;

/** Animated slot-space core drawn inside the stabilized ruin's 12-edge frame. */
public final class RuinCoreItemDecorator implements IItemDecorator {
    public static final RuinCoreItemDecorator INSTANCE = new RuinCoreItemDecorator();

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        float ticks = Minecraft.getInstance().level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : Minecraft.getInstance().level.getGameTime()
                + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        int radius = Mth.sin(ticks * 0.12F) > 0.0F ? 4 : 3;
        boolean reversed = reversed(stack);
        int centerX = x + 8;
        int centerY = y + 8;
        int edge = reversed ? 0x96000000 : 0x96FFFFFF;
        int core = reversed ? 0xFFFFFFFF : 0xFF000000;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        drawDisc(graphics, centerX, centerY, radius, edge);
        drawDisc(graphics, centerX, centerY, Math.max(2, radius - 1), core);
        graphics.pose().popMatrix();
        return false;
    }

    private static boolean reversed(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag.getBooleanOr("Reversed", false);
    }

    private static void drawDisc(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int width = Mth.floor(Math.sqrt(Math.max(0, radiusSquared - dy * dy)));
            graphics.fill(centerX - width, centerY + dy, centerX + width + 1, centerY + dy + 1, color);
        }
    }

    private RuinCoreItemDecorator() {}
}
