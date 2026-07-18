package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.block.entity.SourceVeinManagerBlockEntity;
import com.cultivation.cultivation.block.entity.SourceVeinManagerInventory;
import com.cultivation.cultivation.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Eight rows of source members in one server-authoritative menu. */
public final class SourceVeinManagerMenu extends AbstractContainerMenu {
    public static final int MEMBER_SLOTS = SourceVeinManagerInventory.SLOT_COUNT;
    public static final int PLAYER_START = MEMBER_SLOTS;
    private final Container members;
    private final @Nullable SourceVeinManagerBlockEntity manager;

    public SourceVeinManagerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer));
    }

    public SourceVeinManagerMenu(int id, Inventory inventory, SourceVeinManagerBlockEntity manager) {
        this(id, inventory, manager, manager);
    }

    private SourceVeinManagerMenu(int id, Inventory inventory, @Nullable SourceVeinManagerBlockEntity manager,
                                  Container members) {
        super(ModMenus.SOURCE_VEIN_MANAGER.get(), id);
        this.manager = manager;
        this.members = members;
        checkContainerSize(members, MEMBER_SLOTS);
        members.startOpen(inventory.player);
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                addSlot(new Slot(members, slot, 8 + column * 18, 18 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        return members.canPlaceItem(getContainerSlot(), stack);
                    }
                    @Override public int getMaxStackSize() { return 1; }
                });
            }
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 174 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 232));
        }
    }

    private SourceVeinManagerMenu(int id, Inventory inventory, Resolved resolved) {
        this(id, inventory, resolved.manager(), resolved.container());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(moving, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 0, PLAYER_START, false)) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return moving.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override public boolean stillValid(Player player) {
        return manager == null ? members.stillValid(player) : manager.stillValid(player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        members.stopOpen(player);
    }

    private static Resolved resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (buffer != null) {
            var pos = buffer.readBlockPos();
            if (inventory.player.level().getBlockEntity(pos) instanceof SourceVeinManagerBlockEntity manager) {
                return new Resolved(manager, manager);
            }
        }
        return new Resolved(null, new SimpleContainer(MEMBER_SLOTS));
    }

    private record Resolved(@Nullable SourceVeinManagerBlockEntity manager, Container container) {}
}
