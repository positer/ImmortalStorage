package com.immortalstorage.immortalstorage.api.source;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;

import java.util.concurrent.atomic.AtomicBoolean;

/** Built-in charge-provider registration, kept separate from the mod bootstrap. */
public final class ImmortalStorageChargeProviders {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    public static void registerBuiltins() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        SourceChargeRegistry.register(SourceChargeRegistry.IMMORTAL_YUAN, new ImmortalYuanProvider());
    }

    public static void freezeRegistration() {
        SourceChargeRegistry.freeze();
    }

    /**
     * Reserves by debiting immediately. Settlement refunds the unused balance,
     * so a target can never receive source output before its charge is secured.
     */
    private static final class ImmortalYuanProvider implements SourceChargeProvider {
        @Override
        public boolean canReserve(SourceChargeContext context, long units) {
            var owner = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                    .onlinePlayer(context.level().getServer(), context.owner());
            if (owner == null || units <= 0L) return false;
            synchronized (owner) {
                return ImmortalStoragePlayerData.get(owner).getImmortalYuan() >= units;
            }
        }

        @Override
        public Reservation reserve(SourceChargeContext context, long units) {
            var owner = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                    .onlinePlayer(context.level().getServer(), context.owner());
            if (owner == null || units <= 0L) return null;
            synchronized (owner) {
                ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(owner);
                if (!data.consumeImmortalYuan(units)) return null;
                return new Reservation() {
                    private boolean open = true;

                    @Override
                    public long units() {
                        return units;
                    }

                    @Override
                    public boolean settle(long chargedUnits) {
                        synchronized (owner) {
                            if (!open || chargedUnits < 0L || chargedUnits > units) return false;
                            long refund = units - chargedUnits;
                            if (refund > 0L) {
                                long accepted = data.depositImmortalYuan(refund);
                                if (accepted != refund) {
                                    ImmortalStorageMod.LOG.error(
                                            "Immortal-yuan reservation refund was capped for {}: {}/{}",
                                            owner.getUUID(), accepted, refund);
                                    // The debit already happened. Closing is safer than permitting
                                    // a second settlement that could duplicate a refund.
                                }
                            }
                            open = false;
                            return true;
                        }
                    }
                };
            }
        }
    }

    private ImmortalStorageChargeProviders() {}
}
