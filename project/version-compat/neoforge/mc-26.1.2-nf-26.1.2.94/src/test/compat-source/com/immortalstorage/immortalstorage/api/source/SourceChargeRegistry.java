package com.immortalstorage.immortalstorage.api.source;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** AppliedE/AE-style registration backed by provider-owned exact reservations. */
public final class SourceChargeRegistry {
    public static final Identifier IMMORTAL_YUAN =
            Identifier.fromNamespaceAndPath("immortalstorage", "immortal_yuan");

    private static final Map<Identifier, SourceChargeProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static final AtomicBoolean FROZEN = new AtomicBoolean(false);

    public static void register(Identifier id, SourceChargeProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (FROZEN.get()) {
            throw new IllegalStateException("Source charge provider registration is closed: " + id);
        }
        SourceChargeProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException("Source charge provider already registered: " + id);
        }
    }

    public static boolean isRegistered(Identifier id) {
        return PROVIDERS.containsKey(id);
    }

    /** Closes the startup registration phase; runtime provider replacement is forbidden. */
    public static void freeze() {
        FROZEN.set(true);
    }

    public static boolean isFrozen() {
        return FROZEN.get();
    }

    public static boolean canReserve(SourceChargePlan plan, SourceChargeContext context, long requestedOutputs) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(context, "context");
        if (requestedOutputs <= 0) return false;
        if (plan.isFree()) return true;
        SourceChargeProvider provider = PROVIDERS.get(plan.providerId());
        long required = plan.requiredUnits(requestedOutputs);
        return provider != null && required > 0 && provider.canReserve(context, required);
    }

    public static SourceChargeReservation reserve(SourceChargePlan plan, SourceChargeContext context,
                                                  long requestedOutputs) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(context, "context");
        if (requestedOutputs <= 0) return null;
        if (plan.isFree()) return new FreeReservation(requestedOutputs);
        SourceChargeProvider provider = PROVIDERS.get(plan.providerId());
        if (provider == null) return null;
        long required = plan.requiredUnits(requestedOutputs);
        if (required <= 0) return null;
        SourceChargeProvider.Reservation reservation = provider.reserve(context, required);
        return reservation != null && reservation.units() == required
                ? new ProviderReservation(plan, requestedOutputs, reservation) : null;
    }

    public static long refundableUnits(SourceChargePlan plan, long reservedOutputs, long deliveredOutputs) {
        Objects.requireNonNull(plan, "plan");
        if (reservedOutputs <= 0 || deliveredOutputs >= reservedOutputs || plan.isFree()) return 0;
        long reservedUnits = plan.requiredUnits(reservedOutputs);
        long deliveredUnits = plan.requiredUnits(Math.max(0, deliveredOutputs));
        return Math.max(0, reservedUnits - deliveredUnits);
    }

    private record FreeReservation(long reservedOutputs) implements SourceChargeReservation {
        @Override public boolean commit(long deliveredOutputs) {
            return deliveredOutputs >= 0 && deliveredOutputs <= reservedOutputs;
        }
        @Override public boolean cancel() { return true; }
    }

    private static final class ProviderReservation implements SourceChargeReservation {
        private final SourceChargePlan plan;
        private final long reservedOutputs;
        private final SourceChargeProvider.Reservation providerReservation;
        private final AtomicBoolean open = new AtomicBoolean(true);

        private ProviderReservation(SourceChargePlan plan, long reservedOutputs,
                                    SourceChargeProvider.Reservation providerReservation) {
            this.plan = plan;
            this.reservedOutputs = reservedOutputs;
            this.providerReservation = providerReservation;
        }

        @Override public long reservedOutputs() { return reservedOutputs; }

        @Override
        public boolean commit(long deliveredOutputs) {
            if (deliveredOutputs < 0 || deliveredOutputs > reservedOutputs || !open.compareAndSet(true, false)) {
                return false;
            }
            try {
                if (providerReservation.settle(plan.requiredUnits(deliveredOutputs))) return true;
            } catch (RuntimeException error) {
                com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.error(
                        "Source reservation settlement threw for {}", plan.providerId(), error);
            }
            open.set(true);
            return false;
        }

        @Override
        public boolean cancel() {
            if (!open.compareAndSet(true, false)) return false;
            try {
                if (providerReservation.settle(0)) return true;
            } catch (RuntimeException error) {
                com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.error(
                        "Source reservation cancellation threw for {}", plan.providerId(), error);
            }
            open.set(true);
            return false;
        }
    }

    private SourceChargeRegistry() {}
}
