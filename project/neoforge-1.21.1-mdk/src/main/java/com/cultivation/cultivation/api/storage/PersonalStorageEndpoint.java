package com.cultivation.cultivation.api.storage;

import com.cultivation.cultivation.api.storage.terminal.TerminalFluidStorage;
import com.cultivation.cultivation.api.storage.terminal.TerminalItemStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Stable view of an owner-scoped personal storage endpoint.
 *
 * Addons should prefer {@link #itemHandler()} for automation compatibility.
 * Direct stack helpers are convenience methods over the same backing store.
 */
public interface PersonalStorageEndpoint {
    UUID owner();
    int stage();
    boolean online();
    IItemHandler itemHandler();
    ItemStack insert(ItemStack stack, boolean simulate);
    ItemStack extract(ItemStack template, int amount, boolean simulate);

    /**
     * Optional long-valued item namespace for storage networks that support
     * counts beyond ItemStack's int bridge. Identity includes all components.
     */
    default @Nullable TerminalItemStorage itemStorage() { return null; }

    /**
     * Optional long-mB fluid namespace. It is exposed only by an endpoint that
     * was resolved explicitly with fluid support; legacy resolve calls remain
     * item-only.
     */
    default @Nullable TerminalFluidStorage fluidStorage() { return null; }

    /** Optional standard NeoForge bridge over {@link #fluidStorage()}. */
    default @Nullable IFluidHandler fluidHandler() { return null; }

    /**
     * Optional stage-eight+ namespace for energy, chemicals, mana, source,
     * soul and other integration-defined long-valued resources.
     */
    default @Nullable ExternalResourceStorage externalResourceStorage() { return null; }
}
