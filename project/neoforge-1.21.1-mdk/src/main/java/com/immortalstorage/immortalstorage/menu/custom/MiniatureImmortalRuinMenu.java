package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class MiniatureImmortalRuinMenu extends AbstractContainerMenu {
    private final ContainerData data;
    public MiniatureImmortalRuinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id, inventory, new SimpleContainerData(4)); }
    public MiniatureImmortalRuinMenu(int id, Inventory inventory, ContainerData data) { super(ModMenus.MINIATURE_IMMORTAL_RUIN.get(), id); this.data = data; addDataSlots(data); }
    public int value(int index) { return data.get(index); }
    @Override public boolean clickMenuButton(Player player, int id) { if (id >= 0 && id < 3) { data.set(id, data.get(id) == 0 ? 1 : 0); return true; } if (id == 3) { data.set(3, (data.get(3) + 1) % 5); return true; } return false; }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
