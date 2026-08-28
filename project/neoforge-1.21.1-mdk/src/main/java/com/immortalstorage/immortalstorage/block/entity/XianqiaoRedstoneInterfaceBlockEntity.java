package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.custom.XianqiaoRedstoneInterfaceBlock;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoRedstoneInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class XianqiaoRedstoneInterfaceBlockEntity extends XianqiaoInterfaceBlockEntity implements MenuProvider {
    private long highThreshold = 1, lowThreshold = 0;
    private boolean inverted;
    public XianqiaoRedstoneInterfaceBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.XIANQIAO_REDSTONE_INTERFACE.get(), pos, state); }
    public long highThreshold() { return highThreshold; }
    public long lowThreshold() { return lowThreshold; }
    public boolean inverted() { return inverted; }
    public void setConfiguration(long low, long high, boolean inverted) {
        lowThreshold = Math.max(0, low);
        highThreshold = Math.max(lowThreshold, high);
        this.inverted = inverted;
        setChanged();
        if (level != null && !level.isClientSide) {
            evaluate(configuredOwnerStorageAmount(0));
        }
    }
    public void evaluate(long amount) {
        if (level == null) return;
        boolean active = getBlockState().getValue(XianqiaoRedstoneInterfaceBlock.ACTIVATED);
        if (!inverted) { if (!active && amount > highThreshold) active = true; else if (active && amount < lowThreshold) active = false; }
        else { if (!active && amount < lowThreshold) active = true; else if (active && amount > highThreshold) active = false; }
        if (active != getBlockState().getValue(XianqiaoRedstoneInterfaceBlock.ACTIVATED)) level.setBlock(worldPosition, getBlockState().setValue(XianqiaoRedstoneInterfaceBlock.ACTIVATED, active), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
    @Override public void serverTick() { if (level != null && level.getGameTime() % 5L == 0L) evaluate(configuredOwnerStorageAmount(0)); }
    @Override public Component getDisplayName() { return Component.translatable("container.immortalstorage.xianqiao_redstone_interface"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new XianqiaoRedstoneInterfaceMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putLong("LowThreshold", lowThreshold); tag.putLong("HighThreshold", highThreshold); tag.putBoolean("Inverted", inverted); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); lowThreshold = Math.max(0, tag.getLong("LowThreshold")); highThreshold = Math.max(lowThreshold, tag.getLong("HighThreshold")); inverted = tag.getBoolean("Inverted"); }
}
