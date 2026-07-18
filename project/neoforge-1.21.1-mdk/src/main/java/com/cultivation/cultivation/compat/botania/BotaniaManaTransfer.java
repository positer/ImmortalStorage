package com.cultivation.cultivation.compat.botania;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.LongResourceLedger;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.core.resource.ResourceTransferAction;

import java.util.Objects;

/**
 * Loader-neutral transaction boundary for Botania's int-valued mana pools.
 *
 * <p>The actual Botania API adapter lives beside this class, but the mutation
 * algorithm deliberately depends only on the three observable pool operations.
 * This keeps the long-valued Xianqiao ledger and Immortal-Yuan conversion
 * independently testable when Botania is absent.</p>
 */
public final class BotaniaManaTransfer {
    private BotaniaManaTransfer() {
    }

    /** Minimal projection of Botania's {@code ManaPool} used by the transaction. */
    public interface IntManaPool {
        int currentMana();

        int maxMana();

        void receiveMana(int amount);
    }

    public static AtomicEnergyRefill.ResourceStore ledgerStore(
            LongResourceLedger ledger, ResourceChannelKey key) {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(key, "key");
        return new AtomicEnergyRefill.ResourceStore() {
            @Override
            public long amount() {
                return ledger.amount(key);
            }

            @Override
            public long extract(long requested, ResourceTransferAction action) {
                return ledger.extract(key, requested, action);
            }

            @Override
            public long insert(long offered, ResourceTransferAction action) {
                return ledger.insert(key, offered, action);
            }
        };
    }

    /**
     * Fills one pool using stored mana first, then configured whole Immortal
     * Yuan conversion units. The Botania target is observed after execution,
     * so only the actual positive delta is committed from internal storage.
     */
    public static AtomicEnergyRefill.Result fillPool(
            int requested,
            long perTickLimit,
            long manaPerImmortalYuan,
            AtomicEnergyRefill.ResourceStore manaStorage,
            AtomicEnergyRefill.ChargeSource immortalYuan,
            IntManaPool pool,
            ResourceTransferAction action) {
        return fillPoolIfAllowed(true, requested, perTickLimit, manaPerImmortalYuan,
                manaStorage, immortalYuan, pool, action);
    }

    public static AtomicEnergyRefill.Result fillPoolIfAllowed(
            boolean allowed,
            int requested,
            long perTickLimit,
            long manaPerImmortalYuan,
            AtomicEnergyRefill.ResourceStore manaStorage,
            AtomicEnergyRefill.ChargeSource immortalYuan,
            IntManaPool pool,
            ResourceTransferAction action) {
        Objects.requireNonNull(manaStorage, "manaStorage");
        Objects.requireNonNull(immortalYuan, "immortalYuan");
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(action, "action");

        int current = pool.currentMana();
        int maximum = pool.maxMana();
        int positiveRequest = Math.max(0, requested);
        if (!allowed || positiveRequest == 0 || current < 0 || maximum < 0 || current > maximum) {
            return AtomicEnergyRefill.transfer(
                    positiveRequest, 0L, Math.max(1L, manaPerImmortalYuan),
                    manaStorage, immortalYuan, ignoredTarget(), action);
        }

        int capacity = maximum - current;
        int boundedRequest = Math.min(positiveRequest, capacity);
        return AtomicEnergyRefill.transfer(
                boundedRequest, perTickLimit, manaPerImmortalYuan,
                manaStorage, immortalYuan, new ObservedPoolTarget(pool), action);
    }

    private static AtomicEnergyRefill.EnergyTarget ignoredTarget() {
        return (offered, action) -> 0L;
    }

    private static final class ObservedPoolTarget implements AtomicEnergyRefill.EnergyTarget {
        private final IntManaPool pool;

        private ObservedPoolTarget(IntManaPool pool) {
            this.pool = pool;
        }

        @Override
        public long insert(long offered, ResourceTransferAction action) {
            if (offered <= 0L) return 0L;
            int before = pool.currentMana();
            int maximum = pool.maxMana();
            if (before < 0 || maximum < 0 || before > maximum) return 0L;
            int accepted = (int) Math.min(
                    Math.min(offered, (long) Integer.MAX_VALUE),
                    (long) maximum - before);
            if (!action.executes() || accepted == 0) return accepted;

            pool.receiveMana(accepted);
            int after = pool.currentMana();
            if (after < before || after > maximum) {
                throw new IllegalStateException("Botania mana pool reported invalid execution delta: "
                        + before + " -> " + after + " / " + maximum);
            }
            long observed = (long) after - before;
            if (observed > accepted) {
                throw new IllegalStateException("Botania mana pool accepted " + observed
                        + " from an offered amount of " + accepted);
            }
            return observed;
        }
    }
}
