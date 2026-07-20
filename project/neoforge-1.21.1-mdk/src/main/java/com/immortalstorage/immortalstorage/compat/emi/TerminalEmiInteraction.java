package com.immortalstorage.immortalstorage.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;

/**
 * EMI hover identity for a real terminal cell.
 *
 * <p>The one-argument {@link EmiStackInteraction} constructor marks an
 * interaction as mouse-clickable. That is appropriate for EMI's ingredient
 * overlay, but not for storage cells: left/right click must reach the terminal
 * so it can extract or insert. A non-clickable interaction still exposes the
 * ingredient to EMI's focused lookup keys (R/U) without taking ownership of
 * normal mouse buttons.</p>
 */
final class TerminalEmiInteraction {
    private TerminalEmiInteraction() {
    }

    static EmiStackInteraction lookupOnly(EmiIngredient ingredient) {
        return new EmiStackInteraction(ingredient, null, false);
    }
}
