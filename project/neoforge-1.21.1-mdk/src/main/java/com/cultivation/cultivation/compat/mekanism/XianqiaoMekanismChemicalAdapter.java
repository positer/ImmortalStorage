package com.cultivation.cultivation.compat.mekanism;

import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ExternalResourceChannels;
import com.cultivation.core.resource.ResourceChannelKey;
import com.cultivation.core.resource.ResourceTransferAction;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Official Mekanism chemical capability over the shared Xianqiao resource ledger.
 *
 * <p>Every registered non-empty chemical is exposed as one stable logical tank.
 * This avoids an index shift when a stored chemical reaches zero and permits a
 * pipe to insert a chemical that is not already present.</p>
 */
public final class XianqiaoMekanismChemicalAdapter implements IChemicalHandler {
    private static volatile List<Chemical> registeredChemicals;
    private static volatile int registeredChemicalCount = -1;

    private final Function<ResourceChannelKey, AtomicEnergyRefill.ResourceStore> storage;

    public XianqiaoMekanismChemicalAdapter(
            Function<ResourceChannelKey, AtomicEnergyRefill.ResourceStore> storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public int getChemicalTanks() {
        return chemicals().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        Chemical chemical = chemical(tank);
        if (chemical == null) return ChemicalStack.EMPTY;
        AtomicEnergyRefill.ResourceStore current = storage.apply(key(chemical));
        long amount = current == null ? 0L : Math.max(0L, current.amount());
        return amount == 0L ? ChemicalStack.EMPTY
                : new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), amount);
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        throw new UnsupportedOperationException(
                "Xianqiao chemicals are transactional and cannot be assigned directly");
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return chemical(tank) == null ? 0L : Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        Chemical chemical = chemical(tank);
        return chemical != null && stack != null && !stack.isEmpty() && stack.is(chemical);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (!isValid(tank, stack)) {
            return stack;
        }
        AtomicEnergyRefill.ResourceStore current = storage.apply(key(stack.getChemical()));
        if (current == null) return stack;
        long accepted = current.insert(stack.getAmount(), transferAction(action));
        requireBounded("insert", accepted, stack.getAmount());
        long remainder = stack.getAmount() - accepted;
        return remainder == 0L ? ChemicalStack.EMPTY : stack.copyWithAmount(remainder);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        if (stack == null || stack.isEmpty()) {
            return stack == null ? ChemicalStack.EMPTY : stack;
        }
        AtomicEnergyRefill.ResourceStore current = storage.apply(key(stack.getChemical()));
        if (current == null) return stack;
        long accepted = current.insert(stack.getAmount(), transferAction(action));
        requireBounded("insert", accepted, stack.getAmount());
        long remainder = stack.getAmount() - accepted;
        return remainder == 0L ? ChemicalStack.EMPTY : stack.copyWithAmount(remainder);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        Chemical chemical = chemical(tank);
        return extract(chemical, amount, action);
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        if (stack == null || stack.isEmpty()) return ChemicalStack.EMPTY;
        return extract(stack.getChemical(), stack.getAmount(), action);
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        if (amount <= 0L) {
            return ChemicalStack.EMPTY;
        }
        for (Chemical chemical : chemicals()) {
            AtomicEnergyRefill.ResourceStore current = storage.apply(key(chemical));
            if (current != null && current.amount() > 0L) {
                return extract(chemical, amount, action);
            }
        }
        return ChemicalStack.EMPTY;
    }

    static ResourceChannelKey key(Chemical chemical) {
        if (chemical == null || MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical)
                .is(MekanismAPI.EMPTY_CHEMICAL_KEY)) {
            throw new IllegalArgumentException("unregistered or empty Mekanism chemical");
        }
        ResourceLocation id = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        if (id == null) {
            throw new IllegalArgumentException("unregistered or empty Mekanism chemical");
        }
        return ExternalResourceChannels.mekanismChemical(id.toString());
    }

    private static Chemical chemical(int tank) {
        List<Chemical> chemicals = chemicals();
        return tank < 0 || tank >= chemicals.size() ? null : chemicals.get(tank);
    }

    private static List<Chemical> chemicals() {
        List<Chemical> current = registeredChemicals;
        int registrySize = MekanismAPI.CHEMICAL_REGISTRY.size();
        if (current != null && registeredChemicalCount == registrySize) return current;
        synchronized (XianqiaoMekanismChemicalAdapter.class) {
            current = registeredChemicals;
            if (current == null || registeredChemicalCount != registrySize) {
                ArrayList<Chemical> discovered = new ArrayList<>();
                for (Chemical chemical : MekanismAPI.CHEMICAL_REGISTRY) {
                    if (chemical != null
                            && !MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical)
                            .is(MekanismAPI.EMPTY_CHEMICAL_KEY)
                            && MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical) != null) {
                        discovered.add(chemical);
                    }
                }
                current = List.copyOf(discovered);
                registeredChemicals = current;
                registeredChemicalCount = registrySize;
            }
        }
        return current;
    }

    static List<Chemical> chemicalsSnapshot() {
        return chemicals();
    }

    private static ResourceTransferAction transferAction(Action action) {
        return action.execute() ? ResourceTransferAction.EXECUTE : ResourceTransferAction.SIMULATE;
    }

    private ChemicalStack extract(Chemical chemical, long amount, Action action) {
        if (chemical == null || amount <= 0L) {
            return ChemicalStack.EMPTY;
        }
        AtomicEnergyRefill.ResourceStore current = storage.apply(key(chemical));
        if (current == null) return ChemicalStack.EMPTY;
        long extracted = current.extract(amount, transferAction(action));
        requireBounded("extract", extracted, amount);
        return extracted == 0L ? ChemicalStack.EMPTY
                : new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.wrapAsHolder(chemical), extracted);
    }

    private static void requireBounded(String operation, long result, long requested) {
        if (result < 0L || result > requested) {
            throw new IllegalStateException(
                    "Mekanism chemical " + operation + " returned " + result + " for " + requested);
        }
    }
}
