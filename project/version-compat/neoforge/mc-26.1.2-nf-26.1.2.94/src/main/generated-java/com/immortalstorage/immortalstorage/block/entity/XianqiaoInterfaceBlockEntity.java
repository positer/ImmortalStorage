package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Owner-bound, dimension-independent bridge between automation and Xianqiao
 * item storage. The block owns only nine real one-stack output buffers; all
 * bulk storage remains in the player's stage-six-or-higher Xianqiao.
 */
public class XianqiaoInterfaceBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity implements MenuProvider {
    private static final String OWNER_TAG = "Owner";
    private static final String BUFFERS_TAG = "Buffers";
    private static final String ITEM_SLOTS_TAG = "ItemSlots";
    private static final String SIDE_MODES_TAG = "SideModes";
    private static final String CONFIG_REVISION_TAG = "ConfigRevision";
    private static final String ACTIVE_PULL_TAG = "ActivePull";
    private static final String ACTIVE_PUSH_TAG = "ActivePush";

    public enum SideMode {
        PULL(0),
        PUSH(1),
        DISABLED(2);

        private final int persistedId;

        SideMode(int persistedId) {
            this.persistedId = persistedId;
        }

        public int persistedId() {
            return persistedId;
        }

        public static SideMode byId(int id) {
            for (SideMode mode : values()) if (mode.persistedId == id) return mode;
            return DISABLED;
        }
    }

    private final OwnerBinding ownerBinding = new OwnerBinding();
    private final TerminalItemStorage storageBridge = new OwnerStorageBridge();
    private final TerminalFluidStorage fluidStorageBridge = new OwnerFluidStorageBridge();
    private final ExternalResourceStorage externalResourceStorageBridge =
            new OwnerExternalResourceStorageBridge();
    private final XianqiaoInterfaceInventory inventory = new XianqiaoInterfaceInventory(
            storageBridge, fluidStorageBridge, externalResourceStorageBridge,
            this::hasLiveStorage, this::markInventoryChanged, this::markConfigurationChanged);
    private final IFluidHandler fluidInventory = new XianqiaoInterfaceFluidInventory(inventory);
    private final SideMode[] sideModes = new SideMode[Direction.values().length];
    private final IItemHandler[] sidedItemHandlers = new IItemHandler[Direction.values().length];
    private final IFluidHandler[] sidedFluidHandlers = new IFluidHandler[Direction.values().length];
    private final IEnergyStorage[] sidedEnergyHandlers = new IEnergyStorage[Direction.values().length];
    private final IItemHandler unsidedItemHandler;
    private final IFluidHandler unsidedFluidHandler;
    private final IEnergyStorage unsidedEnergyHandler;
    private long configRevision;
    private boolean activePullEnabled;
    private boolean activePushEnabled;
    private ReleaseState releaseState = ReleaseState.OPEN;
    private boolean preserveRetainedItemBuffersInDrop;
    private long endpointCacheTick = Long.MIN_VALUE;
    private @Nullable UUID endpointCacheOwner;
    private @Nullable PersonalStorageEndpoint endpointCache;

    public XianqiaoInterfaceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.XIANQIAO_INTERFACE.get(), pos, state);
    }

    XianqiaoInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(sideModes, SideMode.DISABLED);
        for (Direction side : Direction.values()) {
            sidedItemHandlers[side.ordinal()] = new XianqiaoInterfaceSidedItemHandler(
                    inventory, side);
            sidedFluidHandlers[side.ordinal()] = new XianqiaoInterfaceSidedFluidHandler(
                    fluidInventory, inventory, side);
            sidedEnergyHandlers[side.ordinal()] = new XianqiaoInterfaceEnergyStorage(
                    () -> resolveExternalResourcePipeStore(ExternalResourceChannels.FE, side));
        }
        unsidedItemHandler = new XianqiaoInterfaceSidedItemHandler(
                inventory, null);
        unsidedFluidHandler = new XianqiaoInterfaceSidedFluidHandler(
                fluidInventory, inventory, null);
        unsidedEnergyHandler = new XianqiaoInterfaceEnergyStorage(
                () -> resolveExternalResourceCache(ExternalResourceChannels.FE));
    }

    public @Nullable UUID getOwner() {
        return ownerBinding.owner();
    }

    /** First placement binds once; neither opening nor capability lookup calls this method. */
    public boolean tryBindOwner(Player placer) {
        if (placer == null) return false;
        if (ImmortalStoragePlayerData.get(placer).getStage() < 6) return false;
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(placer);
        if (accepted && !java.util.Objects.equals(before, ownerBinding.owner())) {
            clearEndpointCache();
            setChanged();
            if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
        }
        return accepted;
    }

    boolean tryBindOwner(@Nullable UUID candidate, int stage) {
        if (candidate == null || stage < 6) return false;
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(candidate);
        if (accepted && before == null) {
            clearEndpointCache();
            setChanged();
            if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
        }
        return accepted;
    }

    public boolean canUse(Player player) {
        if (player == null) return false;
        return ImmortalStoragePlayerData.get(player).getStage() >= 6 && ownerBinding.isOwner(player);
    }

    static boolean canUse(@Nullable UUID actor, int stage, @Nullable UUID owner) {
        return actor != null && owner != null && stage >= 6 && owner.equals(actor);
    }

    /** Placement gate for BLOCK_ENTITY_DATA preserved owner bindings. */
    public static boolean canPlaceStackFor(ItemStack stack, @Nullable UUID placer) {
        if (stack == null || placer == null) return false;
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) return true;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(OWNER_TAG)) return true;
        return com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, OWNER_TAG) && placer.equals(com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, OWNER_TAG));
    }

    public XianqiaoInterfaceInventory getInventory() {
        return inventory;
    }

    /** Each queried face owns a stable wrapper that re-checks its live mode on every call. */
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == null ? unsidedItemHandler : sidedItemHandlers[side.ordinal()];
    }

    /** Unsided fluid access is deliberately disabled so no caller can bypass a face policy. */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return side == null ? unsidedFluidHandler : sidedFluidHandlers[side.ordinal()];
    }

    /** Standard FE uses the same live per-face PULL/PUSH/DISABLED policy. */
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return side == null ? unsidedEnergyHandler : sidedEnergyHandlers[side.ordinal()];
    }

    /**
     * Loader-neutral live resource resolver shared by isolated optional
     * capability adapters. Offline owners and stages below eight fail closed.
     */
    public @Nullable AtomicEnergyRefill.ResourceStore resolveExternalResourceStore(
            ResourceChannelKey channel) {
        if (!(level instanceof ServerLevel serverLevel) || channel == null) return null;
        UUID owner = ownerBinding.owner();
        if (owner == null) return null;
        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
        if (player == null || ImmortalStoragePlayerData.get(player).getStage() < 8) return null;
        return ImmortalStoragePlayerData.get(player).externalResourceStore(channel);
    }

    /**
     * Resolves only this interface block's configured real cache. Directionless
     * target-mod block interactions use this view and never reach through to
     * the owner's backing ledger.
     */
    public @Nullable AtomicEnergyRefill.ResourceStore resolveExternalResourceCache(
            ResourceChannelKey channel) {
        if (channel == null || !hasLiveStorage() || !inventory.hasExternalTarget(channel)) return null;
        return withConfiguredConversion(channel, inventory.externalCacheStore(channel));
    }

    /** Directionless integrations insert into Xianqiao but expose only configured cache for reads. */
    public AtomicEnergyRefill.ResourceStore resolveDirectionlessExternalResource(
            ResourceChannelKey channel) {
        return new AtomicEnergyRefill.ResourceStore() {
            @Override
            public long amount() {
                AtomicEnergyRefill.ResourceStore cache = resolveExternalResourceCache(channel);
                return cache == null ? 0L : cache.amount();
            }

            @Override
            public long extract(long requested, ResourceTransferAction action) {
                AtomicEnergyRefill.ResourceStore cache = resolveExternalResourceCache(channel);
                return cache == null ? 0L : cache.extract(requested, action);
            }

            @Override
            public long insert(long offered, ResourceTransferAction action) {
                AtomicEnergyRefill.ResourceStore ledger = resolveExternalResourceStore(channel);
                return ledger == null ? 0L : ledger.insert(offered, action);
            }
        };
    }

    /** Sided cache view for directional optional capabilities. */
    public @Nullable AtomicEnergyRefill.ResourceStore resolveExternalResourceCache(
            ResourceChannelKey channel, @Nullable Direction side) {
        if (channel == null || side == null || !hasLiveStorage()
                || !inventory.hasExternalTarget(channel, side)) return null;
        return withConfiguredConversion(channel, inventory.externalCacheStore(channel, side));
    }

    /** Pipe insertion is face-independent; pipe extraction retains the slot face mask. */
    public @Nullable AtomicEnergyRefill.ResourceStore resolveExternalResourcePipeStore(
            ResourceChannelKey channel, @Nullable Direction side) {
        AtomicEnergyRefill.ResourceStore insertion = resolveExternalResourceCache(channel);
        if (insertion == null) return null;
        AtomicEnergyRefill.ResourceStore extraction = side == null
                ? insertion : resolveExternalResourceCache(channel, side);
        return new AtomicEnergyRefill.ResourceStore() {
            @Override public long amount() { return extraction == null ? 0L : extraction.amount(); }
            @Override public long extract(long requested, ResourceTransferAction action) {
                return extraction == null ? 0L : extraction.extract(requested, action);
            }
            @Override public long insert(long offered, ResourceTransferAction action) {
                return insertion.insert(offered, action);
            }
        };
    }

    /** PULL faces commit directly to Xianqiao; PUSH faces expose only their configured cache. */
    public @Nullable AtomicEnergyRefill.ResourceStore resolveExternalResourceFaceStore(
            ResourceChannelKey channel, @Nullable Direction side) {
        if (channel == null || side == null) return null;
        return switch (getSideMode(side)) {
            // Input is keyed by the incoming resource itself; it must not require a preconfigured cache slot.
            case PULL -> resolveExternalResourceStore(channel);
            case PUSH -> inventory.hasExternalTarget(channel, side)
                    ? resolveExternalResourceCache(channel, side) : null;
            case DISABLED -> null;
        };
    }

    private AtomicEnergyRefill.ResourceStore withConfiguredConversion(
            ResourceChannelKey channel, AtomicEnergyRefill.ResourceStore cache) {
        // Legacy conversion settings remain loadable for old TOML files, but
        // external namespaces are never backed by Immortal Yuan anymore.
        return cache;
    }

    public SideMode getSideMode(@Nullable Direction side) {
        return side == null ? SideMode.DISABLED : sideModes[side.ordinal()];
    }

    public void setSideMode(@Nullable Direction side, @Nullable SideMode mode) {
        if (side == null || mode == null || sideModes[side.ordinal()] == mode) return;
        sideModes[side.ordinal()] = mode;
        markConfigurationChanged();
        if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
    }

    public long getConfigRevision() {
        return configRevision;
    }

    public boolean isActivePullEnabled() {
        return activePullEnabled;
    }

    public void setActivePullEnabled(boolean enabled) {
        if (activePullEnabled == enabled) return;
        activePullEnabled = enabled;
        markConfigurationChanged();
    }

    public boolean isActivePushEnabled() {
        return activePushEnabled;
    }

    public void setActivePushEnabled(boolean enabled) {
        if (activePushEnabled == enabled) return;
        activePushEnabled = enabled;
        markConfigurationChanged();
    }

    /** One scheduling round checks all nine independent mixed-resource caches. */
    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        inventory.replenishAllSlots(TerminalStorageAction.EXECUTE);
        for (Direction side : Direction.values()) {
            SideMode mode = getSideMode(side);
            if (mode == SideMode.PUSH && activePushEnabled) pushToSide(serverLevel, side);
            if (mode == SideMode.PULL && activePullEnabled) pullFromSide(serverLevel, side);
        }
        XianqiaoInterfaceCompatHooks.serverTick(this, serverLevel);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        XianqiaoInterfaceCompatHooks.onLoad(this);
    }

    private void pushToSide(ServerLevel serverLevel, Direction side) {
        BlockPos targetPos = worldPosition.relative(side);
        // Query the neighbor through the contacted face so its sided input rules remain authoritative.
        IItemHandler itemTarget = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(serverLevel.getCapability(
                Capabilities.Item.BLOCK, targetPos, side.getOpposite()));
        IFluidHandler fluidTarget = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluidHandler(serverLevel.getCapability(
                Capabilities.Fluid.BLOCK, targetPos, side.getOpposite()));
        IEnergyStorage energyTarget = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energyHandler(serverLevel.getCapability(
                Capabilities.Energy.BLOCK, targetPos, side.getOpposite()));
        for (int slot = 0; slot < XianqiaoInterfaceInventory.SLOT_COUNT; slot++) {
            if (!inventory.isOutputFaceEnabled(slot, side)) continue;
            if (itemTarget != null) pushItemSlot(slot, itemTarget);
            if (fluidTarget != null) pushFluidSlot(slot, fluidTarget);
        }
        AtomicEnergyRefill.ResourceStore energy =
                resolveExternalResourceFaceStore(ExternalResourceChannels.FE, side);
        if (energyTarget != null && energy != null) {
            XianqiaoInterfaceEnergyTransfer.pushAll(energy, energyTarget);
        }
    }

    private void pushItemSlot(int slot, IItemHandler target) {
        ItemStack simulated = inventory.extractItem(slot, Integer.MAX_VALUE, true);
        if (simulated.isEmpty()) return;
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, simulated, true);
        int accepted = simulated.getCount() - remainder.getCount();
        if (accepted <= 0) return;
        ItemStack staged = inventory.extractItem(slot, accepted, false);
        if (staged.isEmpty()) return;
        ItemStack executeRemainder = ItemHandlerHelper.insertItemStacked(target, staged, false);
        if (!executeRemainder.isEmpty()) inventory.restoreExtractedItem(slot, executeRemainder);
    }

    private void pushFluidSlot(int slot, IFluidHandler target) {
        FluidStack simulated = inventory.drainFluidFromSlot(slot, Integer.MAX_VALUE, true);
        if (simulated.isEmpty()) return;
        int accepted = Math.min(simulated.getAmount(),
                target.fill(simulated, IFluidHandler.FluidAction.SIMULATE));
        if (accepted <= 0) return;
        FluidStack staged = inventory.drainFluidFromSlot(slot, accepted, false);
        if (staged.isEmpty()) return;
        int committed = Math.min(staged.getAmount(),
                target.fill(staged, IFluidHandler.FluidAction.EXECUTE));
        if (committed < staged.getAmount()) {
            inventory.restoreExtractedFluid(slot,
                    staged.copyWithAmount(staged.getAmount() - committed));
        }
    }

    private void pullFromSide(ServerLevel serverLevel, Direction side) {
        BlockPos targetPos = worldPosition.relative(side);
        // The same contacted-face capability exposes only slots the neighbor permits as outputs.
        IItemHandler itemSource = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.itemHandler(serverLevel.getCapability(
                Capabilities.Item.BLOCK, targetPos, side.getOpposite()));
        IFluidHandler fluidSource = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.fluidHandler(serverLevel.getCapability(
                Capabilities.Fluid.BLOCK, targetPos, side.getOpposite()));
        IEnergyStorage energySource = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energyHandler(serverLevel.getCapability(
                Capabilities.Energy.BLOCK, targetPos, side.getOpposite()));
        if (itemSource != null) pullItems(itemSource, side);
        if (fluidSource != null) pullFluids(fluidSource, side);
        AtomicEnergyRefill.ResourceStore energy =
                resolveExternalResourceFaceStore(ExternalResourceChannels.FE, side);
        if (energySource != null && energy != null) {
            XianqiaoInterfaceEnergyTransfer.pullAll(energySource, energy);
        }
    }

    private void pullItems(IItemHandler source, Direction side) {
        for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
            com.immortalstorage.immortalstorage.network.storage.ItemHandlerTransferTransaction.moveSlot(
                    source, sourceSlot, Integer.MAX_VALUE,
                    (stack, simulate) -> {
                        long accepted = inventory.insertBulk(stack, stack.getCount(), simulate);
                        int remainder = stack.getCount() - (int) Math.min(stack.getCount(), accepted);
                        return remainder <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remainder);
                    },
                    overflow -> {
                        if (level instanceof ServerLevel serverLevel && !overflow.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(serverLevel,
                                    worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                                    worldPosition.getZ() + 0.5D, overflow);
                        }
                    });
        }
    }

    private void pullFluids(IFluidHandler source, Direction side) {
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack prototype = source.getFluidInTank(tank);
            if (prototype.isEmpty()) continue;
            int maximum = Integer.MAX_VALUE;
            FluidStack simulated = source.drain(prototype.copyWithAmount(maximum),
                    IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) continue;
            long accepted = inventory.insertFluidBulk(simulated, simulated.getAmount(), true);
            if (accepted <= 0L) continue;
            if (accepted < simulated.getAmount()) simulated = simulated.copyWithAmount((int) accepted);
            FluidStack extracted = source.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) continue;
            long committed = inventory.insertFluidBulk(extracted, extracted.getAmount(), false);
            if (committed < extracted.getAmount()) {
                source.fill(extracted.copyWithAmount((int) (extracted.getAmount() - committed)),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /**
     * Returns every real buffer to live owner storage first, then emits only
     * the exact uncommitted remainder. The guard makes all removal paths
     * idempotent, including player break, explosions and staff collection.
     */
    public void releaseBuffersForRemoval(ServerLevel serverLevel) {
        List<ItemStack> remainders = prepareBuffersForRemoval();
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty()) Block.popResource(serverLevel, worldPosition, remainder);
        }
    }

    /**
     * Settles buffers without performing world side effects, so the exact
     * compact carrier state can be tested independently from entity spawning.
     */
    List<ItemStack> prepareBuffersForRemoval() {
        if (releaseState != ReleaseState.OPEN) return List.of();
        releaseState = ReleaseState.RELEASING;
        XianqiaoInterfaceInventory.ItemRemovalSettlement settlement;
        try {
            inventory.returnFluidBuffersAndRetainRemainders();
            inventory.returnExternalBuffersAndRetainRemainders();
            settlement = inventory.settleItemBuffersForRemoval();
        } catch (RuntimeException | Error failure) {
            releaseState = ReleaseState.OPEN;
            setChanged();
            throw failure;
        }
        // Settlement is complete before any world-drop side effect. Fluid and
        // oversized item remainders deliberately stay in compact long-valued
        // NBT on the dropped interface item.
        preserveRetainedItemBuffersInDrop = settlement.retainedAmount() > 0L;
        releaseState = ReleaseState.RELEASED;
        setChanged();
        return settlement.materializedDrops();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.xianqiao_interface");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        if (!canUse(player)) return null;
        return new XianqiaoInterfaceMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries);
        ownerBinding.save(tag, OWNER_TAG);
        inventory.saveState(tag, registries);
        int[] encodedModes = new int[sideModes.length];
        for (Direction side : Direction.values()) {
            encodedModes[side.ordinal()] = sideModes[side.ordinal()].persistedId();
        }
        tag.putIntArray(SIDE_MODES_TAG, encodedModes);
        tag.putLong(CONFIG_REVISION_TAG, configRevision);
        tag.putBoolean(ACTIVE_PULL_TAG, activePullEnabled);
        tag.putBoolean(ACTIVE_PUSH_TAG, activePushEnabled);
    }

    @Override
    protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries);
        ownerBinding.load(tag, OWNER_TAG);
        inventory.loadState(tag, registries);
        Arrays.fill(sideModes, SideMode.DISABLED);
        if (tag.contains(SIDE_MODES_TAG)) {
            int[] encodedModes = tag.getIntArray(SIDE_MODES_TAG).orElseGet(() -> new int[0]);
            for (Direction side : Direction.values()) {
                if (side.ordinal() >= encodedModes.length) continue;
                sideModes[side.ordinal()] = SideMode.byId(encodedModes[side.ordinal()]);
            }
        }
        configRevision = Math.max(0L, tag.getLongOr(CONFIG_REVISION_TAG, 0L));
        activePullEnabled = tag.getBooleanOr(ACTIVE_PULL_TAG, false);
        activePushEnabled = tag.getBooleanOr(ACTIVE_PUSH_TAG, false);
        releaseState = ReleaseState.OPEN;
        preserveRetainedItemBuffersInDrop = false;
        clearEndpointCache();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditionalLegacy(tag, registries);
        return tag;
    }

    /** Real buffers are settled separately and must never be duplicated into the dropped block item. */
    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(BUFFERS_TAG);
        if (!preserveRetainedItemBuffersInDrop) {
            net.minecraft.nbt.CompoundTag tag = com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.rawOutput(output);
            for (net.minecraft.nbt.Tag element : tag.getListOrEmpty(ITEM_SLOTS_TAG)) {
                if (element instanceof net.minecraft.nbt.CompoundTag slot) slot.putLong("Cached", 0L);
            }
        }
        if (releaseState != ReleaseState.RELEASED) {
            net.minecraft.nbt.CompoundTag tag = com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.rawOutput(output);
            for (net.minecraft.nbt.Tag element : tag.getListOrEmpty("ExternalResourceSlots")) {
                if (element instanceof net.minecraft.nbt.CompoundTag slot) slot.putLong("Cached", 0L);
            }
        }
    }

    private boolean hasLiveStorage() {
        PersonalStorageEndpoint endpoint = resolveEndpoint();
        return endpoint != null && endpoint.itemStorage() != null
                && endpoint.itemHandler().getSlots() > 0;
    }

    private @Nullable PersonalStorageEndpoint resolveEndpoint() {
        UUID owner = ownerBinding.owner();
        if (!(level instanceof ServerLevel serverLevel) || owner == null) return null;
        long gameTime = serverLevel.getGameTime();
        if (endpointCacheTick == gameTime && owner.equals(endpointCacheOwner)) return endpointCache;
        endpointCache = PersonalStorageApi.resolveXianqiao(serverLevel.getServer(), owner);
        endpointCacheOwner = owner;
        endpointCacheTick = gameTime;
        return endpointCache;
    }

    private @Nullable TerminalItemStorage resolveItemStorage() {
        PersonalStorageEndpoint endpoint = resolveEndpoint();
        return endpoint == null ? null : endpoint.itemStorage();
    }

    private @Nullable IFluidHandler resolveFluidHandler() {
        PersonalStorageEndpoint endpoint = resolveEndpoint();
        return endpoint == null ? null : endpoint.fluidHandler();
    }

    private @Nullable TerminalFluidStorage resolveFluidStorage() {
        PersonalStorageEndpoint endpoint = resolveEndpoint();
        return endpoint == null ? null : endpoint.fluidStorage();
    }

    private @Nullable ExternalResourceStorage resolveExternalResourceStorage() {
        PersonalStorageEndpoint endpoint = resolveEndpoint();
        return endpoint == null ? null : endpoint.externalResourceStorage();
    }

    /** Total owner-Xianqiao amount for one configured resource identity. */
    protected long configuredOwnerStorageAmount(int slot) {
        if (slot < 0 || slot >= XianqiaoInterfaceInventory.SLOT_COUNT) return 0L;
        ItemStack item = inventory.getTarget(slot);
        if (!item.isEmpty()) {
            TerminalItemStorage storage = resolveItemStorage();
            if (storage == null) return 0L;
            for (StorageItemSummary summary : storage.snapshot()) if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(item, summary.prototype())) return summary.amount();
            return 0L;
        }
        net.neoforged.neoforge.fluids.FluidStack fluid = inventory.getFluidTarget(slot);
        if (!fluid.isEmpty()) {
            TerminalFluidStorage storage = resolveFluidStorage();
            return storage == null ? 0L : storage.snapshot().getOrDefault(com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey.of(fluid), 0L);
        }
        com.immortalstorage.core.resource.ResourceChannelKey external = inventory.getExternalTarget(slot);
        ExternalResourceStorage storage = resolveExternalResourceStorage();
        if (external == null || storage == null) return 0L;
        for (com.immortalstorage.core.resource.ResourceChannelEntry entry : storage.snapshot()) if (entry.key().equals(external)) return entry.amount();
        return 0L;
    }

    private void markInventoryChanged() {
        setChanged();
    }

    private void markConfigurationChanged() {
        configRevision = configRevision == Long.MAX_VALUE ? 0L : configRevision + 1L;
        setChanged();
    }

    private void clearEndpointCache() {
        endpointCache = null;
        endpointCacheOwner = null;
        endpointCacheTick = Long.MIN_VALUE;
    }

    @Override
    public void setRemoved() {
        XianqiaoInterfaceCompatHooks.onRemoved(this);
        clearEndpointCache();
        super.setRemoved();
    }

    /** Resolves every transaction through the public live owner endpoint. */
    private final class OwnerStorageBridge implements TerminalItemStorage {
        @Override
        public long revision() {
            TerminalItemStorage storage = resolveItemStorage();
            return storage == null ? 0L : storage.revision();
        }

        @Override
        public List<StorageItemSummary> snapshot() {
            TerminalItemStorage storage = resolveItemStorage();
            return storage == null ? List.of() : storage.snapshot();
        }

        @Override
        public long insert(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            TerminalItemStorage storage = resolveItemStorage();
            return storage == null ? 0L : storage.insert(key, amount, action);
        }

        @Override
        public long extract(TerminalEntryKey key, long amount, TerminalStorageAction action) {
            TerminalItemStorage storage = resolveItemStorage();
            return storage == null ? 0L : storage.extract(key, amount, action);
        }
    }

    private enum ReleaseState {
        OPEN,
        RELEASING,
        RELEASED
    }

    /** Resolves every fluid transaction through the public long-mB owner endpoint. */
    private final class OwnerFluidStorageBridge implements TerminalFluidStorage {
        @Override
        public long revision() {
            TerminalFluidStorage storage = resolveFluidStorage();
            return storage == null ? 0L : storage.revision();
        }

        @Override
        public Map<TerminalFluidKey, Long> snapshot() {
            TerminalFluidStorage storage = resolveFluidStorage();
            return storage == null ? Map.of() : storage.snapshot();
        }

        @Override
        public long insert(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
            TerminalFluidStorage storage = resolveFluidStorage();
            return storage == null ? 0L : storage.insert(key, amountMb, action);
        }

        @Override
        public long extract(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
            TerminalFluidStorage storage = resolveFluidStorage();
            return storage == null ? 0L : storage.extract(key, amountMb, action);
        }
    }

    /** Resolves every optional-resource transaction through the public owner endpoint. */
    private final class OwnerExternalResourceStorageBridge implements ExternalResourceStorage {
        @Override
        public long revision() {
            ExternalResourceStorage storage = resolveExternalResourceStorage();
            return storage == null ? 0L : storage.revision();
        }

        @Override
        public List<ResourceChannelEntry> snapshot() {
            ExternalResourceStorage storage = resolveExternalResourceStorage();
            return storage == null ? List.of() : storage.snapshot();
        }

        @Override
        public long insert(ResourceChannelKey key, long amount, ResourceTransferAction action) {
            ExternalResourceStorage storage = resolveExternalResourceStorage();
            return storage == null ? 0L : storage.insert(key, amount, action);
        }

        @Override
        public long extract(ResourceChannelKey key, long amount, ResourceTransferAction action) {
            ExternalResourceStorage storage = resolveExternalResourceStorage();
            return storage == null ? 0L : storage.extract(key, amount, action);
        }
    }
}
