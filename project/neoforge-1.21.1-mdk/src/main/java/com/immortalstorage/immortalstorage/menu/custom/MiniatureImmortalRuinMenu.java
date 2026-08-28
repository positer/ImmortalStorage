package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class MiniatureImmortalRuinMenu extends AbstractContainerMenu implements MachineRedstoneMenu {
    private final ContainerData data;
    private final com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity blockEntity;
    private final net.minecraft.world.inventory.DataSlot redstoneMode;
    public MiniatureImmortalRuinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainerData(5), buffer == null ? null :
                inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof
                        com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin ? ruin : null);
    }
    public MiniatureImmortalRuinMenu(int id, Inventory inventory, ContainerData data,
                                     com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity blockEntity) {
        super(ModMenus.MINIATURE_IMMORTAL_RUIN.get(), id); this.data = data; this.blockEntity = blockEntity;
        this.redstoneMode = MachineRedstoneMenu.dataSlot(blockEntity); addDataSlots(data); addDataSlot(redstoneMode);
    }
    public int value(int index) { return data.get(index); }
    @Override public net.minecraft.world.inventory.DataSlot redstoneModeSlot() { return redstoneMode; }
    @Override public boolean clickMenuButton(Player player, int id) { if (id == MachineRedstoneMenu.CYCLE_BUTTON_ID) return MachineRedstoneMenu.cycle(blockEntity); if (id >= 0 && id < 3 || id == 4) { data.set(id, data.get(id) == 0 ? 1 : 0); return true; } if (id == 3) { data.set(3, (data.get(3) + 1) % 5); return true; } return false; }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
