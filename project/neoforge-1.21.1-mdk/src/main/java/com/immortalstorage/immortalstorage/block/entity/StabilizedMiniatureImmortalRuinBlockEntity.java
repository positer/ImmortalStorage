package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;

/** 54-slot area collector/ejector with bounded offsets and persistent settings. */
public final class StabilizedMiniatureImmortalRuinBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 54;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private int sizeX = 1, sizeY = 1, sizeZ = 1;
    private int offsetX, offsetY, offsetZ;
    private int frequency = 20;
    private boolean preview;
    private boolean enabled;
    private boolean reversed;

    public StabilizedMiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    public ItemStackHandler itemHandler() { return inventory; }
    public boolean previewEnabled() { return preview; }
    public boolean reversed() { return reversed; }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public int offsetZ() { return offsetZ; }
    public int frequency() { return frequency; }
    public boolean enabled() { return enabled; }

    public void setMenuValue(int index, int value) { menuData().set(index, value); }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return switch (index) {
                case 0 -> sizeX; case 1 -> sizeY; case 2 -> sizeZ;
                case 3 -> offsetX; case 4 -> offsetY; case 5 -> offsetZ;
                case 6 -> frequency; case 7 -> preview ? 1 : 0; case 8 -> enabled ? 1 : 0;
                case 9 -> reversed ? 1 : 0; default -> 0;
            }; }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> sizeX = clamp(value, 1, 13); case 1 -> sizeY = clamp(value, 1, 13); case 2 -> sizeZ = clamp(value, 1, 13);
                    case 3 -> offsetX = clamp(value, -13, 13); case 4 -> offsetY = clamp(value, -13, 13); case 5 -> offsetZ = clamp(value, -13, 13);
                    case 6 -> frequency = clamp(value, 1, 72_000); case 7 -> preview = value != 0; case 8 -> enabled = value != 0;
                    default -> { }
                }
                if (reversed) sizeX = sizeY = sizeZ = 1;
                setChangedAndSync();
            }
            @Override public int getCount() { return 10; }
        };
    }

    public void toggleReversed() {
        reversed = !reversed;
        if (reversed) sizeX = sizeY = sizeZ = 1;
        setChangedAndSync();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel) || !enabled || frequency <= 0
                || serverLevel.getGameTime() % frequency != 0) return;
        if (reversed) eject(serverLevel); else collect(serverLevel);
    }

    private AABB selectedArea() {
        BlockPos min = worldPosition.offset(offsetX, offsetY, offsetZ);
        BlockPos max = min.offset(sizeX, sizeY, sizeZ);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    private void collect(ServerLevel level) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, selectedArea(), ItemEntity::isAlive)) {
            ItemStack remaining = entity.getItem().copy();
            for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
            }
            entity.setItem(remaining);
            if (remaining.isEmpty()) entity.discard();
        }
    }

    private void eject(ServerLevel level) {
        BlockPos target = worldPosition.offset(offsetX, offsetY, offsetZ);
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (stored.isEmpty()) continue;
            ItemStack removed = inventory.extractItem(slot, stored.getCount(), false);
            if (handler != null) {
                for (int targetSlot = 0; targetSlot < handler.getSlots() && !removed.isEmpty(); targetSlot++) {
                    removed = handler.insertItem(targetSlot, removed, false);
                }
            } else if (level.getBlockState(target).isAir()) {
                level.addFreshEntity(new ItemEntity(level, target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, removed));
                removed = ItemStack.EMPTY;
            }
            if (!removed.isEmpty()) inventory.insertItem(slot, removed, false);
            break;
        }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.immortalstorage.stabilized_miniature_immortal_ruin"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu(
                id, playerInventory, this, menuData());
    }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { for (int i = 0; i < SLOT_COUNT; i++) if (!inventory.getStackInSlot(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return inventory.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return inventory.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = inventory.getStackInSlot(slot); inventory.setStackInSlot(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, ItemStack.EMPTY); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("SizeX", sizeX); tag.putInt("SizeY", sizeY); tag.putInt("SizeZ", sizeZ);
        tag.putInt("OffsetX", offsetX); tag.putInt("OffsetY", offsetY); tag.putInt("OffsetZ", offsetZ);
        tag.putInt("Frequency", frequency); tag.putBoolean("Preview", preview); tag.putBoolean("Enabled", enabled);
        tag.putBoolean("Reversed", reversed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        sizeX = clamp(tag.getInt("SizeX"), 1, 13); sizeY = clamp(tag.getInt("SizeY"), 1, 13); sizeZ = clamp(tag.getInt("SizeZ"), 1, 13);
        offsetX = clamp(tag.getInt("OffsetX"), -13, 13); offsetY = clamp(tag.getInt("OffsetY"), -13, 13); offsetZ = clamp(tag.getInt("OffsetZ"), -13, 13);
        frequency = clamp(tag.getInt("Frequency"), 1, 72_000); preview = tag.getBoolean("Preview"); enabled = tag.getBoolean("Enabled");
        reversed = tag.getBoolean("Reversed");
        if (reversed) sizeX = sizeY = sizeZ = 1;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        tag.remove("Inventory");
        return tag;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
