package com.immortalstorage.immortalstorage.villager;

/**
 * 26.1.2 consumes villager trades from the official {@code trade_set} and
 * {@code villager_trade} data registries.  The old event/listing API no
 * longer exists; the target resource pack owns those data entries, so this
 * class remains an intentionally empty event-bus registration anchor.
 */
public final class ModTrades {
    private ModTrades() {
    }
}
