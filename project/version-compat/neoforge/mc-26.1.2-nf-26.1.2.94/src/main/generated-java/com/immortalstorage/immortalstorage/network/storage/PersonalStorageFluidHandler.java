package com.immortalstorage.immortalstorage.network.storage;

import com.immortalstorage.core.amount.LongAmountBridge;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * NeoForge int-call bridge over the terminal-native long-mB fluid namespace.
 * Each fluid-and-components identity is exposed as one virtual tank.
 */
public final class PersonalStorageFluidHandler implements IFluidHandler, TerminalFluidStorage {
    private final ImmortalStoragePlayerData data;
    private final Runnable onChanged;
    private final BooleanSupplier accessAllowed;
    private final MinecraftServer server;
    private final UUID owner;
    private long cachedRevision = Long.MIN_VALUE;
    private List<Map.Entry<TerminalFluidKey, Long>> cachedEntries = List.of();
    private long observedPhysicalGeneration = Long.MIN_VALUE;
    private long observedSourceGeneration = Long.MIN_VALUE;
    private long viewRevision;

    private boolean isAvailable() {
        return data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE
                && accessAllowed.getAsBoolean();
    }

    public PersonalStorageFluidHandler(ImmortalStoragePlayerData data, Runnable onChanged) {
        this(data, onChanged, () -> true);
    }

    public PersonalStorageFluidHandler(ImmortalStoragePlayerData data, Runnable onChanged, BooleanSupplier accessAllowed) {
        this(data, onChanged, accessAllowed, null, null);
    }

    public PersonalStorageFluidHandler(ImmortalStoragePlayerData data, Runnable onChanged,
                                       BooleanSupplier accessAllowed,
                                       MinecraftServer server, UUID owner) {
        if (data == null) throw new IllegalArgumentException("player data is required");
        this.data = data;
        this.onChanged = onChanged == null ? () -> {} : onChanged;
        this.accessAllowed = accessAllowed == null ? () -> false : accessAllowed;
        this.server = server;
        this.owner = owner;
    }

    @Override
    public long revision() {
        if (!isAvailable()) return 0L;
        if (!hasSourceDirectory()) return data.getXianqiaoFluidStorageGeneration();
        long physical = data.getXianqiaoFluidStorageGeneration();
        long source = data.getXianqiaoSourceFluidGeneration();
        if (observedPhysicalGeneration == Long.MIN_VALUE) {
            viewRevision = Math.max(0L, physical);
        } else if (physical != observedPhysicalGeneration || source != observedSourceGeneration) {
            viewRevision = nextRevision(viewRevision);
        }
        observedPhysicalGeneration = physical;
        observedSourceGeneration = source;
        return viewRevision;
    }

    @Override
    public Map<TerminalFluidKey, Long> snapshot() {
        if (!isAvailable()) return Map.of();
        LinkedHashMap<TerminalFluidKey, Long> merged = new LinkedHashMap<>(data.getXianqiaoFluidAmounts());
        if (hasSourceDirectory()) {
            SourceVeinStorageIndex.fluidSnapshot(server, owner).forEach((key, amount) -> {
                long previous = merged.getOrDefault(key, 0L);
                merged.put(key, amount == Integer.MAX_VALUE ? Integer.MAX_VALUE
                        : saturatingAdd(previous, amount));
            });
        }
        return Map.copyOf(merged);
    }

    @Override
    public long insert(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (!isAvailable()) return 0L;
        long accepted = data.insertXianqiaoFluid(key, amountMb, action);
        if (action == TerminalStorageAction.EXECUTE && accepted > 0L) onChanged.run();
        return accepted;
    }

    @Override
    public long extract(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (!isAvailable()) return 0L;
        long physical = data.extractXianqiaoFluid(key, amountMb, action);
        long remaining = Math.max(0L, amountMb - physical);
        long source = hasSourceDirectory() && remaining > 0L
                ? SourceVeinStorageIndex.extractFluid(server, owner, key, remaining, action) : 0L;
        long extracted = saturatingAdd(physical, source);
        if (action == TerminalStorageAction.EXECUTE && extracted > 0L) onChanged.run();
        return extracted;
    }

    @Override
    public int getTanks() {
        if (!isAvailable()) return 0;
        // A permanent empty import tank keeps enumeration-based buses writing
        // even when all stored identities display the int compatibility cap.
        return entries().size() + 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        Map.Entry<TerminalFluidKey, Long> entry = entryAt(tank);
        if (entry == null) return FluidStack.EMPTY;
        return entry.getKey().prototype().copyWithAmount(toIntAmount(entry.getValue()));
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank < 0 || tank >= getTanks() ? 0 : Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return isAvailable() && tank >= 0 && tank < getTanks() && !stack.isEmpty();
    }

    @Override
    public int fill(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || action == null || !isAvailable()) return 0;
        long accepted = insert(TerminalFluidKey.of(resource), resource.getAmount(), toTerminalAction(action));
        return toIntAmount(accepted);
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || action == null || !isAvailable()) return FluidStack.EMPTY;
        long extracted = extract(TerminalFluidKey.of(resource), resource.getAmount(), toTerminalAction(action));
        return extracted <= 0L ? FluidStack.EMPTY : resource.copyWithAmount(toIntAmount(extracted));
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || action == null || !isAvailable()) return FluidStack.EMPTY;
        Map.Entry<TerminalFluidKey, Long> first = entryAt(0);
        if (first == null) return FluidStack.EMPTY;
        long extracted = extract(first.getKey(), maxDrain, toTerminalAction(action));
        return extracted <= 0L ? FluidStack.EMPTY : first.getKey().prototype().copyWithAmount(toIntAmount(extracted));
    }

    private List<Map.Entry<TerminalFluidKey, Long>> entries() {
        if (!isAvailable()) return List.of();
        long revision = revision();
        if (revision == cachedRevision) return cachedEntries;
        List<Map.Entry<TerminalFluidKey, Long>> snapshot = new ArrayList<>();
        snapshot().forEach((key, amount) -> {
            if (amount != null && amount > 0L) snapshot.add(Map.entry(key, amount));
        });
        cachedEntries = List.copyOf(snapshot);
        cachedRevision = revision;
        return cachedEntries;
    }

    private Map.Entry<TerminalFluidKey, Long> entryAt(int tank) {
        List<Map.Entry<TerminalFluidKey, Long>> entries = entries();
        return tank < 0 || tank >= entries.size() ? null : entries.get(tank);
    }

    private static TerminalStorageAction toTerminalAction(FluidAction action) {
        return action.execute() ? TerminalStorageAction.EXECUTE : TerminalStorageAction.SIMULATE;
    }

    private static int toIntAmount(long amount) {
        return LongAmountBridge.saturatingInt(amount);
    }

    private boolean hasSourceDirectory() {
        return server != null && owner != null;
    }

    private static long saturatingAdd(long left, long right) {
        if (left <= 0L) return Math.max(0L, right);
        if (right <= 0L) return left;
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long nextRevision(long revision) {
        return revision == Long.MAX_VALUE ? 0L : revision + 1L;
    }
}
