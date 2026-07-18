package com.cultivation.cultivation.block.entity;

import com.cultivation.cultivation.dimension.CultivationDimensions;
import com.cultivation.cultivation.network.storage.PersonalStorageNetwork;
import com.cultivation.cultivation.worldshard.WorldShardMinerCache;
import com.cultivation.cultivation.worldshard.WorldShardMinerModes;
import com.cultivation.cultivation.worldshard.WorldShardMiningMath;
import com.cultivation.cultivation.worldshard.WorldShardMinerStatus;
import com.cultivation.cultivation.worldshard.WorldShardOutputRouter;
import com.cultivation.cultivation.worldshard.WorldShardPyramid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;
import java.util.UUID;

public final class WorldShardMinerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String CACHE_TAG = "Cache";
    private static final String OWNER_TAG = "Owner";
    private static final String LEVEL_TAG = "ActiveLevel";
    private static final String MODE_TAG = "ActiveMode";
    private static final String COLOR_TAG = "BeamColor";
    private static final String CACHE_FULL_TAG = "CacheFull";
    private static final String STORAGE_UNAVAILABLE_TAG = "StorageUnavailable";

    private final OwnerBinding ownerBinding = new OwnerBinding();
    private final WorldShardMinerCache cache = new WorldShardMinerCache(this::onCacheContentsChanged);
    private int activeLevel;
    private @Nullable ResourceLocation activeMode;
    private int beamColor = 0xFFFFFFFF;
    private boolean cacheFull;
    private boolean storageUnavailable;

    public WorldShardMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORLD_SHARD_MINER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  WorldShardMinerBlockEntity miner) {
        // Activation is dimension-agnostic. The dimension only selects the
        // output destination later (exact owner realm or local 27-slot cache).
        WorldShardPyramid.Result pyramid = WorldShardPyramid.scan(pos, level::getBlockState,
                WorldShardMinerModes.definitions().values());
        miner.updateActivation(level, state, pyramid);
        if (pyramid.level() == 0 || !WorldShardMiningMath.shouldRun(level.getGameTime())
                || !miner.canGenerateOutputs(level)) return;

        WorldShardMinerModes.ResolvedMode resolved = WorldShardMinerModes
                .resolved(pyramid.mode().orElseThrow().id()).orElse(null);
        if (resolved == null || resolved.orePool().isEmpty()) return;
        int samples = WorldShardMiningMath.samplesPerCycle(
                pyramid.level(), resolved.mode().samplingMultiplier());
        Map<Item, Integer> outputs = resolved.orePool().sampleBatch(level.getRandom(), samples);
        if (outputs.isEmpty()) return;
        List<ItemStack> generated = outputs.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
                .toList();
        miner.routeGenerated(level, generated);
    }

    public boolean tryClaimOwner(@Nullable UUID owner) {
        UUID before = ownerBinding.owner();
        boolean accepted = ownerBinding.claim(owner);
        if (accepted && before == null) setChanged();
        return accepted;
    }

    public @Nullable UUID getOwner() {
        return ownerBinding.owner();
    }

    public IItemHandler getCacheHandler() {
        return cache;
    }

    public WorldShardMinerCache getMinerCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cultivation.world_shard_miner");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return createCacheMenu(id, inventory, this);
    }

    static ChestMenu createCacheMenu(int id, Inventory inventory, Container container) {
        return ChestMenu.threeRows(id, inventory, container);
    }

    @Override
    public int getContainerSize() {
        return WorldShardMinerCache.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!cache.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return cache.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return cache.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = cache.removeStackNoUpdate(slot);
        if (!removed.isEmpty()) onCacheContentsChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack stored = stack.copy();
        stored.limitSize(getMaxStackSize(stored));
        cache.setStackInSlot(slot, stored);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            cache.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public int getActiveLevel() {
        return activeLevel;
    }

    public @Nullable ResourceLocation getActiveMode() {
        return activeMode;
    }

    public int getBeamColor() {
        return beamColor;
    }

    public boolean hasActiveBeam() {
        return activeLevel > 0 && activeMode != null;
    }

    public boolean isCacheFull() {
        return cacheFull;
    }

    public boolean isOutputBlocked() {
        return getOperatingStatus().blocksGeneration();
    }

    public WorldShardMinerStatus getOperatingStatus() {
        return WorldShardMinerStatus.resolve(hasActiveBeam(), cacheFull, storageUnavailable);
    }

    /**
     * Cheap preflight shared by ore and treasure generation. A full external
     * cache remains paused until its contents change; an owner-realm endpoint
     * is retried without sampling so login/stage recovery resumes safely.
     */
    public boolean canGenerateOutputs(ServerLevel level) {
        if (!hasActiveBeam()) return false;
        boolean ownerRealm = isExactOwnerRealm(level);
        if (!ownerRealm) {
            if (storageUnavailable) setOutputBlock(cacheFull, false);
            return !cacheFull;
        }
        PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level);
        if (endpoint == null) {
            setOutputBlock(false, true);
            return false;
        }
        if (cacheFull || storageUnavailable) setOutputBlock(false, false);
        return true;
    }

    /**
     * Routes one generated batch without erasing Data Components. Inside the
     * exact owner's Xianqiao realm it is a strict direct-storage transaction;
     * every other dimension uses only the miner's 27-slot cache.
     */
    public WorldShardOutputRouter.RouteResult routeGenerated(ServerLevel level, List<ItemStack> generated) {
        if (isExactOwnerRealm(level)) {
            PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level);
            WorldShardOutputRouter.RouteResult result = endpoint == null
                    ? WorldShardOutputRouter.reject(generated)
                    : WorldShardOutputRouter.routeDirect(generated, endpoint);
            setOutputBlock(false, result.unaccepted() > 0L);
            return result;
        }
        if (cacheFull) return WorldShardOutputRouter.reject(generated);
        WorldShardOutputRouter.RouteResult result = WorldShardOutputRouter.routeCache(generated, cache);
        setOutputBlock(result.unaccepted() > 0L, false);
        return result;
    }

    private boolean isExactOwnerRealm(ServerLevel level) {
        UUID owner = ownerBinding.owner();
        return owner != null && CultivationDimensions.isPersonalRealmFor(level.dimension(), owner);
    }

    private @Nullable PersonalStorageNetwork.Endpoint ownerEndpoint(ServerLevel level) {
        UUID owner = ownerBinding.owner();
        if (owner == null || !CultivationDimensions.isPersonalRealmFor(level.dimension(), owner)) return null;
        return PersonalStorageNetwork.resolveInOwnerRealm(level, owner, this::setChanged);
    }

    private void onCacheContentsChanged() {
        setChanged();
        if (cacheFull) setOutputBlock(false, storageUnavailable);
    }

    private void setOutputBlock(boolean nextCacheFull, boolean nextStorageUnavailable) {
        if (cacheFull == nextCacheFull && storageUnavailable == nextStorageUnavailable) return;
        cacheFull = nextCacheFull;
        storageUnavailable = nextStorageUnavailable;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void updateActivation(ServerLevel level, BlockState state, WorldShardPyramid.Result result) {
        int nextLevel = result.level();
        ResourceLocation nextMode = result.mode().map(mode -> mode.id()).orElse(null);
        int nextColor = result.mode().map(mode -> mode.beamColor()).orElse(0xFFFFFFFF);
        if (nextLevel == activeLevel && java.util.Objects.equals(nextMode, activeMode)
                && nextColor == beamColor) {
            return;
        }
        activeLevel = nextLevel;
        activeMode = nextMode;
        beamColor = nextColor;
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(CACHE_TAG, cache.serializeNBT(registries));
        ownerBinding.save(tag, OWNER_TAG);
        writeClientState(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(CACHE_TAG, CompoundTag.TAG_COMPOUND)) {
            cache.deserializeNBT(registries, tag.getCompound(CACHE_TAG));
        }
        ownerBinding.load(tag, OWNER_TAG);
        activeLevel = Math.max(0, Math.min(WorldShardMiningMath.MAX_LEVEL, tag.getInt(LEVEL_TAG)));
        activeMode = tag.contains(MODE_TAG, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(MODE_TAG)) : null;
        beamColor = tag.contains(COLOR_TAG, CompoundTag.TAG_INT) ? tag.getInt(COLOR_TAG) : 0xFFFFFFFF;
        cacheFull = tag.getBoolean(CACHE_FULL_TAG);
        storageUnavailable = tag.getBoolean(STORAGE_UNAVAILABLE_TAG);
        if (activeLevel == 0 || activeMode == null) {
            activeLevel = 0;
            activeMode = null;
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this,
                (blockEntity, registryAccess) -> ((WorldShardMinerBlockEntity) blockEntity).clientStateTag());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return clientStateTag();
    }

    private CompoundTag clientStateTag() {
        return writeClientState(new CompoundTag());
    }

    private CompoundTag writeClientState(CompoundTag tag) {
        tag.putInt(LEVEL_TAG, activeLevel);
        if (activeMode != null) tag.putString(MODE_TAG, activeMode.toString());
        tag.putInt(COLOR_TAG, beamColor);
        tag.putBoolean(CACHE_FULL_TAG, cacheFull);
        tag.putBoolean(STORAGE_UNAVAILABLE_TAG, storageUnavailable);
        return tag;
    }
}
