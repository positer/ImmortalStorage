package com.immortalstorage.immortalstorage.block.entity;

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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Loot-only machine activated by one already-active World Shard Miner directly
 * below it. The basin owns its cache, generation cycle and output-block state;
 * it only inherits an immutable mode/owner snapshot from the miner.
 */
public final class TreasureBasinBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String CACHE_TAG = "Cache";
    private static final String BASIN_ID_TAG = "BasinId";
    private static final String CYCLE_TAG = "GenerationCycle";
    private static final String MODE_TAG = "ActiveMode";
    private static final String CACHE_FULL_TAG = "CacheFull";
    private static final String STORAGE_UNAVAILABLE_TAG = "StorageUnavailable";

    private final WorldShardMinerCache cache = new WorldShardMinerCache(this::onCacheContentsChanged);
    private UUID basinId = UUID.randomUUID();
    private long generationCycle;
    private TreasureBasinActivation activation = TreasureBasinActivation.inactive();
    private boolean cacheFull;
    private boolean storageUnavailable;

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
        if (!basin.isActive() || !TreasureBasinSchedule.shouldRun(level.getGameTime())) return;
        if (!basin.hasSelectableLoot()) return;
        if (!basin.canGenerateOutputs(level)) return;

        ResourceLocation activeMode = basin.getActiveMode();
        if (activeMode == null) return;
        long selectionTicket = TreasureBasinSeed.selectionTicket(
                basin.basinId, level.dimension().location(), pos, basin.generationCycle);
        WorldShardLootDefinition selected = WorldShardLootCatalog.active()
                .select(activeMode, selectionTicket, WorldShardLootWeightProvider.configured())
                .orElse(null);
        if (selected == null) return;

        LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, selected.lootTable()));
        LootParams.Builder paramsBuilder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));
        UUID ownerId = basin.activation.owner();
        if (ownerId != null) {
            ServerPlayer owner = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(level.getServer(), ownerId);
            if (owner != null) {
                paramsBuilder.withOptionalParameter(LootContextParams.THIS_ENTITY, owner)
                        .withLuck(owner.getLuck());
            }
        }
        LootParams params = paramsBuilder.create(LootContextParamSets.CHEST);
        long lootSeed = TreasureBasinSeed.derive(
                basin.basinId, level.dimension().location(), pos, basin.generationCycle,
                selected.sourceSeed(), selected.lootTable());

        List<ItemStack> generated = table.getRandomItems(params, lootSeed);
        WorldShardOutputRouter.RouteResult routed = basin.routeGenerated(level, generated);
        if (routed.unaccepted() > 0L || routed.accepted() != routed.offered()) return;

        basin.generationCycle = basin.generationCycle == Long.MAX_VALUE
                ? 0L : basin.generationCycle + 1L;
        basin.setChanged();
    }

    /** Routes loot through this basin's own state, never through the miner. */
    public WorldShardOutputRouter.RouteResult routeGenerated(
            ServerLevel level, List<ItemStack> generated) {
        UUID owner = activation.owner();
        if (isExactOwnerRealm(level, owner)) {
            PersonalStorageNetwork.Endpoint endpoint = ownerEndpoint(level, owner);
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
        UUID owner = activation.owner();
        if (!isExactOwnerRealm(level, owner)) {
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
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            cache.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(CACHE_TAG, cache.serializeNBT(registries));
        tag.putUUID(BASIN_ID_TAG, basinId);
        tag.putLong(CYCLE_TAG, generationCycle);
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
        ResourceLocation mode = tag.contains(MODE_TAG, CompoundTag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(MODE_TAG)) : null;
        activation = TreasureBasinActivation.resolve(
                mode != null, mode != null, mode, null);
        cacheFull = tag.getBoolean(CACHE_FULL_TAG);
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
}
