package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.core.amount.ResourceAmountPolicy;
import com.immortalstorage.immortalstorage.api.source.SourceEndpoint;
import com.immortalstorage.immortalstorage.api.source.SourceChargeContext;
import com.immortalstorage.immortalstorage.api.source.SourceChargePlan;
import com.immortalstorage.immortalstorage.api.source.SourceChargeRegistry;
import com.immortalstorage.immortalstorage.api.source.SourceChargeReservation;
import com.immortalstorage.immortalstorage.api.source.SourceBypassTransferRegistry;
import com.immortalstorage.immortalstorage.api.source.SourceBypassTransferTarget;
import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.immortalstorage.immortalstorage.config.ImmortalStorageConfig;
import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.menu.custom.SourceFluxValue;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinMenu;
import com.immortalstorage.immortalstorage.network.storage.SourceVeinStorageIndex;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.List;

/**
 * *    / Source Vein
 *        ?=                           2^31-1 ? *  ?tick                       +          ? *                       ? */
public class SourceVeinBlockEntity extends BlockEntity implements MenuProvider, SourceEndpoint {
    /** Native source cache ceiling. Int-only adapters saturate at their own boundary. */
    public static final long MAX_BUFFER = Long.MAX_VALUE;
    public static final long FLUX_STEP = 64L;
    public static final long MAX_FLUX_LIMIT = SourceFluxValue.MAX_VALUE;
    private static final int SIDE_MODES_VERSION = 1;
    private static final int FACE_FAULTS_VERSION = 1;
    private static final String DEFINITION_TAG = "SourceDefinitionId";
    private static final SourceDefinition UNCONFIGURED = new SourceDefinition(
            ResourceLocation.fromNamespaceAndPath("immortalstorage", "unconfigured"),
            SourceDefinition.OutputType.ITEM,
            ResourceLocation.fromNamespaceAndPath("minecraft", "air"),
            1L, 1L, 10, 0L, 0L, "", 0xFFFFFF, "", List.of(), null);

    private final VeinKind kind;
    private ResourceLocation explicitDefinitionId;
    private final SourceVeinBuffer buffer = new SourceVeinBuffer(MAX_BUFFER);
    private UUID owner = null;
    private long fluxLimit = 64;        // items per tick
    private final SourceSideMode[] sideModes = new SourceSideMode[Direction.values().length];
    private final IFluidHandler fluidHandler = new SourceFluidHandler(null);
    private final IItemHandler itemHandler = new SourceItemHandler(null);
    private final IFluidHandler[] sidedFluidHandlers = new IFluidHandler[Direction.values().length];
    private final IItemHandler[] sidedItemHandlers = new IItemHandler[Direction.values().length];
    private final SourceVeinFluxBudget fluxBudget = new SourceVeinFluxBudget();
    private final SourceVeinFluxBudget[] activeFaceFluxBudgets =
            new SourceVeinFluxBudget[Direction.values().length];
    private final boolean[] faceFaulted = new boolean[Direction.values().length];
    private final long[] faceUncertainInFlight = new long[Direction.values().length];
    private long observedDefinitionGeneration;

    public SourceVeinBlockEntity(BlockPos pos, BlockState state, VeinKind kind) {
        this(ModBlockEntities.SOURCE_VEIN.get(), pos, state, kind);
    }

    SourceVeinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, VeinKind kind) {
        super(type, pos, state);
        this.kind = kind;
        this.observedDefinitionGeneration = SourceDefinitions.generation();
        this.fluxLimit = defaultFluxLimit();
        for (Direction direction : Direction.values()) {
            activeFaceFluxBudgets[direction.ordinal()] = new SourceVeinFluxBudget();
            sidedItemHandlers[direction.ordinal()] = new SourceItemHandler(direction);
            sidedFluidHandlers[direction.ordinal()] = new SourceFluidHandler(direction);
        }
        resetSideModes(SourceSideMode.DISABLED);
        clearAllFaceFaults();
    }

    private long defaultFluxLimit() {
        SourceDefinition definition = definition();
        return Math.min(definition.maxRate(), definition.defaultRate());
    }

    public SourceDefinition definition() {
        return SourceDefinitions.find(sourceDefinitionId()).orElse(UNCONFIGURED);
    }

    public ResourceLocation sourceDefinitionId() {
        return explicitDefinitionId == null ? SourceDefinitions.legacyId(kind) : explicitDefinitionId;
    }

    public boolean setSourceDefinitionId(@Nullable ResourceLocation definitionId) {
        if (!genericDefinitionCarrier() || definitionId == null || SourceDefinitions.find(definitionId).isEmpty()) {
            return false;
        }
        ResourceLocation canonical = SourceDefinitions.find(definitionId).orElseThrow().id();
        if (canonical.equals(explicitDefinitionId)) return true;
        SourceVeinStorageIndex.unregister(this);
        explicitDefinitionId = canonical;
        fluxLimit = defaultFluxLimit();
        if (definition().free()) buffer.fillToCapacityWithoutCharge();
        setChanged();
        SourceVeinStorageIndex.register(this);
        return true;
    }

    private boolean genericDefinitionCarrier() {
        return getBlockState().getBlock() instanceof com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock block
                && block.isGenericDefinitionCarrier();
    }

    private boolean hasValidDefinition() {
        return SourceDefinitions.find(sourceDefinitionId()).isPresent();
    }

    public VeinKind getKind() { return kind; }
    @Override public VeinKind kind() { return kind; }
    @Override
    public Component getDisplayName() {
        String displayName = definition().displayName();
        return displayName.isBlank() ? getBlockState().getBlock().getName() : Component.translatable(displayName);
    }
    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SourceVeinMenu(id, inv, player, this);
    }
    public UUID getOwner() { return owner; }
    @Override public UUID owner() { return owner; }
    public void setOwner(UUID id) {
        if (java.util.Objects.equals(this.owner, id)) return;
        SourceVeinStorageIndex.unregister(this);
        this.owner = id;
        setChanged();
        if (level instanceof ServerLevel serverLevel) refillCacheForAutomation(serverLevel);
        SourceVeinStorageIndex.register(this);
    }
    public boolean isOwnedBy(UUID id) {
        return owner != null && owner.equals(id);
    }

    public boolean isUnowned() {
        return owner == null;
    }

    public boolean canPlayerClaim(Player player) {
        int stage = ImmortalStoragePlayerData.get(player).getStage();
        if (PersistentPlayerIdentity.matches(player, owner)) return stage >= definition().minStage();
        return canClaim(owner, PersistentPlayerIdentity.id(player), stage, definition().minStage(), ImmortalStorageConfig.SOURCE_ALLOW_OTHER_PLAYER_CLAIM.get());
    }

    public ClaimResult claimFor(Player player) {
        UUID actor = PersistentPlayerIdentity.id(player);
        boolean hadOwner = owner != null;
        if (!canPlayerClaim(player)) {
            int stage = ImmortalStoragePlayerData.get(player).getStage();
            return stage < definition().minStage() ? ClaimResult.STAGE_TOO_LOW : ClaimResult.DENIED;
        }
        if (!actor.equals(owner)) {
            setOwner(actor);
        }
        return hadOwner ? ClaimResult.CLAIMED_OTHER : ClaimResult.CLAIMED_UNOWNED;
    }

    public static boolean canClaim(@Nullable UUID currentOwner, UUID actor, int actorStage, int minStage, boolean allowOtherClaim) {
        if (actor == null) return false;
        if (actorStage < minStage) return false;
        if (currentOwner == null || currentOwner.equals(actor)) return true;
        return allowOtherClaim;
    }

    public enum ClaimResult {
        CLAIMED_UNOWNED,
        CLAIMED_OTHER,
        DENIED,
        STAGE_TOO_LOW
    }

    public enum SourceSideMode {
        DISABLED(0, "Off"),
        PUSH(2, "Push"),
        BYPASS_PUSH(3, "Bypass");

        private final int persistedId;
        private final String label;

        SourceSideMode(int persistedId, String label) {
            this.persistedId = persistedId;
            this.label = label;
        }

        public int persistedId() {
            return persistedId;
        }

        public String label() {
            return label;
        }

        public boolean pushes() {
            return this != DISABLED;
        }

        public boolean bypassesTargetLimit() {
            return this == BYPASS_PUSH;
        }

        public SourceSideMode next() {
            return switch (this) {
                case DISABLED -> PUSH;
                case PUSH -> BYPASS_PUSH;
                case BYPASS_PUSH -> DISABLED;
            };
        }

        /**
         * Decodes the stable persisted/menu id. Legacy id 1 represented
         * EXTRACT and intentionally fails closed during migration.
         */
        public static SourceSideMode byId(int id) {
            return switch (id) {
                case 2 -> PUSH;
                case 3 -> BYPASS_PUSH;
                default -> DISABLED;
            };
        }
    }

    public long getFluxLimit() { return fluxLimit; }
    @Override public long fluxLimit() { return fluxLimit; }
    public void setFluxLimit(long fluxLimit) {
        long clamped = clampFluxLimit(fluxLimit);
        if (this.fluxLimit == clamped) return;
        this.fluxLimit = clamped;
        setChanged();
    }

    public void adjustFluxLimit(int direction) {
        if (direction != -1 && direction != 1) return;
        long adjusted = direction > 0
                ? (fluxLimit > MAX_FLUX_LIMIT - FLUX_STEP ? MAX_FLUX_LIMIT : fluxLimit + FLUX_STEP)
                : Math.max(0L, fluxLimit - FLUX_STEP);
        setFluxLimit(adjusted);
    }

    private long clampFluxLimit(long fluxLimit) {
        return Math.min(definition().maxRate(), SourceFluxValue.clamp(fluxLimit));
    }
    public boolean isActiveOutput() { return hasPushSide(); }
    @Override public boolean activeOutput() { return hasPushSide(); }
    public void setActiveOutput(boolean a) {
        clearAllFaceFaults();
        resetSideModes(a ? SourceSideMode.PUSH : SourceSideMode.DISABLED);
        markSideModesChanged();
    }
    public long outputCostPerTick() { return chargePlan().requiredUnits(fluxLimit); }

    @Override
    public SourceChargePlan chargePlan() {
        return new SourceChargePlan(SourceChargeRegistry.IMMORTAL_YUAN,
                definition().yuanCostPerBatch(), definition().outputsPerBatch());
    }

    public SourceSideMode getSideMode(@Nullable Direction side) {
        if (side == null) {
            for (Direction direction : Direction.values()) {
                if (sideModes[direction.ordinal()] == SourceSideMode.BYPASS_PUSH) {
                    return SourceSideMode.BYPASS_PUSH;
                }
            }
            return hasPushSide() ? SourceSideMode.PUSH : SourceSideMode.DISABLED;
        }
        SourceSideMode mode = sideModes[side.ordinal()];
        return mode == null ? SourceSideMode.DISABLED : mode;
    }

    public void setSideMode(Direction side, SourceSideMode mode) {
        if (side == null || mode == null) return;
        if (sideModes[side.ordinal()] == mode) return;
        clearFaceFault(side);
        sideModes[side.ordinal()] = mode;
        markSideModesChanged();
    }

    public boolean isFaceFaulted(@Nullable Direction side) {
        return side != null && faceFaulted[side.ordinal()];
    }

    public long uncertainInFlight(@Nullable Direction side) {
        return side == null ? 0L : faceUncertainInFlight[side.ordinal()];
    }

    private void clearFaceFault(Direction side) {
        int index = java.util.Objects.requireNonNull(side, "side").ordinal();
        if (!faceFaulted[index] && faceUncertainInFlight[index] == 0L) return;
        faceFaulted[index] = false;
        faceUncertainInFlight[index] = 0L;
        setChanged();
    }

    private void clearAllFaceFaults() {
        java.util.Arrays.fill(faceFaulted, false);
        java.util.Arrays.fill(faceUncertainInFlight, 0L);
    }

    private void faultFace(Direction side, long uncertainAmount, String operation, RuntimeException error) {
        int index = java.util.Objects.requireNonNull(side, "side").ordinal();
        faceFaulted[index] = true;
        faceUncertainInFlight[index] = Math.max(0L, uncertainAmount);
        setChanged();
        com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.error(
                "Source could not {} target at {} face {}; this face is frozen with {} units in an uncertain state",
                operation, worldPosition, side, faceUncertainInFlight[index], error);
    }

    private void logSimulationFailure(String operation, Direction side, RuntimeException error) {
        com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.warn(
                "Source could not {} target at {} face {}; skipping this face without a persistent fault",
                operation, worldPosition, side, error);
    }

    public SourceSideMode cycleSideMode(Direction side) {
        SourceSideMode next = getSideMode(side).next();
        setSideMode(side, next);
        return next;
    }

    /** Passive standard-capability extraction is available on every queried face. */
    public boolean canExtractFrom(@Nullable Direction side) {
        return true;
    }

    public boolean canPushTo(Direction side) {
        return side != null && getSideMode(side).pushes();
    }

    private boolean hasPushSide() {
        for (Direction dir : Direction.values()) {
            if (canPushTo(dir)) return true;
        }
        return false;
    }

    private void resetSideModes(SourceSideMode mode) {
        for (Direction dir : Direction.values()) {
            sideModes[dir.ordinal()] = mode;
        }
    }

    private void markSideModesChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static long outputCostPerTick(VeinKind kind, long fluxLimit) {
        if (kind == null) return 0;
        return new SourceChargePlan(SourceChargeRegistry.IMMORTAL_YUAN,
                Math.max(0L, kind.yuanCostPerBatch), Math.max(1, kind.outputsPerBatch)).requiredUnits(fluxLimit);
    }

    public ItemStack sampleOutput() {
        return itemSample(1);
    }

    @Override
    public ItemStack itemSample(int count) {
        SourceDefinition definition = definition();
        if (definition.fluid() || count <= 0) return ItemStack.EMPTY;
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(definition.outputId());
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public static ItemStack kindToStack(VeinKind k, int count) {
        if (k.fluid) return ItemStack.EMPTY;
        Item i = switch (k) {
            case COBBLE -> Items.COBBLESTONE;
            case STONE -> Items.STONE;
            case SMOOTH_STONE -> Items.SMOOTH_STONE;
            case WHITE_CONCRETE -> Items.WHITE_CONCRETE;
            case ORANGE_CONCRETE -> Items.ORANGE_CONCRETE;
            case MAGENTA_CONCRETE -> Items.MAGENTA_CONCRETE;
            case LIGHT_BLUE_CONCRETE -> Items.LIGHT_BLUE_CONCRETE;
            case YELLOW_CONCRETE -> Items.YELLOW_CONCRETE;
            case LIME_CONCRETE -> Items.LIME_CONCRETE;
            case PINK_CONCRETE -> Items.PINK_CONCRETE;
            case GRAY_CONCRETE -> Items.GRAY_CONCRETE;
            case LIGHT_GRAY_CONCRETE -> Items.LIGHT_GRAY_CONCRETE;
            case CYAN_CONCRETE -> Items.CYAN_CONCRETE;
            case PURPLE_CONCRETE -> Items.PURPLE_CONCRETE;
            case BLUE_CONCRETE -> Items.BLUE_CONCRETE;
            case BROWN_CONCRETE -> Items.BROWN_CONCRETE;
            case GREEN_CONCRETE -> Items.GREEN_CONCRETE;
            case RED_CONCRETE -> Items.RED_CONCRETE;
            case BLACK_CONCRETE -> Items.BLACK_CONCRETE;
            case DIRT -> Items.DIRT;
            case OAK_LOG -> Items.OAK_LOG;
            case COAL -> Items.COAL;
            case RAW_COPPER -> Items.RAW_COPPER;
            case RAW_IRON -> Items.RAW_IRON;
            case RAW_GOLD -> Items.RAW_GOLD;
            case LAPIS -> Items.LAPIS_LAZULI;
            case REDSTONE -> Items.REDSTONE;
            case CRUDE_SPIRIT_IRON -> com.immortalstorage.immortalstorage.item.ModItems.CRUDE_SPIRIT_IRON.get();
            case SPIRIT_CRYSTAL -> com.immortalstorage.immortalstorage.item.ModItems.SPIRIT_CRYSTAL.get();
            case DIAMOND -> Items.DIAMOND;
            case EMERALD -> Items.EMERALD;
            case ECHO_SHARD -> Items.ECHO_SHARD;
            case ANCIENT_DEBRIS -> Items.ANCIENT_DEBRIS;
            case NETHER_STAR -> Items.NETHER_STAR;
            case ENCHANTED_GOLDEN_APPLE -> Items.ENCHANTED_GOLDEN_APPLE;
            case DRAGON_EGG -> Items.DRAGON_EGG;
            default -> null;
        };
        return i == null ? ItemStack.EMPTY : new ItemStack(i, count);
    }

    public Fluid sampleFluid() {
        SourceDefinition definition = definition();
        return definition.fluid()
                ? net.minecraft.core.registries.BuiltInRegistries.FLUID.get(definition.outputId())
                : Fluids.EMPTY;
    }

    public ItemStack filledVanillaContainer() {
        Fluid fluid = sampleFluid();
        if (fluid == Fluids.WATER) return new ItemStack(Items.WATER_BUCKET);
        if (fluid == Fluids.LAVA) return new ItemStack(Items.LAVA_BUCKET);
        if (fluid == NeoForgeMod.MILK.value()) return new ItemStack(Items.MILK_BUCKET);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fluidSource() {
        return definition().fluid();
    }

    @Override
    public Fluid fluid() {
        return sampleFluid();
    }

    public IFluidHandler getFluidHandler() {
        return fluidSource() ? fluidHandler : null;
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (!fluidSource()) return null;
        return side == null ? fluidHandler : sidedFluidHandlers[side.ordinal()];
    }

    public IItemHandler getItemHandler() {
        return fluidSource() ? null : itemHandler;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (fluidSource()) return null;
        return side == null ? itemHandler : sidedItemHandlers[side.ordinal()];
    }

    @Override
    public IFluidHandler fluidHandler() {
        return getFluidHandler();
    }

    public long cachedUnits() {
        return amountPolicy().reportedLong(buffer.available());
    }

    /** Stable loaded-source identity; no amount is encoded in the id. */
    public String storageIndexId() {
        String dimension = level instanceof ServerLevel serverLevel
                ? serverLevel.dimension().location().toString() : "unbound";
        return dimension + ":" + worldPosition.asLong();
    }

    /** Native long directory amount; int-only capabilities saturate separately. */
    public long storageVisibleUnits() {
        return buffer.available();
    }

    /** Exact UUID-realm/stage/loaded-state boundary for Xianqiao catalog exposure. */
    public boolean isVisibleInXianqiaoStorage(UUID requestedOwner) {
        if (requestedOwner == null || owner == null || !owner.equals(requestedOwner)
                || isRemoved() || !(level instanceof ServerLevel serverLevel)) return false;
        if (!ImmortalStorageDimensions.isPersonalRealmFor(serverLevel.dimension(), owner)) return false;
        var player = PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
        if (player == null) return false;
        int stage = ImmortalStoragePlayerData.get(player).getStage();
        return stage >= Math.max(6, definition().minStage());
    }

    /** Shared terminal/AE2/RS extraction path over the same cache and flux budget. */
    public long extractForXianqiaoStorage(UUID requestedOwner, long requested, boolean simulate) {
        if (requested <= 0L || !isVisibleInXianqiaoStorage(requestedOwner)) return 0L;
        return extractWithinFlux(requested, simulate);
    }

    private ResourceAmountPolicy amountPolicy() {
        return chargePlan().isFree() ? ResourceAmountPolicy.UNCHANGED : ResourceAmountPolicy.CONSUMED;
    }

    private long logicalAvailableUnits() {
        return amountPolicy().reportedLong(buffer.available());
    }

    private int compatibilityAvailableUnits() {
        return amountPolicy().reportedInt(buffer.available());
    }

    private long currentGameTick() {
        return level == null ? 0L : level.getGameTime();
    }

    private long availableFluxBudget() {
        if (amountPolicy() == ResourceAmountPolicy.UNCHANGED) {
            return ResourceAmountPolicy.UNCHANGED.longPerTickCeiling(buffer.available());
        }
        return fluxBudget.available(currentGameTick(), fluxLimit);
    }

    /** Every PUSH face owns the configured rate independently, including creative sources. */
    private long availableActiveOutputBudget(Direction direction) {
        return activeFaceFluxBudget(direction).available(currentGameTick(), fluxLimit);
    }

    /** Reserves tick allowance and cache together, refunding unused allowance. */
    private long extractWithinFlux(long requested, boolean simulate) {
        ResourceAmountPolicy policy = amountPolicy();
        if (policy == ResourceAmountPolicy.UNCHANGED) {
            return policy.extractable(buffer.available(), requested);
        }
        long tick = currentGameTick();
        long granted = fluxBudget.claim(tick, fluxLimit, requested, simulate);
        long extracted = policy.extractable(buffer.available(), granted);
        if (!simulate) {
            extracted = buffer.extract(extracted, false);
            if (extracted > 0L) markCacheChanged();
        }
        if (!simulate && extracted < granted) {
            fluxBudget.refund(tick, granted - extracted);
        }
        return extracted;
    }

    /**
     * Reserves one face-local PUSH allowance. Free sources retain their backing
     * amount, while paid sources also consume the persistent prepaid cache
     * through the same transaction.
     */
    long extractForActiveOutput(Direction direction, long requested, boolean simulate) {
        ResourceAmountPolicy policy = amountPolicy();
        long tick = currentGameTick();
        SourceVeinFluxBudget faceBudget = activeFaceFluxBudget(direction);
        long granted = faceBudget.claim(tick, fluxLimit, requested, simulate);
        long extracted = policy.extractable(buffer.available(), granted);
        if (!simulate && policy == ResourceAmountPolicy.CONSUMED) {
            extracted = buffer.extract(extracted, false);
            if (extracted > 0L) markCacheChanged();
        }
        if (!simulate && extracted < granted) {
            faceBudget.refund(tick, granted - extracted);
        }
        return extracted;
    }

    private SourceVeinFluxBudget activeFaceFluxBudget(Direction direction) {
        return activeFaceFluxBudgets[java.util.Objects.requireNonNull(direction, "direction").ordinal()];
    }

    /** Rolls back an exact-container commit that unexpectedly delivered only a partial amount. */
    public void rollbackFluidExtraction(long extractedUnits) {
        if (!fluidSource() || extractedUnits <= 0L) return;
        if (amountPolicy() == ResourceAmountPolicy.UNCHANGED) {
            return;
        }
        long restored = buffer.restore(extractedUnits);
        fluxBudget.refund(currentGameTick(), restored);
        if (restored > 0L) markCacheChanged();
    }

    /**
     * Shift-use withdraws exactly one complete item production cycle. Paid
     * cycles enter the persistent cache only after their charge is secured.
     */
    public boolean takeManualBatch(Player player) {
        if (player == null || fluidSource()) return false;
        ItemStack sample = sampleOutput();
        if (sample.isEmpty()) return false;
        SourceDefinition definition = definition();
        int batch = definition.free() ? sample.getMaxStackSize()
                : (int) Math.min(sample.getMaxStackSize(), definition.outputsPerBatch());
        if (batch <= 0) return false;
        if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
            if (buffer.available() < batch && !replenishOneCycle(batch)) return false;
            if (buffer.extract(batch, false) != batch) return false;
            markCacheChanged();
        }

        ItemStack output = sample.copyWithCount(batch);
        player.getInventory().add(output);
        if (!output.isEmpty()) player.drop(output, false);
        return true;
    }

    private void refillCacheForAutomation(ServerLevel serverLevel) {
        if (!hasValidDefinition()) return;
        SourceChargePlan plan = chargePlan();
        if (plan.isFree()) {
            if (buffer.fillToCapacityWithoutCharge() > 0L) markCacheChanged();
            return;
        }

        var ownerPlayer = PersistentPlayerIdentity.onlinePlayer(serverLevel.getServer(), owner);
        if (ownerPlayer == null) return;
        long currencyCap;
        long spendableUnits;
        synchronized (ownerPlayer) {
            ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(ownerPlayer);
            currencyCap = data.getImmortalYuanCapLong();
            spendableUnits = data.getImmortalYuan();
        }
        long target = SourceVeinBuffer.targetForChargeCapacity(currencyCap,
                plan.unitsPerBatch(), plan.outputsPerBatch(), MAX_BUFFER);
        long produced = SourceVeinBuffer.affordableRefill(target, buffer.available(), spendableUnits,
                plan.unitsPerBatch(), plan.outputsPerBatch());
        if (produced > 0L) replenishOneCycle(produced);
    }

    private boolean replenishOneCycle(long producedUnits) {
        boolean added = buffer.addCycle(producedUnits, () -> secureCycleCharge(producedUnits));
        if (added) markCacheChanged();
        return added;
    }

    private void markCacheChanged() {
        setChanged();
        SourceVeinStorageIndex.changed(this);
    }

    private boolean secureCycleCharge(long producedUnits) {
        SourceChargePlan plan = chargePlan();
        if (plan.isFree()) return true;
        SourceChargeReservation reservation = reserveOwnerCharge(producedUnits);
        if (reservation == null) return false;
        if (reservation.commit(producedUnits)) return true;
        reservation.cancel();
        return false;
    }

    /** Replenishes the persistent cache, then optionally pushes cached units. */
    public void serverTick() {
        if (this.level == null || this.level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;
        long definitionGeneration = SourceDefinitions.generation();
        if (observedDefinitionGeneration != definitionGeneration) {
            observedDefinitionGeneration = definitionGeneration;
            fluxLimit = clampFluxLimit(fluxLimit);
            if (definition().free()) buffer.fillToCapacityWithoutCharge();
            setChanged();
            SourceVeinStorageIndex.rebindDefinition(this);
        }
        if (owner == null || !hasValidDefinition()) return;
        refillCacheForAutomation(sl);
        if (!hasPushSide() || logicalAvailableUnits() <= 0L) return;
        if (fluidSource()) {
            tryPushFluid(sl);
            return;
        }
        tryPushItems(sl);
    }

    private void tryPushItems(ServerLevel sl) {
        ItemStack sample = sampleOutput();
        if (sample.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (!canPushTo(dir) || isFaceFaulted(dir)) continue;
            if (getSideMode(dir).bypassesTargetLimit()) {
                SourceBypassTransferTarget nativeTarget = findNativeBypassTarget(sl, dir);
                if (nativeTarget != null && nativeTarget.supportsItems()) {
                    pushItemsToNativeBypassTarget(dir, nativeTarget, sample);
                    continue;
                }
            }
            IItemHandler handler = sl.getCapability(Capabilities.ItemHandler.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) pushItemsToHandler(dir, handler, sample);
        }
    }

    private SourceBypassTransferTarget findNativeBypassTarget(ServerLevel level, Direction direction) {
        try {
            return SourceBypassTransferRegistry.find(
                    level, worldPosition.relative(direction), direction.getOpposite());
        } catch (RuntimeException error) {
            com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.warn(
                    "Native source bypass target lookup failed at {} face {}; using the standard capability",
                    worldPosition, direction, error);
            return null;
        }
    }

    /** Resolves and processes every configured PUSH face in one independent-budget pass. */
    void pushItemsToTargets(java.util.function.Function<Direction, IItemHandler> targetResolver) {
        ItemStack sample = sampleOutput();
        if (sample.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (!canPushTo(dir) || isFaceFaulted(dir)) continue;
            IItemHandler handler = targetResolver.apply(dir);
            if (handler == null) continue;
            pushItemsToHandler(dir, handler, sample);
        }
    }

    /** One face-local item transfer pass, exposed package-private for behavior tests. */
    void pushItemsToHandler(Direction dir, IItemHandler handler, ItemStack sample) {
        SourceSideMode mode = getSideMode(dir);
        if (!mode.pushes() || isFaceFaulted(dir)) return;
        if (mode.bypassesTargetLimit()) {
            if (handler instanceof BulkItemInsertTarget bulkTarget) {
                pushItemsToBulkTarget(dir, bulkTarget, sample);
            } else {
                pushItemsToBypassHandler(dir, handler, sample);
            }
            return;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            int offered = (int) Math.min(Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits()),
                    sample.getMaxStackSize());
            // This face has spent its own allowance. Continue with the next
            // face instead of aborting the six-face PUSH pass.
            if (offered <= 0) break;
            ItemStack offer = sample.copyWithCount(offered);
            ItemStack simulatedRemainder;
            try {
                simulatedRemainder = handler.insertItem(i, offer, true);
            } catch (RuntimeException error) {
                com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.warn(
                        "Source item target rejected simulation at {} face {}; skipping this slot",
                        worldPosition, dir, error);
                continue;
            }
            if (simulatedRemainder == null) continue;
            int accepted = offered - Math.min(offered, simulatedRemainder.getCount());
            if (accepted <= 0) continue;
            int staged = (int) extractForActiveOutput(dir, accepted, false);
            if (staged <= 0) break;
            setChanged();
            try {
                ItemStack remainder = handler.insertItem(i, sample.copyWithCount(staged), false);
                if (remainder == null) {
                    throw new IllegalStateException("target returned a null remainder");
                }
                int refused = remainder.isEmpty() ? 0 : Math.min(staged, remainder.getCount());
                if (refused > 0) {
                    if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
                        long restored = buffer.restore(refused);
                        if (restored > 0L) markCacheChanged();
                    }
                    activeFaceFluxBudget(dir).refund(currentGameTick(), refused);
                }
            } catch (RuntimeException error) {
                faultFace(dir, staged, "execute an item transfer for", error);
                return;
            }
        }
    }

    /**
     * Deliberate compatibility path for condenser-style handlers that accept
     * an int-sized ItemStack count despite the item's normal stack limit.
     */
    private void pushItemsToBypassHandler(Direction dir, IItemHandler handler, ItemStack sample) {
        int offered = (int) Math.min(Integer.MAX_VALUE,
                Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits()));
        if (offered <= 0) return;

        final int slots;
        try {
            slots = handler.getSlots();
        } catch (RuntimeException error) {
            logSimulationFailure("query slots from", dir, error);
            return;
        }

        for (int slot = 0; slot < slots; slot++) {
            final ItemStack simulatedRemainder;
            try {
                simulatedRemainder = handler.insertItem(slot, sample.copyWithCount(offered), true);
            } catch (RuntimeException error) {
                logSimulationFailure("simulate an overstack for", dir, error);
                return;
            }
            if (simulatedRemainder == null) {
                logSimulationFailure("receive a simulation result from", dir,
                        new IllegalStateException("target returned a null remainder"));
                return;
            }

            int accepted = offered - Math.min(offered, Math.max(0, simulatedRemainder.getCount()));
            if (accepted <= 0) continue;
            int staged = (int) extractForActiveOutput(dir, accepted, false);
            if (staged <= 0) return;
            setChanged();

            try {
                ItemStack remainder = handler.insertItem(slot, sample.copyWithCount(staged), false);
                if (remainder == null) {
                    throw new IllegalStateException("target returned a null remainder");
                }
                int refused = remainder.isEmpty() ? 0 : Math.min(staged, Math.max(0, remainder.getCount()));
                refundActiveItemOutput(dir, refused);
                setChanged();
            } catch (RuntimeException error) {
                faultFace(dir, staged, "execute an overstack for", error);
            }
            return;
        }
    }

    private void refundActiveItemOutput(Direction dir, long refused) {
        if (refused <= 0L) return;
        if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
            long restored = buffer.restore(refused);
            if (restored > 0L) markCacheChanged();
        }
        activeFaceFluxBudget(dir).refund(currentGameTick(), refused);
    }

    private void pushItemsToNativeBypassTarget(Direction dir, SourceBypassTransferTarget target,
                                               ItemStack sample) {
        long offered = Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits());
        if (offered <= 0L) return;
        final long accepted;
        try {
            accepted = validateNativeAccepted(target.insertItem(sample, offered, true), offered);
        } catch (RuntimeException error) {
            logSimulationFailure("simulate a native bypass transfer for", dir, error);
            return;
        }
        if (accepted <= 0L) return;
        long staged = extractForActiveOutput(dir, accepted, false);
        if (staged <= 0L) return;
        setChanged();
        try {
            long committed = validateNativeAccepted(target.insertItem(sample, staged, false), staged);
            refundActiveItemOutput(dir, staged - committed);
            setChanged();
        } catch (RuntimeException error) {
            faultFace(dir, staged, "execute a native item bypass transfer for", error);
        }
    }

    private static long validateNativeAccepted(long accepted, long requested) {
        if (accepted < 0L || accepted > requested) {
            throw new IllegalStateException(
                    "native bypass target accepted " + accepted + " from request " + requested);
        }
        return accepted;
    }

    private void pushItemsToBulkTarget(Direction dir, BulkItemInsertTarget target, ItemStack sample) {
        long offered = Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits());
        if (offered <= 0L) return;
        final long accepted;
        try {
            accepted = validateNativeAccepted(target.insertBulk(sample, offered, true), offered);
        } catch (RuntimeException error) {
            logSimulationFailure("simulate a bulk item transfer for", dir, error);
            return;
        }
        if (accepted <= 0L) return;
        long staged = extractForActiveOutput(dir, accepted, false);
        if (staged <= 0L) return;
        try {
            long committed = validateNativeAccepted(target.insertBulk(sample, staged, false), staged);
            refundActiveItemOutput(dir, staged - committed);
            setChanged();
        } catch (RuntimeException error) {
            faultFace(dir, staged, "execute a bulk item transfer for", error);
        }
    }

    private void tryPushFluid(ServerLevel sl) {
        Fluid fluid = sampleFluid();
        if (fluid == Fluids.EMPTY) return;
        for (Direction dir : Direction.values()) {
            if (!canPushTo(dir) || isFaceFaulted(dir)) continue;
            if (getSideMode(dir).bypassesTargetLimit()) {
                SourceBypassTransferTarget nativeTarget = findNativeBypassTarget(sl, dir);
                if (nativeTarget != null && nativeTarget.supportsFluids()) {
                    pushFluidToNativeBypassTarget(dir, nativeTarget, fluid);
                    continue;
                }
            }
            IFluidHandler handler = sl.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(dir), dir.getOpposite());
            if (handler != null) pushFluidToHandler(dir, handler, fluid);
        }
    }

    /** Resolves and processes every configured fluid PUSH face in one independent-budget pass. */
    void pushFluidToTargets(java.util.function.Function<Direction, IFluidHandler> targetResolver) {
        Fluid f = sampleFluid();
        if (f == Fluids.EMPTY) return;
        for (Direction dir : Direction.values()) {
            if (!canPushTo(dir) || isFaceFaulted(dir)) continue;
            IFluidHandler handler = targetResolver.apply(dir);
            if (handler == null) continue;
            pushFluidToHandler(dir, handler, f);
        }
    }

    /** One face-local fluid transfer pass, exposed package-private for behavior tests. */
    void pushFluidToHandler(Direction dir, IFluidHandler handler, Fluid fluid) {
        if (!getSideMode(dir).pushes() || isFaceFaulted(dir)) return;
        int offered = (int) Math.min(Integer.MAX_VALUE,
                Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits()));
        if (offered <= 0) return;
        int accepted;
        try {
            accepted = Math.min(offered,
                    handler.fill(new FluidStack(fluid, offered), IFluidHandler.FluidAction.SIMULATE));
        } catch (RuntimeException error) {
            com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.warn(
                    "Source fluid target rejected simulation at {} face {}; skipping this face",
                    worldPosition, dir, error);
            return;
        }
        if (accepted <= 0) return;
        int staged = (int) extractForActiveOutput(dir, accepted, false);
        if (staged <= 0) return;
        setChanged();
        try {
            int filled = Math.max(0, Math.min(staged,
                    handler.fill(new FluidStack(fluid, staged), IFluidHandler.FluidAction.EXECUTE)));
            int refused = staged - filled;
            if (refused > 0) {
                if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
                    long restored = buffer.restore(refused);
                    if (restored > 0L) markCacheChanged();
                }
                activeFaceFluxBudget(dir).refund(currentGameTick(), refused);
            }
            setChanged();
        } catch (RuntimeException error) {
            faultFace(dir, staged, "execute a fluid transfer for", error);
        }
    }

    private void pushFluidToNativeBypassTarget(Direction dir, SourceBypassTransferTarget target,
                                               Fluid fluid) {
        long offered = Math.min(availableActiveOutputBudget(dir), logicalAvailableUnits());
        if (offered <= 0L) return;
        FluidStack prototype = new FluidStack(fluid, 1);
        final long accepted;
        try {
            accepted = validateNativeAccepted(target.insertFluid(prototype, offered, true), offered);
        } catch (RuntimeException error) {
            logSimulationFailure("simulate a native fluid bypass transfer for", dir, error);
            return;
        }
        if (accepted <= 0L) return;
        long staged = extractForActiveOutput(dir, accepted, false);
        if (staged <= 0L) return;
        setChanged();
        try {
            long committed = validateNativeAccepted(target.insertFluid(prototype, staged, false), staged);
            long refused = staged - committed;
            if (refused > 0L) {
                if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
                    long restored = buffer.restore(refused);
                    if (restored > 0L) markCacheChanged();
                }
                activeFaceFluxBudget(dir).refund(currentGameTick(), refused);
            }
            setChanged();
        } catch (RuntimeException error) {
            faultFace(dir, staged, "execute a native fluid bypass transfer for", error);
        }
    }

    private SourceChargeReservation reserveOwnerCharge(long outputCount) {
        if (!(level instanceof ServerLevel sl) || owner == null) return null;
        return SourceChargeRegistry.reserve(chargePlan(), new SourceChargeContext(sl, worldPosition, owner), outputCount);
    }

    private final class SourceItemHandler implements IItemHandler {
        private final Direction physicalSide;

        private SourceItemHandler(@Nullable Direction physicalSide) {
            this.physicalSide = physicalSide;
        }

        @Override
        public int getSlots() {
            return fluidSource() ? 0 : 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot != 0 || fluidSource()) return ItemStack.EMPTY;
            ItemStack sample = sampleOutput();
            int visible = compatibilityAvailableUnits();
            if (sample.isEmpty() || visible <= 0) return ItemStack.EMPTY;
            if (amountPolicy() == ResourceAmountPolicy.CONSUMED) {
                visible = Math.min(visible, sample.getMaxStackSize());
            }
            sample.setCount(visible);
            return sample;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0 || fluidSource()) return ItemStack.EMPTY;
            ItemStack sample = sampleOutput();
            if (sample.isEmpty()) return ItemStack.EMPTY;
            int count = Math.min(amount, sample.getMaxStackSize());
            count = (int) (physicalSide == null
                    ? extractWithinFlux(count, simulate)
                    : extractForActiveOutput(physicalSide, count, simulate));
            if (count <= 0) return ItemStack.EMPTY;
            if (!simulate) setChanged();
            sample.setCount(count);
            return sample;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot != 0 || fluidSource()) return 0;
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    }

    private final class SourceFluidHandler implements IFluidHandler {
        private final Direction physicalSide;

        private SourceFluidHandler(@Nullable Direction physicalSide) {
            this.physicalSide = physicalSide;
        }

        @Override
        public int getTanks() {
            return fluidSource() ? 1 : 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            Fluid fluid = sampleFluid();
            int available = compatibilityAvailableUnits();
            return tank == 0 && fluid != Fluids.EMPTY && available > 0
                    ? new FluidStack(fluid, available)
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 && fluidSource() ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && fluidSource() && stack != null && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!fluidSource() || resource == null || resource.isEmpty()) return 0;
            // Fluid sources expose an unconditional six-face void inlet. It is
            // deliberately independent from output modes, budgets and caches.
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Fluid fluid = sampleFluid();
            if (fluid == Fluids.EMPTY || resource.isEmpty() || resource.getFluid() != fluid) {
                return FluidStack.EMPTY;
            }
            int amount = (int) (physicalSide == null
                    ? extractWithinFlux(resource.getAmount(), action.simulate())
                    : extractForActiveOutput(physicalSide, resource.getAmount(), action.simulate()));
            if (amount <= 0) return FluidStack.EMPTY;
            if (action.execute()) setChanged();
            return new FluidStack(fluid, amount);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            Fluid fluid = sampleFluid();
            if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;
            int amount = (int) (physicalSide == null
                    ? extractWithinFlux(maxDrain, action.simulate())
                    : extractForActiveOutput(physicalSide, maxDrain, action.simulate()));
            if (amount <= 0) return FluidStack.EMPTY;
            if (action.execute()) setChanged();
            return new FluidStack(fluid, amount);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        tag.putString("Kind", kind.name());
        if (explicitDefinitionId != null) tag.putString(DEFINITION_TAG, explicitDefinitionId.toString());
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("FluxLimit", fluxLimit);
        buffer.save(tag);
        tag.putBoolean("ActiveOutput", hasPushSide());
        tag.putInt("SideModesVersion", SIDE_MODES_VERSION);
        int[] modes = new int[Direction.values().length];
        int[] faults = new int[Direction.values().length];
        long[] inFlight = new long[Direction.values().length];
        boolean anyFaulted = false;
        for (Direction dir : Direction.values()) {
            int index = dir.ordinal();
            modes[index] = getSideMode(dir).persistedId();
            faults[index] = isFaceFaulted(dir) ? 1 : 0;
            inFlight[index] = uncertainInFlight(dir);
            anyFaulted |= faults[index] != 0;
        }
        tag.putIntArray("SideModes", modes);
        tag.putInt("FaceFaultsVersion", FACE_FAULTS_VERSION);
        tag.putIntArray("FaceFaults", faults);
        tag.putLongArray("FaceInFlight", inFlight);
        // One-version downgrade adapter. New readers prefer the per-face data.
        tag.putBoolean("OutputFaulted", anyFaulted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        if (!acceptsPersistedKind(tag, kind)) {
            com.immortalstorage.immortalstorage.ImmortalStorageMod.LOG.warn(
                    "Rejected mismatched source-vein data at {}: expected {}, found {}",
                    worldPosition, kind.name(), tag.getString("Kind"));
            owner = null;
            fluxLimit = defaultFluxLimit();
            buffer.load(new CompoundTag());
            resetSideModes(SourceSideMode.DISABLED);
            clearAllFaceFaults();
            return;
        }
        explicitDefinitionId = null;
        if (genericDefinitionCarrier() && tag.contains(DEFINITION_TAG, Tag.TAG_STRING)) {
            explicitDefinitionId = ResourceLocation.tryParse(tag.getString(DEFINITION_TAG));
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        fluxLimit = defaultFluxLimit();
        if (tag.contains("FluxLimit", Tag.TAG_LONG)) {
            fluxLimit = clampFluxLimit(tag.getLong("FluxLimit"));
        }
        buffer.load(tag);
        if (chargePlan().isFree()) {
            // The persisted cache is authoritative. Free sources materialize
            // MAX here as well as during claim/onLoad so a storage bus never
            // observes a transient finite backing value after deserialization.
            buffer.fillToCapacityWithoutCharge();
        }
        resetSideModes(tag.getBoolean("ActiveOutput") ? SourceSideMode.PUSH : SourceSideMode.DISABLED);
        if (tag.contains("SideModes", Tag.TAG_INT_ARRAY)) {
            int[] modes = tag.getIntArray("SideModes");
            for (Direction dir : Direction.values()) {
                if (dir.ordinal() < modes.length) {
                    sideModes[dir.ordinal()] = SourceSideMode.byId(modes[dir.ordinal()]);
                }
            }
        }
        clearAllFaceFaults();
        boolean hasPerFaceFaults = tag.contains("FaceFaults", Tag.TAG_INT_ARRAY)
                || tag.contains("FaceInFlight", Tag.TAG_LONG_ARRAY);
        if (hasPerFaceFaults) {
            int[] faults = tag.getIntArray("FaceFaults");
            long[] inFlight = tag.getLongArray("FaceInFlight");
            for (Direction dir : Direction.values()) {
                int index = dir.ordinal();
                faceFaulted[index] = index < faults.length && faults[index] != 0;
                faceUncertainInFlight[index] = index < inFlight.length
                        ? Math.max(0L, inFlight[index]) : 0L;
                if (!faceFaulted[index]) faceUncertainInFlight[index] = 0L;
            }
        } else if (tag.getBoolean("OutputFaulted")) {
            // Legacy global failure had no committed-amount record. Preserve
            // the stop state on every face that could previously output.
            for (Direction dir : Direction.values()) {
                if (getSideMode(dir) != SourceSideMode.DISABLED) {
                    faceFaulted[dir.ordinal()] = true;
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        observedDefinitionGeneration = SourceDefinitions.generation();
        if (level instanceof ServerLevel serverLevel && owner != null) {
            refillCacheForAutomation(serverLevel);
        }
        SourceVeinStorageIndex.register(this);
    }

    @Override
    public void setRemoved() {
        SourceVeinStorageIndex.unregister(this);
        super.setRemoved();
    }

    static boolean acceptsPersistedKind(CompoundTag tag, VeinKind expected) {
        if (tag == null || expected == null) return false;
        if (!tag.contains("Kind")) return true;
        return tag.contains("Kind", Tag.TAG_STRING) && expected.name().equals(tag.getString("Kind"));
    }
}
