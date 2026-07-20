package com.cultivation.cultivation.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** MEK-style face button showing the adjacent block or a direction letter. */
final class FacePreviewButton extends Button {
    private final Supplier<ItemStack> preview;
    private final IntSupplier borderColor;

    FacePreviewButton(
            int x, int y, int size, Component direction,
            Supplier<ItemStack> preview, IntSupplier borderColor, OnPress onPress) {
        super(x, y, size, size, direction, onPress, DEFAULT_NARRATION);
        this.preview = preview;
        this.borderColor = borderColor;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int size = getWidth();
        int border = borderColor.getAsInt();
        graphics.fill(x, y, x + size, y + size, border);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2,
                isHoveredOrFocused() ? 0xFFB8B8B8 : 0xFF8A8A8A);

        ItemStack stack = preview.get();
        if (stack == null || stack.isEmpty()) {
            var font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), x + size / 2,
                    y + (size - font.lineHeight) / 2, 0xFFFFFFFF);
        } else {
            graphics.renderFakeItem(stack.copyWithCount(1), x + (size - 16) / 2, y + (size - 16) / 2);
        }
        if (alpha < 1.0F) {
            int shade = Math.min(220, Math.max(0, (int) ((1.0F - alpha) * 220.0F)));
            graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, shade << 24);
        }
        if (isFocused()) graphics.renderOutline(x + 1, y + 1, size - 2, size - 2, 0xFFFFFFFF);
    }
}
