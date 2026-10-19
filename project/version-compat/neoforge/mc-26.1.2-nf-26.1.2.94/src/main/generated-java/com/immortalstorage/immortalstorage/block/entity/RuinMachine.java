package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

/** The configuration, transfer and persistence contract shared by both miniature-ruin machines. */
public interface RuinMachine {
    int CLOSED = 0, PASSIVE_PULL = 1, PASSIVE_PUSH = 2, ACTIVE_PULL = 3, ACTIVE_PUSH = 4;

    boolean formed();
    boolean reversed();
    int mode(int side);
    void cycle(int side);
    boolean xianqiaoOutput();
    void toggleXianqiao();
    ItemStackHandler activeInventory();
    int inputEnd();
    int outputStart();
    IItemHandler handler(Direction side);
    void updateFormation();
    void invalidatePorts();
    void tick(ServerLevel level);
    void ejectCaches(ServerLevel level);
    /** Take path: pays a deferred material cost. The spirit machine pays as it crafts, so it does nothing here. */
    void settlePending();
    /** Teardown path: voids an uncollected product without paying for it. The spirit machine has nothing to void. */
    void discardPending();
    void placedBy(UUID id);
    UUID owner();
    /** True while a deferred transaction keeps the input slots from being emptied by hand. */
    boolean inputsLocked();
    /**
     * A slot is locked by the machine instance the caller holds, never by whatever the world reports now:
     * a kind change only discards on the core's next tick, and mid-menu that gap would otherwise unlock the
     * inputs while the product is still uncollected.
     */
    default boolean locksSlot(int slot) { return slot < inputEnd() && inputsLocked(); }
    String titleKey();
    void save(CompoundTag tag, HolderLookup.Provider registries);
    void load(CompoundTag tag, HolderLookup.Provider registries);
}
