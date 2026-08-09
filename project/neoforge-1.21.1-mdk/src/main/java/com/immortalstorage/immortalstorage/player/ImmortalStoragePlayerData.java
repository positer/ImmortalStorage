package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.core.amount.ResourceAmountPolicy;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.LongResourceLedger;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.core.resource.ResourceTransferAction;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.dimension.RealmTimeScalePolicy;
import com.immortalstorage.immortalstorage.menu.custom.EmbeddedImmortalFurnaceBackend;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalVirtualEntry;
import com.immortalstorage.immortalstorage.player.yuan.YuanAccount;
import com.immortalstorage.immortalstorage.player.yuan.YuanGeneration;
import com.immortalstorage.immortalstorage.player.yuan.YuanKind;
import com.immortalstorage.immortalstorage.player.yuan.YuanItemPolicy;
import com.immortalstorage.immortalstorage.player.yuan.YuanProfile;
import com.immortalstorage.immortalstorage.player.yuan.YuanRule;
import com.immortalstorage.immortalstorage.progression.ImmortalStorageProgressionRules;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Per-player cultivation state.
 *
 * Stages 1..5 use the kongqiao inventory; stages 6..9 use xianqiao (storage)
 * and the personal realm; stage 10 is full ascension. True yuan / immortal yuan
 * are ordinary item stacks. Their cultivation cap is enforced against the live
 * total carried by the owner plus the currently active personal storage. Stage
 * ten is the sole exception: xianqiao exposes immortal yuan as a virtual,
 * non-materialized Long.MAX_VALUE entry while carried stacks stay ordinary.
 *
 * Storage is held as NonNullList (slot-indexed) so that AbstractContainerMenu
 * code can wrap it directly via CraftingContainer / SimpleContainer-style
 * helpers and so the "max slots" semantics map cleanly to menu grid layout.
 */
public final class ImmortalStoragePlayerData implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "player_data");
    private static final String EXTENDED_STACK_COUNT = "immortalstorageExtendedCount";
    private static final String DEFERRED_TRUE_YUAN_MATERIALIZATION = "deferredTrueYuanMaterialization";
    private static final String DEFERRED_IMMORTAL_YUAN_MATERIALIZATION = "deferredImmortalYuanMaterialization";
    private static final String DEFERRED_TRUE_YUAN_DROP = "deferredTrueYuanDrop";
    private static final String DEFERRED_IMMORTAL_YUAN_DROP = "deferredImmortalYuanDrop";
    /** Prevents a single long-valued command/integration call from allocating unbounded physical stacks. */
    private static final int MAX_YUAN_MATERIALIZATION_STACKS_PER_CALL = 2_048;
    private static final int MAX_YUAN_DROP_STACKS_PER_CALL = 256;
    /**
     * A finite long API call is committed in at most this many int-sized
     * backing chunks. Creative/non-consuming entries bypass materialization,
     * so their legal Long.MAX_VALUE response remains O(1).
     */
    private static final int MAX_XIANQIAO_LONG_ITEM_CHUNKS_PER_CALL = 64;
    private static final long MAX_XIANQIAO_LONG_ITEM_TRANSFER_PER_CALL =
            (long) Integer.MAX_VALUE * MAX_XIANQIAO_LONG_ITEM_CHUNKS_PER_CALL;

    /** Legacy backing size retained so old 80-slot saves can migrate without loss.
     *  The active stage-five boundary is 72 and menus hide all higher indices. */
    public static final int KONGQIAO_MAX_SLOTS_CEILING = 80;
    /** Initial xianqiao storage capacity. Grows on demand. */
    public static final int XIANQIAO_INITIAL_SLOTS = 9 * 6;
    public static final int XIANQIAO_GROWTH_SLOTS = 256;
    /** Page size for the xianqiao storage menu (scrollable). */
    public static final int XIANQIAO_PAGE_ROWS = 6;
    public static final int XIANQIAO_PAGE_COLS = 9;
    public static final int XIANQIAO_PAGE_SIZE = XIANQIAO_PAGE_ROWS * XIANQIAO_PAGE_COLS;
    /** Xianqiao items unlock at stage six; its independent fluid namespace starts at stage seven. */
    public static final int XIANQIAO_FLUID_UNLOCK_STAGE = 7;
    /** Optional energy, chemical, mana and similar external channels start at stage eight. */
    public static final int XIANQIAO_EXTERNAL_UNLOCK_STAGE = 8;

    private int stage = 0;
    private int lingqiProgress = 0;
    private int lingqiSaturatedLayers = 0;
    private int lingqiSaturatedTicks = 0;
    private boolean advancedWeak = false;
    private int advancedWeakTicks = 0;
    /** Runtime guard distinguishing legacy/load projection from a player-cleared effect. */
    private boolean advancedWeakProjectionInitialized;

    private boolean carryingJade = false;
    private long carryingStartTime = 0;
    private long jadeCarriedTicks = 0;
    private boolean jadeSleepTriggered = false;
    private boolean startingJadeGranted = false;
    private boolean consumedSpiritPill = false;

    private boolean hasKongqiao = false;
    private boolean hasXianqiao = false;
    private boolean hasXianqiaoRealm = false;
    /**
     * Stable id of the personal realm save. It is intentionally independent
     * from the launcher's current session UUID so the same persisted player
     * data keeps one realm across offline/online launcher identity changes.
     */
    private UUID personalRealmId;
    private int realmRadiusChunks = 1;
    private int realmTimeRatePermille = 1000;
    private int preTribulationRealmTimeRatePermille = 1000;
    private boolean realmDaytime = true;
    private int realmWeatherMode = com.immortalstorage.immortalstorage.dimension.RealmEnvironmentPolicy.CLEAR;
    private final Set<Long> modifiedRealmChunks = new HashSet<>();

    private final NonNullList<ItemStack> kongqiao = NonNullList.withSize(KONGQIAO_MAX_SLOTS_CEILING, ItemStack.EMPTY);
    private final NonNullList<ItemStack> xianqiaoStorage = NonNullList.create();
    private final Map<TerminalEntryKey, List<Integer>> xianqiaoSlotsByKey = new HashMap<>();
    private final ArrayDeque<Integer> xianqiaoEmptySlots = new ArrayDeque<>();
    private boolean xianqiaoIndexValid;
    private long xianqiaoStorageRevision;
    /** Non-persistent cache epoch; advances even when the persisted revision saturates or NBT is reloaded. */
    private long xianqiaoStorageGeneration;
    /** Loaded personal-realm source directory epoch; never serialized or folded into physical scans. */
    private long xianqiaoSourceItemGeneration;
    private int xianqiaoMutationDepth;
    private boolean xianqiaoMutationDirty;
    /** One O(n) physical scan per committed revision, shared by menus, capabilities and yuan accounting. */
    private long xianqiaoSummaryGeneration = Long.MIN_VALUE;
    private List<StorageItemSummary> xianqiaoItemSummary = List.of();
    private long xianqiaoSummaryTrueYuan;
    private long xianqiaoSummaryImmortalYuan;
    /** Fluid storage is a separate long-mB namespace and never occupies item slots. */
    private final Map<TerminalFluidKey, Long> xianqiaoFluidAmounts = new LinkedHashMap<>();
    private final Map<TerminalFluidKey, Long> xianqiaoFluidAmountsView =
            Collections.unmodifiableMap(xianqiaoFluidAmounts);
    private long xianqiaoFluidStorageRevision;
    private long xianqiaoFluidStorageGeneration;
    private long xianqiaoSourceFluidGeneration;
    private int xianqiaoFluidMutationDepth;
    private boolean xianqiaoFluidMutationDirty;
    /**
     * Stage-eight optional-mod resources. The loader-neutral ledger keeps one
     * O(1) long balance per stable channel/resource identity and is never
     * scanned from world ticks.
     */
    private final LongResourceLedger externalResourceLedger = new LongResourceLedger();
    private final Map<ResourceChannelKey, AtomicEnergyRefill.ResourceStore> externalResourceStores =
            new HashMap<>();
    private int kongqiaoMaxSlots = 9;
    private int kongqiaoStackMultiplier = 1;

    private final YuanAccount yuanAccount = new YuanAccount();
    private final EmbeddedImmortalFurnaceBackend embeddedImmortalFurnace =
            new EmbeddedImmortalFurnaceBackend();
    /** Terminal crafting refill uses full Data Components unless the player opts out. */
    private boolean craftAutofillMatchComponents = true;
    private boolean handAutoRefill = true;
    /** Stage-four personal-storage magnet; true preserves pre-0.0.4 behavior for existing worlds. */
    private boolean magnetEnabled = true;
    /** Client-only projection because personal storage itself is not attachment-synced. */
    private long syncedTrueYuan;
    private long syncedImmortalYuan;
    private boolean syncedStageTenInfiniteImmortalYuan;
    private boolean hasSyncedYuanProjection;
    private long lastPublishedTrueYuan = Long.MIN_VALUE;
    private long lastPublishedImmortalYuan = Long.MIN_VALUE;
    /** Persisted migration work; large legacy balances are materialized over bounded server ticks. */
    private long deferredTrueYuanMaterialization;
    private long deferredImmortalYuanMaterialization;
    /** Persisted ejection work; cap overflow is never discarded when the per-tick drop budget is exhausted. */
    private long deferredTrueYuanDrop;
    private long deferredImmortalYuanDrop;
    private boolean hasSpiritCore = false;
    private Item cachedTrueYuanItem;
    private Item cachedImmortalYuanItem;
    /** Guards the one and only true-yuan conversion at the real ascension boundary. */
    private boolean ascensionTrueYuanConverted;
    /** Non-null only for an attachment-owned instance; tests use the detached constructor. */
    private final Player owner;

    private boolean tribulationActive = false;
    private java.util.UUID tribulationAttemptId;
    private java.util.UUID tribulationTargetId;
    private int nextStageOnSuccess = 0;
    private int tribulationTargetMissingTicks;

    private double lastExitX = 0, lastExitY = 0, lastExitZ = 0;
    private String lastExitDim = "";
    private boolean hasExitPosition = false;

    public static final ImmortalStoragePlayerData EMPTY = new ImmortalStoragePlayerData();

    public ImmortalStoragePlayerData() {
        this(null);
    }

    ImmortalStoragePlayerData(Player owner) {
        this.owner = owner;
        ensureXianqiaoSize(XIANQIAO_INITIAL_SLOTS);
        recomputeCaps();
    }

    public static ImmortalStoragePlayerData get(Player p) {
        if (p == null) return EMPTY;
        return p.getData(ModAttachments.PLAYER_DATA);
    }

    public EmbeddedImmortalFurnaceBackend getEmbeddedImmortalFurnace() {
        return embeddedImmortalFurnace;
    }

    public boolean isCraftAutofillMatchComponents() {
        return craftAutofillMatchComponents;
    }

    public void setCraftAutofillMatchComponents(boolean value) {
        craftAutofillMatchComponents = value;
    }

    public boolean isHandAutoRefill() { return handAutoRefill; }

    public void setHandAutoRefill(boolean value) {
        if (handAutoRefill == value) return;
        handAutoRefill = value;
        syncOwner();
    }

    public boolean isMagnetEnabled() { return magnetEnabled; }

    public void setMagnetEnabled(boolean value) {
        if (magnetEnabled == value) return;
        magnetEnabled = value;
        syncOwner();
    }

    public int getStage() { return stage; }

    public void setStage(int s) {
        int next = Math.max(0, Math.min(10, s));
        if (this.stage == next) return;
        int previous = this.stage;
        boolean crossesAscension = previous < 6 && next >= 6 && !ascensionTrueYuanConverted;
        long pendingTrueYuanConversion = 0L;
        if (crossesAscension) {
            pendingTrueYuanConversion = saturatingAdd(
                    removeAllPhysicalYuan(YuanKind.TRUE),
                    yuanAccount.drainLegacyBalance(YuanKind.TRUE));
            pendingTrueYuanConversion = saturatingAdd(
                    pendingTrueYuanConversion,
                    drainDeferredMaterialization(YuanKind.TRUE));
        }
        final long trueYuanToConvert = pendingTrueYuanConversion;
        this.stage = next;
        recomputeCaps();
        if ((previous >= 10) != (next >= 10)) markXianqiaoStorageChanged();
        if (crossesAscension) {
            ascensionTrueYuanConverted = true;
            // The complete finite-to-unbounded boundary is one revisioned
            // transaction, including the newly physical 16:1 Yuan conversion.
            batchXianqiaoMutations(() -> {
                migrateKongqiaoIntoXianqiao();
                materializeConvertedTrueYuan(trueYuanToConvert);
                materializeLegacyYuan(YuanKind.IMMORTAL);
            });
        } else {
            materializeLegacyYuanForCurrentStage();
        }
        enforceYuanCaps();
        syncOwner();
    }

    public void setStage(int s, net.minecraft.server.level.ServerPlayer player) {
        int prev = this.stage;
        setStage(s);
        if (player != null && this.stage != prev) {
            com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.fireForStage(this.stage, player);
        }
    }
    public int getLingqiProgress() { return lingqiProgress; }
    public void setLingqiProgress(int v) {
        int next = Math.max(0, v);
        if (lingqiProgress == next) return;
        this.lingqiProgress = next;
        syncOwner();
    }
    public void addLingqiProgress(int delta) { setLingqiProgress(lingqiProgress + delta); }
    public int getLingqiSaturatedLayers() { return lingqiSaturatedLayers; }
    public int getLingqiSaturatedTicks() { return lingqiSaturatedTicks; }
    public boolean isAdvancedWeak() { return advancedWeak; }
    public int getAdvancedWeakTicks() { return advancedWeakTicks; }
    public boolean isCarryingJade() { return carryingJade; }
    public boolean isStartingJadeGranted() { return startingJadeGranted; }
    public void markStartingJadeGranted() { startingJadeGranted = true; }
    public boolean hasConsumedSpiritPill() { return consumedSpiritPill; }
    public void markConsumedSpiritPill() { consumedSpiritPill = true; syncOwner(); }
    public long getCarryingStartTime() { return carryingStartTime; }
    public long getJadeCarriedTicks() { return jadeCarriedTicks; }

    public boolean isHasKongqiao() { return hasKongqiao; }
    public boolean isHasXianqiao() { return hasXianqiao; }
    public boolean isHasXianqiaoRealm() { return hasXianqiaoRealm; }
    public UUID getPersonalRealmId() { return personalRealmId; }

    /** One-shot legacy migration boundary; never replaces an established binding. */
    public UUID bindPersonalRealmOnce(UUID fallback) {
        if (personalRealmId == null) {
            personalRealmId = Objects.requireNonNull(fallback, "fallback");
            syncOwner();
        }
        return personalRealmId;
    }
    public int getRealmRadiusChunks() { return realmRadiusChunks; }
    public int getRealmTimeRatePermille() { return realmTimeRatePermille; }
    public void setRealmTimeRatePermille(int v) {
        int next = tribulationActive ? RealmTimeScalePolicy.NORMAL_PERMILLE
                : RealmTimeScalePolicy.clampPermille(stage, v);
        if (realmTimeRatePermille == next) return;
        this.realmTimeRatePermille = next;
        syncOwner();
    }
    public boolean isRealmDaytime() { return realmDaytime; }
    public int getRealmWeatherMode() { return realmWeatherMode; }
    public void toggleRealmDaytime() {
        realmDaytime = !realmDaytime;
        syncOwner();
    }
    public void cycleRealmWeather() {
        realmWeatherMode = com.immortalstorage.immortalstorage.dimension.RealmEnvironmentPolicy
                .nextWeatherMode(realmWeatherMode);
        syncOwner();
    }
    public Set<Long> getModifiedRealmChunks() { return Collections.unmodifiableSet(modifiedRealmChunks); }
    public void markRealmChunkModified(net.minecraft.world.level.ChunkPos pos) {
        if (pos != null) modifiedRealmChunks.add(pos.toLong());
    }
    /** Get the personal-realm time scale as a float (1.0 = real time). */
    public float getTimeScale() { return realmTimeRatePermille / 1000f; }
    /** Set the dimension-bound personal-realm time scale, clamped by cultivation stage. */
    public void setTimeScale(float v) {
        setRealmTimeRatePermille(Math.round(v * 1000f));
    }

    public long getTrueYuan() { return visibleYuanTotal(YuanKind.TRUE); }
    public long getImmortalYuan() { return visibleYuanTotal(YuanKind.IMMORTAL); }
    public int getTrueYuanCap() { return legacyIntCap(yuanAccount.profile().trueYuan().cap()); }
    public int getImmortalYuanCap() {
        return isInfiniteImmortalYuan() ? Integer.MAX_VALUE
                : legacyIntCap(yuanAccount.profile().immortalYuan().cap());
    }
    public long getTrueYuanCapLong() { return yuanAccount.profile().trueYuan().cap(); }
    public long getImmortalYuanCapLong() {
        return isInfiniteImmortalYuan() ? Long.MAX_VALUE : yuanAccount.profile().immortalYuan().cap();
    }
    public boolean isInfiniteImmortalYuan() {
        return stage >= 10
                && com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.get();
    }

    public boolean isStageTenInfiniteImmortalYuanConfigured() {
        if (owner != null && owner.level().isClientSide) {
            return syncedStageTenInfiniteImmortalYuan;
        }
        return com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.get();
    }

    /** Logical terminal projection; it never allocates Long.MAX_VALUE physical stacks. */
    public List<TerminalVirtualEntry> getVirtualTerminalEntries() {
        if (!isInfiniteImmortalYuan()) return List.of();
        ItemStack prototype = getInfiniteImmortalYuanPrototype();
        return prototype.isEmpty() ? List.of()
                : List.of(new TerminalVirtualEntry(prototype, Long.MAX_VALUE));
    }

    /** One-count identity used by int-sized compatibility surfaces. */
    public ItemStack getInfiniteImmortalYuanPrototype() {
        if (!isInfiniteImmortalYuan()) return ItemStack.EMPTY;
        Item item = findYuanItem(YuanKind.IMMORTAL);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** True only for the canonical component identity represented by the virtual MAX entry. */
    public boolean isVirtualInfiniteImmortalYuanStack(ItemStack stack) {
        if (!isInfiniteImmortalYuan() || stack == null || stack.isEmpty()) return false;
        ItemStack prototype = getInfiniteImmortalYuanPrototype();
        return !prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
    }
    public boolean hasSpiritCore() { return hasSpiritCore; }
    public int getKongqiaoMaxSlots() { return kongqiaoMaxSlots; }
    public int getKongqiaoStackMultiplier() { return kongqiaoStackMultiplier; }
    /** Per-physical-slot capacity for stages 1-5, including normally unstackable items. */
    public int getKongqiaoStackLimit(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        long limit = (long) Math.max(1, stack.getMaxStackSize()) * Math.max(1, kongqiaoStackMultiplier);
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }
    public NonNullList<ItemStack> getKongqiaoItems() { return kongqiao; }
    /**
     * Defensive compatibility snapshot of the compact physical representation.
     * Neither the list nor its mutable ItemStack values can mutate authority.
     * Production directories and capabilities should use {@link #getXianqiaoItemSummary()}.
     */
    public List<ItemStack> getXianqiaoStorageItems() {
        return xianqiaoStorage.stream().map(ItemStack::copy).toList();
    }
    public long getXianqiaoStorageRevision() { return xianqiaoStorageRevision; }
    public long getXianqiaoStorageGeneration() { return xianqiaoStorageGeneration; }
    public long getXianqiaoSourceItemGeneration() { return xianqiaoSourceItemGeneration; }

    /** Loaded source-vein placement/removal/cache changes share the item directory epoch. */
    public void invalidateXianqiaoSourceItemDirectory() {
        if (stage >= 6) xianqiaoSourceItemGeneration = nextGeneration(xianqiaoSourceItemGeneration);
    }
    /** Revision-cached logical directory; callers never need to enumerate physical overstack slots. */
    public List<StorageItemSummary> getXianqiaoItemSummary() {
        ensureXianqiaoSummary();
        return xianqiaoItemSummary;
    }

    /**
     * Long-valued item insertion used by native storage integrations such as
     * AE2. Simulation and execution share the same bounded acceptance plan;
     * execution publishes one revision even when more than one int backing
     * chunk is required.
     */
    public long insertXianqiaoItem(
            TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (key == null || amount <= 0L || action == null || stage < 6) return 0L;
        ItemStack prototype = key.prototype();
        if (isVirtualInfiniteImmortalYuanStack(prototype)) return amount;

        YuanKind yuanKind = YuanItemPolicy.kindOf(prototype);
        long accepted = Math.min(amount, MAX_XIANQIAO_LONG_ITEM_TRANSFER_PER_CALL);
        if (yuanKind != null) accepted = Math.min(accepted, remainingYuanCapacity(yuanKind));
        if (accepted <= 0L || !action.executes()) return accepted;

        long requested = accepted;
        long committed = batchXianqiaoMutations(() -> {
            long authoritative = requested;
            if (yuanKind != null) {
                authoritative = Math.min(authoritative, remainingYuanCapacity(yuanKind));
            }
            return insertXianqiaoAmountUnchecked(prototype, authoritative);
        });
        if (committed > 0L && yuanKind != null) syncOwner();
        return committed;
    }

    /** Long-valued, full-components extraction counterpart to {@link #insertXianqiaoItem}. */
    public long extractXianqiaoItem(
            TerminalEntryKey key, long amount, TerminalStorageAction action) {
        if (key == null || amount <= 0L || action == null || stage < 6) return 0L;
        ItemStack prototype = key.prototype();
        if (isVirtualInfiniteImmortalYuanStack(prototype)) return amount;

        long available = xianqiaoAmount(key);
        long extracted = Math.min(Math.min(amount, available),
                MAX_XIANQIAO_LONG_ITEM_TRANSFER_PER_CALL);
        if (extracted <= 0L || !action.executes()) return extracted;

        YuanKind yuanKind = YuanItemPolicy.kindOf(prototype);
        long requested = extracted;
        long committed = batchXianqiaoMutations(() ->
                extractXianqiaoAmountUnchecked(key, requested));
        if (committed > 0L && yuanKind != null) syncOwner();
        return committed;
    }

    private long xianqiaoAmount(TerminalEntryKey key) {
        ensureXianqiaoSummary();
        for (StorageItemSummary summary : xianqiaoItemSummary) {
            if (key.matches(summary.prototype())) return summary.amount();
        }
        return 0L;
    }

    private long insertXianqiaoAmountUnchecked(ItemStack prototype, long amount) {
        if (prototype.isEmpty() || amount <= 0L) return 0L;
        ensureXianqiaoIndex();
        TerminalEntryKey key = TerminalEntryKey.of(prototype);
        long remaining = amount;
        for (int slot : List.copyOf(xianqiaoSlotsByKey.getOrDefault(key, List.of()))) {
            if (remaining <= 0L) break;
            ItemStack current = xianqiaoStorage.get(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, prototype)) continue;
            int moved = (int) Math.min((long) Integer.MAX_VALUE - current.getCount(), remaining);
            if (moved <= 0) continue;
            current.grow(moved);
            remaining -= moved;
            markXianqiaoStorageChanged();
        }
        int created = 0;
        while (remaining > 0L && created < MAX_XIANQIAO_LONG_ITEM_CHUNKS_PER_CALL) {
            int moved = (int) Math.min(Integer.MAX_VALUE, remaining);
            int slot = takeEmptyXianqiaoSlot();
            ItemStack inserted = prototype.copyWithCount(moved);
            xianqiaoStorage.set(slot, inserted);
            addSlotToXianqiaoIndex(slot, inserted);
            remaining -= moved;
            created++;
            markXianqiaoStorageChanged();
        }
        return amount - remaining;
    }

    private long extractXianqiaoAmountUnchecked(TerminalEntryKey key, long amount) {
        if (amount <= 0L) return 0L;
        ensureXianqiaoIndex();
        long remaining = amount;
        for (int slot : List.copyOf(xianqiaoSlotsByKey.getOrDefault(key, List.of()))) {
            if (remaining <= 0L) break;
            ItemStack current = xianqiaoStorage.get(slot);
            if (current.isEmpty() || !key.matches(current)) continue;
            int moved = (int) Math.min(current.getCount(), remaining);
            if (moved <= 0) continue;
            ItemStack identity = current.copyWithCount(1);
            current.shrink(moved);
            remaining -= moved;
            if (current.isEmpty()) {
                removeSlotFromXianqiaoIndex(slot, identity);
                xianqiaoEmptySlots.addLast(slot);
            }
            markXianqiaoStorageChanged();
        }
        return amount - remaining;
    }
    /** Read-only live view of independently aggregated fluid identities and long-mB totals. */
    public Map<TerminalFluidKey, Long> getXianqiaoFluidAmounts() { return xianqiaoFluidAmountsView; }
    public long getXianqiaoFluidStorageRevision() { return xianqiaoFluidStorageRevision; }
    public long getXianqiaoFluidStorageGeneration() { return xianqiaoFluidStorageGeneration; }
    public long getXianqiaoSourceFluidGeneration() { return xianqiaoSourceFluidGeneration; }

    /** Loaded fluid-source changes share the stage-seven fluid directory epoch. */
    public void invalidateXianqiaoSourceFluidDirectory() {
        if (stage >= XIANQIAO_FLUID_UNLOCK_STAGE) {
            xianqiaoSourceFluidGeneration = nextGeneration(xianqiaoSourceFluidGeneration);
        }
    }

    /** Stage-eight gate for optional energy, chemical, mana and similar channels. */
    public long getExternalResourceAmount(ResourceChannelKey key) {
        Objects.requireNonNull(key, "key");
        return stage >= XIANQIAO_EXTERNAL_UNLOCK_STAGE ? externalResourceLedger.amount(key) : 0L;
    }

    public long getExternalResourceRevision() {
        return externalResourceLedger.revision();
    }

    /** Immutable snapshot shared by optional storage-network adapters. */
    public List<ResourceChannelEntry> getExternalResourceEntries() {
        return externalResourceLedger.snapshot();
    }

    public long insertExternalResource(
            ResourceChannelKey key, long amount, ResourceTransferAction action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
        return stage >= XIANQIAO_EXTERNAL_UNLOCK_STAGE ? externalResourceLedger.insert(key, amount, action) : 0L;
    }

    public long extractExternalResource(
            ResourceChannelKey key, long amount, ResourceTransferAction action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
        return stage >= XIANQIAO_EXTERNAL_UNLOCK_STAGE ? externalResourceLedger.extract(key, amount, action) : 0L;
    }

    /**
     * Stable owner-scoped view used by optional adapters. The same view remains
     * valid across stage changes and rechecks the gate on every transaction.
     */
    public AtomicEnergyRefill.ResourceStore externalResourceStore(ResourceChannelKey key) {
        Objects.requireNonNull(key, "key");
        return externalResourceStores.computeIfAbsent(key, resource ->
                new AtomicEnergyRefill.ResourceStore() {
                    @Override
                    public long amount() {
                        return getExternalResourceAmount(resource);
                    }

                    @Override
                    public long extract(long requested, ResourceTransferAction action) {
                        return extractExternalResource(resource, requested, action);
                    }

                    @Override
                    public long insert(long offered, ResourceTransferAction action) {
                        return insertExternalResource(resource, offered, action);
                    }
                });
    }
    public long getXianqiaoFluidAmount(TerminalFluidKey key) {
        return key == null ? 0L : xianqiaoFluidAmounts.getOrDefault(key, 0L);
    }

    /**
     * Inserts into the independent fluid namespace and returns the accepted mB.
     * Simulation performs the same overflow/capacity calculation without writes.
     */
    public long insertXianqiaoFluid(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (key == null || amountMb <= 0L || action == null
                || stage < XIANQIAO_FLUID_UNLOCK_STAGE) return 0L;
        long current = xianqiaoFluidAmounts.getOrDefault(key, 0L);
        long accepted = Math.min(amountMb, Long.MAX_VALUE - current);
        if (accepted <= 0L || !action.executes()) return accepted;
        return batchXianqiaoFluidMutations(() -> {
            long authoritative = xianqiaoFluidAmounts.getOrDefault(key, 0L);
            long committed = Math.min(amountMb, Long.MAX_VALUE - authoritative);
            if (committed <= 0L) return 0L;
            xianqiaoFluidAmounts.put(key, authoritative + committed);
            markXianqiaoFluidStorageChanged();
            return committed;
        });
    }

    /** Extracts matching fluid-and-components and returns the extracted mB. */
    public long extractXianqiaoFluid(TerminalFluidKey key, long amountMb, TerminalStorageAction action) {
        if (key == null || amountMb <= 0L || action == null
                || stage < XIANQIAO_FLUID_UNLOCK_STAGE) return 0L;
        long available = xianqiaoFluidAmounts.getOrDefault(key, 0L);
        long extracted = Math.min(amountMb, available);
        if (extracted <= 0L || !action.executes()) return extracted;
        return batchXianqiaoFluidMutations(() -> {
            long authoritative = xianqiaoFluidAmounts.getOrDefault(key, 0L);
            long committed = Math.min(amountMb, authoritative);
            if (committed <= 0L) return 0L;
            long remainder = authoritative - committed;
            if (remainder == 0L) xianqiaoFluidAmounts.remove(key);
            else xianqiaoFluidAmounts.put(key, remainder);
            markXianqiaoFluidStorageChanged();
            return committed;
        });
    }
    public List<ItemStack> snapshotStorage(boolean xianqiao) {
        return (xianqiao ? xianqiaoStorage : kongqiao).stream().map(ItemStack::copy).toList();
    }
    public void replaceStorage(boolean xianqiao, List<ItemStack> snapshot) {
        NonNullList<ItemStack> target = xianqiao ? xianqiaoStorage : kongqiao;
        List<ItemStack> sanitized = sanitizeStorageSnapshot(snapshot);
        if (xianqiao) {
            batchXianqiaoMutations(() -> {
                if (sameStorageSnapshot(target, sanitized)) return;
                target.clear();
                sanitized.forEach(stack -> target.add(stack.copy()));
                compactLoadedXianqiaoStorage();
                markXianqiaoStorageChanged();
            });
            enforceYuanCaps();
            return;
        }
        for (int slot = 0; slot < target.size(); slot++) {
            target.set(slot, slot < sanitized.size() ? sanitized.get(slot).copy() : ItemStack.EMPTY);
        }
        enforceYuanCaps();
    }

    /** Collapses nested storage writes into one monotonic revision commit. */
    public void batchXianqiaoMutations(Runnable mutation) {
        batchXianqiaoMutations(() -> {
            mutation.run();
            return null;
        });
    }

    /** Collapses nested storage writes into one monotonic revision commit. */
    public <T> T batchXianqiaoMutations(Supplier<T> mutation) {
        if (mutation == null) throw new IllegalArgumentException("mutation is required");
        xianqiaoMutationDepth++;
        try {
            return mutation.get();
        } finally {
            xianqiaoMutationDepth--;
            if (xianqiaoMutationDepth == 0 && xianqiaoMutationDirty) {
                xianqiaoMutationDirty = false;
                if (xianqiaoStorageRevision < Long.MAX_VALUE) xianqiaoStorageRevision++;
                xianqiaoStorageGeneration = nextGeneration(xianqiaoStorageGeneration);
            }
        }
    }

    /** Collapses nested fluid writes into one monotonic revision commit. */
    public void batchXianqiaoFluidMutations(Runnable mutation) {
        batchXianqiaoFluidMutations(() -> {
            mutation.run();
            return null;
        });
    }

    /** Collapses nested fluid writes into one monotonic revision commit. */
    public <T> T batchXianqiaoFluidMutations(Supplier<T> mutation) {
        if (mutation == null) throw new IllegalArgumentException("mutation is required");
        xianqiaoFluidMutationDepth++;
        try {
            return mutation.get();
        } finally {
            xianqiaoFluidMutationDepth--;
            if (xianqiaoFluidMutationDepth == 0 && xianqiaoFluidMutationDirty) {
                xianqiaoFluidMutationDirty = false;
                if (xianqiaoFluidStorageRevision < Long.MAX_VALUE) xianqiaoFluidStorageRevision++;
                xianqiaoFluidStorageGeneration = nextGeneration(xianqiaoFluidStorageGeneration);
            }
        }
    }
    public boolean isStorageIsKongqiaoLegacy() { return stage <= 5; }
    public boolean isTribulationActive() { return tribulationActive; }
    public java.util.UUID getTribulationAttemptId() { return tribulationAttemptId; }
    public java.util.UUID getTribulationTargetId() { return tribulationTargetId; }
    public int getNextStageOnSuccess() { return nextStageOnSuccess; }
    public int getTrueYuanGenAccum() { return yuanAccount.progress(YuanKind.TRUE); }
    public int getImmortalYuanGenAccum() { return yuanAccount.progress(YuanKind.IMMORTAL); }
    public int getTrueYuanGenInterval() { return yuanAccount.profile().trueYuan().generationIntervalTicks(); }
    public int getImmortalYuanGenInterval() { return yuanAccount.profile().immortalYuan().generationIntervalTicks(); }
    public long getTrueYuanGenAmount() { return yuanAccount.profile().trueYuan().generationAmount(); }
    public long getImmortalYuanGenAmount() { return yuanAccount.profile().immortalYuan().generationAmount(); }

    public boolean beginTribulation(java.util.UUID attemptId, java.util.UUID targetId, int nextStage) {
        if (tribulationActive || attemptId == null || targetId == null || nextStage <= stage) return false;
        this.tribulationActive = true;
        this.preTribulationRealmTimeRatePermille = this.realmTimeRatePermille;
        this.realmTimeRatePermille = RealmTimeScalePolicy.NORMAL_PERMILLE;
        this.tribulationAttemptId = attemptId;
        this.tribulationTargetId = targetId;
        this.nextStageOnSuccess = nextStage;
        this.tribulationTargetMissingTicks = 0;
        syncOwner();
        return true;
    }

    public boolean completeTribulation(java.util.UUID attemptId, java.util.UUID targetId,
                                       net.minecraft.server.level.ServerPlayer player) {
        if (!matchesTribulation(attemptId, targetId)) return false;
        int newStage = nextStageOnSuccess;
        if (!com.immortalstorage.immortalstorage.progression.TribulationPolicy.allowsNormalAdvance(
                stage, newStage,
                com.immortalstorage.immortalstorage.progression.TribulationPolicy.configuredMaximumStage())) {
            return false;
        }
        clearTribulationState();
        this.realmTimeRatePermille = RealmTimeScalePolicy.clampPermille(
                newStage, preTribulationRealmTimeRatePermille);
        if (newStage > stage) {
            setStage(newStage);
            if (player != null) {
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.fireForStage(newStage, player);
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.TRIBULATION_WON.trigger(player);
                com.immortalstorage.immortalstorage.event.CommonEvents.restoreStageEffects(player);
            }
        }
        syncOwner();
        return true;
    }

    public long failTribulation() {
        if (!tribulationActive) return 0L;
        clearTribulationState();
        this.realmTimeRatePermille = RealmTimeScalePolicy.clampPermille(
                stage, preTribulationRealmTimeRatePermille);
        long removed = removeAllPhysicalYuan(YuanKind.IMMORTAL);
        removed = saturatingAdd(removed, yuanAccount.drainLegacyBalance(YuanKind.IMMORTAL));
        removed = saturatingAdd(removed, drainDeferredMaterialization(YuanKind.IMMORTAL));
        setDeferredDrop(YuanKind.IMMORTAL, 0L);
        syncOwner();
        return removed;
    }

    public void abortTribulation() {
        if (!tribulationActive) return;
        clearTribulationState();
        this.realmTimeRatePermille = RealmTimeScalePolicy.clampPermille(
                stage, preTribulationRealmTimeRatePermille);
        syncOwner();
    }

    public boolean matchesTribulation(java.util.UUID attemptId, java.util.UUID targetId) {
        return tribulationActive && java.util.Objects.equals(tribulationAttemptId, attemptId)
                && java.util.Objects.equals(tribulationTargetId, targetId);
    }

    public boolean noteTribulationTargetMissing(int graceTicks) {
        if (!tribulationActive) return false;
        int boundedGrace = Math.max(1, graceTicks);
        if (tribulationTargetMissingTicks < boundedGrace) tribulationTargetMissingTicks++;
        return tribulationTargetMissingTicks >= boundedGrace;
    }

    public void resetTribulationTargetMissing() {
        tribulationTargetMissingTicks = 0;
    }

    private void clearTribulationState() {
        tribulationActive = false;
        tribulationAttemptId = null;
        tribulationTargetId = null;
        nextStageOnSuccess = 0;
        tribulationTargetMissingTicks = 0;
    }

    public double getLastExitX() { return lastExitX; }
    public double getLastExitY() { return lastExitY; }
    public double getLastExitZ() { return lastExitZ; }
    public String getLastExitDim() { return lastExitDim; }
    public boolean hasExitPosition() { return hasExitPosition; }
    public void markExitPosition(double x, double y, double z, String dim) {
        this.lastExitX = x; this.lastExitY = y; this.lastExitZ = z; this.lastExitDim = dim; this.hasExitPosition = true;
    }
    public void clearExitPosition() { this.hasExitPosition = false; }
    /** Legacy save projection retained only for binary/data compatibility; Spirit Core has no runtime effect. */
    public void setHasSpiritCore(boolean ignored) { this.hasSpiritCore = false; }
    public void setCarryingJade(boolean b, long now) {
        if (b) {
            if (!this.carryingJade) {
                this.carryingJade = true;
                this.carryingStartTime = now;
                this.jadeCarriedTicks = 0L;
                this.jadeSleepTriggered = false;
            }
        } else {
            resetJadeCarryTracking();
        }
    }
    public void markJadeSleepTriggered() { this.jadeSleepTriggered = true; }
    public boolean isJadeSleepTriggered() { return jadeSleepTriggered; }

    /** Advances the uninterrupted, inventory-verified Jade carry window by one server tick. */
    public boolean tickJadeInitiation(boolean hasJade) {
        if (stage != 0 || !hasJade) {
            resetJadeCarryTracking();
            return false;
        }
        if (!carryingJade) {
            carryingJade = true;
            carryingStartTime = 0L;
            jadeCarriedTicks = 0L;
            jadeSleepTriggered = false;
        }
        if (jadeCarriedTicks < ImmortalStorageProgressionRules.JADE_INITIATION_TICKS) {
            jadeCarriedTicks++;
        }
        if (!ImmortalStorageProgressionRules.shouldInitiateWithJade(
                stage, true, jadeCarriedTicks, false)) {
            return false;
        }
        resetJadeCarryTracking();
        setStage(1);
        return true;
    }

    /** Performs the alternative stage-zero initiation when a Jade carrier sleeps. */
    public boolean tryJadeSleepInitiation(boolean hasJade) {
        if (!ImmortalStorageProgressionRules.shouldInitiateWithJade(
                stage, hasJade, jadeCarriedTicks, true)) {
            return false;
        }
        jadeSleepTriggered = true;
        resetJadeCarryTracking();
        setStage(1);
        return true;
    }

    private void resetJadeCarryTracking() {
        carryingJade = false;
        carryingStartTime = 0L;
        jadeCarriedTicks = 0L;
    }
    public void setAdvancedWeak(int ticks) {
        this.advancedWeakTicks = Math.max(0, ticks);
        this.advancedWeak = advancedWeakTicks > 0;
        if (owner instanceof ServerPlayer serverPlayer && advancedWeak) {
            serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                            com.immortalstorage.immortalstorage.effect.ModEffects.ADVANCED_WEAKNESS.get()),
                    advancedWeakTicks, 0, false, true, true));
            advancedWeakProjectionInitialized = true;
        }
        syncOwner();
    }

    private void reconcileAdvancedWeakness(ServerPlayer player) {
        var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                com.immortalstorage.immortalstorage.effect.ModEffects.ADVANCED_WEAKNESS.get());
        var visible = player.getEffect(holder);
        if (!advancedWeakProjectionInitialized) {
            advancedWeakProjectionInitialized = true;
            if (visible == null && advancedWeak && advancedWeakTicks > 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        holder, advancedWeakTicks, 0, false, true, true));
                visible = player.getEffect(holder);
            }
        }
        if (visible == null) {
            advancedWeak = false;
            advancedWeakTicks = 0;
            return;
        }
        advancedWeak = true;
        advancedWeakTicks = Math.max(1, visible.getDuration());
    }
    public void setLingqiSaturated(int layers, int ticks) {
        this.lingqiSaturatedLayers = Math.min(5, layers);
        this.lingqiSaturatedTicks = ticks;
        syncOwner();
    }
    public void addLingqiSaturated(int addLayers, int addTicks) {
        this.lingqiSaturatedLayers = Math.min(5, this.lingqiSaturatedLayers + addLayers);
        this.lingqiSaturatedTicks = Math.max(this.lingqiSaturatedTicks, addTicks);
        syncOwner();
    }
    public void tickLingqiSaturated() { if (lingqiSaturatedTicks > 0) { lingqiSaturatedTicks--; if (lingqiSaturatedTicks <= 0) { lingqiSaturatedLayers = 0; } } }

    /** Legacy no-op retained so old callers and saved data do not gain behavior. */
    public void scanForSpiritCore() { hasSpiritCore = false; }

    /**
     * Insert up to {@code amount} items of the given type into the appropriate
     * per-stage storage (kongqiao for stages 1-5, xianqiao for stages 6+).
     * Returns the leftover that could not be inserted.
     */
    public ItemStack insertStack(ItemStack stack, boolean allowStorage) {
        if (stack.isEmpty()) return stack;
        YuanKind yuanKind = YuanItemPolicy.kindOf(stack);
        if (isVirtualInfiniteImmortalYuanStack(stack)) return ItemStack.EMPTY;
        if (yuanKind != null) {
            long capacity = remainingYuanCapacity(yuanKind);
            int permitted = (int) Math.min(stack.getCount(), Math.max(0L, capacity));
            if (permitted <= 0) return stack.copy();
            ItemStack permittedStack = stack.copyWithCount(permitted);
            ItemStack permittedLeftover = insertStackUnchecked(permittedStack);
            int accepted = permitted - (permittedLeftover.isEmpty() ? 0 : permittedLeftover.getCount());
            if (accepted > 0) syncOwner();
            int totalLeftover = stack.getCount() - accepted;
            return totalLeftover <= 0 ? ItemStack.EMPTY : stack.copyWithCount(totalLeftover);
        }
        return insertStackUnchecked(stack);
    }

    /**
     * Moves a stack whose source is already part of the player's capped total.
     * Menus use this path for inventory-to-terminal transfers so a full account
     * can still be reorganized without transiently counting the source twice.
     */
    public ItemStack insertStackFromPlayerInventory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        if (isVirtualInfiniteImmortalYuanStack(stack)) return ItemStack.EMPTY;
        ItemStack leftover = insertStackUnchecked(stack);
        if (YuanItemPolicy.isYuanItem(stack)
                && (leftover.isEmpty() || leftover.getCount() != stack.getCount())) syncOwner();
        return leftover;
    }

    private ItemStack insertStackUnchecked(ItemStack stack) {
        NonNullList<ItemStack> list = isStorageIsKongqiaoLegacy() ? kongqiao : xianqiaoStorage;
        int max = isStorageIsKongqiaoLegacy() ? kongqiaoMaxSlots : Integer.MAX_VALUE;
        int remaining = stack.getCount();
        if (!isStorageIsKongqiaoLegacy()) {
            final int offered = remaining;
            final int[] mutableRemaining = {remaining};
            return batchXianqiaoMutations(() -> {
                ensureXianqiaoIndex();
                TerminalEntryKey key = TerminalEntryKey.of(stack);
                List<Integer> matchingSlots = List.copyOf(xianqiaoSlotsByKey.getOrDefault(key, List.of()));
                for (int slot : matchingSlots) {
                    if (mutableRemaining[0] <= 0) break;
                    ItemStack current = xianqiaoStorage.get(slot);
                    if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, stack)) continue;
                    long space = (long) Integer.MAX_VALUE - current.getCount();
                    int moved = (int) Math.min(space, mutableRemaining[0]);
                    if (moved <= 0) continue;
                    current.grow(moved);
                    mutableRemaining[0] -= moved;
                    markXianqiaoStorageChanged();
                }
                while (mutableRemaining[0] > 0) {
                    int slot = takeEmptyXianqiaoSlot();
                    int moved = mutableRemaining[0];
                    ItemStack inserted = stack.copyWithCount(moved);
                    xianqiaoStorage.set(slot, inserted);
                    addSlotToXianqiaoIndex(slot, inserted);
                    mutableRemaining[0] -= moved;
                    markXianqiaoStorageChanged();
                }
                return mutableRemaining[0] == 0 ? ItemStack.EMPTY
                        : stack.copyWithCount(Math.min(offered, mutableRemaining[0]));
            });
        }
        // First pass: merge with existing stacks
        for (int i = 0; i < Math.min(max, list.size()) && remaining > 0; i++) {
            ItemStack s = list.get(i);
            if (s.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(s, stack)) {
                int space = Math.max(0, getKongqiaoStackLimit(s) - s.getCount());
                if (space > 0) {
                    int put = Math.min(space, remaining);
                    s.grow(put);
                    remaining -= put;
                }
            }
        }
        // Second pass: fill empty slots
        for (int i = 0; i < Math.min(max, list.size()) && remaining > 0; i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty()) continue;
            int put = Math.min(getKongqiaoStackLimit(stack), remaining);
            list.set(i, stack.copyWithCount(put));
            remaining -= put;
        }
        return remaining == 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    /** Non-mutating insert planner for automation simulation paths. */
    public ItemStack simulateInsertStack(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        YuanKind yuanKind = YuanItemPolicy.kindOf(stack);
        if (isVirtualInfiniteImmortalYuanStack(stack)) return ItemStack.EMPTY;
        if (yuanKind != null) {
            long capacity = remainingYuanCapacity(yuanKind);
            int permitted = (int) Math.min(stack.getCount(), Math.max(0L, capacity));
            if (permitted <= 0) return stack.copy();
            ItemStack permittedLeftover = simulateInsertStackUnchecked(stack.copyWithCount(permitted));
            int accepted = permitted - (permittedLeftover.isEmpty() ? 0 : permittedLeftover.getCount());
            int totalLeftover = stack.getCount() - accepted;
            return totalLeftover <= 0 ? ItemStack.EMPTY : stack.copyWithCount(totalLeftover);
        }
        return simulateInsertStackUnchecked(stack);
    }

    private ItemStack simulateInsertStackUnchecked(ItemStack stack) {
        // Xianqiao is logically unbounded. Avoid scanning its physical backing
        // for a simulation that can never reject an ordinary item.
        if (!isStorageIsKongqiaoLegacy()) return ItemStack.EMPTY;
        NonNullList<ItemStack> list = isStorageIsKongqiaoLegacy() ? kongqiao : xianqiaoStorage;
        int max = isStorageIsKongqiaoLegacy() ? kongqiaoMaxSlots : Integer.MAX_VALUE;
        int existingSlots = Math.min(max, list.size());
        int remaining = stack.getCount();
        for (int i = 0; i < existingSlots && remaining > 0; i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                remaining -= Math.min(Math.max(0, getKongqiaoStackLimit(s) - s.getCount()), remaining);
            }
        }
        for (int i = 0; i < existingSlots && remaining > 0; i++) {
            if (list.get(i).isEmpty()) {
                remaining -= Math.min(getKongqiaoStackLimit(stack), remaining);
            }
        }
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    /** Remove up to {@code amount} of {@code stack} from the appropriate storage. */
    public ItemStack extractStack(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        if (isVirtualInfiniteImmortalYuanStack(stack)) return stack.copyWithCount(amount);
        boolean yuan = YuanItemPolicy.isYuanItem(stack);
        NonNullList<ItemStack> list = isStorageIsKongqiaoLegacy() ? kongqiao : xianqiaoStorage;
        boolean xianqiao = !isStorageIsKongqiaoLegacy();
        if (xianqiao) {
            ItemStack result = batchXianqiaoMutations(() -> {
                ensureXianqiaoIndex();
                List<Integer> matchingSlots = List.copyOf(xianqiaoSlotsByKey.getOrDefault(
                        TerminalEntryKey.of(stack), List.of()));
                int extracted = 0;
                ItemStack assembled = ItemStack.EMPTY;
                for (int slot : matchingSlots) {
                    if (extracted >= amount) break;
                    ItemStack current = xianqiaoStorage.get(slot);
                    if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, stack)) continue;
                    int take = Math.min(current.getCount(), amount - extracted);
                    if (assembled.isEmpty()) assembled = current.copyWithCount(take);
                    else assembled.grow(take);
                    current.shrink(take);
                    if (current.isEmpty()) {
                        removeSlotFromXianqiaoIndex(slot, stack);
                        xianqiaoEmptySlots.addLast(slot);
                    }
                    markXianqiaoStorageChanged();
                    extracted += take;
                }
                return assembled;
            });
            if (yuan && !result.isEmpty()) syncOwner();
            return result;
        }
        Supplier<ItemStack> extraction = () -> {
            int extracted = 0;
            ItemStack result = ItemStack.EMPTY;
            for (int i = 0; i < list.size() && extracted < amount; i++) {
                ItemStack current = list.get(i);
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, stack)) continue;
                int take = Math.min(current.getCount(), amount - extracted);
                if (result.isEmpty()) result = current.copyWithCount(take);
                else result.grow(take);
                current.shrink(take);
                extracted += take;
            }
            return result;
        };
        ItemStack result = extraction.get();
        if (yuan && !result.isEmpty()) syncOwner();
        return result;
    }

    /** Non-mutating extraction planner for template-based storage endpoints. */
    public ItemStack simulateExtractStack(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        if (isVirtualInfiniteImmortalYuanStack(stack)) return stack.copyWithCount(amount);
        NonNullList<ItemStack> list = isStorageIsKongqiaoLegacy() ? kongqiao : xianqiaoStorage;
        int extracted = 0;
        List<Integer> matchingSlots;
        if (isStorageIsKongqiaoLegacy()) {
            matchingSlots = new ArrayList<>(list.size());
            for (int slot = 0; slot < list.size(); slot++) matchingSlots.add(slot);
        } else {
            ensureXianqiaoIndex();
            matchingSlots = List.copyOf(xianqiaoSlotsByKey.getOrDefault(
                    TerminalEntryKey.of(stack), List.of()));
        }
        for (int slot : matchingSlots) {
            if (extracted >= amount) break;
            ItemStack s = list.get(slot);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                extracted += Math.min(s.getCount(), amount - extracted);
            }
        }
        return extracted <= 0 ? ItemStack.EMPTY : stack.copyWithCount(extracted);
    }

    /** Iterate the whole xianqiao storage and remove up to one of every distinct item. */
    public ItemStack extractOneKind(ItemStack template) {
        return extractStack(template, 1);
    }

    public void setKongqiaoSlot(int idx, ItemStack s) {
        if (idx < 0 || idx >= kongqiao.size()) return;
        if (s == null || s.isEmpty()) {
            kongqiao.set(idx, ItemStack.EMPTY);
            return;
        }
        ItemStack stored = s.copy();
        stored.setCount(Math.min(stored.getCount(), getKongqiaoStackLimit(stored)));
        kongqiao.set(idx, stored);
    }
    public void setXianqiaoSlot(int idx, ItemStack s) {
        if (idx < 0) return;
        ItemStack next = s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy();
        batchXianqiaoMutations(() -> {
            ensureXianqiaoSize(idx + 1);
            ItemStack previous = xianqiaoStorage.get(idx);
            if (sameStack(previous, next)) return;
            if (xianqiaoIndexValid) removeSlotFromXianqiaoIndex(idx, previous);
            xianqiaoStorage.set(idx, next);
            if (xianqiaoIndexValid) addSlotToXianqiaoIndex(idx, next);
            markXianqiaoStorageChanged();
        });
    }

    /** Exact slot extraction used by the NeoForge IItemHandler bridge. */
    public ItemStack extractXianqiaoSlot(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= xianqiaoStorage.size() || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = xianqiaoStorage.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = current.copyWithCount(Math.min(amount, current.getCount()));
        if (simulate) return extracted;
        ItemStack result = batchXianqiaoMutations(() -> {
            ItemStack authoritative = xianqiaoStorage.get(slot);
            if (authoritative.isEmpty()) return ItemStack.EMPTY;
            int take = Math.min(amount, authoritative.getCount());
            ItemStack extractedStack = authoritative.copyWithCount(take);
            ItemStack identity = authoritative.copyWithCount(1);
            authoritative.shrink(take);
            if (authoritative.isEmpty() && xianqiaoIndexValid) {
                removeSlotFromXianqiaoIndex(slot, identity);
                xianqiaoEmptySlots.addLast(slot);
            }
            markXianqiaoStorageChanged();
            return extractedStack;
        });
        if (YuanItemPolicy.isYuanItem(result)) syncOwner();
        return result;
    }

    /** Exact active-storage extraction used by the public NeoForge item handler. */
    public ItemStack extractPersonalStorageSlot(int slot, int amount, boolean simulate) {
        if (!isStorageIsKongqiaoLegacy()) return extractXianqiaoSlot(slot, amount, simulate);
        if (slot < 0 || slot >= kongqiaoMaxSlots || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = kongqiao.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = current.copyWithCount(Math.min(amount, current.getCount()));
        if (simulate) return extracted;
        current.shrink(extracted.getCount());
        if (current.isEmpty()) kongqiao.set(slot, ItemStack.EMPTY);
        if (YuanItemPolicy.isYuanItem(extracted)) syncOwner();
        return extracted;
    }
    public void ensureXianqiaoSize(int size) {
        while (xianqiaoStorage.size() < size) {
            int slot = xianqiaoStorage.size();
            xianqiaoStorage.add(ItemStack.EMPTY);
            if (xianqiaoIndexValid) xianqiaoEmptySlots.addLast(slot);
        }
    }

    private void ensureXianqiaoIndex() {
        if (xianqiaoIndexValid) return;
        xianqiaoSlotsByKey.clear();
        xianqiaoEmptySlots.clear();
        for (int slot = 0; slot < xianqiaoStorage.size(); slot++) {
            ItemStack current = xianqiaoStorage.get(slot);
            if (current.isEmpty()) {
                xianqiaoEmptySlots.addLast(slot);
            } else {
                xianqiaoSlotsByKey.computeIfAbsent(TerminalEntryKey.of(current), ignored -> new ArrayList<>()).add(slot);
            }
        }
        xianqiaoIndexValid = true;
    }

    private void addSlotToXianqiaoIndex(int slot, ItemStack stack) {
        if (!xianqiaoIndexValid) return;
        if (stack == null || stack.isEmpty()) {
            if (!xianqiaoEmptySlots.contains(slot)) xianqiaoEmptySlots.addLast(slot);
            return;
        }
        List<Integer> slots = xianqiaoSlotsByKey.computeIfAbsent(
                TerminalEntryKey.of(stack), ignored -> new ArrayList<>());
        if (!slots.contains(slot)) slots.add(slot);
    }

    private void removeSlotFromXianqiaoIndex(int slot, ItemStack stack) {
        if (!xianqiaoIndexValid) return;
        if (stack == null || stack.isEmpty()) {
            xianqiaoEmptySlots.removeFirstOccurrence(slot);
            return;
        }
        TerminalEntryKey key = TerminalEntryKey.of(stack);
        List<Integer> slots = xianqiaoSlotsByKey.get(key);
        if (slots == null) return;
        slots.remove(Integer.valueOf(slot));
        if (slots.isEmpty()) xianqiaoSlotsByKey.remove(key);
    }

    private void markXianqiaoStorageChanged() {
        xianqiaoSummaryGeneration = Long.MIN_VALUE;
        if (xianqiaoMutationDepth > 0) {
            xianqiaoMutationDirty = true;
        } else if (xianqiaoStorageRevision < Long.MAX_VALUE) {
            xianqiaoStorageRevision++;
        }
        if (xianqiaoMutationDepth == 0) {
            xianqiaoStorageGeneration = nextGeneration(xianqiaoStorageGeneration);
        }
    }

    private void markXianqiaoFluidStorageChanged() {
        if (xianqiaoFluidMutationDepth > 0) {
            xianqiaoFluidMutationDirty = true;
        } else if (xianqiaoFluidStorageRevision < Long.MAX_VALUE) {
            xianqiaoFluidStorageRevision++;
        }
        if (xianqiaoFluidMutationDepth == 0) {
            xianqiaoFluidStorageGeneration = nextGeneration(xianqiaoFluidStorageGeneration);
        }
    }

    private static boolean sameStorageSnapshot(List<ItemStack> current, List<ItemStack> next) {
        if (next == null || current.size() != next.size()) return false;
        for (int slot = 0; slot < current.size(); slot++) {
            if (!sameStack(current.get(slot), next.get(slot))) return false;
        }
        return true;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        boolean leftEmpty = left == null || left.isEmpty();
        boolean rightEmpty = right == null || right.isEmpty();
        if (leftEmpty || rightEmpty) return leftEmpty == rightEmpty;
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }

    private int takeEmptyXianqiaoSlot() {
        while (!xianqiaoEmptySlots.isEmpty()) {
            int slot = xianqiaoEmptySlots.removeFirst();
            if (slot >= 0 && slot < xianqiaoStorage.size() && xianqiaoStorage.get(slot).isEmpty()) return slot;
        }
        for (int slot = 0; slot < xianqiaoStorage.size(); slot++) {
            if (xianqiaoStorage.get(slot).isEmpty()) return slot;
        }
        int firstNewSlot = xianqiaoStorage.size();
        ensureXianqiaoSize(firstNewSlot + XIANQIAO_GROWTH_SLOTS);
        return xianqiaoEmptySlots.removeFirst();
    }

    /**
     * Builds the shared logical directory and cultivation-resource summary in
     * one pass. Repeated reads at the same committed revision are O(1).
     */
    private void ensureXianqiaoSummary() {
        if (xianqiaoSummaryGeneration == xianqiaoStorageGeneration) return;
        LinkedHashMap<TerminalEntryKey, MutableStorageSummary> grouped = new LinkedHashMap<>();
        long trueYuan = 0L;
        long immortalYuan = 0L;
        for (ItemStack stack : xianqiaoStorage) {
            if (stack.isEmpty()) continue;
            TerminalEntryKey key = TerminalEntryKey.of(stack);
            MutableStorageSummary summary = grouped.computeIfAbsent(key,
                    ignored -> new MutableStorageSummary(stack.copyWithCount(1)));
            summary.amount = saturatingAdd(summary.amount, Math.max(0, stack.getCount()));
            YuanKind kind = YuanItemPolicy.kindOf(stack);
            if (kind == YuanKind.TRUE) trueYuan = saturatingAdd(trueYuan, Math.max(0, stack.getCount()));
            else if (kind == YuanKind.IMMORTAL) {
                immortalYuan = saturatingAdd(immortalYuan, Math.max(0, stack.getCount()));
            }
        }
        List<StorageItemSummary> entries = new ArrayList<>(grouped.size());
        for (MutableStorageSummary summary : grouped.values()) {
            if (summary.amount > 0L) entries.add(new StorageItemSummary(summary.prototype, summary.amount));
        }
        xianqiaoItemSummary = List.copyOf(entries);
        xianqiaoSummaryTrueYuan = trueYuan;
        xianqiaoSummaryImmortalYuan = immortalYuan;
        xianqiaoSummaryGeneration = xianqiaoStorageGeneration;
    }

    /** Drops legacy empty capacity and combines equal full-component identities into int-sized backing chunks. */
    private void compactLoadedXianqiaoStorage() {
        LinkedHashMap<TerminalEntryKey, MutableStorageSummary> grouped = new LinkedHashMap<>();
        for (ItemStack stack : xianqiaoStorage) {
            if (stack.isEmpty()) continue;
            MutableStorageSummary summary = grouped.computeIfAbsent(TerminalEntryKey.of(stack),
                    ignored -> new MutableStorageSummary(stack.copyWithCount(1)));
            summary.amount = saturatingAdd(summary.amount, Math.max(0, stack.getCount()));
        }
        xianqiaoStorage.clear();
        for (MutableStorageSummary summary : grouped.values()) {
            long remaining = summary.amount;
            while (remaining > 0L) {
                int chunk = (int) Math.min(Integer.MAX_VALUE, remaining);
                xianqiaoStorage.add(summary.prototype.copyWithCount(chunk));
                remaining -= chunk;
            }
        }
        ensureXianqiaoSize(XIANQIAO_INITIAL_SLOTS);
        xianqiaoIndexValid = false;
        xianqiaoSummaryGeneration = Long.MIN_VALUE;
    }

    private static final class MutableStorageSummary {
        private final ItemStack prototype;
        private long amount;

        private MutableStorageSummary(ItemStack prototype) {
            this.prototype = prototype;
        }
    }

    /**
     * The normal progression boundary is one-way: every physical Kongqiao
     * stack is moved into the unbounded Xianqiao namespace exactly once when
     * stage 6 is reached. Clearing the source slots makes the operation
     * naturally idempotent for debug re-entry and old saves.
     */
    private void migrateKongqiaoIntoXianqiao() {
        batchXianqiaoMutations(() -> {
            for (int slot = 0; slot < kongqiao.size(); slot++) {
                ItemStack stored = kongqiao.get(slot);
                if (stored.isEmpty()) continue;
                ItemStack leftover = insertStackUnchecked(stored.copy());
                if (leftover.isEmpty()) {
                    kongqiao.set(slot, ItemStack.EMPTY);
                } else {
                    // The Xianqiao grows on demand, so this is only a defensive
                    // safeguard against a future bounded/custom implementation.
                    kongqiao.set(slot, leftover);
                }
            }
        });
    }

    /** Synchronizes the compact client projection of this persistent attachment to its owner. */
    public void syncTo(ServerPlayer player) {
        if (player == null || player != owner) return;
        player.syncData(ModAttachments.PLAYER_DATA);
    }

    /** Per-tick server side logic: lingqi progress, yuan generation, tribulation countdown, advanced weakness, etc. */
    public void serverTick(net.minecraft.server.level.ServerPlayer p) {
        reconcileAdvancedWeakness(p);
        tickLingqiSaturated();
        advanceDeferredYuanWork();
        syncYuanProjectionIfChanged();
        if (stage == 0) {
            return;
        }
        advanceYuanGeneration(1);
        // Stages 1->5 advance by Lingqi. Stage 5 deliberately stays capped
        // until an Immortal Pill performs the explicit ascension boundary.
        if (stage >= 1 && stage <= 4) {
            int cap = getLingqiCap();
            if (lingqiProgress >= cap
                    && com.immortalstorage.immortalstorage.progression.ImmortalStorageProgressionRules
                    .allowsNormalAdvance(stage, stage + 1)) {
                int newStage = stage + 1;
                setStage(newStage);
                setLingqiProgress(0);
                com.immortalstorage.immortalstorage.advancement.ImmortalStorageCriteriaTriggers.fireForStage(newStage, p);
            }
        }
        // Stage 5->6 conversion is handled atomically in setStage.
    }

    /** The single passive-generation entry point. */
    public YuanGeneration advanceYuanGeneration(int elapsedTicks) {
        YuanGeneration scheduled = yuanAccount.advanceGeneration(elapsedTicks);
        if (!scheduled.changed()) return YuanGeneration.NONE;
        long materializedTrue = materializeYuan(YuanKind.TRUE, scheduled.trueYuan());
        long materializedImmortal = materializeYuan(YuanKind.IMMORTAL, scheduled.immortalYuan());
        YuanGeneration materialized = new YuanGeneration(materializedTrue, materializedImmortal);
        if (materialized.changed()) syncOwner();
        return materialized;
    }

    public long depositTrueYuan(long amount) {
        return depositYuan(YuanKind.TRUE, amount);
    }

    public long depositImmortalYuan(long amount) {
        return depositYuan(YuanKind.IMMORTAL, amount);
    }

    private long depositYuan(YuanKind kind, long amount) {
        long accepted = materializeYuan(kind, amount);
        if (accepted > 0L) syncOwner();
        return accepted;
    }

    /** Legacy signed-add facade; new code should use deposit/consume explicitly. */
    public void addTrueYuan(long amount) {
        changeLegacy(YuanKind.TRUE, amount);
    }

    /** Legacy signed-add facade; new code should use deposit/consume explicitly. */
    public void addImmortalYuan(long amount) {
        changeLegacy(YuanKind.IMMORTAL, amount);
    }

    private void changeLegacy(YuanKind kind, long amount) {
        long changed;
        if (amount >= 0L) {
            changed = materializeYuan(kind, amount);
        } else {
            long requested = amount == Long.MIN_VALUE ? Long.MAX_VALUE : -amount;
            changed = removeYuanUpTo(kind, requested);
        }
        if (changed > 0L) syncOwner();
    }

    public boolean consumeTrueYuan(long amount) {
        return consumeYuan(YuanKind.TRUE, amount);
    }

    public boolean consumeImmortalYuan(long amount) {
        return consumeYuan(YuanKind.IMMORTAL, amount);
    }

    private boolean consumeYuan(YuanKind kind, long amount) {
        if (amount <= 0L || visibleYuanTotal(kind) < amount) return false;
        boolean consumed = removeYuanUpTo(kind, amount) == amount;
        if (consumed) {
            if (owner instanceof ServerPlayer serverPlayer) {
                com.immortalstorage.immortalstorage.enchantment.SpiritRepairService.onYuanConsumed(serverPlayer, amount);
            }
            syncOwner();
        }
        return consumed;
    }

    public void recomputeCaps() {
        int s = stage;
        hasKongqiao = s >= 1 && s <= 5;
        hasXianqiao = s >= 6;
        hasXianqiaoRealm = s >= 6;
        kongqiaoMaxSlots = 0;
        kongqiaoStackMultiplier = 1;
        realmRadiusChunks = 1;
        switch (s) {
            case 1 -> { kongqiaoMaxSlots = 9; kongqiaoStackMultiplier = 1; }
            case 2 -> { kongqiaoMaxSlots = 18; kongqiaoStackMultiplier = 2; }
            case 3 -> { kongqiaoMaxSlots = 36; kongqiaoStackMultiplier = 4; }
            case 4 -> { kongqiaoMaxSlots = 54; kongqiaoStackMultiplier = 8; }
            case 5 -> { kongqiaoMaxSlots = 72; kongqiaoStackMultiplier = 16; }
            case 6 -> realmRadiusChunks = 1;
            case 7 -> realmRadiusChunks = 3;
            case 8 -> realmRadiusChunks = 9;
            case 9 -> realmRadiusChunks = 32;
            case 10 -> realmRadiusChunks = 1024;
            default -> {}
        }
        // Spirit-core rescans and cap recomputation must not erase the user's
        // realm setting. Only clamp it when a stage lowers the permitted rate.
        realmTimeRatePermille = RealmTimeScalePolicy.clampPermille(s, realmTimeRatePermille);
        hasSpiritCore = false;
        yuanAccount.configure(YuanProfile.forStage(s, false));
    }

    private void syncOwner() {
        if (owner instanceof ServerPlayer serverPlayer) syncTo(serverPlayer);
    }

    private void syncYuanProjectionIfChanged() {
        if (!(owner instanceof ServerPlayer)) return;
        YuanTotals totals = authoritativeYuanTotals();
        long trueYuan = totals.trueYuan();
        long immortalYuan = isInfiniteImmortalYuan() ? Long.MAX_VALUE : totals.immortalYuan();
        if (trueYuan == lastPublishedTrueYuan && immortalYuan == lastPublishedImmortalYuan) return;
        lastPublishedTrueYuan = trueYuan;
        lastPublishedImmortalYuan = immortalYuan;
        syncOwner();
    }

    private static int legacyIntCap(long cap) {
        if (cap == YuanRule.UNBOUNDED_CAP) return -1;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, cap));
    }

    /** Network projection intentionally excludes both storage inventories. */
    void writeClientSync(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stage", stage);
        tag.putInt("lingqiProgress", lingqiProgress);
        tag.putInt("lingqiSaturatedLayers", lingqiSaturatedLayers);
        tag.putInt("lingqiSaturatedTicks", lingqiSaturatedTicks);
        tag.putBoolean("advancedWeak", advancedWeak);
        tag.putInt("advancedWeakTicks", advancedWeakTicks);
        tag.putBoolean("hasKongqiao", hasKongqiao);
        tag.putBoolean("hasXianqiao", hasXianqiao);
        tag.putBoolean("hasXianqiaoRealm", hasXianqiaoRealm);
        if (personalRealmId != null) tag.putUUID("personalRealmId", personalRealmId);
        tag.putInt("realmRadiusChunks", realmRadiusChunks);
        tag.putInt("realmTimeRatePermille", realmTimeRatePermille);
        tag.putInt("preTribulationRealmTimeRatePermille", preTribulationRealmTimeRatePermille);
        tag.putBoolean("realmDaytime", realmDaytime);
        tag.putInt("realmWeatherMode", realmWeatherMode);
        tag.putInt("kongqiaoMaxSlots", kongqiaoMaxSlots);
        tag.putInt("kongqiaoStackMultiplier", kongqiaoStackMultiplier);
        tag.putBoolean("hasSpiritCore", false);
        tag.putBoolean("ascensionTrueYuanConverted", ascensionTrueYuanConverted);
        tag.putBoolean("tribulationActive", tribulationActive);
        if (tribulationAttemptId != null) tag.putUUID("tribulationAttemptId", tribulationAttemptId);
        if (tribulationTargetId != null) tag.putUUID("tribulationTargetId", tribulationTargetId);
        tag.putInt("tribulationTargetMissingTicks", tribulationTargetMissingTicks);
        tag.putInt("nextStageOnSuccess", nextStageOnSuccess);
        long visibleTrueYuan = visibleYuanTotal(YuanKind.TRUE);
        long visibleImmortalYuan = visibleYuanTotal(YuanKind.IMMORTAL);
        tag.putLong("visibleTrueYuan", visibleTrueYuan);
        tag.putLong("visibleImmortalYuan", visibleImmortalYuan);
        tag.putBoolean("stageTenInfiniteImmortalYuan",
                com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.STAGE_TEN_INFINITE_IMMORTAL_YUAN.get());
        lastPublishedTrueYuan = visibleTrueYuan;
        lastPublishedImmortalYuan = visibleImmortalYuan;
        tag.putBoolean("magnetEnabled", magnetEnabled);
        tag.put("yuanAccount", yuanAccount.save());
        buffer.writeNbt(tag);
    }

    void readClientSync(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        if (tag == null) return;
        stage = Math.max(0, Math.min(10, tag.getInt("stage")));
        lingqiProgress = Math.max(0, tag.getInt("lingqiProgress"));
        lingqiSaturatedLayers = Math.max(0, tag.getInt("lingqiSaturatedLayers"));
        lingqiSaturatedTicks = Math.max(0, tag.getInt("lingqiSaturatedTicks"));
        advancedWeak = tag.getBoolean("advancedWeak");
        advancedWeakTicks = Math.max(0, tag.getInt("advancedWeakTicks"));
        hasKongqiao = tag.getBoolean("hasKongqiao");
        hasXianqiao = tag.getBoolean("hasXianqiao");
        hasXianqiaoRealm = tag.getBoolean("hasXianqiaoRealm");
        personalRealmId = tag.hasUUID("personalRealmId") ? tag.getUUID("personalRealmId") : null;
        realmRadiusChunks = Math.max(1, tag.getInt("realmRadiusChunks"));
        int syncedRate = tag.contains("realmTimeRatePermille", Tag.TAG_ANY_NUMERIC)
                ? tag.getInt("realmTimeRatePermille") : RealmTimeScalePolicy.NORMAL_PERMILLE;
        realmTimeRatePermille = RealmTimeScalePolicy.clampPermille(stage, syncedRate);
        realmDaytime = !tag.contains("realmDaytime", Tag.TAG_BYTE) || tag.getBoolean("realmDaytime");
        realmWeatherMode = com.immortalstorage.immortalstorage.dimension.RealmEnvironmentPolicy
                .sanitizeWeatherMode(tag.getInt("realmWeatherMode"));
        kongqiaoMaxSlots = Math.max(0, Math.min(KONGQIAO_MAX_SLOTS_CEILING, tag.getInt("kongqiaoMaxSlots")));
        kongqiaoStackMultiplier = Math.max(1, tag.getInt("kongqiaoStackMultiplier"));
        hasSpiritCore = false;
        ascensionTrueYuanConverted = tag.contains("ascensionTrueYuanConverted", Tag.TAG_BYTE)
                ? tag.getBoolean("ascensionTrueYuanConverted") : stage >= 6;
        tribulationActive = tag.getBoolean("tribulationActive");
        tribulationAttemptId = tag.hasUUID("tribulationAttemptId") ? tag.getUUID("tribulationAttemptId") : null;
        tribulationTargetId = tag.hasUUID("tribulationTargetId") ? tag.getUUID("tribulationTargetId") : null;
        nextStageOnSuccess = Math.max(0, tag.getInt("nextStageOnSuccess"));
        tribulationTargetMissingTicks = Math.max(0, tag.getInt("tribulationTargetMissingTicks"));
        if (tribulationAttemptId == null || tribulationTargetId == null || nextStageOnSuccess <= stage) {
            clearTribulationState();
        }
        syncedTrueYuan = Math.max(0L, tag.getLong("visibleTrueYuan"));
        syncedImmortalYuan = Math.max(0L, tag.getLong("visibleImmortalYuan"));
        syncedStageTenInfiniteImmortalYuan = tag.getBoolean("stageTenInfiniteImmortalYuan");
        magnetEnabled = !tag.contains("magnetEnabled", Tag.TAG_BYTE) || tag.getBoolean("magnetEnabled");
        hasSyncedYuanProjection = true;
        YuanProfile profile = YuanProfile.forStage(stage, false);
        if (tag.contains("yuanAccount", Tag.TAG_COMPOUND)) {
            yuanAccount.load(tag.getCompound("yuanAccount"), profile);
        } else {
            yuanAccount.loadLegacy(tag.getLong("trueYuan"), tag.getLong("immortalYuan"), profile);
        }
    }

    public int getLingqiCap() {
        return switch (stage) {
            case 1 -> 120;
            case 2 -> 360;
            case 3 -> 1440;
            case 4 -> 2560;
            case 5 -> 10420;
            default -> 0;
        };
    }

    public static CompoundTag saveStack(HolderLookup.Provider registryAccess, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        int storedCount = stack.getCount();
        // Vanilla 1.21.1's persistent ItemStack codec accepts counts 1..99.
        // Kongqiao deliberately supports larger per-slot counts, so encode a
        // one-count identity and preserve the authoritative count additively.
        ItemStack codecSafe = storedCount > 99 ? stack.copyWithCount(1) : stack;
        Tag encoded = codecSafe.saveOptional(registryAccess);
        CompoundTag result = encoded instanceof CompoundTag compound ? compound.copy() : new CompoundTag();
        if (storedCount > 99) result.putInt(EXTENDED_STACK_COUNT, storedCount);
        return result;
    }

    public static ItemStack loadStack(HolderLookup.Provider registryAccess, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return ItemStack.EMPTY;
        if (tag.contains("Count", Tag.TAG_ANY_NUMERIC) || tag.contains("tag", Tag.TAG_COMPOUND)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) return ItemStack.EMPTY;
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null) return ItemStack.EMPTY;
            ItemStack legacy = new ItemStack(item, Math.max(1, tag.getInt("Count")));
            if (tag.contains("tag", Tag.TAG_COMPOUND)) {
                legacy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.getCompound("tag").copy()));
            }
            if (tag.contains(EXTENDED_STACK_COUNT, Tag.TAG_ANY_NUMERIC)) {
                legacy.setCount(Math.max(1, tag.getInt(EXTENDED_STACK_COUNT)));
            }
            return legacy;
        }
        ItemStack parsed = ItemStack.parseOptional(registryAccess, tag);
        if (!parsed.isEmpty() && tag.contains(EXTENDED_STACK_COUNT, Tag.TAG_ANY_NUMERIC)) {
            parsed.setCount(Math.max(1, tag.getInt(EXTENDED_STACK_COUNT)));
        }
        return parsed;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stage", stage);
        tag.putInt("lingqiProgress", lingqiProgress);
        tag.putInt("lingqiSaturatedLayers", lingqiSaturatedLayers);
        tag.putInt("lingqiSaturatedTicks", lingqiSaturatedTicks);
        tag.putBoolean("advancedWeak", advancedWeak);
        tag.putInt("advancedWeakTicks", advancedWeakTicks);
        tag.putBoolean("carryingJade", carryingJade);
        tag.putLong("carryingStartTime", carryingStartTime);
        tag.putLong("jadeCarriedTicks", jadeCarriedTicks);
        tag.putBoolean("jadeSleepTriggered", jadeSleepTriggered);
        tag.putBoolean("startingJadeGranted", startingJadeGranted);
        tag.putBoolean("consumedSpiritPill", consumedSpiritPill);
        tag.putBoolean("hasKongqiao", hasKongqiao);
        tag.putBoolean("hasXianqiao", hasXianqiao);
        tag.putBoolean("hasXianqiaoRealm", hasXianqiaoRealm);
        if (personalRealmId != null) tag.putUUID("personalRealmId", personalRealmId);
        tag.putInt("realmRadiusChunks", realmRadiusChunks);
        tag.putInt("realmTimeRatePermille", realmTimeRatePermille);
        tag.putBoolean("realmDaytime", realmDaytime);
        tag.putInt("realmWeatherMode", realmWeatherMode);
        long[] dirtyChunks = new long[modifiedRealmChunks.size()];
        int dirtyIndex = 0;
        for (Long packed : modifiedRealmChunks) {
            dirtyChunks[dirtyIndex++] = packed;
        }
        tag.putLongArray("modifiedRealmChunks", dirtyChunks);
        // Flat totals remain as a downgrade/tooling projection only. New loads
        // use yuanAccount's migration fields and physical item stacks, so
        // these values are never imported twice.
        tag.putLong("trueYuan", getTrueYuan());
        tag.putLong("immortalYuan", getImmortalYuan());
        tag.put("yuanAccount", yuanAccount.save());
        tag.putLong(DEFERRED_TRUE_YUAN_MATERIALIZATION, deferredTrueYuanMaterialization);
        tag.putLong(DEFERRED_IMMORTAL_YUAN_MATERIALIZATION, deferredImmortalYuanMaterialization);
        tag.putLong(DEFERRED_TRUE_YUAN_DROP, deferredTrueYuanDrop);
        tag.putLong(DEFERRED_IMMORTAL_YUAN_DROP, deferredImmortalYuanDrop);
        tag.putLong("xianqiaoStorageRevision", xianqiaoStorageRevision);
        tag.putLong("xianqiaoFluidStorageRevision", xianqiaoFluidStorageRevision);
        tag.putBoolean("hasSpiritCore", false);
        tag.putBoolean("ascensionTrueYuanConverted", ascensionTrueYuanConverted);
        tag.putBoolean("tribulationActive", tribulationActive);
        if (tribulationAttemptId != null) tag.putUUID("tribulationAttemptId", tribulationAttemptId);
        if (tribulationTargetId != null) tag.putUUID("tribulationTargetId", tribulationTargetId);
        tag.putInt("tribulationTargetMissingTicks", tribulationTargetMissingTicks);
        tag.putInt("nextStageOnSuccess", nextStageOnSuccess);
        tag.putDouble("lastExitX", lastExitX);
        tag.putDouble("lastExitY", lastExitY);
        tag.putDouble("lastExitZ", lastExitZ);
        tag.putString("lastExitDim", lastExitDim);
        tag.putBoolean("hasExitPosition", hasExitPosition);
        tag.putInt("kongqiaoMaxSlots", kongqiaoMaxSlots);
        tag.putInt("kongqiaoStackMultiplier", kongqiaoStackMultiplier);
        ListTag list = new ListTag();
        for (int i = 0; i < kongqiao.size(); i++) {
            ItemStack st = kongqiao.get(i);
            if (st.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.put("item", saveStack(registryAccess, st));
            list.add(entry);
        }
        tag.put("kongqiao", list);
        ListTag list2 = new ListTag();
        for (int i = 0; i < xianqiaoStorage.size(); i++) {
            ItemStack st = xianqiaoStorage.get(i);
            if (st.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.put("item", saveStack(registryAccess, st));
            list2.add(entry);
        }
        tag.put("xianqiaoStorage", list2);
        ListTag fluids = new ListTag();
        for (Map.Entry<TerminalFluidKey, Long> stored : xianqiaoFluidAmounts.entrySet()) {
            if (stored.getValue() == null || stored.getValue() <= 0L) continue;
            CompoundTag entry = new CompoundTag();
            FluidStack prototype = stored.getKey().prototype();
            Tag encoded = prototype.saveOptional(registryAccess);
            entry.put("fluid", encoded instanceof CompoundTag compound ? compound.copy() : new CompoundTag());
            entry.putLong("amountMb", stored.getValue());
            fluids.add(entry);
        }
        tag.put("xianqiaoFluidStorage", fluids);
        CompoundTag externalResources = new CompoundTag();
        externalResources.putLong("revision", externalResourceLedger.revision());
        ListTag externalEntries = new ListTag();
        for (ResourceChannelEntry stored : externalResourceLedger.snapshot()) {
            if (stored.amount() <= 0L) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("channel", stored.key().channel());
            entry.putString("resourceId", stored.key().resourceId());
            entry.putLong("amount", stored.amount());
            externalEntries.add(entry);
        }
        externalResources.put("entries", externalEntries);
        tag.put("externalResourceLedger", externalResources);
        tag.put("embeddedImmortalFurnace", embeddedImmortalFurnace.save(registryAccess));
        tag.putBoolean("craftAutofillMatchComponents", craftAutofillMatchComponents);
        tag.putBoolean("handAutoRefill", handAutoRefill);
        tag.putBoolean("magnetEnabled", magnetEnabled);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag tag) {
        stage = Math.max(0, Math.min(10, tag.getInt("stage")));
        lingqiProgress = tag.getInt("lingqiProgress");
        lingqiSaturatedLayers = tag.getInt("lingqiSaturatedLayers");
        lingqiSaturatedTicks = tag.getInt("lingqiSaturatedTicks");
        advancedWeak = tag.getBoolean("advancedWeak");
        advancedWeakTicks = tag.getInt("advancedWeakTicks");
        carryingJade = tag.getBoolean("carryingJade");
        carryingStartTime = tag.getLong("carryingStartTime");
        jadeCarriedTicks = Math.max(0L, Math.min(
                ImmortalStorageProgressionRules.JADE_INITIATION_TICKS,
                tag.getLong("jadeCarriedTicks")));
        jadeSleepTriggered = tag.getBoolean("jadeSleepTriggered");
        startingJadeGranted = tag.getBoolean("startingJadeGranted");
        consumedSpiritPill = tag.getBoolean("consumedSpiritPill");
        hasKongqiao = tag.getBoolean("hasKongqiao");
        hasXianqiao = tag.getBoolean("hasXianqiao");
        hasXianqiaoRealm = tag.getBoolean("hasXianqiaoRealm");
        personalRealmId = tag.hasUUID("personalRealmId") ? tag.getUUID("personalRealmId") : null;
        realmRadiusChunks = tag.getInt("realmRadiusChunks");
        realmTimeRatePermille = tag.contains("realmTimeRatePermille", Tag.TAG_ANY_NUMERIC)
                ? tag.getInt("realmTimeRatePermille") : RealmTimeScalePolicy.NORMAL_PERMILLE;
        realmDaytime = !tag.contains("realmDaytime", Tag.TAG_BYTE) || tag.getBoolean("realmDaytime");
        realmWeatherMode = com.immortalstorage.immortalstorage.dimension.RealmEnvironmentPolicy
                .sanitizeWeatherMode(tag.getInt("realmWeatherMode"));
        preTribulationRealmTimeRatePermille = tag.contains("preTribulationRealmTimeRatePermille", Tag.TAG_ANY_NUMERIC)
                ? tag.getInt("preTribulationRealmTimeRatePermille") : realmTimeRatePermille;
        modifiedRealmChunks.clear();
        if (tag.contains("modifiedRealmChunks", Tag.TAG_LONG_ARRAY)) {
            for (long packed : tag.getLongArray("modifiedRealmChunks")) {
                modifiedRealmChunks.add(packed);
            }
        }
        hasSpiritCore = false;
        ascensionTrueYuanConverted = tag.contains("ascensionTrueYuanConverted", Tag.TAG_BYTE)
                ? tag.getBoolean("ascensionTrueYuanConverted") : stage >= 6;
        YuanProfile loadedProfile = YuanProfile.forStage(stage, false);
        if (tag.contains("yuanAccount", Tag.TAG_COMPOUND)) {
            yuanAccount.load(tag.getCompound("yuanAccount"), loadedProfile);
        } else {
            yuanAccount.loadLegacy(tag.getLong("trueYuan"), tag.getLong("immortalYuan"), loadedProfile);
        }
        deferredTrueYuanMaterialization = nonNegativeLong(tag, DEFERRED_TRUE_YUAN_MATERIALIZATION);
        deferredImmortalYuanMaterialization = nonNegativeLong(tag, DEFERRED_IMMORTAL_YUAN_MATERIALIZATION);
        deferredTrueYuanDrop = nonNegativeLong(tag, DEFERRED_TRUE_YUAN_DROP);
        deferredImmortalYuanDrop = nonNegativeLong(tag, DEFERRED_IMMORTAL_YUAN_DROP);
        tribulationActive = tag.getBoolean("tribulationActive");
        tribulationAttemptId = tag.hasUUID("tribulationAttemptId") ? tag.getUUID("tribulationAttemptId") : null;
        tribulationTargetId = tag.hasUUID("tribulationTargetId") ? tag.getUUID("tribulationTargetId") : null;
        nextStageOnSuccess = tag.getInt("nextStageOnSuccess");
        tribulationTargetMissingTicks = Math.max(0, tag.getInt("tribulationTargetMissingTicks"));
        if (tribulationAttemptId == null || tribulationTargetId == null || nextStageOnSuccess <= stage) {
            clearTribulationState();
        }
        lastExitX = tag.getDouble("lastExitX");
        lastExitY = tag.getDouble("lastExitY");
        lastExitZ = tag.getDouble("lastExitZ");
        lastExitDim = tag.getString("lastExitDim");
        hasExitPosition = tag.getBoolean("hasExitPosition");
        kongqiaoMaxSlots = tag.getInt("kongqiaoMaxSlots");
        kongqiaoStackMultiplier = tag.getInt("kongqiaoStackMultiplier");
        for (int i = 0; i < kongqiao.size(); i++) {
            kongqiao.set(i, ItemStack.EMPTY);
        }
        if (tag.contains("kongqiao")) {
            ListTag list = tag.getList("kongqiao", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("slot");
                if (slot >= 0 && slot < kongqiao.size()) {
                    kongqiao.set(slot, loadStack(registryAccess, entry.getCompound("item")));
                }
            }
        }
        xianqiaoStorage.clear();
        xianqiaoIndexValid = false;
        xianqiaoStorageRevision = Math.max(0L, tag.getLong("xianqiaoStorageRevision"));
        xianqiaoMutationDepth = 0;
        xianqiaoMutationDirty = false;
        if (tag.contains("xianqiaoStorage")) {
            ListTag list = tag.getList("xianqiaoStorage", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("slot");
                if (slot >= 0) {
                    ItemStack loaded = loadStack(registryAccess, entry.getCompound("item"));
                    if (!loaded.isEmpty()) xianqiaoStorage.add(loaded);
                }
            }
        }
        // Xianqiao has logical identity semantics, not addressable slot
        // semantics. Old slot ids are compatibility metadata only; ignoring
        // their magnitude also prevents sparse NBT from allocating huge tails.
        compactLoadedXianqiaoStorage();
        xianqiaoFluidAmounts.clear();
        xianqiaoFluidStorageRevision = Math.max(0L, tag.getLong("xianqiaoFluidStorageRevision"));
        xianqiaoFluidMutationDepth = 0;
        xianqiaoFluidMutationDirty = false;
        if (tag.contains("xianqiaoFluidStorage", Tag.TAG_LIST)) {
            ListTag fluids = tag.getList("xianqiaoFluidStorage", Tag.TAG_COMPOUND);
            for (int index = 0; index < fluids.size(); index++) {
                CompoundTag entry = fluids.getCompound(index);
                FluidStack stack = FluidStack.parseOptional(registryAccess, entry.getCompound("fluid"));
                long amountMb = entry.getLong("amountMb");
                if (stack.isEmpty() || amountMb <= 0L) continue;
                TerminalFluidKey key = TerminalFluidKey.of(stack);
                long current = xianqiaoFluidAmounts.getOrDefault(key, 0L);
                xianqiaoFluidAmounts.put(key, saturatingAdd(current, amountMb));
            }
        }
        List<ResourceChannelEntry> externalEntries = new ArrayList<>();
        long externalRevision = 0L;
        if (tag.contains("externalResourceLedger", Tag.TAG_COMPOUND)) {
            CompoundTag externalResources = tag.getCompound("externalResourceLedger");
            externalRevision = Math.max(0L, externalResources.getLong("revision"));
            ListTag storedEntries = externalResources.getList("entries", Tag.TAG_COMPOUND);
            for (int index = 0; index < storedEntries.size(); index++) {
                CompoundTag entry = storedEntries.getCompound(index);
                long amount = entry.getLong("amount");
                if (amount <= 0L) continue;
                try {
                    ResourceChannelKey key = new ResourceChannelKey(
                            entry.getString("channel"), entry.getString("resourceId"));
                    externalEntries.add(new ResourceChannelEntry(key, amount));
                } catch (IllegalArgumentException ignored) {
                    // Unknown well-formed channels are retained. Malformed
                    // external NBT rows are ignored rather than poisoning the
                    // player's entire cultivation attachment.
                }
            }
        }
        externalResourceLedger.restore(externalEntries, externalRevision);
        embeddedImmortalFurnace.load(registryAccess,
                tag.contains("embeddedImmortalFurnace", Tag.TAG_COMPOUND)
                        ? tag.getCompound("embeddedImmortalFurnace") : new CompoundTag());
        craftAutofillMatchComponents = !tag.contains("craftAutofillMatchComponents", Tag.TAG_BYTE)
                || tag.getBoolean("craftAutofillMatchComponents");
        handAutoRefill = !tag.contains("handAutoRefill", Tag.TAG_BYTE)
                || tag.getBoolean("handAutoRefill");
        magnetEnabled = !tag.contains("magnetEnabled", Tag.TAG_BYTE) || tag.getBoolean("magnetEnabled");
        // Deserialization is a cache boundary even when the persisted revision
        // happens to equal the previously loaded value.
        xianqiaoStorageGeneration = nextGeneration(xianqiaoStorageGeneration);
        xianqiaoFluidStorageGeneration = nextGeneration(xianqiaoFluidStorageGeneration);
        recomputeCaps();
        if (stage >= 6) {
            if (kongqiao.stream().anyMatch(stack -> !stack.isEmpty())) {
                // Repair pre-migration stage-6 saves produced by older builds.
                migrateKongqiaoIntoXianqiao();
            }
            materializeLegacyYuan(YuanKind.TRUE);
            materializeLegacyYuan(YuanKind.IMMORTAL);
        } else {
            materializeLegacyYuanForCurrentStage();
        }
        enforceYuanCaps();
    }

    private static long saturatingAdd(long left, long right) {
        if (left < 0L || right < 0L) throw new IllegalArgumentException("stored amounts must not be negative");
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long nextGeneration(long current) {
        return current == Long.MAX_VALUE ? 0L : current + 1L;
    }

    private static long nonNegativeLong(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Math.max(0L, tag.getLong(key)) : 0L;
    }

    private static List<ItemStack> sanitizeStorageSnapshot(List<ItemStack> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return List.of();
        List<ItemStack> sanitized = new ArrayList<>(snapshot.size());
        for (ItemStack stack : snapshot) {
            sanitized.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return sanitized;
    }

    private long visibleYuanTotal(YuanKind kind) {
        if (isVirtualInfiniteYuan(kind)) return ResourceAmountPolicy.UNCHANGED.reportedLong(0L);
        if (owner != null && owner.level().isClientSide && hasSyncedYuanProjection) {
            return kind == YuanKind.TRUE ? syncedTrueYuan : syncedImmortalYuan;
        }
        return countPhysicalYuan(kind);
    }

    /** Live authoritative count: owner inventory plus the currently active personal storage. */
    private long countPhysicalYuan(YuanKind kind) {
        long total = 0L;
        if (isStorageIsKongqiaoLegacy()) {
            total = countYuanIn(kongqiao, Math.min(kongqiaoMaxSlots, kongqiao.size()), kind);
        } else {
            ensureXianqiaoSummary();
            total = kind == YuanKind.TRUE ? xianqiaoSummaryTrueYuan : xianqiaoSummaryImmortalYuan;
        }
        if (owner != null) {
            total = saturatingAdd(total, countYuanIn(owner.getInventory().items,
                    owner.getInventory().items.size(), kind));
            total = saturatingAdd(total, countYuanIn(owner.getInventory().offhand,
                    owner.getInventory().offhand.size(), kind));
            total = saturatingAdd(total, countYuanIn(owner.getInventory().armor,
                    owner.getInventory().armor.size(), kind));
        }
        return total;
    }

    /** Both currencies in one storage-summary read and one small player-inventory pass. */
    private YuanTotals authoritativeYuanTotals() {
        long trueYuan;
        long immortalYuan;
        if (isStorageIsKongqiaoLegacy()) {
            YuanTotals stored = countYuanTotalsIn(kongqiao, Math.min(kongqiaoMaxSlots, kongqiao.size()));
            trueYuan = stored.trueYuan();
            immortalYuan = stored.immortalYuan();
        } else {
            ensureXianqiaoSummary();
            trueYuan = xianqiaoSummaryTrueYuan;
            immortalYuan = xianqiaoSummaryImmortalYuan;
        }
        if (owner != null) {
            YuanTotals inventory = countYuanTotalsIn(owner.getInventory().items,
                    owner.getInventory().items.size());
            YuanTotals offhand = countYuanTotalsIn(owner.getInventory().offhand,
                    owner.getInventory().offhand.size());
            YuanTotals armor = countYuanTotalsIn(owner.getInventory().armor,
                    owner.getInventory().armor.size());
            trueYuan = saturatingAdd(trueYuan,
                    saturatingAdd(inventory.trueYuan(), saturatingAdd(offhand.trueYuan(), armor.trueYuan())));
            immortalYuan = saturatingAdd(immortalYuan,
                    saturatingAdd(inventory.immortalYuan(),
                            saturatingAdd(offhand.immortalYuan(), armor.immortalYuan())));
        }
        return new YuanTotals(trueYuan, immortalYuan);
    }

    private static long countYuanIn(List<ItemStack> stacks, int limit, YuanKind kind) {
        long total = 0L;
        for (int slot = 0; slot < Math.min(limit, stacks.size()); slot++) {
            ItemStack stack = stacks.get(slot);
            if (YuanItemPolicy.kindOf(stack) == kind) {
                total = saturatingAdd(total, Math.max(0, stack.getCount()));
            }
        }
        return total;
    }

    private static YuanTotals countYuanTotalsIn(List<ItemStack> stacks, int limit) {
        long trueYuan = 0L;
        long immortalYuan = 0L;
        for (int slot = 0; slot < Math.min(limit, stacks.size()); slot++) {
            ItemStack stack = stacks.get(slot);
            YuanKind kind = YuanItemPolicy.kindOf(stack);
            if (kind == YuanKind.TRUE) trueYuan = saturatingAdd(trueYuan, Math.max(0, stack.getCount()));
            else if (kind == YuanKind.IMMORTAL) {
                immortalYuan = saturatingAdd(immortalYuan, Math.max(0, stack.getCount()));
            }
        }
        return new YuanTotals(trueYuan, immortalYuan);
    }

    private record YuanTotals(long trueYuan, long immortalYuan) {}

    private long remainingYuanCapacity(YuanKind kind) {
        if (isVirtualInfiniteYuan(kind)) return Long.MAX_VALUE;
        YuanRule rule = yuanAccount.profile().rule(kind);
        if (!rule.enabled()) return 0L;
        if (rule.unbounded()) return Long.MAX_VALUE;
        return Math.max(0L, rule.cap() - countPhysicalYuan(kind));
    }

    /** Creates real item stacks and inserts them without creating a hidden balance. */
    private long materializeYuan(YuanKind kind, long requested) {
        if (kind == null || requested <= 0L) return 0L;
        if (isVirtualInfiniteYuan(kind)) {
            return ResourceAmountPolicy.UNCHANGED.extractable(0L, requested);
        }
        long target = Math.min(requested, remainingYuanCapacity(kind));
        if (target <= 0L) return 0L;
        Item yuanItem = findYuanItem(kind);
        if (yuanItem == null) return 0L;

        long accepted = 0L;
        int materializedStacks = 0;
        int maxStack = Math.max(1, new ItemStack(yuanItem).getMaxStackSize());
        long boundedTarget = Math.min(target,
                (long) maxStack * MAX_YUAN_MATERIALIZATION_STACKS_PER_CALL);
        if (!isStorageIsKongqiaoLegacy()) {
            // Preserve the bounded item budget while materializing it as one
            // internal overstack instead of thousands of heap objects.
            int offered = (int) Math.min(Integer.MAX_VALUE, boundedTarget);
            ItemStack leftover = insertStackUnchecked(new ItemStack(yuanItem, offered));
            return offered - (leftover.isEmpty() ? 0 : leftover.getCount());
        }
        while (accepted < boundedTarget && materializedStacks < MAX_YUAN_MATERIALIZATION_STACKS_PER_CALL) {
            int offered = (int) Math.min(maxStack, boundedTarget - accepted);
            ItemStack leftover = insertStackUnchecked(new ItemStack(yuanItem, offered));
            int inserted = offered - (leftover.isEmpty() ? 0 : leftover.getCount());
            if (!leftover.isEmpty() && owner != null) {
                int beforeInventory = leftover.getCount();
                owner.getInventory().add(leftover);
                inserted += beforeInventory - leftover.getCount();
            }
            if (inserted <= 0) break;
            accepted += inserted;
            materializedStacks++;
        }
        return accepted;
    }

    private Item findYuanItem(YuanKind kind) {
        Item cached = kind == YuanKind.TRUE ? cachedTrueYuanItem : cachedImmortalYuanItem;
        if (cached != null) return cached;
        Item fallback = null;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack candidate = new ItemStack(item);
            if (YuanItemPolicy.kindOf(candidate) == kind) {
                if (fallback == null) fallback = item;
                if (ImmortalStorageMod.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                    fallback = item;
                    break;
                }
            }
        }
        if (kind == YuanKind.TRUE) cachedTrueYuanItem = fallback;
        else cachedImmortalYuanItem = fallback;
        return fallback;
    }

    private boolean isVirtualInfiniteYuan(YuanKind kind) {
        return kind == YuanKind.IMMORTAL && isInfiniteImmortalYuan();
    }

    /** Removes up to the requested amount from active storage first, then the owner inventory. */
    private long removeYuanUpTo(YuanKind kind, long requested) {
        if (kind == null || requested <= 0L) return 0L;
        if (isVirtualInfiniteYuan(kind)) {
            return ResourceAmountPolicy.UNCHANGED.extractable(0L, requested);
        }
        long[] remaining = {requested};
        if (isStorageIsKongqiaoLegacy()) {
            removeYuanFromList(kongqiao, Math.min(kongqiaoMaxSlots, kongqiao.size()), kind, remaining);
        } else {
            batchXianqiaoMutations(() -> removeYuanFromXianqiao(kind, remaining));
        }
        if (owner != null && remaining[0] > 0L) {
            removeYuanFromList(owner.getInventory().items, owner.getInventory().items.size(), kind, remaining);
            removeYuanFromList(owner.getInventory().offhand, owner.getInventory().offhand.size(), kind, remaining);
            removeYuanFromList(owner.getInventory().armor, owner.getInventory().armor.size(), kind, remaining);
        }
        return requested - remaining[0];
    }

    /** Ascension repair must include both storage namespaces and every carried slot. */
    private long removeAllPhysicalYuan(YuanKind kind) {
        long[] remaining = {Long.MAX_VALUE};
        removeYuanFromList(kongqiao, kongqiao.size(), kind, remaining);
        batchXianqiaoMutations(() -> removeYuanFromXianqiao(kind, remaining));
        if (owner != null) {
            removeYuanFromList(owner.getInventory().items, owner.getInventory().items.size(), kind, remaining);
            removeYuanFromList(owner.getInventory().offhand, owner.getInventory().offhand.size(), kind, remaining);
            removeYuanFromList(owner.getInventory().armor, owner.getInventory().armor.size(), kind, remaining);
        }
        return Long.MAX_VALUE - remaining[0];
    }

    private static void removeYuanFromList(List<ItemStack> stacks, int limit, YuanKind kind, long[] remaining) {
        for (int slot = 0; slot < Math.min(limit, stacks.size()) && remaining[0] > 0L; slot++) {
            ItemStack stack = stacks.get(slot);
            if (YuanItemPolicy.kindOf(stack) != kind) continue;
            int removed = (int) Math.min(stack.getCount(), remaining[0]);
            stack.shrink(removed);
            if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
            remaining[0] -= removed;
        }
    }

    private void removeYuanFromXianqiao(YuanKind kind, long[] remaining) {
        for (int slot = 0; slot < xianqiaoStorage.size() && remaining[0] > 0L; slot++) {
            ItemStack stack = xianqiaoStorage.get(slot);
            if (YuanItemPolicy.kindOf(stack) != kind) continue;
            ItemStack identity = stack.copyWithCount(1);
            int removed = (int) Math.min(stack.getCount(), remaining[0]);
            stack.shrink(removed);
            remaining[0] -= removed;
            if (stack.isEmpty() && xianqiaoIndexValid) {
                removeSlotFromXianqiaoIndex(slot, identity);
                if (!xianqiaoEmptySlots.contains(slot)) xianqiaoEmptySlots.addLast(slot);
            }
            markXianqiaoStorageChanged();
        }
    }

    private void materializeLegacyYuanForCurrentStage() {
        if (yuanAccount.profile().trueYuan().enabled()) materializeLegacyYuan(YuanKind.TRUE);
        if (yuanAccount.profile().immortalYuan().enabled()) materializeLegacyYuan(YuanKind.IMMORTAL);
    }

    private void materializeLegacyYuan(YuanKind kind) {
        long pending = yuanAccount.drainLegacyBalance(kind);
        if (pending > 0L) enqueueDeferredMaterialization(kind, pending);
        advanceDeferredMaterialization(kind);
    }

    private void materializeConvertedTrueYuan(long trueAmount) {
        long immortalAmount = yuanAccount.convertTrueToImmortal(trueAmount);
        long accepted = materializeYuan(YuanKind.IMMORTAL, immortalAmount);
        if (immortalAmount > accepted) {
            enqueueDeferredDrop(YuanKind.IMMORTAL, immortalAmount - accepted);
        }
    }

    /** Advances all persisted migration/ejection liabilities with fixed per-kind work budgets. */
    void advanceDeferredYuanWork() {
        for (YuanKind kind : YuanKind.values()) advanceDeferredMaterialization(kind);
        enforceYuanCaps();
        for (YuanKind kind : YuanKind.values()) advanceDeferredDrop(kind);
    }

    private void advanceDeferredMaterialization(YuanKind kind) {
        long pending = deferredMaterialization(kind);
        if (pending <= 0L) return;
        if (isVirtualInfiniteYuan(kind)) {
            setDeferredMaterialization(kind, 0L);
            return;
        }

        Item item = findYuanItem(kind);
        int maxStack = item == null ? 1 : Math.max(1, new ItemStack(item).getMaxStackSize());
        long budget = (long) maxStack * MAX_YUAN_MATERIALIZATION_STACKS_PER_CALL;
        long attempted = Math.min(pending, budget);
        long accepted = materializeYuan(kind, attempted);
        long remaining = pending - accepted;
        setDeferredMaterialization(kind, remaining);

        // A partial attempt means capacity/storage cannot accept the liability.
        // Preserve the old ejection semantics, but enqueue it durably instead of
        // deleting everything beyond this tick's drop budget.
        if (remaining > 0L && (accepted < attempted || remainingYuanCapacity(kind) <= 0L)) {
            setDeferredMaterialization(kind, 0L);
            enqueueDeferredDrop(kind, remaining);
        }
    }

    private void advanceDeferredDrop(YuanKind kind) {
        long pending = deferredDrop(kind);
        if (pending <= 0L) return;
        long dropped = dropYuan(kind, pending);
        if (dropped > 0L) setDeferredDrop(kind, pending - dropped);
    }

    private void enqueueDeferredMaterialization(YuanKind kind, long amount) {
        if (amount <= 0L) return;
        setDeferredMaterialization(kind, saturatingAdd(deferredMaterialization(kind), amount));
    }

    private long drainDeferredMaterialization(YuanKind kind) {
        long pending = deferredMaterialization(kind);
        setDeferredMaterialization(kind, 0L);
        return pending;
    }

    private long deferredMaterialization(YuanKind kind) {
        return kind == YuanKind.TRUE
                ? deferredTrueYuanMaterialization : deferredImmortalYuanMaterialization;
    }

    private void setDeferredMaterialization(YuanKind kind, long amount) {
        if (kind == YuanKind.TRUE) deferredTrueYuanMaterialization = Math.max(0L, amount);
        else deferredImmortalYuanMaterialization = Math.max(0L, amount);
    }

    private void enqueueDeferredDrop(YuanKind kind, long amount) {
        if (amount <= 0L) return;
        setDeferredDrop(kind, saturatingAdd(deferredDrop(kind), amount));
    }

    private long deferredDrop(YuanKind kind) {
        return kind == YuanKind.TRUE ? deferredTrueYuanDrop : deferredImmortalYuanDrop;
    }

    private void setDeferredDrop(YuanKind kind, long amount) {
        if (kind == YuanKind.TRUE) deferredTrueYuanDrop = Math.max(0L, amount);
        else deferredImmortalYuanDrop = Math.max(0L, amount);
    }

    /** Ejects cap overflow into the world; it is never silently retained as a numeric balance. */
    private void enforceYuanCaps() {
        boolean changed = false;
        for (YuanKind kind : YuanKind.values()) {
            YuanRule rule = yuanAccount.profile().rule(kind);
            if (rule.unbounded()) continue;
            long total = countPhysicalYuan(kind);
            long cap = rule.enabled() ? rule.cap() : 0L;
            long excess = Math.max(0L, total - cap);
            if (excess <= 0L) continue;
            long removed = removeYuanUpTo(kind, excess);
            if (removed > 0L) {
                changed = true;
                enqueueDeferredDrop(kind, removed);
            }
        }
        if (changed) syncOwner();
    }

    private long dropYuan(YuanKind kind, long amount) {
        if (amount <= 0L || owner == null || owner.level().isClientSide) return 0L;
        Item item = findYuanItem(kind);
        if (item == null) return 0L;
        int maxStack = Math.max(1, new ItemStack(item).getMaxStackSize());
        long remaining = amount;
        int droppedStacks = 0;
        while (remaining > 0L && droppedStacks < MAX_YUAN_DROP_STACKS_PER_CALL) {
            int count = (int) Math.min(maxStack, remaining);
            owner.drop(new ItemStack(item, count), false);
            remaining -= count;
            droppedStacks++;
        }
        return amount - remaining;
    }

}
