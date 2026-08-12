package com.immortalstorage.immortalstorage.api.storage.terminal;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/**
 * Immutable fluid-and-components identity for terminal storage.
 *
 * <p>The amount is deliberately excluded. This mirrors NeoForge's
 * {@link FluidStack#isSameFluidSameComponents(FluidStack, FluidStack)}
 * identity and keeps component-bearing variants in distinct entries.</p>
 */
public final class TerminalFluidKey {
    private final FluidStack prototype;
    private final int hash;

    private TerminalFluidKey(FluidStack stack) {
        this.prototype = stack.copyWithAmount(1);
        this.hash = FluidStack.hashFluidAndComponents(this.prototype);
    }

    public static TerminalFluidKey of(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("A terminal fluid key requires a non-empty fluid stack");
        }
        return new TerminalFluidKey(stack);
    }

    /** Returns a defensive one-millibucket identity stack for display/codecs. */
    public FluidStack prototype() {
        return prototype.copyWithAmount(1);
    }

    public boolean matches(FluidStack stack) {
        return stack != null && !stack.isEmpty()
                && FluidStack.isSameFluidSameComponents(prototype, stack);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TerminalFluidKey key
                && FluidStack.isSameFluidSameComponents(prototype, key.prototype);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return Objects.toString(prototype.getFluid()) + "@" + Integer.toUnsignedString(hash, 16);
    }
}
