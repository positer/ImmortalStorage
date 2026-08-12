package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.SimulatedSpiritFieldBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.core.BlockPos;
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

/** Slot-for-slot equivalent of the Simulated Reincarnation Furnace menu. */
public final class SimulatedSpiritFieldMenu extends AbstractContainerMenu {
    private final Container container;
    private final SimulatedSpiritFieldBlockEntity field;
    private final ContainerData data;
    private final BlockPos blockPos;

    public SimulatedSpiritFieldMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer), true);
    }
    private SimulatedSpiritFieldMenu(int id, Inventory inventory, SimulatedSpiritFieldBlockEntity field,
                                     boolean clientConstructor) {
        this(id, inventory, field == null ? new SimpleContainer(SimulatedSpiritFieldBlockEntity.SLOT_COUNT) : field,
                field, field == null ? new SimpleContainerData(SimulatedSpiritFieldBlockEntity.DATA_COUNT)
                        : field.dataAccess(), field == null ? BlockPos.ZERO : field.getBlockPos());
    }
    public SimulatedSpiritFieldMenu(int id, Inventory inventory, SimulatedSpiritFieldBlockEntity field) {
        this(id, inventory, (Container) field, field, field.dataAccess(), field.getBlockPos());
    }
    private SimulatedSpiritFieldMenu(int id, Inventory inventory, Container container,
                                     SimulatedSpiritFieldBlockEntity field, ContainerData data, BlockPos blockPos) {
        super(ModMenus.SIMULATED_SPIRIT_FIELD.get(), id);
        this.container = container; this.field = field; this.data = data; this.blockPos = blockPos;
        checkContainerSize(container, SimulatedSpiritFieldBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, SimulatedSpiritFieldBlockEntity.DATA_COUNT);
        addSlot(new SeedSlot(container, 0, 26, 26));
        addSlot(new FuelSlot(container, 1, 26, 62));
        addSlot(new ToolSlot(container, 2, 59, 44));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 4; col++) {
            addSlot(new OutputSlot(container, 3 + row * 4 + col, 132 + col * 18, 26 + row * 18));
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18, 105 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 26 + col * 18, 163));
        container.startOpen(inventory.player); addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public int burnTicks() { return data.get(1); }
    public int storedExperience() { return data.get(2); }
    public boolean xianqiaoOutput() { return data.get(3) != 0; }
    public boolean automaticOutput() { return data.get(4) != 0; }
    public boolean outputFace(int side) { return data.get(5 + side) != 0; }
    public BlockPos blockPos() { return blockPos; }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (field == null) return false;
        if (id == 0) { field.toggleXianqiaoOutput(); return true; }
        if (id == 1) { field.toggleAutomaticOutput(); return true; }
        if (id == 2 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            field.releaseExperience(serverPlayer); return true;
        }
        if (id >= 10 && id < 16) {
            field.toggleOutputFace(net.minecraft.core.Direction.from3DDataValue(id - 10)); return true;
        }
        return false;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem(); ItemStack original = moving.copy();
        if (index < SimulatedSpiritFieldBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(moving, SimulatedSpiritFieldBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (field != null && field.canPlaceItem(SimulatedSpiritFieldBlockEntity.SEED_SLOT, moving)) {
            if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY;
        } else if (field != null && field.canPlaceItem(SimulatedSpiritFieldBlockEntity.FUEL_SLOT, moving)) {
            if (!moveItemStackTo(moving, 1, 2, false)) return ItemStack.EMPTY;
        } else if (moving.getMaxStackSize() == 1) {
            if (!moveItemStackTo(moving, 2, 3, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving); return original;
    }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    private final class SeedSlot extends Slot {
        SeedSlot(Container c, int slot, int x, int y) { super(c, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return field == null || field.isValidSeed(stack); }
    }
    private final class FuelSlot extends Slot {
        FuelSlot(Container c, int slot, int x, int y) { super(c, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return field == null || field.canPlaceItem(1, stack); }
    }
    private static final class ToolSlot extends Slot {
        ToolSlot(Container c, int slot, int x, int y) { super(c, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return stack.getMaxStackSize() == 1; }
        @Override public int getMaxStackSize() { return 1; }
    }
    private static final class OutputSlot extends Slot {
        OutputSlot(Container c, int slot, int x, int y) { super(c, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
    private static SimulatedSpiritFieldBlockEntity resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (buffer == null) return null;
        BlockPos pos = buffer.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof SimulatedSpiritFieldBlockEntity field
                ? field : null;
    }
}
