package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import com.immortalstorage.immortalstorage.api.source.SourceBypassTransferTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native AE2 storage bridge for high-volume source output. */
public final class Ae2SourceBypassTarget implements SourceBypassTransferTarget {
    private static final IActionSource ACTION_SOURCE = IActionSource.empty();
    private static final String CONDENSER_STORAGE_CLASS = "appeng.blockentity.misc.CondenserMEStorage";

    private final MEStorage storage;

    private Ae2SourceBypassTarget(MEStorage storage) {
        this.storage = storage;
    }

    static SourceBypassTransferTarget find(ServerLevel level, BlockPos targetPos, Direction targetSide) {
        MEStorage storage = level.getCapability(AECapabilities.ME_STORAGE, targetPos, targetSide);
        return storage == null ? null : new Ae2SourceBypassTarget(storage);
    }

    @Override
    public boolean supportsItems() {
        return true;
    }

    @Override
    public boolean supportsFluids() {
        // AE2 19.2.17's condenser ME adapter performs integral division by
        // 125 mB while its standard FluidHandler preserves the fractional
        // conversion. Let SourceVeinBlockEntity fall back to that exact path.
        return !storage.getClass().getName().equals(CONDENSER_STORAGE_CLASS);
    }

    @Override
    public long insertItem(ItemStack prototype, long amount, boolean simulate) {
        return insertItem(storage, prototype, amount, simulate);
    }

    @Override
    public long insertFluid(FluidStack prototype, long amount, boolean simulate) {
        return insertFluid(storage, prototype, amount, simulate);
    }

    static long insertItem(MEStorage storage, ItemStack prototype, long amount, boolean simulate) {
        if (storage == null || prototype == null || prototype.isEmpty() || amount <= 0L) return 0L;
        AEItemKey key = AEItemKey.of(prototype);
        if (key == null) return 0L;
        long inserted = storage.insert(key, amount, action(simulate), ACTION_SOURCE);
        return clampAccepted(inserted, amount);
    }

    static long insertFluid(MEStorage storage, FluidStack prototype, long amount, boolean simulate) {
        if (storage == null || prototype == null || prototype.isEmpty() || amount <= 0L) return 0L;
        AEFluidKey key = AEFluidKey.of(prototype);
        if (key == null) return 0L;
        long inserted = storage.insert(key, amount, action(simulate), ACTION_SOURCE);
        return clampAccepted(inserted, amount);
    }

    private static Actionable action(boolean simulate) {
        return simulate ? Actionable.SIMULATE : Actionable.MODULATE;
    }

    private static long clampAccepted(long inserted, long requested) {
        if (inserted < 0L || inserted > requested) {
            throw new IllegalStateException(
                    "AE2 ME storage returned invalid insertion amount " + inserted + " for " + requested);
        }
        return inserted;
    }
}
