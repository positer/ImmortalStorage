package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.immortalstorage.api.storage.CanonicalEnergyStorage;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageApi;
import com.immortalstorage.immortalstorage.api.storage.PersonalStorageEndpoint;
import com.immortalstorage.immortalstorage.block.custom.EnergyCrystalBlock;
import com.immortalstorage.immortalstorage.compat.CrystalResourceCompatHooks;
import com.immortalstorage.immortalstorage.compat.EnergyCrystalItemAccess;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.SpiritDriveItem;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceEngine;
import com.immortalstorage.immortalstorage.menu.custom.EnergyCrystalMenu;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Server-authoritative FE generator and rechargeable-item processor.
 *
 * <p>The FE ledger is deliberately independent of optional energy mods.  The
 * same long-valued FE namespace is exposed to the Xianqiao, AE2 and RS bridges
 * even when Mekanism, Flux Networks, or both are absent.</p>
 */
public final class EnergyCrystalBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity
        implements WorldlyContainer, MenuProvider, ReinforcementPluginHost {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int EXTRA_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int PROCESS_TICKS = 50;
    public static final int DATA_COUNT = 15;

    private static final int[] TOP_PROCESSING_INPUT = {INPUT_SLOT};
    private static final int[] SIDE_FUEL_INPUT = {FUEL_SLOT};
    private static final int[] EXTRA_OUTPUT = {EXTRA_SLOT};
    private static final int[] PROCESSING_AND_EXTRA = {INPUT_SLOT, FUEL_SLOT, EXTRA_SLOT};
    private static final int[] EMPTY = {};
    private static final TagKey<Item> CERTUS_QUARTZ = ItemTags.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "certus_quartz"));
    private static final Direction[] OUTPUT_PRIORITY = {
            Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final CrystalKind kind;
    private final IItemHandler[] itemHandlers = new IItemHandler[Direction.values().length + 1];
    private final boolean[] outputFaces = new boolean[Direction.values().length];
    private UUID owner;
    private boolean xianqiaoOutputEnabled;
    private boolean automaticOutputEnabled = true;
    private long energy;
    /** Long-valued local cache presented to the same transfer code as Xianqiao FE. */
    private final AtomicEnergyRefill.ResourceStore localEnergyStore =
            new AtomicEnergyRefill.ResourceStore() {
                @Override public long amount() { return Math.max(0L, energy); }

                @Override
                public long extract(long requested,
                                    com.immortalstorage.core.resource.ResourceTransferAction action) {
                    long moved = Math.min(Math.max(0L, requested), Math.max(0L, energy));
                    if (action.executes()) energy -= moved;
                    return moved;
                }

                @Override
                public long insert(long offered,
                                   com.immortalstorage.core.resource.ResourceTransferAction action) {
                    long moved = Math.min(Math.max(0L, offered),
                            Math.max(0L, capacity() - Math.max(0L, energy)));
                    if (action.executes()) energy += moved;
                    return moved;
                }
            };
    private int processTicks;
    private int burnTicks;
    private int burnDuration;
    private long lastSyncTick = Long.MIN_VALUE;
    private long syncInvocationCount;
    private long lastSyncInvocation = Long.MIN_VALUE;
    private @Nullable PersonalStorageEndpoint cachedOwnerEndpoint;
    private @Nullable UUID cachedEndpointOwner;
    private @Nullable ServerPlayer cachedEndpointPlayer;
    private @Nullable net.minecraft.server.MinecraftServer cachedEndpointServer;
    private @Nullable net.minecraft.resources.ResourceKey<Level> cachedEndpointDimension;
    private @Nullable PersonalStorageEndpoint cachedEnergyStoreEndpoint;
    private @Nullable AtomicEnergyRefill.ResourceStore cachedEnergyStore;
    private boolean compatLoaded;
    private boolean sparkAttached;
    private boolean sourceStart;
    private boolean sourcePriority;

    private final CanonicalEnergyStorage energyStorage = new CanonicalEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            AtomicEnergyRefill.ResourceStore external = boundExternalEnergy();
            if (external != null) {
                long accepted = external.insert(maxReceive,
                        simulate
                                ? com.immortalstorage.core.resource.ResourceTransferAction.SIMULATE
                                : com.immortalstorage.core.resource.ResourceTransferAction.EXECUTE);
                if (!simulate && accepted > 0L) markChangedForTick();
                return saturatingInt(accepted);
            }
            long accepted = Math.min((long) maxReceive, Math.max(0L, capacity() - energy));
            if (!simulate && accepted > 0L) {
                energy += accepted;
                markChangedForTick();
            }
            return (int) accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            AtomicEnergyRefill.ResourceStore external = boundExternalEnergy();
            if (external != null) {
                long extracted = external.extract(maxExtract,
                        simulate
                                ? com.immortalstorage.core.resource.ResourceTransferAction.SIMULATE
                                : com.immortalstorage.core.resource.ResourceTransferAction.EXECUTE);
                if (!simulate && extracted > 0L) markChangedForTick();
                return saturatingInt(extracted);
            }
            long extracted = Math.min((long) maxExtract, energy);
            if (!simulate && extracted > 0L) {
                energy -= extracted;
                markChangedForTick();
            }
            return (int) extracted;
        }

        @Override public int getEnergyStored() {
            return saturatingInt(Math.min(displayedEnergy(), capacity()));
        }

        @Override public int getMaxEnergyStored() {
            return saturatingInt(capacity());
        }

        @Override public boolean canExtract() { return displayedEnergy() > 0L; }
        @Override public boolean canReceive() {
            return boundExternalEnergy() != null || energy < capacity();
        }

        @Override public com.immortalstorage.core.resource.ResourceChannelKey canonicalChannel() {
            return ExternalResourceChannels.FE;
        }

        @Override public @Nullable UUID canonicalOwner() {
            return xianqiaoOutputEnabled ? owner : null;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> processTicks;
                case 1 -> burnTicks;
                case 2 -> burnDuration;
                case 3 -> (int) displayedEnergy();
                case 4 -> (int) (displayedEnergy() >>> 32);
                case 5 -> (int) capacity();
                case 6 -> (int) (capacity() >>> 32);
                case 7 -> xianqiaoOutput() ? 1 : 0;
                case 8 -> automaticOutput() ? 1 : 0;
                case 9, 10, 11, 12, 13, 14 -> outputFaces[index - 9] ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> processTicks = Math.max(0, value);
                case 1 -> burnTicks = Math.max(0, value);
                case 2 -> burnDuration = Math.max(0, value);
                case 3 -> energy = (energy & 0xFFFFFFFF00000000L) | Integer.toUnsignedLong(value);
                case 4 -> energy = (energy & 0x00000000FFFFFFFFL) | (Integer.toUnsignedLong(value) << 32);
                case 5 -> { /* capacity is config-backed and never client-edited */ }
                case 6 -> { /* capacity is config-backed and never client-edited */ }
                case 7 -> xianqiaoOutputEnabled = value != 0;
                case 8 -> automaticOutputEnabled = value != 0;
                case 9, 10, 11, 12, 13, 14 -> outputFaces[index - 9] = value != 0;
                default -> { }
            }
        }

        @Override public int getCount() { return DATA_COUNT; }
    };

    public EnergyCrystalBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, CrystalKind.ELECTRIC);
    }

    public EnergyCrystalBlockEntity(BlockPos pos, BlockState state, CrystalKind kind) {
        super(ModBlockEntities.typeFor(kind).get(), pos, state);
        this.kind = kind;
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && !compatLoaded) {
            compatLoaded = true;
            CrystalResourceCompatHooks.onLoad(this, serverLevel);
        }
    }

    public CrystalKind kind() { return kind; }

    public com.immortalstorage.core.resource.ResourceChannelKey resourceChannel() {
        return kind.channel();
    }

    public String resourceUnit() { return kind.unit(); }

    public AtomicEnergyRefill.ResourceStore localResourceStore() {
        return localEnergyStore;
    }

    /** Current owner-bound cache, or null while Xianqiao output is disabled. */
    public @Nullable AtomicEnergyRefill.ResourceStore externalResourceStore(ServerLevel level) {
        return ownerExternalEnergy(level);
    }

    /** The live store exposed to an optional external capability. */
    public AtomicEnergyRefill.ResourceStore integrationResourceStore(ServerLevel level) {
        AtomicEnergyRefill.ResourceStore external = externalResourceStore(level);
        return external == null ? localEnergyStore : external;
    }

    public boolean sparkAttached() { return sparkAttached; }
    public void markSparkAttached() {
        if (!sparkAttached) {
            sparkAttached = true;
            setChanged();
        }
    }
    public boolean sourceStart() { return sourceStart; }
    public boolean sourcePriority() { return sourcePriority; }
    public void markSourceStart() { sourceStart = true; setChanged(); }
    public void markSourcePriority() { sourcePriority = true; setChanged(); }
    public void clearSourceDesignation() {
        sourceStart = false;
        sourcePriority = false;
        setChanged();
    }

    public void onRemovedFromWorld(ServerLevel level) {
        CrystalResourceCompatHooks.onRemoved(this, level);
    }

    public ContainerData dataAccess() { return data; }
    public CanonicalEnergyStorage getEnergyHandler(@Nullable Direction side) { return energyStorage; }
    /** Returns the FE amount represented by the currently bound container. */
    public long storedEnergy() { return displayedEnergy(); }
    public long energyCapacity() { return capacity(); }
    public int burnTicks() { return burnTicks; }
    public int burnDuration() { return burnDuration; }
    public int processTicks() { return processTicks; }
    public boolean xianqiaoOutput() { return xianqiaoOutputEnabled; }
    public boolean automaticOutput() { return automaticOutputEnabled; }
    public boolean outputFace(Direction side) {
        return side != null && outputFaces[side.ordinal()];
    }

    public boolean toggleXianqiaoOutput() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        refreshBinding(serverLevel);
        if (xianqiaoOutputEnabled) {
            // Closing only detaches the external container.  The player's FE
            // remains in Xianqiao and this block resumes using its local cache.
            xianqiaoOutputEnabled = false;
            setChangedAndSync();
            return true;
        }

        UUID targetOwner = boundOwner(serverLevel);
        if (targetOwner == null || ownerExternalEnergy(serverLevel, targetOwner) == null) {
            // An unbound crystal must never present an enabled Xianqiao switch.
            xianqiaoOutputEnabled = false;
            setChangedAndSync();
            return false;
        }
        long before = energy;
        flushOutputCacheToXianqiao(serverLevel, targetOwner);
        if (energy != 0L) {
            // Do not claim the FE container was rebound while local FE remains.
            setChangedAndSync();
            return false;
        }
        owner = targetOwner;
        xianqiaoOutputEnabled = true;
        if (before != energy || owner != null) setChangedAndSync();
        return true;
    }

    public void toggleAutomaticOutput() {
        automaticOutputEnabled = !automaticOutputEnabled;
        setChangedAndSync();
    }

    public void toggleOutputFace(Direction side) {
        if (side == null) return;
        outputFaces[side.ordinal()] = !outputFaces[side.ordinal()];
        if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
        setChangedAndSync();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
                                  EnergyCrystalBlockEntity crystal) {
        if (!crystal.compatLoaded) {
            crystal.compatLoaded = true;
            CrystalResourceCompatHooks.onLoad(crystal, level);
        }
        CrystalResourceCompatHooks.serverTick(crystal, level);
        crystal.refreshBinding(level);
        UUID outputOwner = crystal.effectiveOutputOwner(level);
        boolean changed = false;
        if (crystal.burnTicks <= 0 && !crystal.consumeFuel(level)) {
            // No processing occurs without fuel, so this is the only output
            // pass needed for the current logical tick.  Keeping it here
            // avoids a duplicate capability scan before the fuel branch.
            changed |= crystal.pushExtraToFaces(level);
            // Resource caches remain externally interactive even while the
            // generator is idle.  Every configured face gets its full-cache
            // attempt on every server tick, independent of burn state.
            changed |= crystal.pushEnergy(level, outputOwner);
            changed |= crystal.flushOutputCacheToXianqiao(level, outputOwner);
            crystal.updateLit(level, false);
            if (changed) crystal.setChangedAndMaybeSync(level);
            return;
        }

        crystal.burnTicks--;
        crystal.updateLit(level, crystal.burnTicks > 0);
        AtomicEnergyRefill.ResourceStore productionStore = crystal.boundExternalEnergy();
        if (productionStore == null) productionStore = crystal.localEnergyStore;
        changed |= productionStore.insert(
                ReinforcementPluginHost.multiplySaturated(
                        configuredOutput(crystal.kind), crystal.reinforcementMultiplier()),
                com.immortalstorage.core.resource.ResourceTransferAction.EXECUTE) > 0L;
        changed |= crystal.processInput(level);
        // One face pass per logical tick is enough.  The old pre/post passes
        // doubled capability lookups under realm acceleration without adding
        // any transfer capacity because processInput is the only writer.
        changed |= crystal.pushExtraToFaces(level);
        changed |= crystal.pushEnergy(level, outputOwner);
        // Charging and six-sided output have already had priority.  Only the
        // remaining resource cache reaches Xianqiao in this final stage;
        // completed items remain in the extra slot until bottom extraction.
        changed |= crystal.flushOutputCacheToXianqiao(level, outputOwner);
        if (changed || crystal.burnTicks == 0) crystal.setChangedAndMaybeSync(level);
    }

    private boolean consumeFuel(ServerLevel level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (fuel.is(ModItems.TRUE_YUAN.get())) {
            fuel.shrink(1);
            burnTicks = burnDuration = ImmortalFurnaceEngine.TRUE_YUAN.burnTicks();
            return true;
        }
        if (fuel.is(ModItems.IMMORTAL_YUAN.get())) {
            fuel.shrink(1);
            burnTicks = burnDuration = ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks();
            return true;
        }
        boolean spiritDriveFuel = fuel.getItem() instanceof SpiritDriveItem;
        XianqiaoBindingPolicy.Binding binding = XianqiaoBindingPolicy.resolveEnergyCrystal(level, fuel);
        if (!binding.isBound()
                || (!spiritDriveFuel
                && binding.source() == XianqiaoBindingPolicy.BindingSource.PERSONAL_REALM
                && !xianqiaoOutputEnabled)) {
            // With an empty fuel slot, a realm-bound crystal only performs
            // automatic Immortal Yuan refill while Xianqiao output is on.
            // A Spirit Drive is an explicit reusable fuel credential and its
            // replacement path remains independent of that output switch,
            // even when the crystal is inside a personal realm.
            return false;
        }
        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), binding.owner());
        ImmortalStoragePlayerData data = player == null ? null : ImmortalStoragePlayerData.get(player);
        if (data != null && data.consumeImmortalYuan(1L)) {
            burnTicks = burnDuration = ImmortalFurnaceEngine.IMMORTAL_YUAN.burnTicks();
            return true;
        }
        if ((binding.source() == XianqiaoBindingPolicy.BindingSource.SPIRIT_DRIVE || spiritDriveFuel)
                && data != null && data.consumeTrueYuan(1L)) {
            burnTicks = burnDuration = ImmortalFurnaceEngine.TRUE_YUAN.burnTicks();
            return true;
        }
        return false;
    }

    private boolean processInput(ServerLevel level) {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        if (kind != CrystalKind.ELECTRIC) {
            AtomicEnergyRefill.ResourceStore bound = xianqiaoOutputEnabled
                    ? ownerExternalEnergy(level) : null;
            // Once Xianqiao output is enabled, the bound player ledger is the
            // live cache.  The local store is only a migration/rollback
            // remainder and therefore has lower charging priority.
            AtomicEnergyRefill.ResourceStore primary = bound == null
                    ? localEnergyStore : bound;
            AtomicEnergyRefill.ResourceStore secondary = bound == null
                    ? null : localEnergyStore;
            CrystalResourceCompatHooks.InputResult optional =
                    CrystalResourceCompatHooks.processInput(
                            this, level, input, primary,
                            secondary,
                            this::canAcceptExtra);
            if (optional == null) return false;
            if (optional.output() != null) {
                if (hasReinforcementPlugin()) {
                    items.set(INPUT_SLOT, optional.output().copyWithCount(input.getCount()));
                    return true;
                }
                if (!canAcceptExtra(optional.output())) return false;
                input.shrink(1);
                insertExtra(optional.output());
                return true;
            }
            return optional.changed();
        }

        IEnergyStorage rechargeable = EnergyCrystalItemAccess.energy(input);
        if (rechargeable != null && rechargeable.getMaxEnergyStored() > 0) {
            if (rechargeable.getEnergyStored() >= rechargeable.getMaxEnergyStored()) {
                if (hasReinforcementPlugin()) return false;
                return moveInputToExtra(input.copyWithCount(1));
            }
            ItemStack completed = rechargeableOutputPreview(input);
            if (!items.get(EXTRA_SLOT).isEmpty() && !canAcceptExtra(completed)) return false;
            // Charging is an interaction, not generation.  Drain the complete
            // currently available cache through the same transactional bridge
            // used by Xianqiao interface FE pushes; never cap it at the 1k-FE
            // production setting.
            AtomicEnergyRefill.ResourceStore bound = xianqiaoOutputEnabled
                    ? ownerExternalEnergy(level) : null;
            long acceptedFromCache = XianqiaoInterfaceEnergyTransfer.pushAll(
                    bound == null ? localEnergyStore : bound, rechargeable);
            long acceptedFromXianqiao = bound == null ? 0L
                    : XianqiaoInterfaceEnergyTransfer.pushAll(localEnergyStore, rechargeable);
            boolean changed = acceptedFromCache > 0 || acceptedFromXianqiao > 0;
            if (rechargeable.getEnergyStored() >= rechargeable.getMaxEnergyStored()) {
                if (!hasReinforcementPlugin()) changed |= moveInputToExtra(input.copyWithCount(1));
            }
            return changed;
        }

        Item chargedQuartz = hasReinforcementPlugin() ? null : chargedQuartzOutput(input);
        if (chargedQuartz == null || !canAcceptExtra(new ItemStack(chargedQuartz))) {
            processTicks = 0;
            return false;
        }
        processTicks++;
        if (processTicks < PROCESS_TICKS) return true;
        processTicks = 0;
        return moveInputToExtra(new ItemStack(chargedQuartz));
    }

    /**
     * Produces the fully charged form without touching the input stack.  The
     * input slot may contain a complete stack, so the actual item remains in
     * that slot until one unit has finished and can be moved to the extra
     * output slot.
     */
    private ItemStack rechargeableOutputPreview(ItemStack input) {
        ItemStack preview = input.copyWithCount(1);
        IEnergyStorage storage = EnergyCrystalItemAccess.energy(preview);
        if (storage == null || storage.getMaxEnergyStored() <= 0) return ItemStack.EMPTY;
        int remaining = storage.getMaxEnergyStored() - storage.getEnergyStored();
        if (remaining > 0) storage.receiveEnergy(remaining, false);
        return storage.getEnergyStored() >= storage.getMaxEnergyStored()
                ? preview : ItemStack.EMPTY;
    }

    /**
     * Uses the linked player's Xianqiao FE only after the local cache has been
     * offered to the item.  Simulation is performed before extraction so a
     * partially rechargeable item cannot lose external FE.
     */
    private long rechargeFromXianqiao(ServerLevel level, IEnergyStorage rechargeable) {
        if (!xianqiaoOutputEnabled || !rechargeable.canReceive()) return 0L;
        AtomicEnergyRefill.ResourceStore source = ownerExternalEnergy(level);
        return source == null ? 0L
                : XianqiaoInterfaceEnergyTransfer.pushAll(source, rechargeable);
    }

    private boolean moveInputToExtra(ItemStack output) {
        if (!canAcceptExtra(output)) return false;
        ItemStack input = items.get(INPUT_SLOT);
        input.shrink(1);
        insertExtra(output);
        return true;
    }

    private boolean canAcceptExtra(ItemStack output) {
        ItemStack extra = items.get(EXTRA_SLOT);
        return extra.isEmpty() || ItemStack.isSameItemSameComponents(extra, output)
                && extra.getCount() + output.getCount() <= extra.getMaxStackSize();
    }

    private void insertExtra(ItemStack output) {
        ItemStack extra = items.get(EXTRA_SLOT);
        if (extra.isEmpty()) items.set(EXTRA_SLOT, output.copy());
        else extra.grow(output.getCount());
    }

    private boolean hasReinforcementPlugin() {
        return ReinforcementPluginHost.isPlugin(items.get(EXTRA_SLOT));
    }

    @Override public ItemStack reinforcementPlugin() {
        return hasReinforcementPlugin() ? items.get(EXTRA_SLOT) : ItemStack.EMPTY;
    }

    @Override public void setReinforcementPlugin(ItemStack stack) {
        items.set(EXTRA_SLOT, stack.copyWithCount(1));
        setChangedAndSync();
    }

    /**
     * A tag is the primary integration point.  The registry-name fallbacks
     * cover mods that ship a charged pair without publishing the common tag.
     */
    public static @Nullable Item chargedQuartzOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        net.minecraft.resources.Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!stack.is(CERTUS_QUARTZ) && (id == null || !id.getPath().contains("certus_quartz"))) return null;
        String path = id.getPath();
        String[] candidates = {
                "charged_" + path,
                path + "_charged",
                path.replace("certus_quartz", "charged_certus_quartz")
        };
        for (String candidate : candidates) {
            Item item = BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), candidate))
                    .orElse(null);
            if (item != null && item != Items.AIR && item != stack.getItem()) return item;
        }
        return null;
    }

    private boolean pushEnergy(ServerLevel level, @Nullable UUID outputOwner) {
        boolean changed = false;
        AtomicEnergyRefill.ResourceStore bound = xianqiaoOutputEnabled
                ? ownerExternalEnergy(level, outputOwner) : null;
        AtomicEnergyRefill.ResourceStore active = bound == null ? localEnergyStore : bound;
        if (automaticOutputEnabled) {
            for (Direction side : OUTPUT_PRIORITY) {
                if (!outputFace(side) || active.amount() <= 0L) continue;
                if (kind == CrystalKind.ELECTRIC) {
                    net.minecraft.core.BlockPos targetPos = worldPosition.relative(side);
                    IEnergyStorage target = com.immortalstorage.immortalstorage.compat.mc2612.CompatTransfer.energyHandler(level.getCapability(Capabilities.Energy.BLOCK,
                            targetPos, side.getOpposite()));
                    if (target == null || !target.canReceive()) continue;
                    while (active.amount() > 0L) {
                        long accepted = XianqiaoInterfaceEnergyTransfer.pushAll(active, target);
                        if (accepted <= 0L) break;
                        changed = true;
                    }
                } else {
                    changed |= CrystalResourceCompatHooks.pushToFace(
                            this, level, side, active);
                }
            }
        }
        if (bound != null && localEnergyStore.amount() > 0L) {
            // A failed/leftover local write is the secondary cache.  It is
            // flushed only after all configured faces had their turn.
            while (localEnergyStore.amount() > 0L) {
                long accepted = XianqiaoInterfaceEnergyTransfer.pushAll(localEnergyStore, bound);
                if (accepted <= 0L) break;
                changed = true;
            }
        }
        return changed;
    }

    private boolean pushExtraToFaces(ServerLevel level) {
        return MachineOutputScheduler.pushItemsToFaces(
                level, worldPosition, automaticOutputEnabled, outputFaces,
                getItemHandler(null), EXTRA_SLOT, EXTRA_SLOT + 1, Direction.DOWN);
    }

    private boolean flushOutputCacheToXianqiao(ServerLevel level, @Nullable UUID targetOwner) {
        // The extra slot is a physical item output only. Xianqiao output may
        // move the resource cache, but it must never consume completed items.
        return flushEnergyToXianqiao(level, targetOwner) > 0L;
    }

    private @Nullable AtomicEnergyRefill.ResourceStore ownerExternalEnergy(ServerLevel level) {
        UUID outputOwner = effectiveOutputOwner(level);
        return ownerExternalEnergy(level, outputOwner);
    }

    private @Nullable AtomicEnergyRefill.ResourceStore ownerExternalEnergy(
            ServerLevel level, @Nullable UUID outputOwner) {
        if (outputOwner == null) return null;
        PersonalStorageEndpoint endpoint = ownerStorage(level, outputOwner);
        if (endpoint == null) {
            cachedEnergyStoreEndpoint = null;
            cachedEnergyStore = null;
            return null;
        }
        if (endpoint == cachedEnergyStoreEndpoint) return cachedEnergyStore;
        ExternalResourceStorage storage = endpoint.externalResourceStorage();
        if (storage == null) {
            cachedEnergyStoreEndpoint = endpoint;
            cachedEnergyStore = null;
            return null;
        }
        AtomicEnergyRefill.ResourceStore result = new AtomicEnergyRefill.ResourceStore() {
            @Override
            public long amount() {
                for (ResourceChannelEntry entry : storage.snapshot()) {
                    if (kind.channel().equals(entry.key())) return entry.amount();
                }
                return 0L;
            }

            @Override
            public long extract(long requested, com.immortalstorage.core.resource.ResourceTransferAction action) {
                return storage.extract(kind.channel(), requested, action);
            }

            @Override
            public long insert(long offered, com.immortalstorage.core.resource.ResourceTransferAction action) {
                return storage.insert(kind.channel(), offered, action);
            }
        };
        cachedEnergyStoreEndpoint = endpoint;
        cachedEnergyStore = result;
        return result;
    }

    private @Nullable PersonalStorageEndpoint ownerStorage(
            ServerLevel level, @Nullable UUID outputOwner) {
        if (outputOwner == null) return null;
        net.minecraft.server.MinecraftServer server = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level);
        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(server, outputOwner);
        if (player == null) {
            cachedOwnerEndpoint = null;
            cachedEndpointOwner = null;
            cachedEndpointPlayer = null;
            cachedEndpointServer = server;
            cachedEndpointDimension = level.dimension();
            return null;
        }
        if (outputOwner.equals(cachedEndpointOwner)
                && player == cachedEndpointPlayer
                && server == cachedEndpointServer
                && level.dimension().equals(cachedEndpointDimension)
                && cachedOwnerEndpoint != null
                && cachedOwnerEndpoint.online()
                && cachedOwnerEndpoint.owner().equals(outputOwner)
                && cachedOwnerEndpoint.stage() >= ImmortalStoragePlayerData.XIANQIAO_EXTERNAL_UNLOCK_STAGE
                && (!(cachedOwnerEndpoint
                instanceof com.immortalstorage.immortalstorage.network.storage.PersonalStorageNetwork.Endpoint concrete)
                || concrete.data() == ImmortalStoragePlayerData.get(player))) {
            return cachedOwnerEndpoint;
        }
        PersonalStorageEndpoint endpoint = ImmortalStorageDimensions.isPersonalRealmFor(
                level.dimension(), outputOwner)
                ? PersonalStorageApi.resolveInOwnerRealm(level, outputOwner)
                : PersonalStorageApi.resolveXianqiao(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), outputOwner);
        PersonalStorageEndpoint valid = endpoint == null || !endpoint.online() || !outputOwner.equals(endpoint.owner())
                || endpoint.stage() < ImmortalStoragePlayerData.XIANQIAO_EXTERNAL_UNLOCK_STAGE
                ? null : endpoint;
        cachedOwnerEndpoint = valid;
        cachedEndpointOwner = outputOwner;
        cachedEndpointPlayer = player;
        cachedEndpointServer = server;
        cachedEndpointDimension = level.dimension();
        if (valid != cachedEnergyStoreEndpoint) {
            cachedEnergyStoreEndpoint = null;
            cachedEnergyStore = null;
        }
        return valid;
    }

    private @Nullable UUID effectiveOutputOwner(ServerLevel level) {
        if (!xianqiaoOutputEnabled) return null;
        return boundOwner(level);
    }

    private @Nullable UUID boundOwner(ServerLevel level) {
        XianqiaoBindingPolicy.Binding binding = XianqiaoBindingPolicy.resolveEnergyCrystal(
                level, items.get(FUEL_SLOT));
        UUID boundOwner = binding.isBound() ? binding.owner() : null;
        return boundOwner != null
                && PersistentPlayerIdentity.onlinePlayer(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level), boundOwner) != null
                ? boundOwner : null;
    }

    private void refreshBinding(ServerLevel level) {
        UUID nextOwner = boundOwner(level);
        boolean ownerChanged = nextOwner == null ? owner != null : !nextOwner.equals(owner);
        if (!ownerChanged && !xianqiaoOutputEnabled) return;
        UUID previousOwner = owner;
        boolean enabled = xianqiaoOutputEnabled;
        boolean ready = enabled;
        boolean cacheChanged = false;
        if (enabled && previousOwner != null && ownerChanged) {
            long before = energy;
            flushOutputCacheToXianqiao(level, previousOwner);
            ready = energy == 0L;
            cacheChanged = before != energy;
        }
        owner = nextOwner;
        if (enabled) {
            if (nextOwner == null || ownerExternalEnergy(level, nextOwner) == null) {
                ready = false;
            } else if (ready) {
                long before = energy;
                flushOutputCacheToXianqiao(level, nextOwner);
                ready = energy == 0L;
                cacheChanged |= before != energy;
            }
            xianqiaoOutputEnabled = ready;
        }
        if (!ownerChanged && xianqiaoOutputEnabled && !cacheChanged) return;
        setChangedAndSync();
    }

    /** Flushes the local cache before a switch-off or block removal. */
    public void flushBoundCache() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        refreshBinding(serverLevel);
        if (xianqiaoOutputEnabled) flushOutputCacheToXianqiao(serverLevel, owner);
    }

    private @Nullable AtomicEnergyRefill.ResourceStore boundExternalEnergy() {
        if (!(level instanceof ServerLevel serverLevel) || !xianqiaoOutputEnabled) return null;
        return ownerExternalEnergy(serverLevel);
    }

    private long displayedEnergy() {
        AtomicEnergyRefill.ResourceStore external = boundExternalEnergy();
        return external == null ? energy : external.amount();
    }

    private long flushEnergyToXianqiao(ServerLevel level, @Nullable UUID targetOwner) {
        if (energy <= 0L || targetOwner == null) return 0L;
        AtomicEnergyRefill.ResourceStore target = ownerExternalEnergy(level, targetOwner);
        if (target == null) return 0L;
        long moved = 0L;
        while (energy > 0L) {
            long accepted = XianqiaoInterfaceEnergyTransfer.pushAll(localEnergyStore, target);
            if (accepted <= 0L) break;
            moved += accepted;
        }
        return moved;
    }

    public static long configuredOutput() {
        return configuredOutput(CrystalKind.ELECTRIC);
    }

    public static long configuredOutput(CrystalKind kind) {
        // The three variants intentionally share one configurable production
        // contract.  Only the resource channel differs.
        return Math.max(1L, com.immortalstorage.immortalstorage.config.ImmortalStorageConfig
                .ENERGY_CRYSTAL_FE_PER_TICK.get());
    }

    public static long configuredCapacity() {
        return configuredCapacity(CrystalKind.ELECTRIC);
    }

    public static long configuredCapacity(CrystalKind kind) {
        // Keep storage capacity identical across FE, mana, and Source.
        return Math.max(1L, com.immortalstorage.immortalstorage.config.ImmortalStorageConfig
                .ENERGY_CRYSTAL_FE_CAPACITY.get());
    }

    private long capacity() { return configuredCapacity(kind); }

    private static long saturatedAdd(long left, long right) {
        return right <= 0L ? left
                : left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static int saturatingInt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private void updateLit(ServerLevel level, boolean lit) {
        if (getBlockState().getValue(EnergyCrystalBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(EnergyCrystalBlock.LIT, lit), 3);
        }
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

    private void markChangedForTick() {
        if (level instanceof ServerLevel serverLevel) setChangedAndMaybeSync(serverLevel);
        else setChanged();
    }

    /** Direct machine processing writes the output slot outside Slot#setByPlayer. */
    private void broadcastOpenMenu(ServerLevel serverLevel) {
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == serverLevel
                    && player.containerMenu instanceof EnergyCrystalMenu menu
                    && menu.blockPos().equals(worldPosition)) {
                menu.broadcastChanges();
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable(kind.containerTranslationKey());
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnergyCrystalMenu(id, inventory, this);
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        if (slot == FUEL_SLOT && level instanceof ServerLevel serverLevel) refreshBinding(serverLevel);
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (slot == FUEL_SLOT && level instanceof ServerLevel serverLevel) refreshBinding(serverLevel);
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        stack.limitSize(getMaxStackSize(stack));
        items.set(slot, stack);
        setChanged();
        if (slot == FUEL_SLOT && level instanceof ServerLevel serverLevel) refreshBinding(serverLevel);
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() {
        items.clear();
        setChanged();
        if (level instanceof ServerLevel serverLevel) refreshBinding(serverLevel);
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == EXTRA_SLOT) return ReinforcementPluginHost.isPlugin(stack)
                && (items.get(EXTRA_SLOT).isEmpty() || hasReinforcementPlugin());
        if (slot == FUEL_SLOT) return stack.is(ModItems.TRUE_YUAN.get())
                || stack.is(ModItems.IMMORTAL_YUAN.get()) || stack.getItem() instanceof SpiritDriveItem;
        return slot == INPUT_SLOT && (kind == CrystalKind.ELECTRIC
                ? EnergyCrystalItemAccess.energy(stack) != null
                        || !hasReinforcementPlugin() && chargedQuartzOutput(stack) != null
                : CrystalResourceCompatHooks.acceptsInput(kind, stack));
    }
    @Override public int[] getSlotsForFace(Direction side) {
        if (side == null) return PROCESSING_AND_EXTRA;
        if (side == Direction.DOWN) {
            return automaticOutputEnabled && outputFace(side) ? EXTRA_OUTPUT : EMPTY;
        }
        return side == Direction.UP ? TOP_PROCESSING_INPUT : SIDE_FUEL_INPUT;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (side == null || side == Direction.DOWN) return false;
        if (side == Direction.UP) return slot == INPUT_SLOT && canPlaceItem(slot, stack);
        return slot == FUEL_SLOT && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return automaticOutputEnabled && side == Direction.DOWN
                && outputFace(side) && slot == EXTRA_SLOT
                && !ReinforcementPluginHost.isPlugin(stack);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        int index = side == null ? Direction.values().length : side.ordinal();
        IItemHandler handler = itemHandlers[index];
        if (handler == null) itemHandlers[index] = handler = side == null
                ? new InvWrapper(this) : new SidedInvWrapper(this, side);
        return handler;
    }

    @Override protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditionalLegacy(tag, registries);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.saveItems(tag, items, registries);
        tag.putString("CrystalKind", kind.registryPath());
        tag.putLong("Energy", energy);
        tag.putInt("ProcessTicks", processTicks);
        tag.putInt("BurnTicks", burnTicks);
        tag.putInt("BurnDuration", burnDuration);
        tag.putBoolean("XianqiaoOutput", xianqiaoOutputEnabled);
        tag.putBoolean("AutomaticFaceOutput", automaticOutputEnabled);
        tag.putBoolean("SparkAttached", sparkAttached);
        tag.putBoolean("SourceStart", sourceStart);
        tag.putBoolean("SourcePriority", sourcePriority);
        if (owner != null) com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.putUuid(tag, "Owner", owner);
        int[] faces = new int[outputFaces.length];
        for (Direction side : Direction.values()) faces[side.ordinal()] = outputFaces[side.ordinal()] ? 1 : 0;
        tag.putIntArray("OutputFaces", faces);
    }

    @Override protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditionalLegacy(tag, registries);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo.loadItems(tag, items, registries);
        energy = Math.max(0L, Math.min(capacity(), tag.getLongOr("Energy", 0L)));
        processTicks = Math.max(0, tag.getIntOr("ProcessTicks", 0));
        burnTicks = Math.max(0, tag.getIntOr("BurnTicks", 0));
        burnDuration = Math.max(0, tag.getIntOr("BurnDuration", 0));
        xianqiaoOutputEnabled = tag.contains("XianqiaoOutput")
                ? tag.getBooleanOr("XianqiaoOutput", false)
                : tag.contains("AutomaticXianqiaoOutput") && tag.getBooleanOr("AutomaticXianqiaoOutput", false);
        automaticOutputEnabled = !tag.contains("AutomaticFaceOutput")
                || tag.getBooleanOr("AutomaticFaceOutput", false);
        sparkAttached = tag.getBooleanOr("SparkAttached", false);
        sourceStart = tag.getBooleanOr("SourceStart", false);
        sourcePriority = tag.getBooleanOr("SourcePriority", false);
        owner = com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.hasUuid(tag, "Owner") ? com.immortalstorage.immortalstorage.compat.mc2612.CompatNbt.getUuid(tag, "Owner") : null;
        Arrays.fill(outputFaces, false);
        int[] faces = tag.getIntArray("OutputFaces").orElseGet(() -> new int[0]);
        for (int i = 0; i < Math.min(faces.length, outputFaces.length); i++) outputFaces[i] = faces[i] != 0;
    }
}
