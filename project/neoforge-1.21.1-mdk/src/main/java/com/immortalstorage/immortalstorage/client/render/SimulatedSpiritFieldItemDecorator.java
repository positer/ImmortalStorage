package com.immortalstorage.immortalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/**
 * Draws the simulated spirit field's internal substrate layer and a pulsing crop
 * glyph inside the 12-edge frame in slot-space, so the item preview mirrors the
 * block's dynamic processing.
 */
public final class SimulatedSpiritFieldItemDecorator implements IItemDecorator {
    public static final SimulatedSpiritFieldItemDecorator INSTANCE = new SimulatedSpiritFieldItemDecorator();
    private static final int SOIL = 0xFF6B4A2B;
    private static final int SOIL_DARK = 0xFF3E2A17;
    private static final int CROP = 0xFF54B435;
    private static final int CROP_DARK = 0xFF2F7A1E;

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        float ticks = Minecraft.getInstance().level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : Minecraft.getInstance().level.getGameTime()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        int cx = x + 8;
        int cy = y + 8;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 150.0F);
        // hydrated farmland substrate band near the bottom of the frame
        graphics.fill(cx - 6, cy + 2, cx + 7, cy + 3, SOIL);
        graphics.fill(cx - 7, cy + 3, cx + 8, cy + 4, SOIL_DARK);
        graphics.fill(cx - 6, cy + 4, cx + 7, cy + 5, SOIL_DARK);
        // crop glyph above the substrate; height pulses with the field's 50-tick cycle
        float phase = (ticks % 50.0F) / 50.0F;
        int cropHeight = 2 + Mth.floor(phase * 3.0F);
        for (int row = 0; row < cropHeight; row++) {
            int yy = cy - 1 - row;
            int spread = row == cropHeight - 1 ? 1 : 0;
            graphics.fill(cx - spread, yy, cx + 1 + spread, yy + 1, row == 0 ? CROP_DARK : CROP);
        }
        graphics.fill(cx - 1, cy - 1 - cropHeight, cx + 2, cy - cropHeight, CROP_DARK);
        graphics.pose().popPose();
        return false;
    }

    private SimulatedSpiritFieldItemDecorator() {}
}
