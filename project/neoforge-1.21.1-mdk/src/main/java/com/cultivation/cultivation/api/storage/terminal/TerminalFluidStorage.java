package com.cultivation.cultivation.api.storage.terminal;

import java.util.Map;

/**
 * Long-millibucket storage surface for integrations that are not limited to
 * NeoForge's int-sized {@code IFluidHandler} calls. Cultivation exposes the
 * block capability only on a first-owner-bound Xianqiao Manager; cached
 * handlers re-check that owner's current online attachment identity.
 *
 * <p>Interaction semantics were behaviorally compared with AE2's public
 * simulate/modulate storage contracts; no AE2 source or assets are copied.</p>
 *
 * @see <a href="https://github.com/AppliedEnergistics/Applied-Energistics-2">AE2 official repository</a>
 */
public interface TerminalFluidStorage {
    long revision();

    /** Immutable point-in-time identity/amount snapshot. */
    Map<TerminalFluidKey, Long> snapshot();

    /** Returns the amount accepted (or that would be accepted). */
    long insert(TerminalFluidKey key, long amountMb, TerminalStorageAction action);

    /** Returns the amount extracted (or that would be extracted). */
    long extract(TerminalFluidKey key, long amountMb, TerminalStorageAction action);
}
