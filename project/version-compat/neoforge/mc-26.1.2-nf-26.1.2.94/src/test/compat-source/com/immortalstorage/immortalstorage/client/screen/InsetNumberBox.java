package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Native input, cursor and selection with a compact in-panel visual instead of a black stock frame. */
final class InsetNumberBox extends EditBox {
    private final Font textFont;
    private int foreground;

    InsetNumberBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x, y, width, height, label);
        this.textFont = font;
        setBordered(false);
        setTextColor(0xFF363636);
        setTextColorUneditable(0xFF646464);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        this.foreground = color;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean editing = isFocused() && this.active;
        graphics.fill(x, y, x + w, y + h, editing ? 0xFF8BAD95 : 0xFF898989);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1,
                editing ? 0xFF34493C : this.active ? 0xFFE6E6E6 : 0xFFD4D4D4);
        if (editing) {
            // The game's edit pass draws a dark drop shadow; use light glyphs on the active well
            // while retaining vanilla clipping, cursor and selection. Restore the hitbox and idle color.
            super.setTextColor(this.foreground == 0xFFA32C2E ? 0xFFFFADAD : 0xFFE9F4EC);
            setY(y + Math.max(0, (h - 8) / 2));
            try {
                super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
            } finally {
                setY(y);
                super.setTextColor(this.foreground);
            }
        } else {
            graphics.text(this.textFont, getValue(), x + 3, y + (h - 8) / 2,
                    this.active ? this.foreground : 0xFF646464, false);
        }
    }
}
