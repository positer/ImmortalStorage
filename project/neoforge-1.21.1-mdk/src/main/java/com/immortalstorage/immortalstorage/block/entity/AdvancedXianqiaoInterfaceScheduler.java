package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Range scheduler for the advanced Xianqiao Interface. Reuses the plain
 * interface's six-face PULL/PUSH/DISABLED modes and the active pull/push
 * toggles, but applies them to every container inside the configurable box:
 * a PULL face extracts all interactive content (items/fluids/FE/chemicals) from
 * each in-area container; a PUSH face exports the interface cache slots that
 * permit that face into the in-area containers. Ordering by Manhattan distance,
 * poll-skip / force-poll access and item-by-item / group-by-group split match
 * the advanced stabilized miniature immortal ruin series.
 */
final class AdvancedXianqiaoInterfaceScheduler {
    private static final int MAX_SLOT_SCAN = 1_000;

    private AdvancedXianqiaoInterfaceScheduler() {}

    static void tick(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level) {
        for (Direction face : Direction.values()) {
            XianqiaoInterfaceBlockEntity.SideMode mode = be.getSideMode(face);
            if (mode == XianqiaoInterfaceBlockEntity.SideMode.PULL
                    && be.isActivePullEnabled()) {
                pullRange(be, level, face);
            } else if (mode == XianqiaoInterfaceBlockEntity.SideMode.PUSH
                    && be.isActivePushEnabled()) {
                pushRange(be, level, face);
            }
        }
    }

    private static List<BlockPos> positions(AdvancedXianqiaoInterfaceBlockEntity be) {
        List<BlockPos> positions = AdvancedRuinScheduler.enumerate(
                be.getBlockPos(), be.offsetX(), be.offsetY(), be.offsetZ(),
                be.sizeX(), be.sizeY(), be.sizeZ());
        positions.sort(Comparator.comparingInt(pos -> manhattan(be.getBlockPos(), pos)));
        if (be.orderMode() == AdvancedXianqiaoInterfaceBlockEntity.ORDER_FAR_FIRST) {
            java.util.Collections.reverse(positions);
        }
        return positions;
    }

    private static void pullRange(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                  Direction face) {
        boolean forcePoll = be.accessMode() == AdvancedXianqiaoInterfaceBlockEntity.ACCESS_FORCE_POLL;
        boolean itemByItem = be.splitMode() == AdvancedXianqiaoInterfaceBlockEntity.SPLIT_ITEM_BY_ITEM;
        for (BlockPos pos : positions(be)) {
            pullItems(be, level, pos, face, forcePoll, itemByItem);
            pullFluids(be, level, pos, face);
            pullEnergy(be, level, pos, face);
            XianqiaoInterfaceCompatHooks.scheduledChemicalTransfer(be, level, pos, face, true);
        }
    }

    private static void pullItems(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                  BlockPos pos, Direction face, boolean forcePoll, boolean itemByItem) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (handler == null) return;
        int slots = Math.min(handler.getSlots(), MAX_SLOT_SCAN);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stored = handler.getStackInSlot(slot);
            if (stored.isEmpty()) continue;
            int amount = itemByItem ? 1 : stored.getCount();
            ItemStack removed = handler.extractItem(slot, amount, false);
            if (removed.isEmpty()) continue;
            long accepted = be.getInventory().insertBulk(removed, removed.getCount(), false);
            long remainder = removed.getCount() - accepted;
            if (remainder > 0L) {
                handler.insertItem(slot, removed.copyWithCount((int) remainder), false);
            }
        }
    }

    private static void pullFluids(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                   BlockPos pos, Direction face) {
        IFluidHandler source = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, face);
        if (source == null) return;
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack prototype = source.getFluidInTank(tank);
            if (prototype.isEmpty()) continue;
            FluidStack simulated = source.drain(prototype.copyWithAmount(Integer.MAX_VALUE),
                    IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) continue;
            long accepted = be.getInventory().insertFluidBulk(simulated, simulated.getAmount(), true);
            if (accepted <= 0L) continue;
            FluidStack toExtract = accepted < simulated.getAmount()
                    ? simulated.copyWithAmount((int) accepted) : simulated;
            FluidStack extracted = source.drain(toExtract, IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) continue;
            be.getInventory().insertFluidBulk(extracted, extracted.getAmount(), false);
        }
    }

    private static void pullEnergy(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                   BlockPos pos, Direction face) {
        IEnergyStorage source = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, pos, face);
        if (source == null) return;
        AtomicEnergyRefill.ResourceStore ledger =
                be.resolveExternalResourceStore(ExternalResourceChannels.FE);
        if (ledger == null) return;
        int simulated = source.extractEnergy(Integer.MAX_VALUE, true);
        if (simulated <= 0) return;
        int accepted = (int) Math.min(simulated,
                ledger.insert(simulated, ResourceTransferAction.SIMULATE));
        if (accepted <= 0) return;
        int extracted = source.extractEnergy(accepted, false);
        if (extracted > 0) {
            ledger.insert(extracted, ResourceTransferAction.EXECUTE);
        }
    }

    private static void pushRange(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                  Direction face) {
        List<BlockPos> targets = positions(be);
        if (targets.isEmpty()) return;
        pushItems(be, level, targets, face);
        pushFluids(be, level, targets, face);
        pushEnergy(be, level, targets, face);
        for (BlockPos pos : targets) {
            XianqiaoInterfaceCompatHooks.scheduledChemicalTransfer(be, level, pos, face, false);
        }
    }

    private static void pushItems(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                  List<BlockPos> targets, Direction face) {
        XianqiaoInterfaceInventory inventory = be.getInventory();
        int[] cursor = be.groupCursor();
        for (int slot = 0; slot < XianqiaoInterfaceInventory.SLOT_COUNT; slot++) {
            if (!inventory.isOutputFaceEnabled(slot, face)) continue;
            ItemStack buffered = inventory.getBufferedStack(slot);
            if (buffered.isEmpty()) continue;
            ItemStack removed = inventory.extractItem(slot, buffered.getCount(), false);
            if (removed.isEmpty()) continue;
            ItemStack remaining = distributeItem(removed, level, targets, face, cursor);
            if (!remaining.isEmpty()) {
                ItemStack leftover = inventory.restoreExtractedItem(slot, remaining);
                if (!leftover.isEmpty()) {
                    inventory.insertBulk(leftover, leftover.getCount(), false);
                }
            }
        }
    }

    private static ItemStack distributeItem(ItemStack stack, ServerLevel level,
                                            List<BlockPos> targets, Direction face, int[] cursor) {
        ItemStack remaining = stack.copy();
        if (targets.isEmpty()) return remaining;
        int count = targets.size();
        while (!remaining.isEmpty()) {
            boolean anyAccepted = false;
            for (int i = 0; i < count && !remaining.isEmpty(); i++) {
                BlockPos pos = targets.get(cursor[0] % count);
                cursor[0] = (cursor[0] + 1) % count;
                IItemHandler handler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK, pos, face);
                if (handler == null) continue;
                ItemStack one = remaining.copyWithCount(1);
                ItemStack leftover = handler.insertItem(0, one, false);
                if (leftover.isEmpty()) {
                    remaining.shrink(1);
                    anyAccepted = true;
                }
            }
            if (!anyAccepted) return remaining;
        }
        return remaining;
    }

    private static void pushFluids(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                   List<BlockPos> targets, Direction face) {
        XianqiaoInterfaceInventory inventory = be.getInventory();
        for (int slot = 0; slot < XianqiaoInterfaceInventory.SLOT_COUNT; slot++) {
            if (!inventory.isOutputFaceEnabled(slot, face)) continue;
            FluidStack buffered = inventory.getBufferedFluid(slot);
            if (buffered.isEmpty()) continue;
            int amount = (int) Math.min(Integer.MAX_VALUE, buffered.getAmount());
            FluidStack removed = inventory.drainFluidFromSlot(slot, amount, false);
            if (removed.isEmpty()) continue;
            FluidStack remaining = distributeFluid(removed, level, targets, face);
            if (!remaining.isEmpty()) {
                FluidStack leftover = inventory.restoreExtractedFluid(slot, remaining);
                if (!leftover.isEmpty()) {
                    inventory.insertFluidBulk(leftover, leftover.getAmount(), false);
                }
            }
        }
    }

    private static FluidStack distributeFluid(FluidStack stack, ServerLevel level,
                                              List<BlockPos> targets, Direction face) {
        FluidStack remaining = stack.copy();
        for (BlockPos pos : targets) {
            if (remaining.isEmpty()) break;
            IFluidHandler target = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, pos, face);
            if (target == null) continue;
            int accepted = target.fill(remaining, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            int committed = target.fill(remaining.copyWithAmount(accepted),
                    IFluidHandler.FluidAction.EXECUTE);
            remaining.shrink(committed);
        }
        return remaining;
    }

    private static void pushEnergy(AdvancedXianqiaoInterfaceBlockEntity be, ServerLevel level,
                                   List<BlockPos> targets, Direction face) {
        AtomicEnergyRefill.ResourceStore cache =
                be.resolveExternalResourceCache(ExternalResourceChannels.FE, face);
        if (cache == null) return;
        long available = cache.amount();
        if (available <= 0L) return;
        long remaining = available;
        for (BlockPos pos : targets) {
            if (remaining <= 0L) break;
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK, pos, face);
            if (target == null || !target.canReceive()) continue;
            int toSend = (int) Math.min(Integer.MAX_VALUE, remaining);
            long extracted = cache.extract(toSend, ResourceTransferAction.SIMULATE);
            if (extracted <= 0L) continue;
            int committed = target.receiveEnergy((int) extracted, false);
            if (committed > 0) {
                cache.extract(committed, ResourceTransferAction.EXECUTE);
                remaining -= committed;
            }
        }
    }

    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }
}
