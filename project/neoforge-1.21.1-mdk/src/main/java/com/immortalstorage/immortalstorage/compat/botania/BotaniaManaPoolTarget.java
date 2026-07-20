package com.immortalstorage.immortalstorage.compat.botania;

import vazkii.botania.api.mana.ManaPool;

import java.util.Objects;

/** Thin official-API projection used by the loader-neutral transfer engine. */
public final class BotaniaManaPoolTarget implements BotaniaManaTransfer.IntManaPool {
    private final ManaPool pool;

    public BotaniaManaPoolTarget(ManaPool pool) {
        this.pool = Objects.requireNonNull(pool, "pool");
    }

    @Override
    public int currentMana() {
        return pool.getCurrentMana();
    }

    @Override
    public int maxMana() {
        return pool.getMaxMana();
    }

    @Override
    public void receiveMana(int amount) {
        pool.receiveMana(amount);
    }
}
