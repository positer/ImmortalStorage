package com.immortalstorage.immortalstorage.block.entity;

/**
 * Reusable single-channel output allowance. A source owns one instance for
 * capability extraction and one independent instance for every active PUSH
 * face; simulations only inspect the current game-tick allowance.
 */
final class SourceVeinFluxBudget {
    private long gameTick = Long.MIN_VALUE;
    private long spent;

    long available(long currentGameTick, long limit) {
        beginTick(currentGameTick);
        return Math.max(0L, Math.max(0L, limit) - spent);
    }

    long claim(long currentGameTick, long limit, long requested, boolean simulate) {
        if (requested <= 0L) return 0L;
        long granted = Math.min(requested, available(currentGameTick, limit));
        if (!simulate) spent += granted;
        return granted;
    }

    void refund(long currentGameTick, long amount) {
        if (amount <= 0L || gameTick != currentGameTick) return;
        spent = Math.max(0L, spent - amount);
    }

    long spent(long currentGameTick) {
        beginTick(currentGameTick);
        return spent;
    }

    private void beginTick(long currentGameTick) {
        if (gameTick == currentGameTick) return;
        gameTick = currentGameTick;
        spent = 0L;
    }
}
