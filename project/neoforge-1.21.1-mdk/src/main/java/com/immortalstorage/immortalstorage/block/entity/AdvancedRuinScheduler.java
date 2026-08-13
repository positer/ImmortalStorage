package com.immortalstorage.immortalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Container-dispatch scheduler for the advanced stabilized ruins. Operates over
 * every item handler inside a bounded range, ordered by Manhattan distance from
 * the machine (then position), with poll-skip / force-poll access logic and
 * item-by-item / group-by-group equal split. The ruins are independent
 * containers: normal mode pulls one allowed stack per container into the ruin's
 * own inventory; reversed mode pushes the ruin's own inventory into the
 * containers. Never drops items into the world; unaccepted leftovers stay in
 * the ruin's own inventory.
 */
final class AdvancedRuinScheduler {
    private static final int MAX_SLOT_SCAN = 1_000;

    private AdvancedRuinScheduler() {}

    record Target(BlockPos pos, @Nullable IItemHandler handler) {}

    /** All six bits set: one bit per {@link Direction} ordinal (DOWN=0 .. EAST=5). */
    static final int ALL_FACES = 0x3F;

    /**
     * Enumerates the operation-area positions, one {@link Direction} ordinal bit
     * set per enabled interaction face ({@code faceMask}). Scans only positions
     * strictly inside the configured box ({@code origin+offset} to
     * {@code origin+offset+size}, exclusive upper bound) so it matches the
     * preview outline exactly and never reaches adjacent containers outside the
     * operation area. Each enabled face yields its own {@link Target} so sided
     * containers are addressed through the correct context.
     */
    static List<Target> scan(ServerLevel level, BlockPos origin, int offsetX, int offsetY, int offsetZ,
                             int sizeX, int sizeY, int sizeZ, boolean farFirst, int faceMask) {
        List<Target> targets = new ArrayList<>();
        for (BlockPos pos : enumerate(origin, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ)) {
            for (Direction face : enabledFaces(faceMask)) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
                targets.add(new Target(pos, handler));
            }
        }
        return sort(targets, origin, farFirst);
    }

    /** Pure position enumeration inside the operation box, exclusive upper bound, origin excluded. */
    static List<BlockPos> enumerate(BlockPos origin, int offsetX, int offsetY, int offsetZ,
                                    int sizeX, int sizeY, int sizeZ) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    BlockPos pos = origin.offset(offsetX + dx, offsetY + dy, offsetZ + dz);
                    if (pos.equals(origin)) continue;
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    /** Pure mask decoding: the enabled {@link Direction}s in {@link Direction} ordinal order. */
    static List<Direction> enabledFaces(int faceMask) {
        List<Direction> faces = new ArrayList<>();
        for (Direction face : Direction.values()) {
            if ((faceMask & (1 << face.ordinal())) != 0) faces.add(face);
        }
        return faces;
    }

    /** Pure ordering helper: Manhattan distance from {@code origin}, tie-broken by position. */
    static List<Target> sort(List<Target> targets, BlockPos origin, boolean farFirst) {
        Comparator<Target> byDistance = Comparator.comparingInt(target -> manhattan(origin, target.pos()));
        Comparator<Target> order = farFirst ? byDistance.reversed() : byDistance;
        List<Target> result = new ArrayList<>(targets);
        result.sort(order
                .thenComparing(Comparator.comparingInt(target -> target.pos().getX()))
                .thenComparing(Comparator.comparingInt(target -> target.pos().getY()))
                .thenComparing(Comparator.comparingInt(target -> target.pos().getZ())));
        return result;
    }

    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * Normal mode: each container yields one allowed stack per operation tick,
     * which is moved into the ruin's own inventory through the standard NeoForge
     * {@code IItemHandler} (all target slots); any leftover is returned to its
     * source slot, so nothing is ever dropped or lost.
     */
    static void collect(ItemStackHandler buffer, List<Target> targets, boolean forcePoll, boolean itemByItem,
                        Predicate<ItemStack> allows) {
        for (Target target : targets) {
            IItemHandler handler = target.handler();
            if (handler == null) {
                if (forcePoll) break;
                continue;
            }
            int slots = Math.min(handler.getSlots(), MAX_SLOT_SCAN);
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stored = handler.getStackInSlot(slot);
                if (stored.isEmpty() || !allows.test(stored)) continue;
                int amount = itemByItem ? 1 : stored.getCount();
                ItemStack removed = handler.extractItem(slot, amount, false);
                if (removed.isEmpty()) continue;
                for (int targetSlot = 0; targetSlot < buffer.getSlots() && !removed.isEmpty(); targetSlot++) {
                    removed = buffer.insertItem(targetSlot, removed, false);
                }
                if (!removed.isEmpty()) handler.insertItem(slot, removed, false);
                break;
            }
        }
    }

    /** Reversed mode: push the ruin's own inventory into the surrounding containers. */
    static boolean eject(ItemStackHandler buffer,
                         List<Target> targets, boolean forcePoll, boolean itemByItem,
                         Predicate<ItemStack> allows, int[] groupCursor) {
        long before = storedCount(buffer, allows);
        distribute(buffer, targets, forcePoll, itemByItem, allows, groupCursor);
        return storedCount(buffer, allows) < before;
    }

    private static long storedCount(ItemStackHandler buffer, Predicate<ItemStack> allows) {
        long count = 0L;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            ItemStack stack = buffer.getStackInSlot(slot);
            if (!stack.isEmpty() && allows.test(stack)) count += stack.getCount();
        }
        return count;
    }

    private static void distribute(ItemStackHandler buffer, List<Target> targets,
                                   boolean forcePoll, boolean itemByItem,
                                   Predicate<ItemStack> allows, int[] groupCursor) {
        if (targets.isEmpty()) return;
        if (itemByItem) {
            for (int slot = 0; slot < buffer.getSlots(); slot++) {
                ItemStack stack = buffer.getStackInSlot(slot);
                if (stack.isEmpty() || !allows.test(stack)) continue;
                ItemStack remaining = distributeOneRotating(stack, targets, forcePoll, groupCursor);
                buffer.setStackInSlot(slot, remaining);
            }
        } else {
            Target target = nextGroupTarget(targets, forcePoll, groupCursor);
            if (target == null || target.handler() == null) return;
            for (int slot = 0; slot < buffer.getSlots(); slot++) {
                ItemStack stack = buffer.getStackInSlot(slot);
                if (stack.isEmpty() || !allows.test(stack)) continue;
                ItemStack remaining = insertInto(target.handler(), stack);
                buffer.setStackInSlot(slot, remaining);
                if (!remaining.isEmpty() && forcePoll) break;
            }
            groupCursor[0] = (groupCursor[0] + 1) % targets.size();
        }
    }

    /** Item-by-item equal split: each submitted item moves to the next allowed container. */
    private static ItemStack distributeOneRotating(ItemStack stack, List<Target> targets,
                                                   boolean forcePoll, int[] groupCursor) {
        ItemStack remaining = stack.copy();
        int count = targets.size();
        while (!remaining.isEmpty()) {
            boolean anyAccepted = false;
            for (int i = 0; i < count && !remaining.isEmpty(); i++) {
                Target target = targets.get(groupCursor[0] % count);
                groupCursor[0] = (groupCursor[0] + 1) % count;
                if (target.handler() == null) {
                    if (forcePoll) return remaining;
                    continue;
                }
                ItemStack one = remaining.copyWithCount(1);
                ItemStack accepted = insertInto(target.handler(), one);
                if (accepted.isEmpty()) {
                    remaining.shrink(1);
                    anyAccepted = true;
                } else if (forcePoll) {
                    return remaining;
                }
            }
            if (!anyAccepted) return remaining;
        }
        return remaining;
    }

    private static @Nullable Target nextGroupTarget(List<Target> targets, boolean forcePoll, int[] cursor) {
        int count = targets.size();
        int visited = 0;
        while (visited < count) {
            Target target = targets.get(cursor[0] % count);
            cursor[0] = (cursor[0] + 1) % count;
            visited++;
            if (target.handler() != null) return target;
            if (forcePoll) return null;
        }
        return null;
    }

    private static ItemStack insertInto(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        int slots = Math.min(handler.getSlots(), MAX_SLOT_SCAN);
        for (int slot = 0; slot < slots && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }
}
