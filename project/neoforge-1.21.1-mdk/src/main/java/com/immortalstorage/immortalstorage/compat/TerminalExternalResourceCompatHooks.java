package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-neutral terminal bridge for optional external-resource containers.
 *
 * <p>The terminal only knows how to route a carried stack and a long-valued
 * external storage endpoint. Optional integrations own capability discovery,
 * resource identity conversion and the container transaction itself.</p>
 */
public final class TerminalExternalResourceCompatHooks {
    private static final CopyOnWriteArrayList<Hook> HOOKS = new CopyOnWriteArrayList<>();

    public static void register(Hook hook) {
        Hook checked = Objects.requireNonNull(hook, "hook");
        if (!HOOKS.contains(checked)) HOOKS.add(checked);
    }

    /** Returns whether any installed optional integration recognizes the stack as a container. */
    public static boolean isContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Hook hook : HOOKS) {
            if (hook.isContainer(stack)) return true;
        }
        return false;
    }

    public static TransferResult depositToStorage(
            ItemStack carried, ExternalResourceStorage storage, IItemHandler returnInventory) {
        for (Hook hook : HOOKS) {
            TransferResult result = hook.depositToStorage(carried, storage, returnInventory);
            if (result != null && result.handled()) return result;
        }
        return TransferResult.notHandled(carried);
    }

    public static TransferResult withdrawFromStorage(
            ItemStack carried, com.immortalstorage.core.resource.ResourceChannelKey key,
            ExternalResourceStorage storage, IItemHandler returnInventory) {
        for (Hook hook : HOOKS) {
            TransferResult result = hook.withdrawFromStorage(carried, key, storage, returnInventory);
            if (result != null && result.handled()) return result;
        }
        return TransferResult.notHandled(carried);
    }

    public interface Hook {
        default boolean isContainer(ItemStack stack) {
            return false;
        }

        default TransferResult depositToStorage(
                ItemStack carried, ExternalResourceStorage storage, IItemHandler returnInventory) {
            return TransferResult.notHandled(carried);
        }

        default TransferResult withdrawFromStorage(
                ItemStack carried, com.immortalstorage.core.resource.ResourceChannelKey key,
                ExternalResourceStorage storage, IItemHandler returnInventory) {
            return TransferResult.notHandled(carried);
        }
    }

    public enum Failure {
        INCOMPATIBLE_OR_NO_SPACE,
        EXECUTE_FAILED
    }

    public record TransferResult(boolean handled, boolean success, ItemStack carried, Failure failure) {
        public TransferResult {
            carried = carried == null ? ItemStack.EMPTY : carried.copy();
            if (!handled && success) {
                throw new IllegalArgumentException("an unhandled transfer cannot succeed");
            }
            if ((success && failure != null) || (!success && failure == null)) {
                throw new IllegalArgumentException("inconsistent transfer result");
            }
        }

        @Override
        public ItemStack carried() {
            return carried.copy();
        }

        public static TransferResult success(ItemStack carried) {
            return new TransferResult(true, true, carried, null);
        }

        public static TransferResult failure(Failure failure) {
            return new TransferResult(true, false, ItemStack.EMPTY,
                    Objects.requireNonNull(failure, "failure"));
        }

        public static TransferResult notHandled(ItemStack carried) {
            return new TransferResult(false, false, carried, Failure.INCOMPATIBLE_OR_NO_SPACE);
        }
    }

    private TerminalExternalResourceCompatHooks() {}
}
