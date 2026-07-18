package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.menu.ModMenus;
import com.cultivation.cultivation.api.storage.PersonalStorageEndpoint;
import com.cultivation.cultivation.dimension.CultivationDimensions;
import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 *                                   
 *                UI
 *                          ?AE2/RS ? */
public class XianqiaoManagerBlockEntity extends BlockEntity implements MenuProvider {
    private final OwnerBinding ownerBinding = new OwnerBinding();
    private @Nullable PersonalStorageNetwork.Endpoint cachedEndpoint;
    private @Nullable UUID cachedEndpointOwner;
    private boolean observedCapabilityState;
    private @Nullable UUID observedCapabilityOwner;
    private @Nullable CultivationPlayerData observedCapabilityData;
    private int observedCapabilityLayout = Integer.MIN_VALUE;

    public XianqiaoManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XIANQIAO_MANAGER.get(), pos, state);
    }

    public void setOwner(Player player) {
        tryClaimOwner(player);
    }

    /** First claim wins; opening the menu can never silently rebind another owner's manager. */
    public boolean tryClaimOwner(Player player) {
        if (player == null || CultivationPlayerData.get(player).getStage() < 6
                || level == null
                || !CultivationDimensions.isPersonalRealmFor(level.dimension(), player.getUUID())) return false;
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(player.getUUID());
        if (accepted && before == null) {
            invalidateCapabilityCache();
            setChanged();
        }
        return accepted;
    }

    public @Nullable UUID getOwner() {
        return ownerBinding.owner();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cultivation.xianqiao_manager");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        if (!tryClaimOwner(p)) return null;
        if (ModMenus.XIANQIAO_STORAGE == null) return null;
        return ModMenus.XIANQIAO_STORAGE.get().create(id, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        ownerBinding.save(tag, "Owner");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        ownerBinding.load(tag, "Owner");
        clearCachedEndpoint();
    }

    public IItemHandler getItemHandler() {
        PersonalStorageEndpoint endpoint = storageEndpoint();
        return endpoint == null ? null : endpoint.itemHandler();
    }

    /** Optional AE2/NeoForge fluid bridge; never aliases item slots. */
    public @Nullable IFluidHandler getFluidHandler() {
        PersonalStorageEndpoint endpoint = fluidStorageEndpoint();
        return endpoint == null ? null : endpoint.fluidHandler();
    }

    public @Nullable PersonalStorageEndpoint storageEndpoint() {
        return currentEndpoint();
    }

    private @Nullable PersonalStorageEndpoint fluidStorageEndpoint() {
        return currentEndpoint();
    }

    private @Nullable PersonalStorageNetwork.Endpoint currentEndpoint() {
        UUID owner = getOwner();
        if (!(level instanceof ServerLevel serverLevel) || owner == null) {
            clearCachedEndpoint();
            return null;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            clearCachedEndpoint();
            return null;
        }
        CultivationPlayerData data = CultivationPlayerData.get(player);
        boolean needsFluids = data.getStage() >= CultivationPlayerData.XIANQIAO_FLUID_UNLOCK_STAGE;
        if (cachedEndpoint != null
                && owner.equals(cachedEndpointOwner)
                && cachedEndpoint.data() == data
                && cachedEndpoint.stage() >= 6
                && cachedEndpoint.itemHandler().getSlots() > 0
                && (cachedEndpoint.fluidHandler() != null) == needsFluids) {
            return cachedEndpoint;
        }
        cachedEndpoint = PersonalStorageNetwork.resolveInOwnerRealm(
                serverLevel, owner, this::setChanged);
        cachedEndpointOwner = cachedEndpoint == null ? null : owner;
        return cachedEndpoint;
    }

    private void clearCachedEndpoint() {
        cachedEndpoint = null;
        cachedEndpointOwner = null;
    }

    /**
     * NeoForge caches block capabilities. A bounded once-per-second shape
     * check invalidates that external cache when owner/data, the stage-seven
     * fluid boundary, or the stage-ten virtual-item layout changes. The
     * handlers still perform their own live access checks on every call.
     */
    public static void serverTick(ServerLevel level, BlockPos pos, XianqiaoManagerBlockEntity manager) {
        if (Math.floorMod(level.getGameTime(), 20L) != Math.floorMod(pos.asLong(), 20L)) return;
        manager.refreshCapabilityLayout(level);
    }

    private void refreshCapabilityLayout(ServerLevel serverLevel) {
        UUID owner = getOwner();
        CultivationPlayerData data = null;
        int layout = 0;
        if (owner != null && CultivationDimensions.isPersonalRealmFor(serverLevel.dimension(), owner)) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                data = CultivationPlayerData.get(player);
                if (data.getStage() >= 10) layout = 3;
                else if (data.getStage() >= CultivationPlayerData.XIANQIAO_FLUID_UNLOCK_STAGE) layout = 2;
                else if (data.getStage() >= 6) layout = 1;
            }
        }
        if (!observedCapabilityState
                || !java.util.Objects.equals(observedCapabilityOwner, owner)
                || observedCapabilityData != data
                || observedCapabilityLayout != layout) {
            observedCapabilityState = true;
            observedCapabilityOwner = owner;
            observedCapabilityData = data;
            observedCapabilityLayout = layout;
            clearCachedEndpoint();
            serverLevel.invalidateCapabilities(worldPosition);
        }
    }

    private void invalidateCapabilityCache() {
        clearCachedEndpoint();
        observedCapabilityState = false;
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
    }

    @Override
    public void setRemoved() {
        clearCachedEndpoint();
        super.setRemoved();
    }
}
