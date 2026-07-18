package com.cultivation.cultivation.menu.custom;

import com.cultivation.cultivation.block.entity.ImmortalFurnaceBlockEntity;
import com.cultivation.cultivation.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One server-authoritative menu containing all three furnace channels. */
public class ImmortalFurnaceMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOT_COUNT = ImmortalFurnaceBlockEntity.SLOT_COUNT;
    public static final int PLAYER_SLOT_COUNT = 36;
    private static final int PLAYER_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + PLAYER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_START + 27;

    private final Container container;
    private final ContainerData data;
    private final ImmortalFurnaceBlockEntity blockEntity;

    public ImmortalFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(ImmortalFurnaceBlockEntity.DATA_COUNT), null);
    }

    public ImmortalFurnaceMenu(int id, Inventory inventory, ImmortalFurnaceBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getDataAccessPublic(), blockEntity);
    }

    public ImmortalFurnaceMenu(int id, Inventory inventory, Container container, ContainerData data) {
        this(id, inventory, container, data, null);
    }

    private ImmortalFurnaceMenu(int id, Inventory inventory, Container container,
                                ContainerData data, ImmortalFurnaceBlockEntity blockEntity) {
        super(ModMenus.IMMORTAL_FURNACE.get(), id);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ImmortalFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockEntity = blockEntity;

        addSlot(new InputSlot(container, ImmortalFurnaceBlockEntity.INPUT_1, 53, 20));
        addSlot(new FuelSlot(container, ImmortalFurnaceBlockEntity.FUEL, 17, 50));
        addSlot(new FurnaceResultSlot(inventory.player, container,
                ImmortalFurnaceBlockEntity.RESULT_1, 143, 20));
        addSlot(new InputSlot(container, ImmortalFurnaceBlockEntity.INPUT_2, 53, 50));
        addSlot(new FurnaceResultSlot(inventory.player, container,
                ImmortalFurnaceBlockEntity.RESULT_2, 143, 50));
        addSlot(new InputSlot(container, ImmortalFurnaceBlockEntity.INPUT_3, 53, 80));
        addSlot(new FurnaceResultSlot(inventory.player, container,
                ImmortalFurnaceBlockEntity.RESULT_3, 143, 80));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        17 + column * 18, 134 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 17 + column * 18, 192));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return container.stillValid(player);
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5D,
                blockEntity.getBlockPos().getY() + 0.5D,
                blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    public boolean isFuel(ItemStack stack) {
        return ImmortalFurnaceBlockEntity.isImmortalFuel(stack)
                || stack != null && stack.getItem() instanceof com.cultivation.cultivation.item.custom.SpiritDriveItem;
    }

    public boolean canSmelt(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isFuel(stack)) return false;
        return blockEntity == null || blockEntity.isRecipeInput(stack);
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    public float getLitProgress() {
        int duration = data.get(1);
        if (duration <= 0) duration = ImmortalFurnaceEngine.TRUE_YUAN.burnTicks();
        return Mth.clamp((float) data.get(0) / duration, 0.0F, 1.0F);
    }

    public float getCookProgress(int channel) {
        if (channel < 0 || channel >= ImmortalFurnaceEngine.CHANNEL_COUNT) return 0.0F;
        int progress = data.get(2 + channel * 2);
        int total = data.get(3 + channel * 2);
        return total > 0 ? Mth.clamp((float) progress / total, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(moving, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
            if (isResultMenuSlot(index)) slot.onQuickCraft(moving, original);
        } else if (isFuel(moving)) {
            if (!moveItemStackTo(moving, 1, 2, false)) return ItemStack.EMPTY;
        } else if (canSmelt(moving)) {
            if (!moveIntoInputSlots(moving)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(moving, PLAYER_INVENTORY_END, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, PLAYER_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    private boolean moveIntoInputSlots(ItemStack stack) {
        boolean moved = false;
        for (int menuSlot : new int[] {0, 3, 5}) {
            int before = stack.getCount();
            moveItemStackTo(stack, menuSlot, menuSlot + 1, false);
            moved |= stack.getCount() != before;
            if (stack.isEmpty()) break;
        }
        return moved;
    }

    private static boolean isResultMenuSlot(int index) {
        return index == 2 || index == 4 || index == 6;
    }

    private final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canSmelt(stack);
        }
    }

    private final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }
}
