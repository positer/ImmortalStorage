package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.GridServices;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.source.SourceBypassTransferRegistry;
import com.immortalstorage.immortalstorage.block.entity.ModBlockEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Isolated AE2 integration entry point.
 *
 * <p>The base mod must invoke this class only after confirming that AE2 is
 * loaded. Keeping every AE2 type below this package prevents optional API
 * signatures from leaking into ImmortalStorage's core classes.</p>
 */
public final class Ae2Compat {
    private static boolean gridServiceRegistered;
    private static boolean cellHandlerRegistered;
    private static boolean sourceBypassRegistered;

    private Ae2Compat() {}

    /** Called during the block registry event, matching AE2 addon registration timing. */
    public static synchronized void registerExternalResourceKeyType() {
        ImmortalStorageExternalResourceKeyType.register();
        Ae2ExternalKeyBridges.register(ImmortalStorageExternalResourceKeyBridge.INSTANCE);
    }

    /**
     * Gives an AE2 storage bus one native, long-valued view of the manager.
     * The view contains items, fluids and every registered external-resource
     * key, so the bus does not fall back to item/fluid-only NeoForge facades.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.ME_STORAGE, ModBlockEntities.XIANQIAO_MANAGER.get(),
                (manager, side) -> managerStorage(manager));
    }

    private static MEStorage managerStorage(
            com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity manager) {
        UUID owner = manager.getOwner();
        if (owner == null) return null;
        UUID identity = UUID.nameUUIDFromBytes(("xianqiao-manager:" + owner + ":"
                + manager.getBlockPos().asLong()).getBytes(StandardCharsets.UTF_8));
        XianqiaoExchangeStorageCell storage = new XianqiaoExchangeStorageCell(
                owner, identity, Component.translatable("block.immortalstorage.xianqiao_manager"));
        storage.setActive(true);
        return storage;
    }

    /** Registers the exchange-cell bridge exactly once for this class loader. */
    public static synchronized void initialize() {
        registerExternalResourceKeyType();
        InstalledAddonExternalKeyBridges.registerPresent();
        Ae2ExternalKeyBridges.register(RegisteredAe2KeyBridge.INSTANCE);
        Ae2StorageApiDescriptor.Probe amountProbe =
                Ae2StorageApiDescriptor.probe(Ae2Compat.class.getClassLoader());
        XianqiaoExchangeStorageCell.setLongAmountApiSupported(amountProbe.supportsLongAmounts());
        if (amountProbe.compatible()) {
            ImmortalStorageMod.LOG.info(
                    "[Compat/AE2] ME storage amount probe passed: long insert/extract and KeyCounter path are active");
        } else {
            ImmortalStorageMod.LOG.warn(
                    "[Compat/AE2] ME storage amount probe failed ({}); using the confirmed int-safe display fallback",
                    amountProbe.summary());
        }
        boolean changed = false;
        if (!gridServiceRegistered) {
            GridServices.register(XianqiaoExchangeGridService.class, XianqiaoExchangeGridService.class);
            gridServiceRegistered = true;
            changed = true;
        }
        if (!cellHandlerRegistered) {
            StorageCells.addCellHandler(XianqiaoExchangeCellHandler.INSTANCE);
            cellHandlerRegistered = true;
            changed = true;
        }
        if (!sourceBypassRegistered) {
            SourceBypassTransferRegistry.register(
                    Identifier.fromNamespaceAndPath("ae2", "me_storage_source_bypass"),
                    Ae2SourceBypassTarget::find);
            sourceBypassRegistered = true;
            changed = true;
        }
        if (changed) {
            ImmortalStorageMod.LOG.info("[Compat/AE2] Registered Xianqiao exchange-cell storage, per-grid owner "
                    + "deduplication, and native source bypass transfer");
        }
        ImmortalStorageMod.LOG.info("[Compat/AE2] Discovered registered addon key types: {}",
                RegisteredAe2KeyBridge.registeredAddonTypeIds());
    }
}
