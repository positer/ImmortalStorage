package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.RedstoneWorkMode;
import com.immortalstorage.immortalstorage.block.entity.MachineRedstoneControl;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Shared, server-authoritative menu contract for the three-state machine redstone control. */
public interface MachineRedstoneMenu {
    int CYCLE_BUTTON_ID = 9_000;

    DataSlot redstoneModeSlot();

    default RedstoneWorkMode redstoneMode() {
        return RedstoneWorkMode.byId(redstoneModeSlot().get());
    }

    static DataSlot dataSlot(@Nullable BlockEntity blockEntity) {
        return new DataSlot() {
            private int clientValue = RedstoneWorkMode.IGNORE.ordinal();

            @Override public int get() {
                return blockEntity == null ? clientValue : MachineRedstoneControl.mode(blockEntity).ordinal();
            }

            @Override public void set(int value) {
                if (blockEntity == null) clientValue = RedstoneWorkMode.byId(value).ordinal();
                else MachineRedstoneControl.set(blockEntity, RedstoneWorkMode.byId(value));
            }
        };
    }

    static boolean cycle(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) return false;
        MachineRedstoneControl.cycle(blockEntity);
        return true;
    }
}
