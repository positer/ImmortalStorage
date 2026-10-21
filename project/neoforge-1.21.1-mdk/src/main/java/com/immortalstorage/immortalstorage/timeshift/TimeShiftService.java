package com.immortalstorage.immortalstorage.timeshift;

import java.util.List;

import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.TimeShiftStaffItem;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.network.ModPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative staff interaction: applying, boosting, clearing and
 * broadcasting the per-dimension record.
 */
public final class TimeShiftService {
    /** Called after the vanilla block interaction world ticks; returns nothing but always messages the player. */
    public static void handleBlockUse(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos, boolean shiftDown) {
        TimeShiftStore store = TimeShiftStore.get(level);
        TimeShiftEntry current = store.get(pos);
        if (shiftDown) {
            if (current == null) {
                feedback(player, "immortalstorage.time_shift.nothing_to_clear");
                return;
            }
            store.remove(pos);
            broadcast(level);
            feedback(player, "immortalstorage.time_shift.cleared");
            return;
        }
        if (current == null) {
            if (!TimeShiftAcceleration.isAccelerable(level, pos, level.getBlockState(pos), level.getBlockEntity(pos))) {
                feedback(player, "immortalstorage.time_shift.not_accelerable");
                return;
            }
            int limit = ImmortalStorageConfig.TIME_SHIFT_MAX_BLOCKS.get();
            if (store.size() >= limit) {
                feedback(player, "immortalstorage.time_shift.limit");
                return;
            }
            int tier = TimeShiftTiers.clampTier(TimeShiftStaffItem.tierOf(stack));
            store.put(pos, TimeShiftEntry.permanent(tier));
            broadcast(level);
            feedback(player, "immortalstorage.time_shift.applied",
                    TimeShiftTiers.label(TimeShiftTiers.permanentMultiplier(tier)));
            return;
        }
        long now = System.currentTimeMillis();
        boolean boosting = current.boostActive(now);
        if (!consumePrimordialQi(player)) {
            feedback(player, "immortalstorage.time_shift.need_primordial_qi");
            return;
        }
        long durationMillis = ImmortalStorageConfig.TIME_SHIFT_BOOST_SECONDS.get() * 1000L;
        store.put(pos, current.withBoost(now + durationMillis));
        broadcast(level);
        int boostMultiplier = TimeShiftTiers.boostMultiplier(ImmortalStorageConfig.TIME_SHIFT_BOOST_LOG2.get());
        feedback(player, boosting ? "immortalstorage.time_shift.renewed" : "immortalstorage.time_shift.boosted",
                TimeShiftTiers.label(boostMultiplier), ImmortalStorageConfig.TIME_SHIFT_BOOST_SECONDS.get());
    }

    /** Sneak+scroll tier change. Returns the new tier, or -1 when the staff is not held. */
    public static int adjustTier(ServerPlayer player, int delta) {
        ItemStack stack = equippedStaff(player);
        if (stack.isEmpty()) return -1;
        int tier = TimeShiftTiers.cycle(TimeShiftStaffItem.tierOf(stack), delta);
        TimeShiftStaffItem.setTier(stack, tier);
        feedback(player, "immortalstorage.time_shift.tier",
                TimeShiftTiers.label(TimeShiftTiers.permanentMultiplier(tier)));
        return tier;
    }

    public static ItemStack equippedStaff(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof TimeShiftStaffItem) return main;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof TimeShiftStaffItem) return offhand;
        return ItemStack.EMPTY;
    }

    /**
     * Consumes one Primordial Qi from the player's inventory first and then from
     * the Xianqiao storage.  Returns false when neither holds one.
     */
    public static boolean consumePrimordialQi(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty() || !candidate.is(ModItems.PRIMORDIAL_QI.get())) continue;
            inventory.removeItem(slot, 1);
            inventory.setChanged();
            return true;
        }
        PersonalStorageEndpoint endpoint = PersonalStorageApi.resolveXianqiao(player.getServer(), player.getUUID());
        if (endpoint == null) return false;
        ItemStack extracted = endpoint.extract(new ItemStack(ModItems.PRIMORDIAL_QI.get()), 1, false);
        return !extracted.isEmpty();
    }

    /** Sends the whole dimension's record to every player in that dimension. */
    public static void broadcast(ServerLevel level) {
        ModPayloads.TimeShiftSync payload = snapshot(level);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Sends the given player the record of the dimension they are currently in. */
    public static void sendTo(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayer(player, snapshot(level));
        }
    }

    public static ModPayloads.TimeShiftSync snapshot(ServerLevel level) {
        TimeShiftStore store = TimeShiftStore.get(level);
        List<ModPayloads.TimeShiftSyncEntry> entries = store.snapshot().stream()
                .map(entry -> new ModPayloads.TimeShiftSyncEntry(
                        entry.getKey().asLong(), entry.getValue().tier(), entry.getValue().boosted(),
                        entry.getValue().boostEndEpochMillis()))
                .toList();
        return new ModPayloads.TimeShiftSync(entries);
    }

    /** Clears the record when the shifted block is broken. Returns true when something was removed. */
    public static boolean onBlockBroken(ServerLevel level, BlockPos pos) {
        TimeShiftStore store = TimeShiftStore.get(level);
        if (store.remove(pos) == null) return false;
        broadcast(level);
        return true;
    }

    /** Sends the current dimension's record when a player joins. */
    public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    /** Sends the new dimension's record after a dimension change. */
    public static void onPlayerChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    /** Drops a record when its block is broken, so a new block at the same position starts clean. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) onBlockBroken(level, event.getPos());
    }

    private static void feedback(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private TimeShiftService() {}
}
