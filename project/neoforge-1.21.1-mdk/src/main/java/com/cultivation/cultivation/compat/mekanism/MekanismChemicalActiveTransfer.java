package com.cultivation.cultivation.compat.mekanism;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.LongActiveResourceTransfer;
import com.cultivation.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.function.Function;

/** Two-phase long-native active chemical transfer for one interface face. */
final class MekanismChemicalActiveTransfer {
    static long push(
            Function<com.cultivation.core.resource.ResourceChannelKey,
                    AtomicEnergyRefill.ResourceStore> storage,
            IChemicalHandler target) {
        if (storage == null || target == null) return 0L;
        long total = 0L;
        for (Chemical chemical : XianqiaoMekanismChemicalAdapter.chemicalsSnapshot()) {
            AtomicEnergyRefill.ResourceStore source = storage.apply(
                    XianqiaoMekanismChemicalAdapter.key(chemical));
            if (source == null) continue;
            long committed = LongActiveResourceTransfer.push(
                    source, endpoint(target, chemical, -1));
            total = saturatedAdd(total, committed);
        }
        return total;
    }

    static long pull(
            IChemicalHandler source,
            Function<com.cultivation.core.resource.ResourceChannelKey,
                    AtomicEnergyRefill.ResourceStore> storage) {
        if (source == null || storage == null) return 0L;
        long total = 0L;
        for (int tank = 0; tank < source.getChemicalTanks(); tank++) {
            ChemicalStack visible = source.getChemicalInTank(tank);
            if (visible == null || visible.isEmpty() || visible.getAmount() <= 0L) continue;
            AtomicEnergyRefill.ResourceStore target = storage.apply(
                    XianqiaoMekanismChemicalAdapter.key(visible.getChemical()));
            if (target == null) continue;
            long committed = LongActiveResourceTransfer.pull(
                    endpoint(source, visible.getChemical(), tank), target);
            total = saturatedAdd(total, committed);
        }
        return total;
    }

    private static ChemicalStack stack(Chemical chemical, long amount) {
        return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), amount);
    }

    private static long amount(ChemicalStack stack) {
        return stack == null || stack.isEmpty() ? 0L : stack.getAmount();
    }

    private static LongActiveResourceTransfer.Endpoint endpoint(
            IChemicalHandler handler, Chemical chemical, int tank) {
        return new LongActiveResourceTransfer.Endpoint() {
            @Override public boolean canInsert() { return true; }
            @Override public boolean canExtract() { return tank >= 0; }
            @Override
            public long insert(long offered, ResourceTransferAction action) {
                if (offered <= 0L) return 0L;
                ChemicalStack proposed = stack(chemical, offered);
                ChemicalStack remainder = handler.insertChemical(
                        proposed, mekanismAction(action));
                long remaining = amount(remainder);
                if (remaining < 0L || remaining > offered) {
                    throw new IllegalStateException("invalid Mekanism chemical insert remainder");
                }
                return offered - remaining;
            }
            @Override
            public long extract(long requested, ResourceTransferAction action) {
                if (tank < 0 || requested <= 0L) return 0L;
                ChemicalStack extracted = handler.extractChemical(
                        tank, requested, mekanismAction(action));
                long moved = amount(extracted);
                if (moved < 0L || moved > requested
                        || moved > 0L && !extracted.is(chemical)) {
                    throw new IllegalStateException("invalid Mekanism chemical extraction");
                }
                return moved;
            }
        };
    }

    private static Action mekanismAction(ResourceTransferAction action) {
        return action.executes() ? Action.EXECUTE : Action.SIMULATE;
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private MekanismChemicalActiveTransfer() {}
}
