package com.cultivation.cultivation.client.screen;

import net.minecraft.client.renderer.Rect2i;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/** Typed fluid hover surface for the mixed item/fluid terminal grid. */
public interface TerminalFluidScreenAccess {
    /** Fluid lookup has priority; callers may fall back to the ordinary item slot when empty. */
    Optional<FluidHover> cultivation$getFluidAt(double mouseX, double mouseY);

    List<FluidHover> cultivation$getVisibleFluids();

    record FluidHover(FluidStack stack, long amountMb, Rect2i bounds) {
        public FluidHover {
            if (stack == null || stack.isEmpty() || amountMb <= 0L || bounds == null) {
                throw new IllegalArgumentException("Invalid terminal fluid hover");
            }
            stack = stack.copyWithAmount(1);
        }

        @Override public FluidStack stack() { return stack.copyWithAmount(1); }
    }
}
