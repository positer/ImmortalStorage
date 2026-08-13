package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.menu.custom.WorldShardMinerMenu;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerModes;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMiningMath;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerStatus;
import com.immortalstorage.immortalstorage.worldshard.WorldShardOutputRouter;
import com.immortalstorage.immortalstorage.worldshard.WorldShardPyramid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

public final class WorldShardMinerBlockEntity extends BlockEntity implements Container, MenuProvider, ReinforcementPluginHost {
    private static final String CACHE_TAG = "Cache";
    private static final String OWNER_TAG = "Owner";
    private static final String LEVEL_TAG = "ActiveLevel";
    private static final String MODE_TAG = "ActiveMode";
    private static final String COLOR_TAG = "BeamColor";
    private static final String CACHE_FULL_TAG = "CacheFull";
    private static final String STORAGE_UNAVAILABLE_TAG = "StorageUnavailable";
    private static final String PENDING_OUTPUT_TAG = "PendingOutput";

    private final OwnerBinding ownerBinding = new OwnerBinding();
    private final WorldShardMinerCache cache = new WorldShardMinerCache(this::onCacheContentsChanged);
    private int activeLevel;
    private @Nullable ResourceLocation activeMode;
    private int beamColor = 0xFFFFFFFF;
    private boolean cacheFull;
    private boolean storageUnavailable;
    private ItemStack plugin = ItemStack.EMPTY;
    private boolean xianqiaoOutput = true;
    private boolean automaticOutput = true;
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private final List<ItemStack> pendingOutput = new ArrayList<>();

    public WorldShardMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORLD_SHARD_MINER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  WorldShardMinerBlockEntity miner) {
        miner.pushCacheToFaces(level);
        miner.flushCacheToXianqiao(level);
        if (!miner.settlePendingOutput(level)) return;
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
        generated = ReinforcementPluginHost.multiplyOutputs(
                generated, miner.reinforcementMultiplier());
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
        return Component.translatable("block.immortalstorage.world_shard_miner");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WorldShardMinerMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return WorldShardMinerCache.SLOT_COUNT + 1;
    }

    @Override
    public boolean isEmpty() {
        if (!plugin.isEmpty() || !pendingOutput.isEmpty()) return false;
        for (int slot = 0; slot < WorldShardMinerCache.SLOT_COUNT; slot++) {
            if (!cache.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == WorldShardMinerCache.SLOT_COUNT ? plugin : cache.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == WorldShardMinerCache.SLOT_COUNT) {
            ItemStack removed = plugin.split(amount);
            if (!removed.isEmpty()) setChangedAndSync();
            return removed;
        }
        return cache.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == WorldShardMinerCache.SLOT_COUNT) {
            ItemStack removed = plugin;
            plugin = ItemStack.EMPTY;
            return removed;
        }
        ItemStack removed = cache.removeStackNoUpdate(slot);
        if (!removed.isEmpty()) onCacheContentsChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == WorldShardMinerCache.SLOT_COUNT) {
            plugin = ReinforcementPluginHost.isPlugin(stack)
                    ? stack.copyWithCount(1) : ItemStack.EMPTY;
            setChangedAndSync();
            return;
        }
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
        for (int slot = 0; slot < WorldShardMinerCache.SLOT_COUNT; slot++) {
            cache.setStackInSlot(slot, ItemStack.EMPTY);
        }
        plugin = ItemStack.EMPTY;
        pendingOutput.clear();
        cacheFull = false;
        storageUnavailable = false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == WorldShardMinerCache.SLOT_COUNT && ReinforcementPluginHost.isPlugin(stack);
    }

    @Override public ItemStack reinforcementPlugin() { return plugin; }
    @Override public void setReinforcementPlugin(ItemStack stack) {
        plugin = ReinforcementPluginHost.isPlugin(stack)
                ? stack.copyWithCount(1) : ItemStack.EMPTY;
        setChangedAndSync();
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
        if (!pendingOutput.isEmpty()) return false;
        boolean ownerRealm = xianqiaoOutput && isExactOwnerRealm(level);
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
        if (!pendingOutput.isEmpty()) return WorldShardOutputRouter.reject(generated);
        if (xianqiaoOutput && isExactOwnerRealm(level)) {
            PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level);
            WorldShardOutputRouter.RouteResult result = endpoint == null
                    ? WorldShardOutputRouter.reject(generated)
                    : WorldShardOutputRouter.routeDirect(generated, endpoint);
            if (result.unaccepted() > 0L) {
                replacePendingOutput(generated);
                setOutputBlock(false, true);
                return completedRoute(result);
            }
            setOutputBlock(false, false);
            return result;
        }
        WorldShardOutputRouter.CacheRouteResult cacheRoute =
                WorldShardOutputRouter.routeCacheWithOverflow(generated, cache);
        List<ItemStack> temporary = new ArrayList<>();
        for (ItemStack overflow : cacheRoute.overflow()) {
            ItemStack remainder = MachineOutputScheduler.pushItemToFaces(
                    level, worldPosition, automaticOutput, outputFaces, overflow.copy());
            if (!remainder.isEmpty()) temporary.add(remainder.copy());
        }
        replacePendingOutput(temporary);
        setOutputBlock(!pendingOutput.isEmpty(), false);
        WorldShardOutputRouter.RouteResult result = cacheRoute.route();
        return completedRoute(result);
    }

    /**
     * Publishes a completed batch before a later mining cycle is allowed.
     * Newly freed local slots are filled before enabled accepting faces; any
     * remaining stacks stay persisted and keep the miner paused.
     */
    private boolean settlePendingOutput(ServerLevel level) {
        if (pendingOutput.isEmpty()) return true;
        if (xianqiaoOutput && isExactOwnerRealm(level)) {
            WorldShardOutputRouter.RouteResult result =
                    WorldShardOutputRouter.routeDirect(pendingOutput, ownerEndpoint(level));
            if (result.unaccepted() > 0L) {
                setOutputBlock(false, true);
                return false;
            }
            pendingOutput.clear();
            setOutputBlock(false, false);
            setChanged();
            return true;
        }

        WorldShardOutputRouter.CacheRouteResult cacheRoute =
                WorldShardOutputRouter.routeCacheWithOverflow(List.copyOf(pendingOutput), cache);
        List<ItemStack> temporary = new ArrayList<>();
        for (ItemStack overflow : cacheRoute.overflow()) {
            ItemStack remainder = MachineOutputScheduler.pushItemToFaces(
                    level, worldPosition, automaticOutput, outputFaces, overflow.copy());
            if (!remainder.isEmpty()) temporary.add(remainder.copy());
        }
        replacePendingOutput(temporary);
        setOutputBlock(!pendingOutput.isEmpty(), false);
        return pendingOutput.isEmpty();
    }

    private static WorldShardOutputRouter.RouteResult completedRoute(
            WorldShardOutputRouter.RouteResult route) {
        return new WorldShardOutputRouter.RouteResult(route.offered(), route.offered(), 0L);
    }

    private void replacePendingOutput(List<ItemStack> stacks) {
        pendingOutput.clear();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) pendingOutput.add(stack.copy());
            }
        }
        setChanged();
    }

    /** Returns and clears completed production when the miner is removed. */
    public List<ItemStack> drainPendingOutputForRemoval() {
        List<ItemStack> drained = pendingOutput.stream().map(ItemStack::copy).toList();
        pendingOutput.clear();
        setOutputBlock(false, false);
        setChanged();
        return drained;
    }

    private boolean isExactOwnerRealm(ServerLevel level) {
        UUID owner = ownerBinding.owner();
        return owner != null && ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), owner);
    }

    private @Nullable PersonalStorageNetwork.Endpoint ownerEndpoint(ServerLevel level) {
        UUID owner = ownerBinding.owner();
        if (owner == null || !ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), owner)) return null;
        return PersonalStorageNetwork.resolveInOwnerRealm(level, owner, this::setChanged);
    }

    private boolean pushCacheToFaces(ServerLevel level) {
        return MachineOutputScheduler.pushItemsToFaces(level, worldPosition, automaticOutput,
                outputFaces, cache, 0, WorldShardMinerCache.SLOT_COUNT);
    }

    private boolean flushCacheToXianqiao(ServerLevel level) {
        if (!xianqiaoOutput) return false;
        return MachineOutputScheduler.flushItemsToXianqiao(cache, 0,
                WorldShardMinerCache.SLOT_COUNT, ownerEndpoint(level));
    }

    public boolean xianqiaoOutput() { return xianqiaoOutput; }
    public boolean automaticOutput() { return automaticOutput; }
    public boolean outputFace(Direction side) {
        return side != null && outputFaces[side.ordinal()];
    }
    public void toggleXianqiaoOutput() {
        xianqiaoOutput = !xianqiaoOutput;
        if (xianqiaoOutput && level instanceof ServerLevel serverLevel) {
            flushCacheToXianqiao(serverLevel);
        }
        setChangedAndSync();
    }
    public void toggleAutomaticOutput() {
        automaticOutput = !automaticOutput;
        setChangedAndSync();
    }
    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        setChangedAndSync();
    }

    private void onCacheContentsChanged() {
        setChanged();
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
        if (!plugin.isEmpty()) tag.put("ReinforcementPlugin", plugin.save(registries));
        tag.putBoolean("XianqiaoOutput", xianqiaoOutput);
        tag.putBoolean("AutomaticFaceOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) {
            faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        }
        tag.putIntArray("OutputFaces", faces);
        if (!pendingOutput.isEmpty()) {
            ListTag pending = new ListTag();
            for (ItemStack stack : pendingOutput) pending.add(stack.save(registries));
            tag.put(PENDING_OUTPUT_TAG, pending);
        }
        writeClientState(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(CACHE_TAG, CompoundTag.TAG_COMPOUND)) {
            cache.deserializeNBT(registries, tag.getCompound(CACHE_TAG));
        }
        ownerBinding.load(tag, OWNER_TAG);
        plugin = tag.contains("ReinforcementPlugin")
                ? ItemStack.parseOptional(registries, tag.getCompound("ReinforcementPlugin"))
                : ItemStack.EMPTY;
        xianqiaoOutput = !tag.contains("XianqiaoOutput") || tag.getBoolean("XianqiaoOutput");
        automaticOutput = !tag.contains("AutomaticFaceOutput") || tag.getBoolean("AutomaticFaceOutput");
        java.util.Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces");
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) {
            outputFaces[i] = faces[i] != 0;
        }
        pendingOutput.clear();
        ListTag pending = tag.getList(PENDING_OUTPUT_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, pending.getCompound(index));
            if (!stack.isEmpty()) pendingOutput.add(stack);
        }
        activeLevel = Math.max(0, Math.min(WorldShardMiningMath.MAX_LEVEL, tag.getInt(LEVEL_TAG)));
        activeMode = tag.contains(MODE_TAG, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(MODE_TAG)) : null;
        beamColor = tag.contains(COLOR_TAG, CompoundTag.TAG_INT) ? tag.getInt(COLOR_TAG) : 0xFFFFFFFF;
        cacheFull = !pendingOutput.isEmpty();
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

    private void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
