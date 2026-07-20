package com.cultivation.cultivation.compat.ae2;

import appeng.api.networking.GridServices;
import appeng.api.storage.StorageCells;
import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.api.source.SourceBypassTransferRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Isolated AE2 integration entry point.
 *
 * <p>The base mod must invoke this class only after confirming that AE2 is
 * loaded. Keeping every AE2 type below this package prevents optional API
 * signatures from leaking into Cultivation's core classes.</p>
 */
public final class Ae2Compat {
    private static boolean gridServiceRegistered;
    private static boolean cellHandlerRegistered;
    private static boolean sourceBypassRegistered;

    private Ae2Compat() {}

    /** Called during the block registry event, matching AE2 addon registration timing. */
    public static synchronized void registerExternalResourceKeyType() {
        CultivationExternalResourceKeyType.register();
        Ae2ExternalKeyBridges.register(CultivationExternalResourceKeyBridge.INSTANCE);
    }

    /** Registers the exchange-cell bridge exactly once for this class loader. */
    public static synchronized void initialize() {
        registerExternalResourceKeyType();
        InstalledAddonExternalKeyBridges.registerPresent();
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
                    ResourceLocation.fromNamespaceAndPath("ae2", "me_storage_source_bypass"),
                    Ae2SourceBypassTarget::find);
            sourceBypassRegistered = true;
            changed = true;
        }
        if (changed) {
            CultivationMod.LOG.info("[Compat/AE2] Registered Xianqiao exchange-cell storage, per-grid owner "
                    + "deduplication, and native source bypass transfer");
        }
    }
}
