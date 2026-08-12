package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class SimulatedReincarnationFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final SimulatedReincarnationFurnaceBlockEntity blockEntity;
    private final ContainerData data;
    private final BlockPos blockPos;

    public SimulatedReincarnationFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer), true);
    }
    private SimulatedReincarnationFurnaceMenu(int id, Inventory inventory,
                                               SimulatedReincarnationFurnaceBlockEntity blockEntity,
                                               boolean clientConstructor) {
                this(id, inventory, blockEntity == null
                        ? new SimpleContainer(SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT) : blockEntity,
                blockEntity, blockEntity == null
                        ? new SimpleContainerData(SimulatedReincarnationFurnaceBlockEntity.DATA_COUNT)
                        : blockEntity.dataAccess(),
                blockEntity == null ? BlockPos.ZERO : blockEntity.getBlockPos());
    }
    public SimulatedReincarnationFurnaceMenu(int id, Inventory inventory,
                                             SimulatedReincarnationFurnaceBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity, blockEntity.dataAccess(), blockEntity.getBlockPos());
    }
    private SimulatedReincarnationFurnaceMenu(int id, Inventory inventory, Container container,
                                              SimulatedReincarnationFurnaceBlockEntity blockEntity,
                                              ContainerData data, BlockPos blockPos) {
        super(ModMenus.SIMULATED_REINCARNATION_FURNACE.get(), id);
        this.container = container; this.blockEntity = blockEntity; this.data = data; this.blockPos = blockPos;
        checkContainerSize(container, SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, SimulatedReincarnationFurnaceBlockEntity.DATA_COUNT);
        addSlot(new Slot(container, 0, 26, 26));
        addSlot(new Slot(container, 1, 26, 62));
        addSlot(new Slot(container, 2, 59, 44));
        for (int row=0; row<3; row++) for(int col=0; col<4; col++)
            addSlot(new Slot(container, 3 + row*4 + col, 132 + col*18, 26 + row*18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++)
            addSlot(new Slot(inventory, col + row*9 + 9, 26 + col*18, 105 + row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory, col, 26 + col*18, 163));
        container.startOpen(inventory.player);
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int burnTicks() { return data.get(1); }
    public int storedExperience() { return data.get(2); }
    public boolean xianqiaoOutput() { return data.get(3) != 0; }
    public boolean automaticOutput() { return data.get(4) != 0; }
    public boolean outputFace(int side) { return data.get(5 + side) != 0; }
    public BlockPos blockPos() { return blockPos; }
    public SimulatedReincarnationFurnaceBlockEntity blockEntity() { return blockEntity; }
    public void toggleXianqiaoOutput() { if (blockEntity != null) blockEntity.toggleXianqiaoOutput(); }
    public void toggleAutomaticOutput() { if (blockEntity != null) blockEntity.toggleAutomaticOutput(); }
    public void releaseExperience(Player player) { if(blockEntity != null && player instanceof net.minecraft.server.level.ServerPlayer sp) blockEntity.releaseExperience(sp); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id == 0) { toggleXianqiaoOutput(); return true; }
        if (id == 1) { toggleAutomaticOutput(); return true; }
        if (id == 2) { releaseExperience(player); return true; }
        if (id >= 10 && id < 16 && blockEntity != null) {
            blockEntity.toggleOutputFace(net.minecraft.core.Direction.from3DDataValue(id - 10));
            return true;
        }
        return false;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result=ItemStack.EMPTY; Slot slot=slots.get(index); if(!slot.hasItem()) return result;
        ItemStack current=slot.getItem(); result=current.copy();
        if(index < SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT) {
            if(!moveItemStackTo(current, SimulatedReincarnationFurnaceBlockEntity.SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else if(!moveItemStackTo(current,0,3,false)) return ItemStack.EMPTY;
        if(current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    private static SimulatedReincarnationFurnaceBlockEntity resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (buffer == null) return null;
        BlockPos pos = buffer.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof SimulatedReincarnationFurnaceBlockEntity furnace
                ? furnace : null;
    }
}
