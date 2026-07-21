package com.immortalstorage.immortalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

/** Animated GUI counterpart of the manager block entity's luminous core. */
public final class XianqiaoManagerItemDecorator implements IItemDecorator {
    public static final XianqiaoManagerItemDecorator INSTANCE = new XianqiaoManagerItemDecorator();

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        float ticks = Minecraft.getInstance().level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : Minecraft.getInstance().level.getGameTime()
                + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float bob = Mth.sin(ticks * 0.075F);
        float sway = Mth.sin(ticks * 0.04F);
        int centerX = x + 8 + (sway > 0.45F ? 1 : sway < -0.45F ? -1 : 0);
        int centerY = y + 8 + (bob > 0.35F ? -1 : bob < -0.35F ? 1 : 0);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 150.0F);
        graphics.fill(centerX - 3, centerY - 2, centerX + 3, centerY + 2, 0x68F7FBFF);
        graphics.fill(centerX - 2, centerY - 3, centerX + 2, centerY + 3, 0x68F7FBFF);
        graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFF7FBFF);
        graphics.pose().popPose();
        return false;
    }

    private XianqiaoManagerItemDecorator() {}
}
