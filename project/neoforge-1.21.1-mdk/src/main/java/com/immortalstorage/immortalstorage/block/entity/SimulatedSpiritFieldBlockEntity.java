package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceEngine;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedSpiritFieldMenu;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.spiritfield.SimulatedSpiritFieldCropCatalog;
import com.immortalstorage.immortalstorage.worldshard.WorldShardOutputRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** One seed, one fuel, one harvesting tool and twelve atomic output slots. */
public final class SimulatedSpiritFieldBlockEntity extends BlockEntity
        implements WorldlyContainer, MenuProvider, ReinforcementPluginHost {
    public static final int SEED_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int TOOL_SLOT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_COUNT = 12;
    public static final int OUTPUT_END = OUTPUT_START + OUTPUT_COUNT;
    public static final int PLUGIN_SLOT = TOOL_SLOT;
    public static final int SLOT_COUNT = OUTPUT_END;
    public static final int PROCESS_TICKS = 50;
    public static final int DATA_COUNT = 11;
    private static final int[] SEED_ONLY = {SEED_SLOT};
    private static final int[] FUEL_ONLY = {FUEL_SLOT};
    private static final int[] OUTPUTS = java.util.stream.IntStream.range(
            OUTPUT_START, OUTPUT_END).toArray();
    private static final int[] EMPTY = {};
    private static final TagKey<Item> SEED_TAG = ItemTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    ImmortalStorageMod.MODID, "simulated_spirit_field_seeds"));
    private static final TagKey<Block> SUBSTRATE_TAG = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    ImmortalStorageMod.MODID, "simulated_spirit_field_substrates"));
    private static final TagKey<Block> END_STONE_TAG = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "end_stones"));
    private static final TagKey<Block> SOIL_TAG = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "soils"));
    private static final TagKey<Block> FARMLAND_TAG = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "farmlands"));

    private net.minecraft.core.NonNullList<ItemStack> items =
            net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int burnTicks;
    private int burnDuration;
    private int storedExperience;
    private boolean xianqiaoOutput = true;
    private boolean automaticOutput = true;
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private BlockState substrate = Blocks.FARMLAND.defaultBlockState()
            .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
    private BlockState substrateSource = Blocks.DIRT.defaultBlockState();
    private final IItemHandler[] handlers = new IItemHandler[7];
    private long lastSyncTick = Long.MIN_VALUE;
    private long syncInvocationCount;
    private long lastSyncInvocation = Long.MIN_VALUE;
    private ItemStack legacyPluginOverflow = ItemStack.EMPTY;
    private @Nullable PersonalStorageNetwork.Endpoint cachedOutputEndpoint;
    private @Nullable UUID cachedEndpointOwner;
    private @Nullable ServerPlayer cachedEndpointPlayer;
    private @Nullable net.minecraft.server.MinecraftServer cachedEndpointServer;
    private @Nullable net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> cachedEndpointDimension;
    private @Nullable List<ItemStack> pendingHarvestDrops;
    private ItemStack cachedCropSeed = ItemStack.EMPTY;
    private Optional<BlockState> cachedCrop = Optional.empty();
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> burnTicks; case 2 -> storedExperience;
            case 3 -> xianqiaoOutput ? 1 : 0;
            case 4 -> automaticOutput ? 1 : 0;
            case 5, 6, 7, 8, 9, 10 -> outputFaces[index - 5] ? 1 : 0;
            default -> 0; }; }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value; else if (index == 1) burnTicks = value;
            else if (index == 2) storedExperience = value;
            else if (index == 3) xianqiaoOutput = value != 0;
            else if (index == 4) automaticOutput = value != 0;
            else if (index >= 5 && index < 11) outputFaces[index - 5] = value != 0;
        }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public SimulatedSpiritFieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMULATED_SPIRIT_FIELD.get(), pos, state);
    }
    public ContainerData dataAccess() { return data; }
    public BlockState substrate() { return substrate; }
    public int progress() { return progress; }
    public boolean displaysGrowingChorusFlower() { return items.get(SEED_SLOT).is(Items.CHORUS_FRUIT); }
    public int burnTicks() { return burnTicks; }
    public int storedExperience() { return storedExperience; }
    public boolean xianqiaoOutput() { return xianqiaoOutput; }
    public boolean automaticOutput() { return automaticOutput; }
    public boolean outputFace(Direction side) { return side != null && outputFaces[side.ordinal()]; }
    public void toggleXianqiaoOutput() {
        xianqiaoOutput = !xianqiaoOutput;
        if (level instanceof ServerLevel serverLevel) {
            PersonalStorageNetwork.Endpoint endpoint = endpoint(
                    serverLevel, effectiveXianqiaoOwner(serverLevel));
            if (xianqiaoOutput) {
                flushOutputCacheToXianqiao(endpoint);
            }
            if (pendingHarvestDrops != null) {
                publishCompletedHarvest(serverLevel, endpoint, pendingHarvestDrops);
            }
        }
        setChangedAndSync();
    }
    public void toggleAutomaticOutput() { automaticOutput = !automaticOutput; setChangedAndSync(); }
    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        handlers[side.ordinal()] = null;
        setChangedAndSync();
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        IItemHandler handler = handlers[index];
        if (handler == null) handlers[index] = handler = side == null
                ? new InvWrapper(this) : new SidedInvWrapper(this, side);
        return handler;
    }

    public boolean isValidSeed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (SimulatedSpiritFieldCropCatalog.find(stack.getItem()).isPresent()) return true;
        if (stack.is(SEED_TAG) || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(Items.CHORUS_FLOWER) || stack.is(Items.CHORUS_FRUIT)
                || stack.is(Items.NETHER_WART)) return true;
        boolean plantableTag = stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("seed") || path.contains("plantable");
        });
        return plantableTag && stack.getItem() instanceof BlockItem;
    }

    public boolean isValidSubstrate(BlockState state) {
        return state != null && !state.isAir() && (state.is(SUBSTRATE_TAG)
                || state.is(BlockTags.DIRT) || state.is(SOIL_TAG) || state.is(FARMLAND_TAG)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.END_STONE) || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL) || likelySoil(state));
    }

    private static boolean likelySoil(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("dirt") || path.contains("soil") || path.contains("farmland")
                || path.contains("end_stone") || path.contains("soul_sand");
    }

    public void replaceSubstrate(ServerPlayer player, BlockState replacement, ItemStack held) {
        if (!isValidSubstrate(replacement) || replacement.is(substrate.getBlock())) return;
        ItemStack previous = new ItemStack(substrateSource.getBlock().asItem());
        substrateSource = replacement;
        substrate = hydrated(replacement);
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
        setChangedAndSync();
    }

    public void dropStoredSubstrate() {
        if (level == null) return;
        ItemStack stack = new ItemStack(substrateSource.getBlock().asItem());
        if (!stack.isEmpty()) Block.popResource(level, worldPosition, stack);
    }

    private static BlockState hydrated(BlockState state) {
        if (state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND)) {
            return Blocks.FARMLAND.defaultBlockState().setValue(
                    net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
        }
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty moisture && "moisture".equals(property.getName())) {
                int max = moisture.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
                return state.setValue(moisture, max);
            }
        }
        return state;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  SimulatedSpiritFieldBlockEntity field) {
        if (!field.legacyPluginOverflow.isEmpty()) {
            Block.popResource(level, pos, field.legacyPluginOverflow);
            field.legacyPluginOverflow = ItemStack.EMPTY;
            field.setChanged();
        }
        // Face output is attempted before the Xianqiao fallback on every
        // server tick, including ticks with no harvest ready.
        boolean changed = field.pushCachedToFaces(level);
        UUID outputOwner = field.effectiveXianqiaoOwner(level);
        PersonalStorageNetwork.Endpoint endpoint = field.endpoint(level, outputOwner);
        changed |= field.flushOutputCacheToXianqiao(endpoint);
        if (field.pendingHarvestDrops != null) {
            boolean settled = field.publishCompletedHarvest(
                    level, endpoint, field.pendingHarvestDrops);
            changed |= settled;
            if (!settled) {
                field.progress = 0;
                field.setChangedAndMaybeSync(level);
                return;
            }
        }
        if (!field.hasHarvestableCrop()) {
            field.progress = 0;
            if (changed) field.setChangedAndMaybeSync(level);
            return;
        }
        if (field.burnTicks <= 0 && !field.consumeFuel(level)) {
            if (changed) field.setChangedAndMaybeSync(level);
            return;
        }
        field.burnTicks--;
        if (++field.progress >= PROCESS_TICKS) {
            // The seed is a permanent specimen: harvesting never shrinks or replaces SEED_SLOT.
            List<ItemStack> drops = field.harvestDrops(level);
            if (!drops.isEmpty()) field.publishCompletedHarvest(level, endpoint, drops);
            field.progress = 0;
        }
        field.setChangedAndMaybeSync(level);
    }

    private boolean pushCachedToFaces(ServerLevel level) {
        return MachineOutputScheduler.pushItemsToFaces(
                level, worldPosition, automaticOutput, outputFaces,
                getItemHandler(null), OUTPUT_START, OUTPUT_END);
    }

    private boolean flushOutputCacheToXianqiao(@Nullable PersonalStorageNetwork.Endpoint endpoint) {
        return MachineOutputScheduler.flushItemsToXianqiao(
                getItemHandler(null), OUTPUT_START, OUTPUT_END, endpoint);
    }

    private boolean consumeFuel(ServerLevel level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.is(ModItems.TRUE_YUAN.get())) {
            fuel.shrink(1); burnTicks = burnDuration = ImmortalFurnaceEngine.TRUE_YUAN.burnTicks(); return true;
        }
        if (fuel.is(ModItems.IMMORTAL_YUAN.get())) {
            fuel.shrink(1); burnTicks = burnDuration = ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks(); return true;
        }
        XianqiaoBindingPolicy.Binding binding = XianqiaoBindingPolicy.resolve(level, fuel);
        UUID payer = binding.isBound() ? binding.owner() : null;
        ServerPlayer owner = payer == null ? null
                : com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.onlinePlayer(level.getServer(), payer);
        ImmortalStoragePlayerData ownerData = owner == null ? null : ImmortalStoragePlayerData.get(owner);
        if (ownerData != null && ownerData.consumeImmortalYuan(1L)) {
            burnTicks = burnDuration = ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks(); return true;
        }
        if (fuel.getItem() instanceof SpiritDriveItem && ownerData != null && ownerData.consumeTrueYuan(1L)) {
            burnTicks = burnDuration = ImmortalFurnaceEngine.TRUE_YUAN.burnTicks(); return true;
        }
        return false;
    }

    public Optional<BlockState> displayCrop() {
        return cropFor(items.get(SEED_SLOT)).map(crop -> ageForProgress(crop, progress));
    }

    private Optional<BlockState> cropFor(ItemStack seed) {
        if (seed == null) seed = ItemStack.EMPTY;
        if (ItemStack.isSameItemSameComponents(cachedCropSeed, seed)) return cachedCrop;
        cachedCropSeed = seed.isEmpty() ? ItemStack.EMPTY : seed.copyWithCount(1);
        cachedCrop = Optional.empty();
        if (!isValidSeed(seed)) return cachedCrop;
        Optional<SimulatedSpiritFieldCropCatalog.Entry> mapped =
                SimulatedSpiritFieldCropCatalog.find(seed.getItem());
        if (mapped.isPresent()) {
            cachedCrop = Optional.of(mapped.get().crop());
            return cachedCrop;
        }
        if (seed.is(Items.CHORUS_FLOWER) || seed.is(Items.CHORUS_FRUIT)) {
            cachedCrop = Optional.of(Blocks.CHORUS_FLOWER.defaultBlockState());
            return cachedCrop;
        }
        if (seed.is(Items.NETHER_WART)) {
            cachedCrop = Optional.of(Blocks.NETHER_WART.defaultBlockState());
            return cachedCrop;
        }
        cachedCrop = seed.getItem() instanceof BlockItem blockItem
                ? Optional.of(blockItem.getBlock().defaultBlockState()) : Optional.empty();
        return cachedCrop;
    }

    private static boolean compatible(BlockState crop, BlockState base) {
        if (crop.is(Blocks.CHORUS_FLOWER) || crop.is(Blocks.CHORUS_PLANT)) {
            return base.is(Blocks.END_STONE) || base.is(END_STONE_TAG);
        }
        if (crop.is(Blocks.NETHER_WART)) return base.is(Blocks.SOUL_SAND);
        return !base.is(Blocks.END_STONE) && !base.is(Blocks.SOUL_SAND) && !base.is(Blocks.SOUL_SOIL);
    }

    private boolean compatibleSeed(ItemStack seed, BlockState crop, BlockState base) {
        Optional<SimulatedSpiritFieldCropCatalog.Entry> mapped =
                SimulatedSpiritFieldCropCatalog.find(seed.getItem());
        return mapped.map(entry -> entry.accepts(base)).orElseGet(() -> compatible(crop, base));
    }

    private static BlockState ageForProgress(BlockState crop, int progress) {
        int scaled = Math.min(PROCESS_TICKS, Math.max(0, progress));
        if (crop.getBlock() instanceof CropBlock crops) {
            return crops.getStateForAge(crops.getMaxAge() * scaled / PROCESS_TICKS);
        }
        for (var property : crop.getProperties()) {
            if (property instanceof IntegerProperty age && "age".equals(property.getName())) {
                int min = age.getPossibleValues().stream().min(Integer::compareTo).orElse(0);
                int max = age.getPossibleValues().stream().max(Integer::compareTo).orElse(min);
                return crop.setValue(age, min + (max - min) * scaled / PROCESS_TICKS);
            }
        }
        return crop;
    }

    private boolean hasHarvestableCrop() {
        Optional<BlockState> crop = cropFor(items.get(SEED_SLOT));
        return crop.isPresent() && compatibleSeed(items.get(SEED_SLOT), crop.get(), substrate);
    }

    private List<ItemStack> harvestDrops(ServerLevel level) {
        Optional<BlockState> crop = cropFor(items.get(SEED_SLOT));
        if (crop.isEmpty() || !compatibleSeed(items.get(SEED_SLOT), crop.get(), substrate)) return List.of();
        if (items.get(SEED_SLOT).is(Items.CHORUS_FRUIT)) {
            return multiplyDrops(List.of(new ItemStack(Items.CHORUS_FLOWER), new ItemStack(Items.CHORUS_FRUIT, 2)));
        }
        BlockState mature = ageForProgress(crop.get(), PROCESS_TICKS);
        if (mature.is(Blocks.CHORUS_FLOWER)) mature = Blocks.CHORUS_PLANT.defaultBlockState();
        List<ItemStack> drops = Block.getDrops(mature, level, worldPosition.above(), null, null,
                items.get(TOOL_SLOT));
        return multiplyDrops(drops.isEmpty() && mature.is(Blocks.CHORUS_PLANT)
                ? List.of(new ItemStack(Items.CHORUS_FRUIT)) : drops);
    }

    private List<ItemStack> multiplyDrops(List<ItemStack> source) {
        long multiplier = (long) Math.max(1, items.get(SEED_SLOT).getCount()) * reinforcementMultiplier();
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : source) {
            long remaining = Math.min(Integer.MAX_VALUE, (long) stack.getCount() * multiplier);
            while (remaining > 0L) {
                int count = (int) Math.min(stack.getMaxStackSize(), remaining);
                result.add(stack.copyWithCount(count));
                remaining -= count;
            }
        }
        return result;
    }

    /**
     * Commits as much of one completed harvest as possible to the visible
     * output cache, then to enabled accepting faces. The exact remainder is
     * returned for the persistent temporary cache; production never ejects it.
     */
    private List<ItemStack> routeToLocalCache(ServerLevel level, List<ItemStack> drops) {
        List<ItemStack> temporary = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            for (int slot = OUTPUT_START; slot < OUTPUT_END && !remaining.isEmpty(); slot++) {
                ItemStack target = items.get(slot);
                if (target.isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    items.set(slot, remaining.copyWithCount(moved)); remaining.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(target, remaining)) {
                    int moved = Math.min(remaining.getCount(), target.getMaxStackSize() - target.getCount());
                    target.grow(moved); remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                remaining = MachineOutputScheduler.pushItemToFaces(
                        level, worldPosition, automaticOutput, outputFaces, remaining);
                if (!remaining.isEmpty()) temporary.add(remaining.copy());
            }
        }
        return List.copyOf(temporary);
    }

    /**
     * Publishes one completed harvest according to the selected output domain.
     * Direct Xianqiao output never checks local free space and never ejects an
     * overflow stack. A rejected direct transaction remains pending intact.
     */
    private boolean publishCompletedHarvest(
            ServerLevel level,
            @Nullable PersonalStorageNetwork.Endpoint endpoint,
            List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            pendingHarvestDrops = null;
            return true;
        }
        if (xianqiaoOutput) {
            WorldShardOutputRouter.RouteResult route =
                    WorldShardOutputRouter.routeDirect(drops, endpoint);
            if (route.unaccepted() > 0L) {
                if (pendingHarvestDrops == null) pendingHarvestDrops = copyStacks(drops);
                return false;
            }
            pendingHarvestDrops = null;
            return true;
        }
        List<ItemStack> temporary = routeToLocalCache(level, drops);
        pendingHarvestDrops = temporary.isEmpty() ? null : temporary;
        setChanged();
        return pendingHarvestDrops == null;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> source) {
        List<ItemStack> copies = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            if (stack != null && !stack.isEmpty()) copies.add(stack.copy());
        }
        return copies;
    }

    /** Returns and clears production awaiting output when the block is removed. */
    public List<ItemStack> drainPendingOutputForRemoval() {
        if (pendingHarvestDrops == null || pendingHarvestDrops.isEmpty()) return List.of();
        List<ItemStack> drained = copyStacks(pendingHarvestDrops);
        pendingHarvestDrops = null;
        setChanged();
        return drained;
    }

    private @Nullable UUID effectiveXianqiaoOwner(ServerLevel level) {
        if (!xianqiaoOutput) return null;
        UUID boundOwner = XianqiaoBindingPolicy.resolve(level, items.get(FUEL_SLOT)).owner();
        return boundOwner != null && com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                .onlinePlayer(level.getServer(), boundOwner) != null ? boundOwner : null;
    }

    private @Nullable PersonalStorageNetwork.Endpoint endpoint(ServerLevel level, @Nullable UUID outputOwner) {
        if (outputOwner == null) return null;
        net.minecraft.server.MinecraftServer server = level.getServer();
        ServerPlayer player = com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity
                .onlinePlayer(server, outputOwner);
        if (player == null) {
            cachedOutputEndpoint = null;
            cachedEndpointOwner = null;
            cachedEndpointPlayer = null;
            return null;
        }
        if (outputOwner.equals(cachedEndpointOwner)
                && player == cachedEndpointPlayer
                && server == cachedEndpointServer
                && level.dimension().equals(cachedEndpointDimension)
                && cachedOutputEndpoint != null
                && cachedOutputEndpoint.online()
                && outputOwner.equals(cachedOutputEndpoint.owner())
                && cachedOutputEndpoint.stage() >= 6
                && (!(cachedOutputEndpoint
                instanceof PersonalStorageNetwork.Endpoint concrete)
                || concrete.data() == ImmortalStoragePlayerData.get(player))) {
            return cachedOutputEndpoint;
        }
        PersonalStorageNetwork.Endpoint resolved = ImmortalStorageDimensions.isPersonalRealmFor(
                level.dimension(), outputOwner)
                ? PersonalStorageNetwork.resolveInOwnerRealm(level, outputOwner, this::setChanged)
                : PersonalStorageNetwork.resolve(level.getServer(), outputOwner, this::setChanged);
        cachedOutputEndpoint = resolved;
        cachedEndpointOwner = outputOwner;
        cachedEndpointPlayer = player;
        cachedEndpointServer = server;
        cachedEndpointDimension = level.dimension();
        return resolved;
    }

    public void releaseExperience(ServerPlayer player) {
        if (storedExperience <= 0) return;
        player.giveExperiencePoints(storedExperience);
        storedExperience = 0;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            lastSyncTick = serverLevel.getGameTime();
            lastSyncInvocation = ++syncInvocationCount;
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            broadcastOpenMenu(serverLevel);
        }
    }

    private void setChangedAndMaybeSync(ServerLevel serverLevel) {
        setChanged();
        long invocation = ++syncInvocationCount;
        if (!MachineTickSync.due(serverLevel, lastSyncTick, invocation, lastSyncInvocation)) return;
        lastSyncTick = serverLevel.getGameTime();
        lastSyncInvocation = invocation;
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        broadcastOpenMenu(serverLevel);
    }

    private void broadcastOpenMenu(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == serverLevel
                    && player.containerMenu instanceof SimulatedSpiritFieldMenu menu
                    && menu.blockPos().equals(worldPosition)) {
                menu.broadcastChanges();
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("container.immortalstorage.simulated_spirit_field");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SimulatedSpiritFieldMenu(id, inventory, this);
    }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() {
        return (pendingHarvestDrops == null || pendingHarvestDrops.isEmpty())
                && items.stream().allMatch(ItemStack::isEmpty);
    }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack); stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() {
        items.clear();
        pendingHarvestDrops = null;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) return stack.is(ModItems.TRUE_YUAN.get())
                || stack.is(ModItems.IMMORTAL_YUAN.get()) || stack.getItem() instanceof SpiritDriveItem;
        if (slot == TOOL_SLOT) return ReinforcementPluginHost.isPlugin(stack)
                || stack.getMaxStackSize() == 1;
        return slot == SEED_SLOT && isValidSeed(stack);
    }
    @Override public int[] getSlotsForFace(Direction side) {
        int[] input = side == Direction.UP ? SEED_ONLY
                : side.getAxis().isHorizontal() ? FUEL_ONLY : EMPTY;
        return automaticOutput && outputFace(side)
                ? java.util.stream.IntStream.concat(Arrays.stream(input), Arrays.stream(OUTPUTS)).toArray()
                : input.clone();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && ((side == Direction.UP && slot == SEED_SLOT)
                || (side.getAxis().isHorizontal() && slot == FUEL_SLOT)) && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return automaticOutput && outputFace(side) && slot >= OUTPUT_START && slot < OUTPUT_END;
    }
    @Override public ItemStack reinforcementPlugin() { return items.get(PLUGIN_SLOT); }
    @Override public void setReinforcementPlugin(ItemStack stack) {
        setItem(PLUGIN_SLOT, stack.copyWithCount(1));
        setChangedAndSync();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress); tag.putInt("BurnTicks", burnTicks);
        tag.putInt("BurnDuration", burnDuration); tag.put("Substrate", NbtUtils.writeBlockState(substrate));
        tag.put("SubstrateSource", NbtUtils.writeBlockState(substrateSource));
        tag.putInt("StoredExperience", storedExperience);
        tag.putBoolean("XianqiaoOutput", xianqiaoOutput);
        tag.putBoolean("AutomaticFaceOutput", automaticOutput);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        tag.putIntArray("OutputFaces", faces);
        if (!legacyPluginOverflow.isEmpty()) {
            tag.put("LegacyPluginOverflow", legacyPluginOverflow.save(registries));
        }
        if (pendingHarvestDrops != null && !pendingHarvestDrops.isEmpty()) {
            ListTag pending = new ListTag();
            for (ItemStack stack : pendingHarvestDrops) pending.add(stack.save(registries));
            tag.put("PendingHarvestDrops", pending);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        net.minecraft.core.NonNullList<ItemStack> loaded =
                net.minecraft.core.NonNullList.withSize(OUTPUT_END + 1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, loaded, registries);
        items = net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOT_COUNT; slot++) items.set(slot, loaded.get(slot));
        legacyPluginOverflow = tag.contains("LegacyPluginOverflow")
                ? ItemStack.parseOptional(registries, tag.getCompound("LegacyPluginOverflow"))
                : ItemStack.EMPTY;
        migrateLegacyPlugin(loaded.get(OUTPUT_END));
        pendingHarvestDrops = null;
        ListTag pending = tag.getList("PendingHarvestDrops", Tag.TAG_COMPOUND);
        if (!pending.isEmpty()) {
            pendingHarvestDrops = new ArrayList<>(pending.size());
            for (int index = 0; index < pending.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(registries, pending.getCompound(index));
                if (!stack.isEmpty()) pendingHarvestDrops.add(stack);
            }
            if (pendingHarvestDrops.isEmpty()) pendingHarvestDrops = null;
        }
        cachedCropSeed = ItemStack.EMPTY;
        cachedCrop = Optional.empty();
        progress = tag.getInt("Progress"); burnTicks = tag.getInt("BurnTicks");
        burnDuration = tag.getInt("BurnDuration");
        storedExperience = tag.getInt("StoredExperience");
        xianqiaoOutput = tag.contains("XianqiaoOutput")
                ? tag.getBoolean("XianqiaoOutput")
                : !tag.contains("AutomaticOutput") || tag.getBoolean("AutomaticOutput");
        automaticOutput = !tag.contains("AutomaticFaceOutput") || tag.getBoolean("AutomaticFaceOutput");
        Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces");
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
        if (tag.contains("Substrate")) substrate = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Substrate"));
        if (tag.contains("SubstrateSource")) substrateSource = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("SubstrateSource"));
    }

    private void migrateLegacyPlugin(ItemStack legacy) {
        if (!ReinforcementPluginHost.isPlugin(legacy)) return;
        if (items.get(PLUGIN_SLOT).isEmpty()) {
            items.set(PLUGIN_SLOT, legacy.copyWithCount(1));
            return;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, legacy.copyWithCount(1));
                return;
            }
        }
        legacyPluginOverflow = legacy.copyWithCount(1);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
