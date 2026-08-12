package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 2×3 toggle grid for the container interaction faces: top row UP/NORTH/DOWN,
 * bottom row WEST/SOUTH/EAST (matching the 仙窍/源矿脉 attachment faces). Each
 * button flips one Direction-ordinal bit of the authoritative face mask by
 * invoking {@code onToggle} with its menu-button id ({@code localBase + ordinal});
 * the lit state is shown via alpha. All six off means the ruin does not touch
 * any container. The caller registers the returned buttons as widgets.
 */
public final class RuinFaceGrid {
    /** Visual grid order as Direction ordinals: UP(1), NORTH(2), DOWN(0), WEST(4), SOUTH(3), EAST(5). */
    public static final int[] ORDER = {1, 2, 0, 4, 3, 5};

    private RuinFaceGrid() {
    }

    /** Builds six 28x18 toggle buttons in grid order, three per row, starting at (panelX, panelY). */
    public static List<Button> add(IntConsumer onToggle, int localBase, int panelX, int panelY) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < ORDER.length; i++) {
            int ordinal = ORDER[i];
            int id = localBase + ordinal;
            Button button = Button.builder(Component.literal(RuinFaceText.abbr(ordinal)), clicked -> onToggle.accept(id))
                    .bounds(panelX + (i % 3) * 30, panelY + (i / 3) * 20, 28, 18)
                    .tooltip(Tooltip.create(tooltip(ordinal)))
                    .build();
            buttons.add(button);
        }
        return buttons;
    }

    /** Refreshes the grid from the authoritative mask: lit = enabled face; hidden = no interaction. */
    public static void sync(List<Button> buttons, int mask, boolean visible) {
        for (int i = 0; i < buttons.size() && i < ORDER.length; i++) {
            Button button = buttons.get(i);
            button.visible = visible;
            button.setAlpha((mask & (1 << ORDER[i])) != 0 ? 1.0F : 0.35F);
        }
    }

    private static Component tooltip(int ordinal) {
        return Component.translatable("container.immortalstorage.stabilized_ruin.face_"
                + switch (ordinal) {
                    case 0 -> "down"; case 1 -> "up"; case 2 -> "north";
                    case 3 -> "south"; case 4 -> "west"; default -> "east";
                });
    }
}
