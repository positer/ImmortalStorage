package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.dimension.RealmHelper;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
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
    private @Nullable ImmortalStoragePlayerData observedCapabilityData;
    private int observedCapabilityLayout = Integer.MIN_VALUE;

    public XianqiaoManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XIANQIAO_MANAGER.get(), pos, state);
    }

    public void setOwner(Player player) {
        tryClaimOwner(player);
    }

    /** First claim wins; opening the menu can never silently rebind another owner's manager. */
    public boolean tryClaimOwner(Player player) {
        if (player == null || ImmortalStoragePlayerData.get(player).getStage() < 6
                || level == null
                || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !RealmHelper.isInOwnRealm(serverPlayer)) return false;
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(player);
        if (accepted && !java.util.Objects.equals(before, ownerBinding.owner())) {
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
        return Component.translatable("block.immortalstorage.xianqiao_manager");
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
        ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
        if (player == null) {
            clearCachedEndpoint();
            return null;
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        boolean needsFluids = data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE;
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
        ImmortalStoragePlayerData data = null;
        int layout = 0;
        if (owner != null && ImmortalStorageDimensions.isPersonalRealmFor(serverLevel.dimension(), owner)) {
            ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
            if (player != null) {
                data = ImmortalStoragePlayerData.get(player);
                if (data.getStage() >= 10) layout = 3;
                else if (data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE) layout = 2;
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
