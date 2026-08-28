package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
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

/** Simulated-machine menu geometry with the right 3x4 output footprint reserved for FE memory. */
public final class EnergyCrystalMenu extends AbstractContainerMenu implements MachineRedstoneMenu {
    private final Container container;
    private final EnergyCrystalBlockEntity crystal;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final net.minecraft.world.inventory.DataSlot redstoneMode;

    public EnergyCrystalMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer), true);
    }

    private EnergyCrystalMenu(int id, Inventory inventory, EnergyCrystalBlockEntity crystal,
                              boolean clientConstructor) {
        this(id, inventory,
                crystal == null ? new SimpleContainer(EnergyCrystalBlockEntity.SLOT_COUNT) : crystal,
                crystal,
                crystal == null ? new SimpleContainerData(EnergyCrystalBlockEntity.DATA_COUNT)
                        : crystal.dataAccess(),
                crystal == null ? BlockPos.ZERO : crystal.getBlockPos());
    }

    public EnergyCrystalMenu(int id, Inventory inventory, EnergyCrystalBlockEntity crystal) {
        this(id, inventory, (Container) crystal, crystal, crystal.dataAccess(), crystal.getBlockPos());
    }

    private EnergyCrystalMenu(int id, Inventory inventory, Container container,
                              EnergyCrystalBlockEntity crystal, ContainerData data, BlockPos blockPos) {
        super(ModMenus.ENERGY_CRYSTAL.get(), id);
        this.container = container;
        this.crystal = crystal;
        this.data = data;
        this.blockPos = blockPos;
        this.redstoneMode = MachineRedstoneMenu.dataSlot(crystal);
        checkContainerSize(container, EnergyCrystalBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, EnergyCrystalBlockEntity.DATA_COUNT);
        addSlot(new InputSlot(container, EnergyCrystalBlockEntity.INPUT_SLOT, 26, 26));
        addSlot(new FuelSlot(container, EnergyCrystalBlockEntity.FUEL_SLOT, 26, 62));
        addSlot(new ExtraSlot(container, EnergyCrystalBlockEntity.EXTRA_SLOT, 59, 44));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18, 105 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 26 + col * 18, 163));
        container.startOpen(inventory.player);
        addDataSlots(data);
        addDataSlot(redstoneMode);
    }

    @Override public net.minecraft.world.inventory.DataSlot redstoneModeSlot() { return redstoneMode; }

    public int processTicks() { return data.get(0); }
    public int burnTicks() { return data.get(1); }
    public int burnDuration() { return data.get(2); }
    public long storedEnergy() {
        return Integer.toUnsignedLong(data.get(3))
                | (Integer.toUnsignedLong(data.get(4)) << 32);
    }
    public long energyCapacity() {
        return Integer.toUnsignedLong(data.get(5))
                | (Integer.toUnsignedLong(data.get(6)) << 32);
    }
    public boolean xianqiaoOutput() { return data.get(7) != 0; }
    public boolean automaticOutput() { return data.get(8) != 0; }
    public boolean outputFace(int side) { return data.get(9 + side) != 0; }
    public BlockPos blockPos() { return blockPos; }
    public CrystalKind kind() { return crystal == null ? CrystalKind.ELECTRIC : crystal.kind(); }
    public String uiKey(String suffix) { return kind().uiTranslationKey(suffix); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id == MachineRedstoneMenu.CYCLE_BUTTON_ID) return MachineRedstoneMenu.cycle(crystal);
        if (crystal == null) return false;
        if (id == 0) {
            return crystal.toggleXianqiaoOutput();
        }
        if (id == 1) {
            crystal.toggleAutomaticOutput();
            return true;
        }
        if (id >= 10 && id < 16) {
            crystal.toggleOutputFace(net.minecraft.core.Direction.from3DDataValue(id - 10));
            return true;
        }
        return false;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        if (index < EnergyCrystalBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(moving, EnergyCrystalBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (crystal != null && crystal.canPlaceItem(EnergyCrystalBlockEntity.INPUT_SLOT, moving)) {
            if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY;
        } else if (crystal != null && crystal.canPlaceItem(EnergyCrystalBlockEntity.FUEL_SLOT, moving)) {
            if (!moveItemStackTo(moving, 1, 2, false)) return ItemStack.EMPTY;
        } else if (com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(moving)) {
            if (!moveItemStackTo(moving, 2, 3, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private final class InputSlot extends Slot {
        private InputSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return crystal == null || crystal.canPlaceItem(EnergyCrystalBlockEntity.INPUT_SLOT, stack);
        }
        @Override public int getMaxStackSize(ItemStack stack) {
            return stack == null || stack.isEmpty() ? 64 : stack.getMaxStackSize();
        }
    }

    private final class FuelSlot extends Slot {
        private FuelSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return crystal == null || crystal.canPlaceItem(EnergyCrystalBlockEntity.FUEL_SLOT, stack);
        }
    }

    private final class ExtraSlot extends Slot {
        private ExtraSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return crystal == null
                    ? com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(stack)
                    : crystal.canPlaceItem(EnergyCrystalBlockEntity.EXTRA_SLOT, stack);
        }
        @Override public int getMaxStackSize() { return 1; }
    }

    private static EnergyCrystalBlockEntity resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (buffer == null) return null;
        BlockPos pos = buffer.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof EnergyCrystalBlockEntity crystal
                ? crystal : null;
    }
}
