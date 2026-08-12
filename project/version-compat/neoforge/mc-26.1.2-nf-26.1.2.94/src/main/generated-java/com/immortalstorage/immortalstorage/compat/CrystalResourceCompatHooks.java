package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Loader-neutral seam for optional mana/source capabilities.  No Botania or
 * Ars Nouveau type may escape the optional packages; this class is safe to
 * load in a plain FE-only instance.
 */
public final class CrystalResourceCompatHooks {
    private static final CopyOnWriteArrayList<Hook> HOOKS = new CopyOnWriteArrayList<>();

    public static void register(Hook hook) {
        Hook checked = Objects.requireNonNull(hook, "hook");
        if (!HOOKS.contains(checked)) HOOKS.add(checked);
    }

    public static void onLoad(EnergyCrystalBlockEntity crystal, ServerLevel level) {
        for (Hook hook : HOOKS) {
            if (hook.supports(crystal.kind())) hook.onLoad(crystal, level);
        }
    }

    public static void onRemoved(EnergyCrystalBlockEntity crystal, ServerLevel level) {
        for (Hook hook : HOOKS) {
            if (hook.supports(crystal.kind())) hook.onRemoved(crystal, level);
        }
    }

    public static void serverTick(EnergyCrystalBlockEntity crystal, ServerLevel level) {
        for (Hook hook : HOOKS) {
            if (hook.supports(crystal.kind())) hook.serverTick(crystal, level);
        }
    }

    public static boolean pushToFace(EnergyCrystalBlockEntity crystal, ServerLevel level,
                                     Direction side, AtomicEnergyRefill.ResourceStore source) {
        boolean changed = false;
        for (Hook hook : HOOKS) {
            if (!hook.supports(crystal.kind())) continue;
            Boolean result = hook.pushToFace(crystal, level, side, source);
            if (result != null) changed |= result;
        }
        return changed;
    }

    public static boolean acceptsInput(CrystalKind kind, ItemStack stack) {
        for (Hook hook : HOOKS) {
            if (hook.supports(kind) && hook.acceptsInput(stack)) return true;
        }
        return false;
    }

    public static @Nullable InputResult processInput(
            EnergyCrystalBlockEntity crystal, ServerLevel level, ItemStack input,
            AtomicEnergyRefill.ResourceStore local,
            @Nullable AtomicEnergyRefill.ResourceStore external,
            Predicate<ItemStack> canAcceptExtra) {
        for (Hook hook : HOOKS) {
            if (!hook.supports(crystal.kind())) continue;
            InputResult result = hook.processInput(
                    crystal, level, input, local, external, canAcceptExtra);
            if (result != null) return result;
        }
        return null;
    }

    public static InteractionResult useItemOn(
            EnergyCrystalBlockEntity crystal, Player player, ItemStack stack,
            InteractionHand hand, BlockHitResult hit) {
        for (Hook hook : HOOKS) {
            if (!hook.supports(crystal.kind())) continue;
            InteractionResult result = hook.useItemOn(crystal, player, stack, hand, hit);
            if (result != null && result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    public record InputResult(boolean changed, @Nullable ItemStack output) {
        public InputResult {
            if (output != null && output.isEmpty()) output = null;
        }

        public static InputResult updated() {
            return new InputResult(true, null);
        }

        public static InputResult complete(ItemStack output) {
            return new InputResult(true, output.copyWithCount(1));
        }
    }

    public interface Hook {
        boolean supports(CrystalKind kind);

        default void onLoad(EnergyCrystalBlockEntity crystal, ServerLevel level) {}

        default void onRemoved(EnergyCrystalBlockEntity crystal, ServerLevel level) {}

        default void serverTick(EnergyCrystalBlockEntity crystal, ServerLevel level) {}

        /** Return null when this hook does not expose a face container. */
        default @Nullable Boolean pushToFace(
                EnergyCrystalBlockEntity crystal, ServerLevel level, Direction side,
                AtomicEnergyRefill.ResourceStore source) {
            return null;
        }

        /** Return null when this hook does not recognize the input item. */
        default boolean acceptsInput(ItemStack stack) {
            return false;
        }

        /** Return null when this hook does not recognize the input item. */
        default @Nullable InputResult processInput(
                EnergyCrystalBlockEntity crystal, ServerLevel level, ItemStack input,
                AtomicEnergyRefill.ResourceStore local,
                @Nullable AtomicEnergyRefill.ResourceStore external,
                Predicate<ItemStack> canAcceptExtra) {
            return null;
        }

        default InteractionResult useItemOn(
                EnergyCrystalBlockEntity crystal, Player player, ItemStack stack,
                InteractionHand hand, BlockHitResult hit) {
            return InteractionResult.PASS;
        }
    }

    private CrystalResourceCompatHooks() {}
}
