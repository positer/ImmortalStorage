package com.immortalstorage.core.amount;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/** Safe transfer sizing for integrations whose public quantity is only int. */
public final class LongAmountBridge {
    private LongAmountBridge() {
    }

    public static int saturatingInt(long amount) {
        if (amount <= 0L) return 0;
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    public static int nextChunk(long remaining, int apiLimit) {
        if (remaining <= 0L || apiLimit <= 0) return 0;
        return (int) Math.min(remaining, (long) apiLimit);
    }

    public static long committed(long completed, int accepted) {
        if (completed < 0L || accepted < 0) {
            throw new IllegalArgumentException("transfer amounts must be non-negative");
        }
        return Long.MAX_VALUE - completed < accepted
                ? Long.MAX_VALUE : completed + accepted;
    }

    /**
     * Runs at most {@code maxChunks} int-sized calls. Simulation and execution
     * receive the same offered size; only the execution result advances the
     * long total. A zero, partial, or inconsistent chunk stops immediately so
     * callers can retry from the exact committed offset on a later tick.
     */
    public static TransferProgress transferBounded(
            long requested, int apiLimit, int maxChunks,
            IntUnaryOperator simulate, IntUnaryOperator execute) {
        if (requested < 0L) throw new IllegalArgumentException("requested must be non-negative");
        if (apiLimit <= 0) throw new IllegalArgumentException("apiLimit must be positive");
        if (maxChunks <= 0) throw new IllegalArgumentException("maxChunks must be positive");
        Objects.requireNonNull(simulate, "simulate");
        Objects.requireNonNull(execute, "execute");

        long totalCommitted = 0L;
        int attemptedChunks = 0;
        boolean blocked = false;
        while (totalCommitted < requested && attemptedChunks < maxChunks) {
            int offered = nextChunk(requested - totalCommitted, apiLimit);
            if (offered <= 0) break;
            attemptedChunks++;
            int simulated = checkedAdapterResult("simulate", simulate.applyAsInt(offered), offered);
            if (simulated == 0) {
                blocked = true;
                break;
            }
            int executed = checkedAdapterResult("execute", execute.applyAsInt(offered), offered);
            totalCommitted = committed(totalCommitted, executed);
            if (executed == 0 || executed != simulated || executed != offered) {
                blocked = true;
                break;
            }
        }
        return new TransferProgress(requested, totalCommitted, attemptedChunks,
                totalCommitted == requested, blocked);
    }

    private static int checkedAdapterResult(String phase, int accepted, int offered) {
        if (accepted < 0 || accepted > offered) {
            throw new IllegalStateException(phase + " returned " + accepted
                    + " for an offered chunk of " + offered);
        }
        return accepted;
    }

    public record TransferProgress(long requested, long committed, int attemptedChunks,
                                   boolean complete, boolean blocked) {
        public TransferProgress {
            if (requested < 0L || committed < 0L || committed > requested || attemptedChunks < 0) {
                throw new IllegalArgumentException("invalid transfer progress");
            }
            if (complete != (requested == committed)) {
                throw new IllegalArgumentException("complete must match the committed total");
            }
        }
    }
}
