package com.cultivation.core.resource;

import java.util.Objects;

/**
 * Loader-neutral transaction used to feed a configured personal-realm device.
 * Existing stored energy is used first. Only the remaining accepted amount may
 * consume whole charge units (immortal yuan), and unused converted energy is
 * returned to the same long-valued storage channel.
 *
 * <p>The target is simulated before execution. If a third-party target accepts
 * less during execution, the internal energy and charge commits are recomputed
 * from that actual accepted amount, preventing overcharging or phantom balance
 * loss. Implementations of the internal store and charge source must preserve
 * the normal server-thread simulation/execution invariant.</p>
 */
public final class AtomicEnergyRefill {
    private AtomicEnergyRefill() {
    }

    public interface ResourceStore {
        long amount();

        long extract(long requested, ResourceTransferAction action);

        long insert(long offered, ResourceTransferAction action);
    }

    public interface ChargeSource {
        long availableUnits();

        long consume(long requestedUnits, ResourceTransferAction action);
    }

    public interface EnergyTarget {
        long insert(long offered, ResourceTransferAction action);
    }

    public static Result transfer(
            long requested,
            long perTickLimit,
            long resourcePerChargeUnit,
            ResourceStore storage,
            ChargeSource chargeSource,
            EnergyTarget target,
            ResourceTransferAction action) {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(chargeSource, "chargeSource");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(action, "action");
        if (requested <= 0L || perTickLimit <= 0L || resourcePerChargeUnit <= 0L) {
            return Result.empty(requested);
        }

        long stored = nonNegative(storage.amount(), "stored resource");
        long availableUnits = nonNegative(chargeSource.availableUnits(), "available charge units");
        long convertible = saturatingMultiply(availableUnits, resourcePerChargeUnit);
        long maximumSupply = saturatingAdd(stored, convertible);
        long offered = Math.min(Math.min(requested, perTickLimit), maximumSupply);
        if (offered <= 0L) return Result.empty(requested);

        long simulatedAcceptance = checkedParticipantResult(
                "target simulation", target.insert(offered, ResourceTransferAction.SIMULATE), offered);
        SupplyPlan simulatedPlan = plan(simulatedAcceptance, stored, resourcePerChargeUnit);
        verifyInternalSimulation(storage, chargeSource, simulatedPlan);

        if (!action.executes()) {
            return simulatedPlan.toResult(requested, simulatedAcceptance, false);
        }

        long executedAcceptance = checkedParticipantResult(
                "target execution", target.insert(simulatedAcceptance, ResourceTransferAction.EXECUTE),
                simulatedAcceptance);
        SupplyPlan executedPlan = plan(executedAcceptance, stored, resourcePerChargeUnit);
        verifyInternalSimulation(storage, chargeSource, executedPlan);
        commitInternal(storage, chargeSource, executedPlan);
        return executedPlan.toResult(
                requested, simulatedAcceptance, executedAcceptance != simulatedAcceptance);
    }

    private static SupplyPlan plan(long delivered, long stored, long conversionRate) {
        if (delivered <= 0L) return SupplyPlan.EMPTY;
        long storedUsed = Math.min(stored, delivered);
        long conversionUsed = delivered - storedUsed;
        long units = ceilDiv(conversionUsed, conversionRate);
        long generated = saturatingMultiply(units, conversionRate);
        long remainder = generated - conversionUsed;
        return new SupplyPlan(delivered, storedUsed, units, remainder);
    }

    private static void verifyInternalSimulation(
            ResourceStore storage, ChargeSource chargeSource, SupplyPlan plan) {
        if (plan.storedUsed > 0L) {
            long extracted = storage.extract(plan.storedUsed, ResourceTransferAction.SIMULATE);
            requireExact("stored resource simulation", plan.storedUsed, extracted);
        }
        if (plan.chargeUnits > 0L) {
            long consumed = chargeSource.consume(plan.chargeUnits, ResourceTransferAction.SIMULATE);
            requireExact("charge simulation", plan.chargeUnits, consumed);
        }
    }

    private static void commitInternal(
            ResourceStore storage, ChargeSource chargeSource, SupplyPlan plan) {
        if (plan.delivered == 0L) return;
        if (plan.storedUsed > 0L) {
            requireExact("stored resource execution", plan.storedUsed,
                    storage.extract(plan.storedUsed, ResourceTransferAction.EXECUTE));
        }
        if (plan.chargeUnits > 0L) {
            requireExact("charge execution", plan.chargeUnits,
                    chargeSource.consume(plan.chargeUnits, ResourceTransferAction.EXECUTE));
        }
        if (plan.conversionRemainder > 0L) {
            requireExact("conversion remainder storage", plan.conversionRemainder,
                    storage.insert(plan.conversionRemainder, ResourceTransferAction.EXECUTE));
        }
    }

    private static long checkedParticipantResult(String phase, long accepted, long offered) {
        if (accepted < 0L || accepted > offered) {
            throw new IllegalStateException(phase + " returned " + accepted
                    + " for an offered amount of " + offered);
        }
        return accepted;
    }

    private static long nonNegative(long value, String label) {
        if (value < 0L) throw new IllegalStateException(label + " must be non-negative");
        return value;
    }

    private static void requireExact(String phase, long expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(phase + " returned " + actual + ", expected " + expected);
        }
    }

    private static long ceilDiv(long value, long divisor) {
        if (value <= 0L) return 0L;
        return 1L + (value - 1L) / divisor;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private record SupplyPlan(
            long delivered, long storedUsed, long chargeUnits, long conversionRemainder) {
        private static final SupplyPlan EMPTY = new SupplyPlan(0L, 0L, 0L, 0L);

        Result toResult(long originalRequest, long simulatedAcceptance, boolean executionReduced) {
            return new Result(originalRequest, delivered, storedUsed, chargeUnits,
                    conversionRemainder, delivered >= Math.max(0L, originalRequest),
                    simulatedAcceptance, executionReduced);
        }
    }

    public record Result(
            long requested,
            long delivered,
            long storedResourceUsed,
            long chargeUnitsConsumed,
            long conversionRemainderStored,
            boolean requestSatisfied,
            long targetSimulatedAcceptance,
            boolean targetExecutionReduced) {
        public Result {
            if (delivered < 0L || storedResourceUsed < 0L || chargeUnitsConsumed < 0L
                    || conversionRemainderStored < 0L || targetSimulatedAcceptance < delivered) {
                throw new IllegalArgumentException("invalid energy-refill result");
            }
        }

        private static Result empty(long requested) {
            return new Result(requested, 0L, 0L, 0L, 0L,
                    requested <= 0L, 0L, false);
        }
    }
}
