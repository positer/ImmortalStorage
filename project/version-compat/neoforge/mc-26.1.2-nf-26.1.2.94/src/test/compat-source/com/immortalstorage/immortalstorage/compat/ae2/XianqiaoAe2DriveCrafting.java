package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.blockentity.storage.DriveBlockEntity;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Loader-safe entry point for the drive half of the exchange crafting bridge.
 *
 * <p>The mixin that runs this code lives in {@code mixin/ae2} and is forbidden from naming any
 * {@code appeng} type in an import (the optional-AE2 boundary test scans every source file
 * outside {@code compat/ae2}), so it only forwards {@link BlockEntity} references into this
 * class. Every AE2 type stays here.</p>
 *
 * <p><b>Registration timing is the whole point of this class.</b> {@code ManagedGridNode.addService}
 * calls {@code getInitData()}, which throws once {@code create(...)} has cleared the init data;
 * the drive's node is created from {@code AENetworkedBlockEntity.onReady()}, i.e. after the block
 * entity constructor returned. Attaching the service from the constructor tail is therefore the
 * only point where AE2 still accepts it. The node state is re-checked here before touching it so a
 * future AE2 change degrades to "no recipes from this drive" instead of failing world load.</p>
 *
 * <p>Handles are kept in a weak map so a drive that leaves the world cannot pin its provider (or
 * its level) in memory; the block entity is strongly held by the chunk while it exists.</p>
 */
public final class XianqiaoAe2DriveCrafting {
    private static final Map<BlockEntity, XianqiaoAe2DriveCraftingProvider> PROVIDERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private XianqiaoAe2DriveCrafting() {}

    /**
     * Attaches the exchange crafting provider to one drive's main node.
     *
     * <p>Called from the drive constructor's tail. Returns nothing: the mixin keeps no state, it
     * only forwards the block entity, and every later lookup goes through the weak map.</p>
     */
    public static void attach(BlockEntity blockEntity) {
        if (!(blockEntity instanceof DriveBlockEntity drive)) return;
        IManagedGridNode node = drive.getMainNode();
        if (node == null) return;
        if (node.getNode() != null) {
            // The node already ran create(): addService would throw IllegalStateException and the
            // drive would load without recipes rather than crash, but there is nothing to attach.
            ImmortalStorageMod.LOG.debug(
                    "[Compat/AE2] drive node at {} was already initialised; no exchange provider attached",
                    drive.getBlockPos());
            return;
        }
        XianqiaoAe2DriveCraftingProvider provider = new XianqiaoAe2DriveCraftingProvider(drive, node);
        try {
            node.addService(ICraftingProvider.class, provider);
        } catch (Throwable failure) {
            ImmortalStorageMod.LOG.error(
                    "[Compat/AE2] could not register the drive exchange crafting provider; "
                            + "this drive will store but not craft", failure);
            return;
        }
        PROVIDERS.put(blockEntity, provider);
    }

    /** Called from the drive's slot-read hook; cheap recognition only (no rebuild). */
    public static void noteSlotRead(BlockEntity blockEntity, int slot) {
        XianqiaoAe2DriveCraftingProvider provider = PROVIDERS.get(blockEntity);
        if (provider != null) provider.noteSlotRead(slot);
    }

    /** Called after the drive rebuilt its slot watchers, which is when a disk appeared or left. */
    public static void noteStateRefreshed(BlockEntity blockEntity) {
        XianqiaoAe2DriveCraftingProvider provider = PROVIDERS.get(blockEntity);
        if (provider != null) provider.refresh();
    }
}
