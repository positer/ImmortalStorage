package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.block.RedstoneWorkMode;
import com.immortalstorage.immortalstorage.menu.custom.MachineRedstoneMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Compact vanilla-style cycle button shared by every machine configuration screen. */
public final class MachineRedstoneModeButton {
    public static final int WIDTH = 78;
    public static final int HEIGHT = 16;

    public static Button create(int x, int y, MachineRedstoneMenu menu) {
        Button button = Button.builder(label(menu.redstoneMode()), ignored -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameMode != null && menu instanceof net.minecraft.world.inventory.AbstractContainerMenu container) {
                minecraft.gameMode.handleInventoryButtonClick(container.containerId, MachineRedstoneMenu.CYCLE_BUTTON_ID);
            }
        }).bounds(x, y, WIDTH, HEIGHT).build();
        button.setTooltip(Tooltip.create(Component.translatable("container.immortalstorage.redstone_mode.tooltip")));
        return button;
    }

    public static void refresh(Button button, MachineRedstoneMenu menu) {
        if (button != null) button.setMessage(label(menu.redstoneMode()));
    }

    private static Component label(RedstoneWorkMode mode) {
        return Component.translatable("container.immortalstorage.redstone_mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    private MachineRedstoneModeButton() { }
}
