package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinActivation;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinSchedule;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinSeed;
import com.immortalstorage.immortalstorage.worldshard.WorldShardLootCatalog;
import com.immortalstorage.immortalstorage.worldshard.WorldShardLootDefinition;
import com.immortalstorage.immortalstorage.worldshard.WorldShardLootWeightProvider;
import com.immortalstorage.immortalstorage.worldshard.WorldShardMinerCache;
import com.immortalstorage.immortalstorage.worldshard.WorldShardOutputRouter;
import com.immortalstorage.immortalstorage.worldshard.TreasureBasinStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Loot-only machine activated by one already-active World Shard Miner directly
 * below it. The basin owns its cache, generation cycle and output-block state;
 * it only inherits an immutable mode/owner snapshot from the miner.
 */
public final class TreasureBasinBlockEntity extends BlockEntity implements Container, MenuProvider, ReinforcementPluginHost {
    private static final String CACHE_TAG = "Cache";
    private static final String BASIN_ID_TAG = "BasinId";
    private static final String CYCLE_TAG = "GenerationCycle";
    private static final String MODE_TAG = "ActiveMode";
    private static final String CACHE_FULL_TAG = "CacheFull";
    private static final String STORAGE_UNAVAILABLE_TAG = "StorageUnavailable";
    private static final String PENDING_OUTPUT_TAG = "PendingOutput";
    private static final String SCHEDULE_PROGRESS_TAG = "ScheduleProgress";

    private final WorldShardMinerCache cache = new WorldShardMinerCache(this::onCacheContentsChanged);
    private UUID basinId = UUID.randomUUID();
    private long generationCycle;
    private long scheduleProgress;
    private TreasureBasinActivation activation = TreasureBasinActivation.inactive();
    private boolean cacheFull;
    private boolean storageUnavailable;
    private ItemStack plugin = ItemStack.EMPTY;
    private boolean xianqiaoOutput = true;
    private boolean automaticOutput = true;
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private final List<ItemStack> pendingOutput = new ArrayList<>();

    public TreasureBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREASURE_BASIN.get(), pos, state);
    }

    /** Direct adjacency lookup only; other nearby miners never activate this basin. */
    private @Nullable WorldShardMinerBlockEntity attachedMinerBelow() {
        if (level == null) return null;
        return level.getBlockEntity(worldPosition.below()) instanceof WorldShardMinerBlockEntity miner
                ? miner : null;
    }

    public IItemHandler getCacheHandler() {
        return cache;
    }

    public WorldShardMinerCache getBasinCache() {
        return cache;
    }

    public @Nullable ResourceLocation getActiveMode() {
        return activation.mode();
    }

    public boolean isActive() {
        return activation.active();
    }

    public TreasureBasinStatus getOperatingStatus() {
        return TreasureBasinStatus.resolve(
                isActive(), hasSelectableLoot(), cacheFull, storageUnavailable);
    }

    public boolean isCacheFull() {
        return cacheFull;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.immortalstorage.treasure_basin");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TreasureBasinMenu(id, inventory, this);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, TreasureBasinBlockEntity basin) {
        basin.refreshActivation(level);
        basin.pushCacheToFaces(level);
        basin.flushCacheToXianqiao(level);
        if (!basin.settlePendingOutput(level)) return;
        if (!basin.isActive()) return;
        if (!basin.hasSelectableLoot()) return;

        // The reinforcement plugin accelerates the basin: instead of
        // multiplying one roll's drops, it advances the schedule progress by
        // the plugin multiplier each tick, so a stronger plugin completes
        // rolls faster while each roll keeps its vanilla drop size.
        int speed = Math.max(1, basin.reinforcementMultiplier());
        if (!basin.canGenerateOutputs(level)) return;
        TreasureBasinSchedule.Advance advance = TreasureBasinSchedule.advance(
                basin.scheduleProgress, speed, TreasureBasinSchedule.INTERVAL_TICKS);
        basin.scheduleProgress = advance.remainder();
        boolean changed = false;
        for (long roll = 0L; roll < advance.rolls(); roll++) {
            if (basin.rollOnce(level, pos)) changed = true;
        }
        if (changed) basin.setChanged();
    }

    /** Rolls exactly one structure-chest loot table and routes its complete result. */
    private boolean rollOnce(ServerLevel level, BlockPos pos) {
        ResourceLocation activeMode = getActiveMode();
        if (activeMode == null) return false;
        long selectionTicket = TreasureBasinSeed.selectionTicket(
                basinId, level.dimension().location(), pos, generationCycle);
        WorldShardLootDefinition selected = WorldShardLootCatalog.active()
                .select(activeMode, selectionTicket, WorldShardLootWeightProvider.configured())
                .orElse(null);
        if (selected == null) {
            ImmortalStorageMod.LOG.warn("[Basin] no selectable loot for mode={}", activeMode);
            return false;
        }

        LootTable table = WorldShardLootCatalog.active().resolveLootTable(selected.lootTable());
        if (table == LootTable.EMPTY) {
            ImmortalStorageMod.LOG.warn("[Basin] resolved EMPTY table for {} (mode={})", selected.lootTable(), activeMode);
        }
        LootParams.Builder paramsBuilder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));
        UUID ownerId = activation.owner();
        if (ownerId != null) {
            ServerPlayer owner = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(level.getServer(), ownerId);
            if (owner != null) {
                paramsBuilder.withOptionalParameter(LootContextParams.THIS_ENTITY, owner)
                        .withLuck(owner.getLuck());
            }
        }
        LootParams params = paramsBuilder.create(LootContextParamSets.CHEST);
        long lootSeed = TreasureBasinSeed.derive(
                basinId, level.dimension().location(), pos, generationCycle,
                selected.sourceSeed(), selected.lootTable());

        // Roll the vanilla table, then run the NeoForge global-loot-modifier
        // chain (where the 0.1.0 25% Ascension Dan, pill, and other injection
        // modifiers live) so structure chests honor the same loot rules as a
        // player opening the real chest.
        // Set the queried loot-table id so NeoForge's loot_table_id GLM
        // condition matches (the 25% Ascension Dan and pill injections gate on
        // it); without this the modifier chain silently no-ops.
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(lootSeed)
                .withQueriedLootTableId(selected.lootTable())
                .create(Optional.empty());
        ObjectArrayList<ItemStack> rolled = new ObjectArrayList<>();
        table.getRandomItemsRaw(context, rolled::add);
        int rolledBeforeGlm = rolled.size();
        rolled = CommonHooks.modifyLoot(selected.lootTable(), rolled, context);
        if (rolled.size() != rolledBeforeGlm) {
            ImmortalStorageMod.LOG.info("[Basin] mode={} table={} glm {}->{} items",
                    activeMode, selected.lootTable(), rolledBeforeGlm, rolled.size());
        }
        List<ItemStack> generated = new ArrayList<>(rolled);
        WorldShardOutputRouter.RouteResult routed = routeGenerated(level, generated);
        if (routed.unaccepted() > 0L || routed.accepted() != routed.offered()) return false;

        generationCycle = generationCycle == Long.MAX_VALUE ? 0L : generationCycle + 1L;
        return true;
    }

    /** Routes loot through this basin's own state, never through the miner. */
    public WorldShardOutputRouter.RouteResult routeGenerated(
            ServerLevel level, List<ItemStack> generated) {
        if (!pendingOutput.isEmpty()) return WorldShardOutputRouter.reject(generated);
        UUID owner = activation.owner();
        if (xianqiaoOutput && isExactOwnerRealm(level, owner)) {
            PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level, owner);
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
     * Drains one previously completed batch before another roll may start.
     * Local mode always refills the visible cache first; direct owner-realm
     * mode remains an all-or-nothing Xianqiao transaction.
     */
    private boolean settlePendingOutput(ServerLevel level) {
        if (pendingOutput.isEmpty()) return true;
        UUID owner = activation.owner();
        if (xianqiaoOutput && isExactOwnerRealm(level, owner)) {
            WorldShardOutputRouter.RouteResult result = WorldShardOutputRouter.routeDirect(
                    pendingOutput, ownerEndpoint(level, owner));
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

    /** Returns and clears completed production when the basin is removed. */
    public List<ItemStack> drainPendingOutputForRemoval() {
        List<ItemStack> drained = pendingOutput.stream().map(ItemStack::copy).toList();
        pendingOutput.clear();
        setOutputBlock(false, false);
        setChanged();
        return drained;
    }

    private boolean pushCacheToFaces(ServerLevel level) {
        return MachineOutputScheduler.pushItemsToFaces(level, worldPosition, automaticOutput,
                outputFaces, cache, 0, WorldShardMinerCache.SLOT_COUNT);
    }

    private boolean flushCacheToXianqiao(ServerLevel level) {
        if (!xianqiaoOutput) return false;
        UUID owner = activation.owner();
        return MachineOutputScheduler.flushItemsToXianqiao(cache, 0,
                WorldShardMinerCache.SLOT_COUNT, ownerEndpoint(level, owner));
    }

    public boolean xianqiaoOutput() { return xianqiaoOutput; }
    public boolean automaticOutput() { return automaticOutput; }
    public boolean outputFace(Direction side) { return side != null && outputFaces[side.ordinal()]; }
    public void toggleXianqiaoOutput() { xianqiaoOutput = !xianqiaoOutput; setChangedAndSync(); }
    public void toggleAutomaticOutput() { automaticOutput = !automaticOutput; setChangedAndSync(); }
    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        setChangedAndSync();
    }

    private void refreshActivation(ServerLevel serverLevel) {
        WorldShardMinerBlockEntity miner = attachedMinerBelow();
        TreasureBasinActivation next = TreasureBasinActivation.resolve(
                miner != null,
                miner != null && miner.hasActiveBeam(),
                miner == null ? null : miner.getActiveMode(),
                miner == null ? null : miner.getOwner());
        if (next.equals(activation)) return;
        activation = next;
        setChanged();
        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private boolean canGenerateOutputs(ServerLevel level) {
        if (!isActive()) return false;
        if (!pendingOutput.isEmpty()) return false;
        UUID owner = activation.owner();
        if (!xianqiaoOutput || !isExactOwnerRealm(level, owner)) {
            if (storageUnavailable) setOutputBlock(cacheFull, false);
            return !cacheFull;
        }
        PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level, owner);
        if (endpoint == null) {
            setOutputBlock(false, true);
            return false;
        }
        if (cacheFull || storageUnavailable) setOutputBlock(false, false);
        return true;
    }

    private boolean hasSelectableLoot() {
        ResourceLocation mode = activation.mode();
        return mode != null && WorldShardLootCatalog.active()
                .hasSelectable(mode, WorldShardLootWeightProvider.configured());
    }

    private static boolean isExactOwnerRealm(ServerLevel level, @Nullable UUID owner) {
        return owner != null && ImmortalStorageDimensions.isPersonalRealmFor(level.dimension(), owner);
    }

    private @Nullable PersonalStorageNetwork.Endpoint ownerEndpoint(
            ServerLevel level, @Nullable UUID owner) {
        if (!isExactOwnerRealm(level, owner)) return null;
        return PersonalStorageNetwork.resolveInOwnerRealm(level, owner, this::setChanged);
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
            ItemStack removed = plugin.split(amount); setChanged(); return removed;
        }
        return cache.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == WorldShardMinerCache.SLOT_COUNT) {
            ItemStack removed = plugin; plugin = ItemStack.EMPTY; return removed;
        }
        ItemStack removed = cache.removeStackNoUpdate(slot);
        if (!removed.isEmpty()) onCacheContentsChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == WorldShardMinerCache.SLOT_COUNT) {
            plugin = ReinforcementPluginHost.isPlugin(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
            setChanged(); return;
        }
        ItemStack stored = stack.copy();
        stored.limitSize(getMaxStackSize(stored));
        cache.setStackInSlot(slot, stored);
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

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == WorldShardMinerCache.SLOT_COUNT && ReinforcementPluginHost.isPlugin(stack);
    }
    @Override public ItemStack reinforcementPlugin() { return plugin; }
    @Override public void setReinforcementPlugin(ItemStack stack) {
        plugin = stack.copyWithCount(1); setChangedAndSync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(CACHE_TAG, cache.serializeNBT(registries));
        tag.putUUID(BASIN_ID_TAG, basinId);
        tag.putLong(CYCLE_TAG, generationCycle);
        tag.putLong(SCHEDULE_PROGRESS_TAG, scheduleProgress);
        if (!plugin.isEmpty()) tag.put("ReinforcementPlugin", plugin.save(registries));
        tag.putBoolean("XianqiaoOutput", xianqiaoOutput);
        tag.putBoolean("AutomaticFaceOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
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
        if (tag.hasUUID(BASIN_ID_TAG)) basinId = tag.getUUID(BASIN_ID_TAG);
        generationCycle = Math.max(0L, tag.getLong(CYCLE_TAG));
        scheduleProgress = Math.max(0L, tag.getLong(SCHEDULE_PROGRESS_TAG));
        plugin = tag.contains("ReinforcementPlugin")
                ? ItemStack.parseOptional(registries, tag.getCompound("ReinforcementPlugin")) : ItemStack.EMPTY;
        xianqiaoOutput = !tag.contains("XianqiaoOutput") || tag.getBoolean("XianqiaoOutput");
        automaticOutput = !tag.contains("AutomaticFaceOutput") || tag.getBoolean("AutomaticFaceOutput");
        java.util.Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces");
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
        pendingOutput.clear();
        ListTag pending = tag.getList(PENDING_OUTPUT_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, pending.getCompound(index));
            if (!stack.isEmpty()) pendingOutput.add(stack);
        }
        ResourceLocation mode = tag.contains(MODE_TAG, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(MODE_TAG)) : null;
        activation = TreasureBasinActivation.resolve(
                mode != null, mode != null, mode, null);
        cacheFull = !pendingOutput.isEmpty();
        storageUnavailable = tag.getBoolean(STORAGE_UNAVAILABLE_TAG);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this,
                (blockEntity, registryAccess) -> ((TreasureBasinBlockEntity) blockEntity).clientStateTag());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return clientStateTag();
    }

    private CompoundTag clientStateTag() {
        return writeClientState(new CompoundTag());
    }

    private CompoundTag writeClientState(CompoundTag tag) {
        if (activation.mode() != null) tag.putString(MODE_TAG, activation.mode().toString());
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
