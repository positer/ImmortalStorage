package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.RedstoneWorkMode;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Persistent redstone AND-gate shared by every tagged machine block entity. */
public final class MachineRedstoneControl {
    private static final String KEY = "ImmortalStorageRedstoneMode";
    public static RedstoneWorkMode mode(BlockEntity be) { if (!be.getPersistentData().contains(KEY)) return RedstoneWorkMode.IGNORE; int id = be.getPersistentData().getIntOr(KEY, 0); return RedstoneWorkMode.values()[Math.max(0, Math.min(RedstoneWorkMode.values().length - 1, id))]; }
    public static void set(BlockEntity be, RedstoneWorkMode mode) { be.getPersistentData().putInt(KEY, mode.ordinal()); be.setChanged(); }
    public static RedstoneWorkMode cycle(BlockEntity be) { RedstoneWorkMode next = mode(be).next(); set(be, next); return next; }
    public static boolean allows(BlockEntity be) { return be.getLevel() != null && mode(be).allows(be.getLevel(), be.getBlockPos()); }
    public static boolean allows(BlockEntity be, boolean internalSwitch) { return internalSwitch && allows(be); }
    private MachineRedstoneControl() {}
}
