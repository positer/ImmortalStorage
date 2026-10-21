package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-neutral lifecycle hooks for optional network integrations of the Xianqiao manager.
 *
 * <p>The block entity must never reference a third-party type directly (RS absent would turn that
 * into {@code NoClassDefFoundError}), so it calls these hooks instead and each bridge registers
 * itself when it initialises.</p>
 *
 * <p>{@code managerAdded} fires from {@code clearRemoved()}, which is the one moment Minecraft
 * guarantees after a block entity exists in a level — RS drives its own containers from exactly
 * that point, and the manager has to do the same or nobody ever asks for its capability.</p>
 */
public final class XianqiaoManagerCompatHooks {
    private static final CopyOnWriteArrayList<Hook> HOOKS = new CopyOnWriteArrayList<>();

    private XianqiaoManagerCompatHooks() {
    }

    public static void register(Hook hook) {
        Hook checked = Objects.requireNonNull(hook, "hook");
        if (!HOOKS.contains(checked)) HOOKS.add(checked);
    }

    public static void managerAdded(XianqiaoManagerBlockEntity manager) {
        if (manager == null) return;
        for (Hook hook : HOOKS) {
            try {
                hook.managerAdded(manager);
            } catch (RuntimeException failure) {
                com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.error(
                        "A network integration hook failed while attaching a Xianqiao manager",
                        failure);
            }
        }
    }

    /** One optional integration. */
    public interface Hook {
        void managerAdded(XianqiaoManagerBlockEntity manager);
    }
}
