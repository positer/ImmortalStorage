package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Server-only material escrow and placement commit for Spirit Staff build mode. */
public final class SpiritStaffBuildExecutor {
    public static Result execute(UseOnContext context, int configuredLimit) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return Result.failed(Failure.INVALID_CONTEXT);
        }
        PreparedJob job = prepare(player, context.getHand(), context.getClickedPos(),
                context.getClickedFace(), configuredLimit);
        if (job.failure() != Failure.NONE) return Result.failed(job.failure());

        MaterialEscrow escrow = MaterialEscrow.reserve(player, job.material().template(), job.plan().size());
        if (!player.isCreative() && escrow.available() <= 0) {
            return new Result(job.plan().size(), 0, 0, Failure.NO_MATERIALS);
        }

        int placementBudget = player.isCreative()
                ? job.plan().size() : Math.min(job.plan().size(), escrow.available());
        int placed = 0;
        try {
            for (BlockPos target : job.plan()) {
                if (placed >= placementBudget) break;
                if (!level.getWorldBorder().isWithinBounds(target)
                        || !player.canInteractWithBlock(target, 1.0D)
                        || !level.mayInteract(player, target)) {
                    continue;
                }

                BlockPlaceContext placeContext = SpiritStaffBuildPlan.placementContext(
                        level, player, context.getHand(), target, context.getClickedFace(),
                        job.material().template());
                InteractionResult result = job.material().blockItem().place(placeContext);
                if (!result.consumesAction()) continue;

                escrow.consumeOne();
                placed++;
            }
        } finally {
            escrow.refund(player);
        }

        if (placed > 0) {
            context.getItemInHand().hurtAndBreak(
                    1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
            player.getCooldowns().addCooldown(context.getItemInHand().getItem(), 4);
            return new Result(job.plan().size(), placementBudget, placed, Failure.NONE);
        }
        return new Result(job.plan().size(), placementBudget, 0, Failure.BLOCKED);
    }

    /** Non-mutating server snapshot used by the client build preview. */
    public static Preview preview(
            ServerPlayer player, net.minecraft.world.InteractionHand hand,
            BlockPos clicked, Direction face, int configuredLimit) {
        PreparedJob job = prepare(player, hand, clicked, face, configuredLimit);
        return new Preview(job.plan(), job.failure());
    }

    public static Preview previewRemoval(
            ServerPlayer player, net.minecraft.world.InteractionHand hand,
            BlockPos clicked, Direction face, int configuredLimit) {
        if (!validRemovalContext(player, hand, clicked, face)) {
            return new Preview(List.of(), Failure.INVALID_CONTEXT);
        }
        BlockState state = player.serverLevel().getBlockState(clicked);
        if (state.isAir() || state.hasBlockEntity()) return new Preview(List.of(), Failure.BLOCKED);
        return new Preview(SpiritStaffBuildPlan.removalLayer(player.serverLevel(), clicked, face, state,
                Math.max(1, Math.min(4096, configuredLimit))), Failure.NONE);
    }

    public static Result removeLayer(
            ServerPlayer player, net.minecraft.world.InteractionHand hand,
            BlockPos clicked, Direction face, int configuredLimit) {
        Preview preview = previewRemoval(player, hand, clicked, face, configuredLimit);
        if (preview.failure() != Failure.NONE || preview.positions().isEmpty()) {
            return Result.failed(preview.failure() == Failure.NONE ? Failure.NO_TARGETS : preview.failure());
        }
        ServerLevel level = player.serverLevel();
        int removed = 0;
        for (BlockPos pos : preview.positions()) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || !level.mayInteract(player, pos)
                    || !player.canInteractWithBlock(pos, 1.0D)) continue;
            GameType gameType = player.gameMode.getGameModeForPlayer();
            if (CommonHooks.fireBlockBreak(level, gameType, player, pos, state).isCanceled()) continue;
            if (level.removeBlock(pos, false)) removed++;
        }
        if (removed > 0) {
            ItemStack staff = player.getItemInHand(hand);
            staff.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            player.getCooldowns().addCooldown(staff.getItem(), 4);
            return new Result(preview.positions().size(), removed, removed, Failure.NONE);
        }
        return new Result(preview.positions().size(), 0, 0, Failure.BLOCKED);
    }

    private static boolean validRemovalContext(
            ServerPlayer player, net.minecraft.world.InteractionHand hand,
            BlockPos clicked, Direction face) {
        return player != null && hand != null && clicked != null && face != null && !player.isSpectator()
                && player.getItemInHand(hand).getItem() instanceof SpiritStaffItem
                && SpiritStaffItem.getMode(player.getItemInHand(hand)) == SpiritStaffItem.MODE_BUILD
                && player.serverLevel().hasChunkAt(clicked)
                && player.serverLevel().mayInteract(player, clicked)
                && player.canInteractWithBlock(clicked, 1.0D);
    }

    private static PreparedJob prepare(
            ServerPlayer player, net.minecraft.world.InteractionHand hand,
            BlockPos clicked, Direction face, int configuredLimit) {
        if (player == null || clicked == null || face == null || player.isSpectator()
                || !(player.getItemInHand(hand).getItem() instanceof SpiritStaffItem staff)
                || SpiritStaffItem.getMode(player.getItemInHand(hand)) != SpiritStaffItem.MODE_BUILD) {
            return PreparedJob.failed(Failure.INVALID_CONTEXT);
        }
        ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(clicked) || !level.mayInteract(player, clicked)
                || !player.canInteractWithBlock(clicked, 1.0D)) {
            return PreparedJob.failed(Failure.BLOCKED);
        }
        Material material = selectMaterial(player, level.getBlockState(clicked));
        if (material == null) return PreparedJob.failed(Failure.NOT_A_BLOCK_ITEM);

        int limit = Math.max(1, Math.min(4096, configuredLimit));
        int available = player.isCreative() ? limit : countAvailable(player, material.template(), limit);
        if (available <= 0) return PreparedJob.failed(Failure.NO_MATERIALS);
        int plannedLimit = Math.min(limit, available);
        List<BlockPos> plan = SpiritStaffBuildPlan.create(
                level, player, hand, clicked, face, level.getBlockState(clicked),
                material.template(), plannedLimit);
        return plan.isEmpty()
                ? PreparedJob.failed(Failure.NO_TARGETS)
                : new PreparedJob(material, List.copyOf(plan), Failure.NONE);
    }

    private static @Nullable Material selectMaterial(ServerPlayer player, BlockState clickedState) {
        ItemStack offhand = player.getOffhandItem();
        ItemStack template = offhand.getItem() instanceof BlockItem
                ? offhand.copyWithCount(1)
                : new ItemStack(clickedState.getBlock().asItem());
        if (template.isEmpty() || !(template.getItem() instanceof BlockItem blockItem)) return null;
        // Construction Wand defaults to rejecting block entities. This avoids
        // cloning configuration-bearing machine items through a bulk plan.
        if (blockItem.getBlock().defaultBlockState().hasBlockEntity()) return null;
        return new Material(template, blockItem);
    }

    private static int countAvailable(ServerPlayer player, ItemStack template, int limit) {
        int inventory = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                inventory = Math.min(limit, inventory + stack.getCount());
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                inventory = Math.min(limit, inventory + stack.getCount());
            }
        }
        if (inventory >= limit) return limit;
        PersonalStorageEndpoint endpoint = PersonalStorageApi.resolve(
                player.serverLevel().getServer(), player.getUUID());
        if (endpoint == null || endpoint.itemStorage() == null) return inventory;
        long storage = endpoint.itemStorage().extract(
                TerminalEntryKey.of(template), limit - inventory, TerminalStorageAction.SIMULATE);
        return (int) Math.min(limit, inventory + Math.max(0L, storage));
    }

    public enum Failure {
        NONE,
        INVALID_CONTEXT,
        NOT_A_BLOCK_ITEM,
        NO_TARGETS,
        NO_MATERIALS,
        BLOCKED
    }

    public record Result(int planned, int reserved, int placed, Failure failure) {
        static Result failed(Failure failure) {
            return new Result(0, 0, 0, failure);
        }

        public boolean succeeded() {
            return placed > 0;
        }
    }

    public record Preview(List<BlockPos> positions, Failure failure) {
        public Preview {
            positions = positions == null ? List.of() : List.copyOf(positions);
            failure = failure == null ? Failure.INVALID_CONTEXT : failure;
        }
    }

    private record Material(ItemStack template, BlockItem blockItem) {
        private Material {
            template = template.copyWithCount(1);
        }

        @Override public ItemStack template() { return template.copy(); }
    }

    private record PreparedJob(@Nullable Material material, List<BlockPos> plan, Failure failure) {
        private static PreparedJob failed(Failure failure) {
            return new PreparedJob(null, List.of(), failure);
        }
    }

    private static final class MaterialEscrow {
        private final ItemStack template;
        private final @Nullable PersonalStorageEndpoint endpoint;
        private int inventoryRemaining;
        private int storageRemaining;

        private MaterialEscrow(ItemStack template, @Nullable PersonalStorageEndpoint endpoint,
                               int inventoryRemaining, int storageRemaining) {
            this.template = template.copyWithCount(1);
            this.endpoint = endpoint;
            this.inventoryRemaining = inventoryRemaining;
            this.storageRemaining = storageRemaining;
        }

        static MaterialEscrow reserve(ServerPlayer player, ItemStack template, int requested) {
            PersonalStorageEndpoint endpoint = PersonalStorageApi.resolve(
                    player.serverLevel().getServer(), player.getUUID());
            if (player.isCreative()) return new MaterialEscrow(template, endpoint, 0, 0);

            int fromInventory = removeFromPlayer(player, template, requested);
            try {
                int remaining = requested - fromInventory;
                int fromStorage = remaining <= 0 ? 0 : extractFromStorage(endpoint, template, remaining);
                return new MaterialEscrow(template, endpoint, fromInventory, fromStorage);
            } catch (RuntimeException failure) {
                MaterialEscrow rollback = new MaterialEscrow(template, endpoint, fromInventory, 0);
                rollback.refund(player);
                throw failure;
            }
        }

        int available() {
            return inventoryRemaining + storageRemaining;
        }

        void consumeOne() {
            if (inventoryRemaining > 0) {
                inventoryRemaining--;
            } else if (storageRemaining > 0) {
                storageRemaining--;
            }
        }

        void refund(ServerPlayer player) {
            refundInventoryOrigin(player, inventoryRemaining);
            refundStorageOrigin(player, storageRemaining);
            inventoryRemaining = 0;
            storageRemaining = 0;
        }

        private void refundInventoryOrigin(ServerPlayer player, int amount) {
            refundInChunks(player, amount, true);
        }

        private void refundStorageOrigin(ServerPlayer player, int amount) {
            refundInChunks(player, amount, false);
        }

        private void refundInChunks(ServerPlayer player, int amount, boolean inventoryFirst) {
            int remaining = amount;
            int maxStack = Math.max(1, template.getMaxStackSize());
            while (remaining > 0) {
                int chunkSize = Math.min(remaining, maxStack);
                ItemStack remainder = template.copyWithCount(chunkSize);
                if (inventoryFirst) {
                    remainder = addToPlayer(player, remainder);
                    remainder = insertIntoStorage(endpoint, remainder);
                } else {
                    remainder = insertIntoStorage(endpoint, remainder);
                    remainder = addToPlayer(player, remainder);
                }
                if (!remainder.isEmpty()) {
                    Block.popResource(player.serverLevel(), player.blockPosition(), remainder);
                }
                remaining -= chunkSize;
            }
        }

        private static int removeFromPlayer(ServerPlayer player, ItemStack template, int requested) {
            int remaining = requested;
            for (ItemStack stack : player.getInventory().items) {
                if (remaining <= 0) break;
                if (!ItemStack.isSameItemSameComponents(stack, template)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (remaining <= 0) break;
                if (!ItemStack.isSameItemSameComponents(stack, template)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
            player.getInventory().setChanged();
            return requested - remaining;
        }

        private static int extractFromStorage(
                @Nullable PersonalStorageEndpoint endpoint, ItemStack template, int requested) {
            if (endpoint == null || endpoint.itemStorage() == null || requested <= 0) return 0;
            TerminalItemStorage storage = endpoint.itemStorage();
            TerminalEntryKey key = TerminalEntryKey.of(template);
            long simulated = storage.extract(key, requested, TerminalStorageAction.SIMULATE);
            int planned = (int) Math.min(requested, Math.max(0L, simulated));
            if (planned <= 0) return 0;
            long executed = storage.extract(key, planned, TerminalStorageAction.EXECUTE);
            return (int) Math.min(planned, Math.max(0L, executed));
        }

        private static ItemStack addToPlayer(ServerPlayer player, ItemStack stack) {
            if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack remaining = stack.copy();
            player.getInventory().add(remaining);
            return remaining;
        }

        private static ItemStack insertIntoStorage(
                @Nullable PersonalStorageEndpoint endpoint, ItemStack stack) {
            if (endpoint == null || stack == null || stack.isEmpty()) return stack == null ? ItemStack.EMPTY : stack;
            ItemStack remaining = stack.copy();
            while (!remaining.isEmpty()) {
                int chunkSize = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack chunk = remaining.copyWithCount(chunkSize);
                ItemStack chunkRemainder = endpoint.insert(chunk, false);
                int accepted = chunkSize - (chunkRemainder.isEmpty() ? 0 : chunkRemainder.getCount());
                if (accepted <= 0) break;
                remaining.shrink(accepted);
            }
            return remaining;
        }
    }

    private SpiritStaffBuildExecutor() {}
}
