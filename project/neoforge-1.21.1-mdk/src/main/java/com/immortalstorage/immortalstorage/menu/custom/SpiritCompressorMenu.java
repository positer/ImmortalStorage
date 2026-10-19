package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.RuinMachine;
import com.immortalstorage.immortalstorage.compressor.RuinMachineKind;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class SpiritCompressorMenu extends AbstractContainerMenu implements MachineRedstoneMenu {
    private final MiniatureImmortalRuinBlockEntity ruin;
    private final RuinMachine machine;
    private final ContainerData data;
    private final DataSlot redstone;
    private final boolean reversed;
    private final RuinMachineKind kind;
    public boolean reversed() { return reversed; }
    /** The title key of the bound machine, kind and polarity included. */
    public String titleKey() { return machine == null ? "container.immortalstorage.spirit_compressor" : machine.titleKey(); }
    /**
     * A pending deferred transaction keeps the machine's own inputs in place, and the check must read the
     * machine these slots were built from: re-deriving it from the world would unlock the inputs in the gap
     * between a kind change and the core's next tick, while settleOutput would still bill the captured one.
     */
    private boolean lockedInput(int slot) {
        return machine != null && machine.locksSlot(slot);
    }
    /** A container take of a product slot pays the deferred cost too: only the handler hook sees pipes. */
    private void settleOutput(Player player) {
        if (machine != null && !player.level().isClientSide) machine.settlePending();
    }
    public net.minecraft.core.BlockPos blockPos() { return ruin == null ? net.minecraft.core.BlockPos.ZERO : ruin.getBlockPos(); }
    public SpiritCompressorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer != null && inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof MiniatureImmortalRuinBlockEntity entity ? entity : null);
    }
    public SpiritCompressorMenu(int id, Inventory inventory, MiniatureImmortalRuinBlockEntity ruin) {
        super(ModMenus.SPIRIT_COMPRESSOR.get(), id); this.ruin = ruin;
        machine = ruin == null ? null : ruin.machine();
        reversed = machine != null && machine.reversed();
        kind = ruin == null ? null : ruin.machineKind();
        var handler = ruin == null || inventory.player.level().isClientSide ? new ItemStackHandler(13) : machine.activeInventory();
        if (reversed) {
            addSlot(new SlotItemHandler(handler, 0, 26, 46) { @Override public boolean mayPickup(Player player) { return !lockedInput(index); } });
            for (int i = 0; i < 12; i++) addSlot(new SlotItemHandler(handler, i + 1, 98 + i % 4 * 18, 28 + i / 4 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); settleOutput(player); }
            });
        } else {
            for (int i = 0; i < 12; i++) addSlot(new SlotItemHandler(handler, i, 8 + i % 4 * 18, 28 + i / 4 * 18) {
                @Override public boolean mayPickup(Player player) { return !lockedInput(index); }
            });
            addSlot(new SlotItemHandler(handler, 12, 134, 46) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); settleOutput(player); }
            });
        }
        for (int i = 0; i < 27; i++) addSlot(new Slot(inventory, i + 9, 8 + i % 9 * 18, 100 + i / 9 * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 8 + i * 18, 158));
        data = ruin == null || inventory.player.level().isClientSide ? new SimpleContainerData(9) : new ContainerData() {
            @Override public int get(int i) { return i < 6 ? machine.mode(i) : i == 6 ? (machine.xianqiaoOutput() ? 1 : 0) : i == 7 ? (machine.formed() ? 1 : 0) : (machine.reversed() ? 1 : 0); }
            @Override public void set(int i, int value) {}
            @Override public int getCount() { return 9; }
        };
        redstone = MachineRedstoneMenu.dataSlot(ruin); addDataSlots(data); addDataSlot(redstone);
    }
    public int value(int i) { return data.get(i); }
    @Override public DataSlot redstoneModeSlot() { return redstone; }
    @Override public boolean stillValid(Player player) { return ruin != null && machine != null && reversed == machine.reversed() && kind == ruin.machineKind() && !ruin.isRemoved() && ruin.getLevel() == player.level()
            && player.distanceToSqr(ruin.getBlockPos().getX() + .5, ruin.getBlockPos().getY() + .5, ruin.getBlockPos().getZ() + .5) <= 64; }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (id == MachineRedstoneMenu.CYCLE_BUTTON_ID) return MachineRedstoneMenu.cycle(ruin);
        if (id >= 0 && id < 6) { machine.cycle(id); return true; }
        if (id == 6) { machine.toggleXianqiao(); return true; } return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem() || lockedInput(index)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 13 ? !moveItemStackTo(stack, 13, 49, true) : !moveItemStackTo(stack, 0, reversed ? 1 : 12, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return original;
    }
}
