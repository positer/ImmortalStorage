package com.cultivation.cultivation.compat.ae2;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.cultivation.cultivation.item.custom.XianqiaoExchangeCellItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Creates one independent AE2 wrapper for every mounted cell slot. */
final class XianqiaoExchangeCellHandler implements ICellHandler {
    static final XianqiaoExchangeCellHandler INSTANCE = new XianqiaoExchangeCellHandler();

    private XianqiaoExchangeCellHandler() {}

    @Override
    public boolean isCell(ItemStack stack) {
        return identities(stack).isPresent();
    }

    @Override
    public @Nullable StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider saveProvider) {
        return identities(stack)
                .map(ids -> new XianqiaoExchangeStorageCell(ids.owner(), ids.diskId(), stack.getHoverName()))
                .orElse(null);
    }

    /** Centralizes bound-stack identity validation for endpoint-backed cell views. */
    static @Nullable XianqiaoExchangeStorageCell createCell(
            ItemStack stack, XianqiaoExchangeStorageCell.EndpointResolver endpointResolver) {
        return identities(stack)
                .map(ids -> new XianqiaoExchangeStorageCell(
                        ids.owner(), ids.diskId(), stack.getHoverName(), endpointResolver))
                .orElse(null);
    }

    private static Optional<CellIdentities> identities(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof XianqiaoExchangeCellItem)) {
            return Optional.empty();
        }
        Optional<UUID> owner = XianqiaoExchangeCellItem.owner(stack);
        Optional<UUID> diskId = XianqiaoExchangeCellItem.diskId(stack);
        return owner.isPresent() && diskId.isPresent()
                ? Optional.of(new CellIdentities(owner.get(), diskId.get()))
                : Optional.empty();
    }

    private record CellIdentities(UUID owner, UUID diskId) {}
}
