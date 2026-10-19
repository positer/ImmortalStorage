package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.compressor.SpiritCompressorRecipes;
import com.immortalstorage.immortalstorage.compressor.SpiritCompressorStructure;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import java.util.UUID;

/** Inventory and configuration belong to the existing ruin controller, never to a transient multiblock. */
public final class SpiritCompressorLogic implements RuinMachine {
    public static final int CLOSED = 0, PASSIVE_PULL = 1, PASSIVE_PUSH = 2, ACTIVE_PULL = 3, ACTIVE_PUSH = 4;
    private final MiniatureImmortalRuinBlockEntity controller;
    private final int[] faces = new int[6];
    private UUID owner;
    private boolean xianqiao;
    private boolean lastFormation;
    public final ItemStackHandler inventory = new ItemStackHandler(13) {
        @Override public int getSlotLimit(int slot) { return slot == 12 ? Integer.MAX_VALUE : 64; }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == 12 ? Integer.MAX_VALUE : super.getStackLimit(slot, stack); }
        @Override protected void onContentsChanged(int slot) { controller.setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot < 12 && SpiritCompressorRecipes.accepts(controller.getLevel(), stack);
        }
    };
    public final ItemStackHandler decompressionInventory = new ItemStackHandler(13) {
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 64 : Integer.MAX_VALUE; }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == 0 ? super.getStackLimit(slot, stack) : Integer.MAX_VALUE; }
        @Override protected void onContentsChanged(int slot) { controller.setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && SpiritCompressorRecipes.accepts(controller.getLevel(), stack, true);
        }
    };
    public void ejectCaches(ServerLevel level) {
        com.immortalstorage.immortalstorage.compressor.SpiritCompressorCacheEjection.eject(
                java.util.List.of(inventory, decompressionInventory), stack -> {
                    var pos = controller.getBlockPos();
                    var dropped = new net.minecraft.world.entity.item.ItemEntity(level,
                            pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, stack);
                    dropped.setDefaultPickUpDelay();
                    return level.addFreshEntity(dropped);
                });
        controller.setChanged();
    }
    public boolean reversed() { return controller.getBlockState().getValue(com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock.REVERSED); }
    public ItemStackHandler activeInventory() { return reversed() ? decompressionInventory : inventory; }
    public int inputEnd() { return reversed() ? 1 : 12; }
    public int outputStart() { return reversed() ? 1 : 12; }
    public SpiritCompressorLogic(MiniatureImmortalRuinBlockEntity controller) { this.controller = controller; }
    public boolean formed() { return !controller.isRemoved() && SpiritCompressorStructure.matches(controller.getLevel(), controller.getBlockPos(),
            com.immortalstorage.immortalstorage.compressor.RuinMachineKind.SPIRIT); }
    public void updateFormation() {
        boolean current = formed();
        if (current != lastFormation) { lastFormation = current; invalidatePorts(); }
    }
    public void invalidatePorts() {
        var level = controller.getLevel(); if (level == null || level.isClientSide) return;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            var pos = controller.getBlockPos().offset(x, y, z);
            if (level.hasChunkAt(pos)) level.invalidateCapabilities(pos);
        }
    }
    public void placedBy(UUID id) { if (id != null) { owner = id; controller.setChanged(); } }
    public UUID owner() { return owner; }
    /** The spirit machine spends its materials in the same tick that it crafts; nothing is ever deferred. */
    @Override public void settlePending() {}
    @Override public void discardPending() {}
    @Override public boolean inputsLocked() { return false; }
    @Override public String titleKey() { return com.immortalstorage.immortalstorage.compressor.RuinMachineKind.SPIRIT.titleKey(reversed()); }
    public int mode(int side) { return faces[side]; }
    public boolean xianqiaoOutput() { return xianqiao; }
    public void cycle(int side) { faces[side] = (faces[side] + 1) % 5; controller.setChanged(); }
    public void toggleXianqiao() { xianqiao = !xianqiao; controller.setChanged(); }

    public IItemHandler handler(Direction side) {
        return new IItemHandler() {
            private boolean pull() { return side != null && formed() && (faces[side.ordinal()] == PASSIVE_PULL || faces[side.ordinal()] == ACTIVE_PULL); }
            private boolean push() { return !xianqiao && side != null && formed() && (faces[side.ordinal()] == PASSIVE_PUSH || faces[side.ordinal()] == ACTIVE_PUSH); }
            @Override public int getSlots() { return 13; }
            @Override public ItemStack getStackInSlot(int slot) { return activeInventory().getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return activeInventory().getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return pull() && activeInventory().isItemValid(slot, stack); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return pull() && slot < inputEnd() ? activeInventory().insertItem(slot, stack, simulate) : stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return push() && slot >= outputStart() && slot < 13 ? activeInventory().extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        };
    }

    public void tick(ServerLevel level) {
        if (!formed()) return;
        ItemStackHandler inventory = activeInventory();
        if (xianqiao) { flush(level); for (int i = outputStart(); i < 13; i++) if (!inventory.getStackInSlot(i).isEmpty()) return; }
        for (Direction side : Direction.values()) if (faces[side.ordinal()] == ACTIVE_PULL || faces[side.ordinal()] == ACTIVE_PUSH) {
            // Ports are one block beyond the 3x3x3 envelope, including the two vertical caps.
            var pos = controller.getBlockPos().relative(side, 2);
            if (!level.hasChunkAt(pos)) continue;
            IItemHandler neighbor = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side.getOpposite());
            if (neighbor == null) continue;
            int budget = 64;
            if (faces[side.ordinal()] == ACTIVE_PULL) {
                for (int slot = 0; slot < neighbor.getSlots() && budget > 0; slot++) {
                    ItemStack offered = neighbor.extractItem(slot, budget, true);
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(inventory, offered, true);
                    int amount = offered.getCount() - rest.getCount();
                    if (amount <= 0) continue;
                    ItemStack taken = neighbor.extractItem(slot, amount, false);
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, taken, false);
                    budget -= taken.getCount() - remainder.getCount();
                    if (!remainder.isEmpty()) {
                        ItemStack returned = neighbor.insertItem(slot, remainder, false);
                        if (!returned.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, controller.getBlockPos(), returned);
                    }
                }
            } else if (!xianqiao) {
                for (int slot = outputStart(); slot < 13 && budget > 0; slot++) {
                    ItemStack offered = inventory.extractItem(slot, budget, true);
                    int accepted = offered.getCount() - ItemHandlerHelper.insertItemStacked(neighbor, offered, true).getCount();
                    if (accepted > 0) {
                        ItemStack taken = inventory.extractItem(slot, accepted, false);
                        ItemStack rest = ItemHandlerHelper.insertItemStacked(neighbor, taken, false);
                        budget -= taken.getCount() - rest.getCount();
                        if (!rest.isEmpty()) {
                            ItemStack stored = inventory.getStackInSlot(slot).copy();
                            if (stored.isEmpty()) stored = rest; else stored.grow(rest.getCount());
                            inventory.setStackInSlot(slot, stored);
                        }
                    }
                }
            }
        }
        var recipes = SpiritCompressorRecipes.entries(level, reversed()).stream()
                .sorted(java.util.Comparator.comparingLong(SpiritCompressorRecipes.Entry::totalInputCount).reversed()
                        .thenComparing(entry -> entry.id().toString())).toList();
        var endpoint = xianqiao ? PersonalStorageNetwork.resolveXianqiao(level.getServer(), owner, controller::setChanged) : null;
        for (var entry : recipes) {
            int[] stock = new int[inputEnd()];
            for (int i = 0; i < stock.length; i++) stock[i] = inventory.getStackInSlot(i).getCount();
            var requirements = entry.inputs().stream().map(input ->
                    new com.immortalstorage.immortalstorage.compressor.SpiritCompressionInputs.Requirement(input.count(),
                            slot -> input.ingredient().test(inventory.getStackInSlot(slot)))).toList();
            var materials = com.immortalstorage.immortalstorage.compressor.SpiritCompressionInputs.maximum(stock, requirements);
            if (materials == null) continue;
            long multiplier = materials.multiplier();
            if (xianqiao) {
                if (endpoint == null || endpoint.itemStorage() == null) return;
                var amounts = new java.util.LinkedHashMap<com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey, Long>();
                for (var result : entry.outputs()) {
                    var key = com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey.of(result);
                    amounts.merge(key, multiplier * result.getCount(), Long::sum);
                }
                boolean fits = true;
                for (var amount : amounts.entrySet()) if (endpoint.itemStorage().insert(amount.getKey(), amount.getValue(),
                        com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.SIMULATE) != amount.getValue()) fits = false;
                if (!fits) continue;
                var inserted = new java.util.LinkedHashMap<com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey, Long>();
                for (var amount : amounts.entrySet()) {
                    long accepted = endpoint.itemStorage().insert(amount.getKey(), amount.getValue(),
                            com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.EXECUTE);
                    inserted.put(amount.getKey(), accepted);
                    if (accepted != amount.getValue()) { fits = false; break; }
                }
                if (!fits) {
                    for (var amount : inserted.entrySet()) if (amount.getValue() > 0) endpoint.itemStorage().extract(amount.getKey(), amount.getValue(),
                            com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.EXECUTE);
                    continue;
                }
            } else {
                var plan = com.immortalstorage.immortalstorage.compressor.SpiritCompressionOutputs.plan(inventory, outputStart(), 13, entry.outputs(), multiplier);
                if (plan == null) break;
                for (int i = 0; i < plan.size(); i++) inventory.setStackInSlot(outputStart() + i, plan.get(i));
            }
            for (int i = 0; i < materials.consumed().length; i++)
                if (materials.consumed()[i] > 0) inventory.extractItem(i, materials.consumed()[i], false);
        }
    }
    private void flush(ServerLevel level) {
        var endpoint = PersonalStorageNetwork.resolveXianqiao(level.getServer(), owner, controller::setChanged);
        MachineOutputScheduler.flushItemsToXianqiao(activeInventory(), outputStart(), 13, endpoint);
    }
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStackHandler saved = new ItemStackHandler(13);
        for (int i = 0; i < 13; i++) saved.setStackInSlot(i, i == 12 ? inventory.getStackInSlot(i).copyWithCount(1) : inventory.getStackInSlot(i).copy());
        tag.put("CompressorInventory", saved.serializeNBT(registries)); tag.putInt("CompressorOutputCount", inventory.getStackInSlot(12).getCount());
        ItemStackHandler reverseSaved = new ItemStackHandler(13);
        int[] counts = new int[12];
        for (int i = 0; i < 13; i++) {
            var stack = decompressionInventory.getStackInSlot(i);
            reverseSaved.setStackInSlot(i, i == 0 ? stack.copy() : stack.copyWithCount(1));
            if (i > 0) counts[i - 1] = stack.getCount();
        }
        tag.put("DecompressorInventory", reverseSaved.serializeNBT(registries)); tag.putIntArray("DecompressorOutputCounts", counts);
        tag.putIntArray("CompressorFaces", faces);
        tag.putBoolean("CompressorXianqiao", xianqiao); if (owner != null) tag.putUUID("CompressorOwner", owner);
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("CompressorInventory")) inventory.deserializeNBT(registries, tag.getCompound("CompressorInventory"));
        if (!inventory.getStackInSlot(12).isEmpty() && tag.contains("CompressorOutputCount")) inventory.setStackInSlot(12,
                inventory.getStackInSlot(12).copyWithCount(Math.max(0, tag.getInt("CompressorOutputCount"))));
        if (tag.contains("DecompressorInventory")) decompressionInventory.deserializeNBT(registries, tag.getCompound("DecompressorInventory"));
        int[] outputCounts = tag.getIntArray("DecompressorOutputCounts");
        for (int i = 1; i < 13; i++) if (!decompressionInventory.getStackInSlot(i).isEmpty() && i - 1 < outputCounts.length)
            decompressionInventory.setStackInSlot(i, decompressionInventory.getStackInSlot(i).copyWithCount(Math.max(0, outputCounts[i - 1])));
        int[] saved = tag.getIntArray("CompressorFaces"); for (int i = 0; i < 6; i++) faces[i] = i < saved.length ? Math.max(0, Math.min(4, saved[i])) : 0;
        xianqiao = tag.getBoolean("CompressorXianqiao"); owner = tag.hasUUID("CompressorOwner") ? tag.getUUID("CompressorOwner") : null;
    }
}
