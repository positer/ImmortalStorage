package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Six-row inventory plus server-validated configuration button actions. */
public class StabilizedMiniatureImmortalRuinMenu extends AbstractContainerMenu {
    protected final Container container;
    protected final ContainerData data;
    protected final StabilizedMiniatureImmortalRuinBlockEntity blockEntity;
    protected final net.minecraft.core.BlockPos blockPos;

    public StabilizedMiniatureImmortalRuinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(ModMenus.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), id, inventory, clientContainer(),
                new SimpleContainerData(11), null,
                buffer == null ? net.minecraft.core.BlockPos.ZERO : buffer.readBlockPos());
    }

    public StabilizedMiniatureImmortalRuinMenu(int id, Inventory inventory, Container container, ContainerData data) {
        this(ModMenus.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), id, inventory, container, data,
                container instanceof StabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin : null,
                container instanceof StabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin.getBlockPos() : net.minecraft.core.BlockPos.ZERO);
    }

    protected StabilizedMiniatureImmortalRuinMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inventory,
                                                   Container container, ContainerData data,
                                                   StabilizedMiniatureImmortalRuinBlockEntity blockEntity,
                                                   net.minecraft.core.BlockPos blockPos) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        this.blockPos = blockPos;
        checkContainerSize(container, 54);
        container.startOpen(inventory.player);
        for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        addDataSlots(data);
    }

    private static Container clientContainer() { return new SimpleContainer(54); }
    public int value(int index) { return data.get(index); }
    public void setAuthoritativeValue(int index, int value) {
        if (blockEntity != null && index >= 0 && index < data.getCount()) blockEntity.setMenuValue(index, value);
    }
    public net.minecraft.core.BlockPos blockPos() { return blockPos; }
    public StabilizedMiniatureImmortalRuinBlockEntity blockEntity() { return blockEntity; }
    public void setFilter(int slot, ItemStack stack) { if (blockEntity != null) blockEntity.setFilter(slot, stack); }
    public void toggleFilterMode(int mode) { if (blockEntity != null) { if (mode == 0) blockEntity.toggleFilterMatchComponents(); else blockEntity.toggleFilterWhitelist(); } }

    /** Data-slot index exposing the interaction face ordinal (-1 = any). */
    protected int faceDataIndex() { return 10; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 12) {
            int index = id / 2;
            data.set(index, data.get(index) + (id % 2 == 0 ? -1 : 1));
            return true;
        }
        if (id == 12) { data.set(7, data.get(7) == 0 ? 1 : 0); return true; }
        if (id == 13) { data.set(8, data.get(8) == 0 ? 1 : 0); return true; }
        if (id == 14) { data.set(6, Math.max(1, data.get(6) - 1)); return true; }
        if (id == 15) { data.set(6, data.get(6) + 1); return true; }
        if (id >= 20 && id <= 25) {
            int bit = 1 << (id - 20);
            int current = data.get(faceDataIndex());
            data.set(faceDataIndex(), (current & bit) == 0 ? current | bit : current & ~bit);
            return true;
        }
        return false;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 54 ? !moveItemStackTo(original, 54, slots.size(), true) : !moveItemStackTo(original, 0, 54, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
}
