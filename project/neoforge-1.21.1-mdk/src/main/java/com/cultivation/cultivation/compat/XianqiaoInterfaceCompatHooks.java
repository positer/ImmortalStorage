package com.cultivation.cultivation.compat;

import com.cultivation.cultivation.block.entity.XianqiaoInterfaceBlockEntity;
import com.cultivation.core.resource.ResourceChannelKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Loader-neutral lifecycle hooks for optional Xianqiao Interface integrations. */
public final class XianqiaoInterfaceCompatHooks {
    private static final CopyOnWriteArrayList<Hook> HOOKS = new CopyOnWriteArrayList<>();

    public static void register(Hook hook) {
        Hook checked = Objects.requireNonNull(hook, "hook");
        if (!HOOKS.contains(checked)) HOOKS.add(checked);
    }

    public static void onLoad(XianqiaoInterfaceBlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;
        for (Hook hook : HOOKS) hook.onLoad(blockEntity, level);
    }

    public static void serverTick(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {
        for (Hook hook : HOOKS) hook.serverTick(blockEntity, level);
    }

    public static void onRemoved(XianqiaoInterfaceBlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;
        for (Hook hook : HOOKS) hook.onRemoved(blockEntity, level);
    }

    public static Optional<ContainedExternalResource> containedExternalResource(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        for (Hook hook : HOOKS) {
            Optional<ContainedExternalResource> content = hook.containedExternalResource(stack);
            if (content != null && content.isPresent()) return content;
        }
        return Optional.empty();
    }

    public static InteractionResult useItemOn(
            XianqiaoInterfaceBlockEntity blockEntity, Player player,
            ItemStack stack, InteractionHand hand, BlockHitResult hit) {
        for (Hook hook : HOOKS) {
            InteractionResult result = hook.useItemOn(blockEntity, player, stack, hand, hit);
            if (result != null && result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    public record ContainedExternalResource(ResourceChannelKey key, long amount) {
        public ContainedExternalResource {
            Objects.requireNonNull(key, "key");
            if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
        }
    }

    public interface Hook {
        default void onLoad(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {}

        default void serverTick(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {}

        default void onRemoved(XianqiaoInterfaceBlockEntity blockEntity, ServerLevel level) {}

        default Optional<ContainedExternalResource> containedExternalResource(ItemStack stack) {
            return Optional.empty();
        }

        default InteractionResult useItemOn(
                XianqiaoInterfaceBlockEntity blockEntity, Player player,
                ItemStack stack, InteractionHand hand, BlockHitResult hit) {
            return InteractionResult.PASS;
        }
    }

    private XianqiaoInterfaceCompatHooks() {}
}
