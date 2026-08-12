package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.core.HolderLookup;

/**
 * Target-side spelling of NeoForge's pre-26.1 NBT serialization contract.
 * The application keeps this small contract so persisted player data can be
 * migrated without importing an obsolete loader interface.
 */
public interface LegacyNbtSerializable<T> {
    T serializeNBT(HolderLookup.Provider registryAccess);

    void deserializeNBT(HolderLookup.Provider registryAccess, T tag);
}
