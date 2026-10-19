package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.compressor.DeferredEnchantmentTransaction;
import com.immortalstorage.immortalstorage.compressor.EnchantmentCompressionPlan;
import com.immortalstorage.immortalstorage.compressor.EnchantmentDecompressionPlan;
import com.immortalstorage.immortalstorage.compressor.EnchantmentStackRules;
import com.immortalstorage.immortalstorage.compressor.RuinMachineKind;
import com.immortalstorage.immortalstorage.compressor.SpiritCompressorCacheEjection;
import com.immortalstorage.immortalstorage.compressor.SpiritCompressorStructure;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

/**
 * The enchantment machine keeps the ruin controller, the six-face contract and the Xianqiao path,
 * but it produces the product first and registers the material cost (design section 4): the inputs
 * are consumed, and the stripped ones cleared, only when the product actually leaves the machine.
 */
public final class EnchantmentMachineLogic implements RuinMachine {
    private static final int SLOTS = 13;
    private final MiniatureImmortalRuinBlockEntity controller;
    private final DeferredEnchantmentTransaction transaction = new DeferredEnchantmentTransaction();
    private final int[] faces = new int[6];
    private UUID owner;
    private boolean xianqiao;
    private boolean lastFormation;

    public final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override public int getSlotLimit(int slot) { return slot == EnchantmentCompressionPlan.OUTPUT_SLOT ? Integer.MAX_VALUE : 64; }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == EnchantmentCompressionPlan.OUTPUT_SLOT ? Integer.MAX_VALUE : super.getStackLimit(slot, stack); }
        @Override protected void onContentsChanged(int slot) { controller.setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot < EnchantmentCompressionPlan.INPUT_SLOTS && EnchantmentStackRules.acceptsCarrier(stack);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) transaction.afterExtract(this, slot, EnchantmentCompressionPlan.OUTPUT_SLOT);
            return extracted;
        }
    };
    public final ItemStackHandler decompressionInventory = new ItemStackHandler(SLOTS) {
        @Override public int getSlotLimit(int slot) { return slot == EnchantmentDecompressionPlan.INPUT_SLOT ? 64 : Integer.MAX_VALUE; }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return slot == EnchantmentDecompressionPlan.INPUT_SLOT ? super.getStackLimit(slot, stack) : Integer.MAX_VALUE; }
        @Override protected void onContentsChanged(int slot) { controller.setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == EnchantmentDecompressionPlan.INPUT_SLOT && !EnchantmentStackRules.enchantments(stack).isEmpty();
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) transaction.afterExtract(this, slot, EnchantmentDecompressionPlan.OUTPUT_START);
            return extracted;
        }
    };

    public EnchantmentMachineLogic(MiniatureImmortalRuinBlockEntity controller) { this.controller = controller; }

    @Override public boolean formed() {
        return !controller.isRemoved()
                && SpiritCompressorStructure.matches(controller.getLevel(), controller.getBlockPos(), RuinMachineKind.ENCHANTMENT);
    }

    @Override public void updateFormation() {
        boolean current = formed();
        if (current == lastFormation) return;
        lastFormation = current;
        invalidatePorts();
        // v1.2: a structure that comes apart voids the uncollected product and leaves the material alone.
        if (!current) discardPending();
    }

    @Override public void invalidatePorts() {
        var level = controller.getLevel(); if (level == null || level.isClientSide) return;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            var pos = controller.getBlockPos().offset(x, y, z);
            if (level.hasChunkAt(pos)) level.invalidateCapabilities(pos);
        }
    }

    @Override public boolean reversed() { return controller.getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED); }
    @Override public ItemStackHandler activeInventory() { return reversed() ? decompressionInventory : inventory; }
    @Override public int inputEnd() { return reversed() ? 1 : EnchantmentCompressionPlan.INPUT_SLOTS; }
    @Override public int outputStart() { return reversed() ? EnchantmentDecompressionPlan.OUTPUT_START : EnchantmentCompressionPlan.OUTPUT_SLOT; }
    @Override public int mode(int side) { return faces[side]; }
    @Override public boolean xianqiaoOutput() { return xianqiao; }
    @Override public void cycle(int side) { faces[side] = (faces[side] + 1) % 5; controller.setChanged(); }
    @Override public void toggleXianqiao() { xianqiao = !xianqiao; controller.setChanged(); }
    @Override public void placedBy(UUID id) { if (id != null) { owner = id; controller.setChanged(); } }

    /** A kind switch inherits the owner the ruin already recorded. */
    @Override public UUID owner() { return owner != null ? owner : controller.compressor().owner(); }

    @Override public boolean inputsLocked() { return transaction.pending(); }
    @Override public String titleKey() { return RuinMachineKind.ENCHANTMENT.titleKey(reversed()); }
    @Override public void settlePending() { transaction.settle(activeInventory()); }
    /** Teardown rule: the uncollected product is deleted, never dropped, and the material stays in its slots. */
    @Override public void discardPending() { transaction.discard(activeInventory(), outputStart()); }

    @Override public void ejectCaches(ServerLevel level) {
        discardPending();
        SpiritCompressorCacheEjection.eject(List.of(inventory, decompressionInventory), stack -> {
            var pos = controller.getBlockPos();
            var dropped = new net.minecraft.world.entity.item.ItemEntity(level,
                    pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, stack);
            dropped.setDefaultPickUpDelay();
            return level.addFreshEntity(dropped);
        });
        controller.setChanged();
    }

    @Override public IItemHandler handler(Direction side) {
        return new IItemHandler() {
            private boolean pull() { return side != null && formed() && (faces[side.ordinal()] == PASSIVE_PULL || faces[side.ordinal()] == ACTIVE_PULL); }
            private boolean push() { return !xianqiao && side != null && formed() && (faces[side.ordinal()] == PASSIVE_PUSH || faces[side.ordinal()] == ACTIVE_PUSH); }
            @Override public int getSlots() { return SLOTS; }
            @Override public ItemStack getStackInSlot(int slot) { return activeInventory().getStackInSlot(slot); }
            @Override public int getSlotLimit(int slot) { return activeInventory().getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return pull() && activeInventory().isItemValid(slot, stack); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return pull() && slot < inputEnd() ? activeInventory().insertItem(slot, stack, simulate) : stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return push() && slot >= outputStart() && slot < SLOTS ? activeInventory().extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        };
    }

    @Override public void tick(ServerLevel level) {
        if (!formed()) return;
        ItemStackHandler inventory = activeInventory();
        if (xianqiao) {
            flush(level);
            if (hasStoredOutput(inventory)) return;
        }
        transferActivePorts(level, inventory);
        runCycle();
        // A direct Xianqiao write counts as the product being collected, in the same tick.
        if (xianqiao && transaction.pending()) flush(level);
    }

    /** A stored product blocks the next cycle until it has been collected. */
    private boolean hasStoredOutput(ItemStackHandler inventory) {
        for (int slot = outputStart(); slot < SLOTS && slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** One production attempt; it needs an unsettled product collected and the whole output row empty. */
    void runCycle() {
        if (transaction.pending()) return;
        ItemStackHandler inventory = activeInventory();
        if (hasStoredOutput(inventory)) return;
        if (reversed()) decompose(inventory); else compose(inventory);
    }

    // Same active-transfer loop as SpiritCompressorLogic, kept local so the spirit machine stays frozen.
    private void transferActivePorts(ServerLevel level, ItemStackHandler inventory) {
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
                for (int slot = outputStart(); slot < SLOTS && budget > 0; slot++) {
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
    }

    private void compose(ItemStackHandler inventory) {
        ItemStack[] slots = new ItemStack[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++) slots[slot] = inventory.getStackInSlot(slot);
        EnchantmentCompressionPlan.Result plan = EnchantmentCompressionPlan.plan(slots);
        if (plan == null || !accepts(inventory, EnchantmentCompressionPlan.OUTPUT_SLOT, plan.output())) return;
        inventory.setStackInSlot(EnchantmentCompressionPlan.OUTPUT_SLOT, plan.output());
        transaction.arm(plan.consume(), plan.strip());
        controller.setChanged();
    }

    private void decompose(ItemStackHandler inventory) {
        EnchantmentDecompressionPlan.Result plan = EnchantmentDecompressionPlan.plan(
                inventory.getStackInSlot(EnchantmentDecompressionPlan.INPUT_SLOT));
        if (plan == null) return;
        for (int index = 0; index < plan.outputs().size(); index++) {
            ItemStack output = plan.outputs().get(index);
            if (!output.isEmpty() && !accepts(inventory, EnchantmentDecompressionPlan.OUTPUT_START + index, output)) return;
        }
        for (int index = 0; index < plan.outputs().size(); index++) {
            if (plan.outputs().get(index).isEmpty()) continue;
            inventory.setStackInSlot(EnchantmentDecompressionPlan.OUTPUT_START + index, plan.outputs().get(index));
        }
        transaction.arm(plan.consume(), plan.strip());
        controller.setChanged();
    }

    private static boolean accepts(ItemStackHandler inventory, int slot, ItemStack stack) {
        ItemStack stored = inventory.getStackInSlot(slot);
        return stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, stack)
                && stored.getCount() + stack.getCount() <= inventory.getSlotLimit(slot);
    }

    private void flush(ServerLevel level) {
        var endpoint = PersonalStorageNetwork.resolveXianqiao(level.getServer(), owner(), controller::setChanged);
        MachineOutputScheduler.flushItemsToXianqiao(activeInventory(), outputStart(), SLOTS, endpoint);
    }

    @Override public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStackHandler saved = new ItemStackHandler(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) saved.setStackInSlot(slot, slot == EnchantmentCompressionPlan.OUTPUT_SLOT
                ? inventory.getStackInSlot(slot).copyWithCount(1) : inventory.getStackInSlot(slot).copy());
        tag.put("EnchantMachineInventory", saved.serializeNBT(registries));
        tag.putInt("EnchantMachineOutputCount", inventory.getStackInSlot(EnchantmentCompressionPlan.OUTPUT_SLOT).getCount());
        ItemStackHandler reverseSaved = new ItemStackHandler(SLOTS);
        int[] counts = new int[EnchantmentDecompressionPlan.OUTPUT_SLOTS];
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = decompressionInventory.getStackInSlot(slot);
            reverseSaved.setStackInSlot(slot, slot == EnchantmentDecompressionPlan.INPUT_SLOT ? stack.copy() : stack.copyWithCount(1));
            if (slot >= EnchantmentDecompressionPlan.OUTPUT_START) counts[slot - EnchantmentDecompressionPlan.OUTPUT_START] = stack.getCount();
        }
        tag.put("EnchantMachineDecompressorInventory", reverseSaved.serializeNBT(registries));
        tag.putIntArray("EnchantMachineDecompressorOutputCounts", counts);
        tag.putIntArray("EnchantMachineFaces", faces);
        tag.putBoolean("EnchantMachineXianqiao", xianqiao);
        if (owner != null) tag.putUUID("EnchantMachineOwner", owner);
        if (transaction.pending()) {
            tag.putIntArray("EnchantMachinePendingConsume", transaction.consume());
            tag.putByteArray("EnchantMachinePendingStrip", pack(transaction.strip()));
        }
    }

    @Override public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("EnchantMachineInventory")) inventory.deserializeNBT(registries, tag.getCompound("EnchantMachineInventory"));
        if (!inventory.getStackInSlot(EnchantmentCompressionPlan.OUTPUT_SLOT).isEmpty() && tag.contains("EnchantMachineOutputCount"))
            inventory.setStackInSlot(EnchantmentCompressionPlan.OUTPUT_SLOT, inventory.getStackInSlot(EnchantmentCompressionPlan.OUTPUT_SLOT)
                    .copyWithCount(Math.max(0, tag.getInt("EnchantMachineOutputCount"))));
        if (tag.contains("EnchantMachineDecompressorInventory")) decompressionInventory.deserializeNBT(registries, tag.getCompound("EnchantMachineDecompressorInventory"));
        int[] counts = tag.getIntArray("EnchantMachineDecompressorOutputCounts");
        for (int slot = EnchantmentDecompressionPlan.OUTPUT_START; slot < SLOTS; slot++)
            if (!decompressionInventory.getStackInSlot(slot).isEmpty() && slot - EnchantmentDecompressionPlan.OUTPUT_START < counts.length)
                decompressionInventory.setStackInSlot(slot, decompressionInventory.getStackInSlot(slot)
                        .copyWithCount(Math.max(0, counts[slot - EnchantmentDecompressionPlan.OUTPUT_START])));
        int[] saved = tag.getIntArray("EnchantMachineFaces");
        for (int i = 0; i < 6; i++) faces[i] = i < saved.length ? Math.max(0, Math.min(4, saved[i])) : 0;
        xianqiao = tag.getBoolean("EnchantMachineXianqiao");
        owner = tag.hasUUID("EnchantMachineOwner") ? tag.getUUID("EnchantMachineOwner") : null;
        if (tag.contains("EnchantMachinePendingConsume")) {
            int[] consume = tag.getIntArray("EnchantMachinePendingConsume");
            transaction.restore(consume, unpack(tag.getByteArray("EnchantMachinePendingStrip"), consume.length));
        } else transaction.clear();
    }

    private static byte[] pack(boolean[] values) {
        byte[] packed = new byte[values.length];
        for (int i = 0; i < values.length; i++) packed[i] = (byte) (values[i] ? 1 : 0);
        return packed;
    }

    private static boolean[] unpack(byte[] packed, int length) {
        boolean[] values = new boolean[length];
        for (int i = 0; i < length && i < packed.length; i++) values[i] = packed[i] != 0;
        return values;
    }
}
