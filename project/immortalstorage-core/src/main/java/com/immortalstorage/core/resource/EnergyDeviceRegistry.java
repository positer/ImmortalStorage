package com.immortalstorage.core.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent-model registry for devices explicitly configured by the spirit
 * staff. A secondary owner/dimension index lets the server tick only relevant
 * bindings instead of scanning all block entities in a personal realm.
 */
public final class EnergyDeviceRegistry {
    private final Map<EnergyDeviceKey, EnergyDeviceBinding> bindings = new LinkedHashMap<>();
    private final Map<ScopeKey, LinkedHashSet<EnergyDeviceKey>> scopeIndex = new LinkedHashMap<>();
    private long revision;

    public long revision() {
        return revision;
    }

    /** Returns true only when the persisted binding actually changed. */
    public boolean configure(EnergyDeviceKey key, BlockSide inputSide, long inputLimitPerTick) {
        EnergyDeviceBinding replacement =
                new EnergyDeviceBinding(key, inputSide, inputLimitPerTick);
        EnergyDeviceBinding previous = bindings.get(key);
        if (replacement.equals(previous)) return false;
        bindings.put(key, replacement);
        if (previous == null) {
            scopeIndex.computeIfAbsent(scopeOf(key), ignored -> new LinkedHashSet<>()).add(key);
        }
        advanceRevision();
        return true;
    }

    public Optional<EnergyDeviceBinding> get(EnergyDeviceKey key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(bindings.get(key));
    }

    public boolean remove(EnergyDeviceKey key) {
        Objects.requireNonNull(key, "key");
        if (bindings.remove(key) == null) return false;
        ScopeKey scope = scopeOf(key);
        Set<EnergyDeviceKey> indexed = scopeIndex.get(scope);
        if (indexed != null) {
            indexed.remove(key);
            if (indexed.isEmpty()) scopeIndex.remove(scope);
        }
        advanceRevision();
        return true;
    }

    /** Immutable view containing only this owner's configured devices in one dimension. */
    public List<EnergyDeviceBinding> bindingsFor(UUID owner, String dimensionId) {
        ScopeKey scope = new ScopeKey(owner, dimensionId);
        Set<EnergyDeviceKey> keys = scopeIndex.get(scope);
        if (keys == null || keys.isEmpty()) return List.of();
        List<EnergyDeviceBinding> result = new ArrayList<>(keys.size());
        for (EnergyDeviceKey key : keys) {
            EnergyDeviceBinding binding = bindings.get(key);
            if (binding != null) result.add(binding);
        }
        return List.copyOf(result);
    }

    public List<EnergyDeviceBinding> snapshot() {
        return List.copyOf(bindings.values());
    }

    public void restore(List<EnergyDeviceBinding> restored, long restoredRevision) {
        Objects.requireNonNull(restored, "restored");
        bindings.clear();
        scopeIndex.clear();
        for (EnergyDeviceBinding binding : restored) {
            Objects.requireNonNull(binding, "binding");
            bindings.put(binding.key(), binding);
        }
        for (EnergyDeviceKey key : bindings.keySet()) {
            scopeIndex.computeIfAbsent(scopeOf(key), ignored -> new LinkedHashSet<>()).add(key);
        }
        revision = Math.max(0L, restoredRevision);
    }

    private static ScopeKey scopeOf(EnergyDeviceKey key) {
        return new ScopeKey(key.owner(), key.dimensionId());
    }

    private void advanceRevision() {
        if (revision < Long.MAX_VALUE) revision++;
    }

    private record ScopeKey(UUID owner, String dimensionId) {
        private ScopeKey {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(dimensionId, "dimensionId");
        }
    }
}
