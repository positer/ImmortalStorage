package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity;
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

/**
 * Merged advanced ruin storage (54 slots) plus two independent advanced
 * scheduling menus. Button ids below 100 configure the normal side, ids 100-199
 * the reversed side; within each side the local id follows the advanced layout
 * (0-11 range steppers, 12/13 preview/enabled, 14/15 frequency, 16/17/18
 * access/split/order cycling). Filter mutations carry a side selector.
 */
public final class AdvancedEntangledMiniatureRuinMenu extends AbstractContainerMenu implements SideFilterMenu, MachineRedstoneMenu {
    private static final int SIDE_SPAN = 100;
    private final Container container;
    private final ContainerData data;
    private final AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity blockEntity;
    private final net.minecraft.core.BlockPos blockPos;
    private boolean pluginVisible;
    private final net.minecraft.world.inventory.DataSlot redstoneMode;

    public AdvancedEntangledMiniatureRuinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(55), new SimpleContainerData(26), null,
                buffer == null ? net.minecraft.core.BlockPos.ZERO : buffer.readBlockPos());
    }

    public AdvancedEntangledMiniatureRuinMenu(int id, Inventory inventory, Container container, ContainerData data) {
        this(id, inventory, container, data,
                container instanceof AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin : null,
                container instanceof AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin.getBlockPos() : net.minecraft.core.BlockPos.ZERO);
    }

    private AdvancedEntangledMiniatureRuinMenu(int id, Inventory inventory, Container container, ContainerData data,
                                               AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity blockEntity,
                                               net.minecraft.core.BlockPos blockPos) {
        super(ModMenus.ADVANCED_ENTANGLED_MINIATURE_IMMORTAL_RUIN.get(), id);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;
        this.blockPos = blockPos;
        this.redstoneMode = MachineRedstoneMenu.dataSlot(blockEntity);
        checkContainerSize(container, 55);
        container.startOpen(inventory.player);
        for (int row = 0; row < 6; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
        addSlot(new Slot(container, 54, 188, 198) {
            @Override public boolean mayPlace(ItemStack stack) { return com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(stack); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return pluginVisible; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        addDataSlots(data);
        addDataSlot(redstoneMode);
    }

    public int value(int index) { return data.get(index); }
    public net.minecraft.core.BlockPos blockPos() { return blockPos; }
    public AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity blockEntity() { return blockEntity; }
    @Override public net.minecraft.world.inventory.DataSlot redstoneModeSlot() { return redstoneMode; }
    public void setPluginVisible(boolean visible) { pluginVisible = visible; }
    public void setAuthoritativeValue(int side, int index, int value) {
        if (blockEntity != null && index >= 0 && index < 13) blockEntity.setMenuValue(side, index, value);
    }
    @Override public void setFilter(int side, int slot, ItemStack stack) { if (blockEntity != null) blockEntity.setFilter(side, slot, stack); }
    @Override public void toggleFilterMode(int side, int mode) {
        if (blockEntity != null) {
            if (mode == 0) blockEntity.toggleFilterMatchComponents(side);
            else blockEntity.toggleFilterWhitelist(side);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == MachineRedstoneMenu.CYCLE_BUTTON_ID) return MachineRedstoneMenu.cycle(blockEntity);
        int side = id >= SIDE_SPAN ? 1 : 0;
        int local = id >= SIDE_SPAN ? id - SIDE_SPAN : id;
        int base = side * 13;
        if (local >= 0 && local < 12) {
            int index = base + local / 2;
            data.set(index, data.get(index) + (local % 2 == 0 ? -1 : 1));
            return true;
        }
        if (local == 12) { data.set(base + 7, data.get(base + 7) == 0 ? 1 : 0); return true; }
        if (local == 13) { data.set(base + 8, data.get(base + 8) == 0 ? 1 : 0); return true; }
        if (local == 14) { data.set(base + 6, Math.max(1, data.get(base + 6) - 1)); return true; }
        if (local == 15) { data.set(base + 6, data.get(base + 6) + 1); return true; }
        if (local == 16) { data.set(base + 9, (data.get(base + 9) + 1) % 2); return true; }
        if (local == 17) { data.set(base + 10, (data.get(base + 10) + 1) % 2); return true; }
        if (local == 18) { data.set(base + 11, (data.get(base + 11) + 1) % 2); return true; }
        if (local >= 20 && local <= 25) {
            int bit = 1 << (local - 20);
            int current = data.get(base + 12);
            data.set(base + 12, (current & bit) == 0 ? current | bit : current & ~bit);
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
        if (index < 55) {
            if (!moveItemStackTo(original, 55, slots.size(), true)) return ItemStack.EMPTY;
        } else if (com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(original)) {
            if (!moveItemStackTo(original, 54, 55, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 54, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
}
