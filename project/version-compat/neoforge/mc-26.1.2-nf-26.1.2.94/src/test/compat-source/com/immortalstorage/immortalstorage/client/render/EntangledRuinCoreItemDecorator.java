package com.immortalstorage.immortalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/** Two smaller counter-rotating opposite-state spheres drawn inside the merged ruin frame. */
public final class EntangledRuinCoreItemDecorator implements IItemDecorator {
    public static final EntangledRuinCoreItemDecorator INSTANCE = new EntangledRuinCoreItemDecorator();
    private static final float ORBIT = 3.2F;

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        float ticks = Minecraft.getInstance().level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : Minecraft.getInstance().level.getGameTime()
                + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        int centerX = x + 8;
        int centerY = y + 8;
        float breath = Mth.sin(ticks * 0.12F) > 0.0F ? 1 : 0;
        int radius = 2 + Mth.floor(breath);
        float angleA = ticks * 0.30F;
        float angleB = -ticks * 0.30F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, 0.0F);
        drawOrbiter(graphics, centerX, centerY, angleA, radius, false);
        drawOrbiter(graphics, centerX, centerY, angleB, radius, true);
        graphics.pose().popMatrix();
        return false;
    }

    private void drawOrbiter(GuiGraphicsExtractor graphics, int centerX, int centerY, float angle, int radius, boolean reversed) {
        int bx = centerX + (int) Math.round(Mth.cos(angle) * ORBIT);
        int by = centerY + (int) Math.round(Mth.sin(angle) * ORBIT);
        int edge = reversed ? 0x96000000 : 0x96FFFFFF;
        int core = reversed ? 0xFFFFFFFF : 0xFF000000;
        drawDisc(graphics, bx, by, radius, edge);
        drawDisc(graphics, bx, by, Math.max(1, radius - 1), core);
    }

    private static void drawDisc(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int width = Mth.floor(Math.sqrt(Math.max(0, radiusSquared - dy * dy)));
            graphics.fill(centerX - width, centerY + dy, centerX + width + 1, centerY + dy + 1, color);
        }
    }

    private EntangledRuinCoreItemDecorator() {}
}
