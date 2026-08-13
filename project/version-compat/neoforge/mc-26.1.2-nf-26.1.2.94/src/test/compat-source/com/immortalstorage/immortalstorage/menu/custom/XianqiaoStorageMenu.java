package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.SmithingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalCraftingLayout;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalAction;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryCatalog;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidCatalog;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalExternalResourceEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalFluidStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalItemStorage;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageFluidHandler;
import com.immortalstorage.immortalstorage.network.storage.PersonalStorageLongItemStorage;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.immortalstorage.compat.TerminalExternalResourceCompatHooks;
import com.immortalstorage.immortalstorage.compat.XianqiaoInterfaceCompatHooks;
import com.immortalstorage.immortalstorage.api.storage.ExternalResourceStorage;
import com.immortalstorage.core.resource.ResourceChannelEntry;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceTransferAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

/** Server-authoritative aggregated Xianqiao storage terminal. */
public class XianqiaoStorageMenu extends AbstractContainerMenu implements StorageTerminalView, CraftingTransferTarget, SmithingTransferTarget {
    public static final int VISIBLE_ROWS = TerminalViewport.DEFAULT_ROWS;
    public static final int VISIBLE_COLS = TerminalViewport.COLUMNS;
    public static final int MAX_VISIBLE_ROWS = TerminalViewport.MAX_ROWS;
    public static final int MAX_BUFFERED_ROWS = TerminalViewport.MAX_BUFFERED_ROWS;
    public static final int BUFFERED_STORAGE_SLOTS = MAX_BUFFERED_ROWS * VISIBLE_COLS;
    public static final int VISIBLE_STORAGE_SLOTS = BUFFERED_STORAGE_SLOTS;
    public static final int TRIBULATION_SLOT = -1;
    public static final int AUTO_FURNACE_FUEL_BUTTON = 11;
    public static final int CRAFT_MATCH_COMPONENTS_BUTTON = 12;
    public static final int AUTO_FURNACE_FILL_BUTTON = 13;
    public static final int HAND_AUTO_REFILL_BUTTON = 14;
    public static final int SORT_PLAYER_INVENTORY_BUTTON = 15;
    public static final int DEPOSIT_PLAYER_INVENTORY_BUTTON = 16;
    public static final int WITHDRAW_FILTERED_BUTTON = 17;
    public static final int MAGNET_BUTTON = 18;
    public static final int CRAFT_START = VISIBLE_STORAGE_SLOTS;
    public static final int CRAFT_END = CRAFT_START + 9;
    public static final int CRAFT_RESULT_SLOT = CRAFT_END;
    public static final int SMITHING_START = CRAFT_RESULT_SLOT + 1;
    public static final int SMITHING_TEMPLATE_SLOT = SMITHING_START;
    public static final int SMITHING_BASE_SLOT = SMITHING_START + 1;
    public static final int SMITHING_ADDITION_SLOT = SMITHING_START + 2;
    public static final int SMITHING_RESULT_SLOT = SMITHING_START + 3;
    public static final int SMITHING_END = SMITHING_START + 4;
    public static final int FURNACE_START = SMITHING_END;
    public static final int FURNACE_INPUT_SLOT = FURNACE_START;
    public static final int FURNACE_FUEL_SLOT = FURNACE_INPUT_SLOT + 1;
    public static final int FURNACE_RESULT_SLOT = FURNACE_FUEL_SLOT + 1;
    public static final int FURNACE_INPUT_2_SLOT = FURNACE_RESULT_SLOT + 1;
    public static final int FURNACE_RESULT_2_SLOT = FURNACE_INPUT_2_SLOT + 1;
    public static final int FURNACE_INPUT_3_SLOT = FURNACE_RESULT_2_SLOT + 1;
    public static final int FURNACE_RESULT_3_SLOT = FURNACE_INPUT_3_SLOT + 1;
    public static final int FURNACE_PLUGIN_SLOT = FURNACE_RESULT_3_SLOT + 1;
    public static final int FURNACE_END = FURNACE_PLUGIN_SLOT + 1;
    public static final int ARMOR_START = FURNACE_END;
    public static final int ARMOR_END = ARMOR_START + 4;
    public static final int PLAYER_START = ARMOR_END;

    private static final int FURNACE_INPUT_X = 48;
    private static final int FURNACE_INPUT_Y = 129;
    private static final int FURNACE_FUEL_X = 8;
    private static final int FURNACE_FUEL_Y = 165;
    private static final int FURNACE_PLUGIN_Y = 129;
    private static final int FURNACE_RESULT_X = 134;
    private static final int FURNACE_RESULT_Y = 129;
    private static final int FURNACE_LANE_PITCH = 18;

    private final ImmortalStoragePlayerData data;
    private final AggregatedContainer storage;
    private final TerminalEntryCatalog catalog = new TerminalEntryCatalog();
    private final TerminalFluidCatalog fluidCatalog = new TerminalFluidCatalog();
    private final TerminalItemStorage itemStorage;
    private final PersonalStorageFluidHandler fluidStorage;
    private final ExternalResourceStorage externalResourceStorage;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final EmbeddedSmithingBackend smithing;
    private final EmbeddedImmortalFurnaceBackend furnace;
    private final Player player;
    private TerminalQuery terminalQuery = TerminalQuery.DEFAULT;
    private List<TerminalEntry> filteredEntries = List.of();
    private boolean filteredEntriesInitialized;
    private List<TerminalFluidEntry> filteredFluidEntries = List.of();
    private boolean filteredFluidEntriesInitialized;
    private List<TerminalExternalResourceEntry> filteredExternalEntries = List.of();
    private int activeModule = -1;
    private int visibleRows = VISIBLE_ROWS;
    private int baseRow;
    private int bufferBaseRow;
    private int clientSnapshotBufferBaseRow;
    private int clientRequestedBaseRow;
    private int clientTotalRows = -1;
    private int clientTotalItemEntries;
    private long clientRevision;
    private long clientFluidRevision;
    private List<TransferIngredient> clientRecipeSources = List.of();
    private final java.util.Map<Integer, List<TransferIngredient>> pendingRecipeSourceChunks = new java.util.HashMap<>();
    private long pendingRecipeSourceRevision = -1L;
    private int pendingRecipeSourceChunkCount;
    private long lastSnapshotRevision = -1L;
    private int lastSnapshotRows = -1;
    private int lastSnapshotBufferBaseRow = -1;
    private TerminalQuery lastSnapshotQuery;
    private long lastRecipeSourcesSnapshotRevision = -1L;
    private long lastFluidSnapshotRevision = -1L;
    private int lastFluidSnapshotRows = -1;
    private int lastFluidSnapshotBufferBaseRow = -1;
    private TerminalQuery lastFluidSnapshotQuery;
    private long lastObservedFluidStorageRevision = Long.MIN_VALUE;
    private long clientExternalRevision;
    private long lastExternalSnapshotRevision = -1L;
    private int clientTotalFluidEntries;
    private int clientTotalExternalEntries;

    public XianqiaoStorageMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player);
    }

    public XianqiaoStorageMenu(int id, Inventory inv, Player player) {
        super(ModMenus.XIANQIAO_STORAGE.get(), id);
        this.data = ImmortalStoragePlayerData.get(player);
        this.furnace = data.getEmbeddedImmortalFurnace();
        this.player = player;
        this.smithing = new EmbeddedSmithingBackend(this, player, data, true,
                SMITHING_RESULT_SLOT, this::extractCraftingIngredient);
        this.storage = new AggregatedContainer(this);
        net.minecraft.server.MinecraftServer server = player instanceof ServerPlayer serverPlayer
                ? com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(serverPlayer.level()) : null;
        this.itemStorage = new PersonalStorageLongItemStorage(data, this::invalidateItemSnapshot,
                () -> data.getStage() >= 6, server,
                com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player));
        this.fluidStorage = new PersonalStorageFluidHandler(data, this::invalidateFluidSnapshot,
                () -> data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE,
                server, com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(player));
        this.externalResourceStorage = new ExternalResourceStorage() {
            @Override public long revision() { return data.getExternalResourceRevision(); }

            @Override
            public List<ResourceChannelEntry> snapshot() {
                return data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_EXTERNAL_UNLOCK_STAGE
                        ? data.getExternalResourceEntries() : List.of();
            }

            @Override
            public long insert(com.immortalstorage.core.resource.ResourceChannelKey key,
                               long amount, ResourceTransferAction action) {
                return data.insertExternalResource(key, amount, action);
            }

            @Override
            public long extract(com.immortalstorage.core.resource.ResourceChannelKey key,
                                long amount, ResourceTransferAction action) {
                return data.extractExternalResource(key, amount, action);
            }
        };
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return activeModule + 1;
            }

            @Override
            public void set(int value) {
                activeModule = value - 1;
            }
        });
        this.addDataSlots(furnace.dataAccess());
        this.addDataSlot(new DataSlot() {
            @Override public int get() { return data.isCraftAutofillMatchComponents() ? 1 : 0; }
            @Override public void set(int value) { data.setCraftAutofillMatchComponents(value != 0); }
        });
        this.addDataSlot(new DataSlot() {
            @Override public int get() { return data.isHandAutoRefill() ? 1 : 0; }
            @Override public void set(int value) { data.setHandAutoRefill(value != 0); }
        });
        if (!player.level().isClientSide()) {
            rebuildCatalog();
            rebuildFluidCatalog();
            rebuildExternalCatalog();
        }

        for (int row = 0; row < MAX_BUFFERED_ROWS; row++) {
            for (int column = 0; column < VISIBLE_COLS; column++) {
                int viewIndex = row * VISIBLE_COLS + column;
                addSlot(new AggregatedSlot(storage, viewIndex, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new CraftModuleSlot(craftSlots, column + row * 3,
                        TerminalCraftingLayout.inputX(column),
                        TerminalCraftingLayout.inputY(TerminalCraftingLayout.MENU_BASELINE_IMAGE_HEIGHT, row)));
            }
        }
        addSlot(new CraftModuleResultSlot(player, craftSlots, resultSlots, 0,
                TerminalCraftingLayout.RESULT_X,
                TerminalCraftingLayout.resultY(TerminalCraftingLayout.MENU_BASELINE_IMAGE_HEIGHT)));
        addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.TEMPLATE, 30, 129));
        addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.BASE, 48, 129));
        addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.ADDITION, 66, 129));
        addSlot(new SmithingModuleResultSlot(smithing.result, 0, 120, 129));
        addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT,
                FURNACE_INPUT_X, FURNACE_INPUT_Y));
        addSlot(new FurnaceModuleFuelSlot(furnace, EmbeddedImmortalFurnaceBackend.FUEL,
                FURNACE_FUEL_X, FURNACE_FUEL_Y));
        addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT,
                FURNACE_RESULT_X, FURNACE_RESULT_Y));
        addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT_2,
                FURNACE_INPUT_X, FURNACE_INPUT_Y + FURNACE_LANE_PITCH));
        addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT_2,
                FURNACE_RESULT_X, FURNACE_RESULT_Y + FURNACE_LANE_PITCH));
        addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT_3,
                FURNACE_INPUT_X, FURNACE_INPUT_Y + FURNACE_LANE_PITCH * 2));
        addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT_3,
                FURNACE_RESULT_X, FURNACE_RESULT_Y + FURNACE_LANE_PITCH * 2));
        addSlot(new FurnaceModulePluginSlot(furnace, EmbeddedImmortalFurnaceBackend.PLUGIN,
                FURNACE_FUEL_X, FURNACE_PLUGIN_Y));

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        net.minecraft.resources.Identifier[] armorIcons = {
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
        };
        for (int row = 0; row < armorSlots.length; row++) {
            addSlot(new TerminalArmorSlot(inv, player, armorSlots[row], 39 - row,
                    8, 203 + row * 18, armorIcons[row]));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inv, column + row * 9 + 9, 30 + column * 18, 203 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inv, column, 30 + column * 18, 261));
        }
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            rebuildCatalog();
            rebuildFluidCatalog();
            rebuildExternalCatalog();
        }
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer
                && (catalog.revision() != lastSnapshotRevision
                || fluidCatalog.revision() != lastFluidSnapshotRevision
                || externalRevision() != lastExternalSnapshotRevision
                || visibleRows != lastSnapshotRows
                || bufferBaseRow != lastSnapshotBufferBaseRow || !terminalQuery.equals(lastSnapshotQuery)
                || (activeModule == 0 && catalog.revision() != lastRecipeSourcesSnapshotRevision))) {
            com.immortalstorage.immortalstorage.network.ModNetwork.sendTerminalSnapshot(serverPlayer, this);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player actor, int slotIndex) {
        if (!hasLiveTerminalAccess(actor)) return ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        // The client owns more than vanilla's 128-entry changed-slot packet budget.
        // Predicting a player-to-storage quick move rebuilds every aggregated proxy,
        // so leave this bounded vanilla QUICK_MOVE request entirely to the server.
        if (actor.level().isClientSide() && slotIndex >= PLAYER_START) return ItemStack.EMPTY;
        if (slotIndex < VISIBLE_STORAGE_SLOTS) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (slotIndex == CRAFT_RESULT_SLOT) {
            source.onCraftedBy(actor, source.getCount());
            if (!moveItemStackTo(source, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(source, original);
        } else if (slotIndex >= CRAFT_START && slotIndex <= CRAFT_RESULT_SLOT) {
            if (!moveItemStackTo(source, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (isSmithingSlotIndex(slotIndex)) {
            if (!isSmithingVisible() || !moveItemStackTo(source, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (isFurnaceSlotIndex(slotIndex)) {
            if (!isFurnaceVisible()) return ItemStack.EMPTY;
            if (!moveItemStackTo(source, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
            if (isFurnaceResultSlotIndex(slotIndex)) slot.onQuickCraft(source, original);
        } else if (slotIndex >= PLAYER_START) {
            if (isSmithingVisible() && moveItemStackToSmithingInput(source)) {
                // Inserted into the first compatible empty smithing slot.
            } else if (isFurnaceVisible()
                    && com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(source)) {
                if (!moveItemStackTo(source, FURNACE_PLUGIN_SLOT, FURNACE_PLUGIN_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFurnaceVisible() && furnace.isFuel(source)) {
                if (!moveItemStackTo(source, FURNACE_FUEL_SLOT, FURNACE_FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFurnaceVisible() && furnace.isRecipeInput(actor, source)) {
                if (!moveItemStackToFurnaceInputs(source)) {
                    return ItemStack.EMPTY;
                }
            } else {
                ItemStack leftover = TerminalMenuSupport.insertXianqiao(data, source);
                source.setCount(leftover.getCount());
                rebuildCatalog();
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(actor, source);
        if (slotIndex == CRAFT_RESULT_SLOT && !source.isEmpty()) {
            actor.drop(source, false);
        }
        return original;
    }

    private boolean moveItemStackToFurnaceInputs(ItemStack stack) {
        int before = stack.getCount();
        int[] inputs = {FURNACE_INPUT_SLOT, FURNACE_INPUT_2_SLOT, FURNACE_INPUT_3_SLOT};
        for (int input : inputs) {
            if (stack.isEmpty()) break;
            moveItemStackTo(stack, input, input + 1, false);
        }
        return stack.getCount() < before;
    }

    private boolean moveItemStackToSmithingInput(ItemStack stack) {
        for (int menuSlot = SMITHING_TEMPLATE_SLOT; menuSlot <= SMITHING_ADDITION_SLOT; menuSlot++) {
            int input = menuSlot - SMITHING_START;
            if (!slots.get(menuSlot).hasItem() && smithing.accepts(input, stack)) {
                return moveItemStackTo(stack, menuSlot, menuSlot + 1, false);
            }
        }
        return false;
    }

    public static boolean isSmithingSlotIndex(int slotIndex) {
        return slotIndex >= SMITHING_START && slotIndex < SMITHING_END;
    }

    public static boolean isFurnaceSlotIndex(int slotIndex) {
        return slotIndex >= FURNACE_START && slotIndex < FURNACE_END;
    }

    public static boolean isFurnaceResultSlotIndex(int slotIndex) {
        return slotIndex == FURNACE_RESULT_SLOT
                || slotIndex == FURNACE_RESULT_2_SLOT
                || slotIndex == FURNACE_RESULT_3_SLOT;
    }

    public static boolean isFurnaceInputSlotIndex(int slotIndex) {
        return slotIndex == FURNACE_INPUT_SLOT
                || slotIndex == FURNACE_INPUT_2_SLOT
                || slotIndex == FURNACE_INPUT_3_SLOT;
    }

    public static int furnaceChannelForSlot(int slotIndex) {
        if (slotIndex == FURNACE_INPUT_SLOT || slotIndex == FURNACE_RESULT_SLOT) return 0;
        if (slotIndex == FURNACE_INPUT_2_SLOT || slotIndex == FURNACE_RESULT_2_SLOT) return 1;
        if (slotIndex == FURNACE_INPUT_3_SLOT || slotIndex == FURNACE_RESULT_3_SLOT) return 2;
        return -1;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player actor) {
        if (!hasLiveTerminalAccess(actor)) return;
        if (slotId >= 0 && slotId < VISIBLE_STORAGE_SLOTS) {
            // Aggregated proxy slots are operated only through TerminalEntryAction.
            // Ignoring vanilla container clicks prevents stale client slot indices from becoming authority.
            return;
        }
        if (isFurnaceSlotIndex(slotId) && !isFurnaceVisible()) {
            return;
        }
        if (isSmithingSlotIndex(slotId) && !isSmithingVisible()) return;
        super.clicked(slotId, button, clickType, actor);
    }

    public boolean handleEntryAction(ServerPlayer actor, long expectedRevision, long entryId, TerminalAction action) {
        if (!hasLiveTerminalAccess(actor)) return false;
        rebuildCatalog();
        if (expectedRevision > catalog.revision() || action == null) return false;
        if (entryId == 0L && (action == TerminalAction.INSERT_CARRIED || action == TerminalAction.INSERT_ONE)) {
            return hasDisplayedEmptySlot() && insertCarriedAny(action == TerminalAction.INSERT_ONE);
        }
        TerminalEntry entry = displayedEntry(entryId);
        if (entry == null) return false;
        return switch (action) {
            case PICKUP_STACK -> extractToCarried(actor, entry, entry.displayStack().getMaxStackSize());
            case PICKUP_ONE -> extractToCarried(actor, entry, 1);
            case QUICK_MOVE_TO_PLAYER -> !quickMoveEntry(actor, entry).isEmpty();
            case INSERT_CARRIED -> insertCarried(entry, false);
            case INSERT_ONE -> insertCarried(entry, true);
        };
    }

    public boolean hasCurrentRevision(long expectedRevision) {
        rebuildCatalog();
        return expectedRevision == catalog.revision();
    }

    /**
     * Executes one AE-style fluid-container interaction against the exact
     * visible fluid directory revision. Container discovery and conversion are
     * delegated to NeoForge's item fluid capability and FluidUtil; no bucket
     * item class is special-cased here.
     */
    public boolean handleFluidContainerAction(ServerPlayer actor, long expectedRevision,
                                               long entryId, boolean deposit) {
        if (!hasLiveFluidAccess(actor)) return false;
        rebuildFluidCatalog();
        if (expectedRevision > fluidCatalog.revision()) return false;

        ItemStack carried = getCarried();
        if (carried.isEmpty() || FluidUtil.getFluidHandler(carried.copyWithCount(1)).isEmpty()) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    "message.immortalstorage.terminal.fluid_container_required"), true);
            return false;
        }

        TerminalFluidEntry selected = entryId == 0L ? null : displayedFluidEntry(entryId);
        if (entryId != 0L && selected == null) return false;
        if (deposit) {
            Optional<FluidStack> contained = FluidUtil.getFluidContained(carried);
            if (contained.isEmpty()) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                        "message.immortalstorage.terminal.fluid_deposit_mismatch"), true);
                return false;
            }
            return finishFluidContainerTransfer(actor, true, fluidStorage, null);
        }

        if (selected == null || FluidUtil.getFluidContained(carried).isPresent()) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    "message.immortalstorage.terminal.fluid_empty_container_required"), true);
            return false;
        }
        return finishFluidContainerTransfer(actor, false, fluidStorage, selected.key());
    }

    /** Executes one Mekanism chemical-container interaction against the external directory. */
    public boolean handleExternalResourceContainerAction(ServerPlayer actor, long expectedRevision,
                                                         long entryId, boolean deposit) {
        if (!hasLiveExternalAccess(actor)) return false;
        rebuildExternalCatalog();
        if (expectedRevision > data.getExternalResourceRevision()) return false;

        ItemStack carried = getCarried();
        if (carried.isEmpty() || !TerminalExternalResourceCompatHooks.isContainer(carried)) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    "message.immortalstorage.terminal.chemical_container_required"), true);
            return false;
        }

        TerminalExternalResourceEntry selected = entryId == 0L ? null : displayedExternalEntry(entryId);
        if (entryId != 0L && selected == null) return false;
        Optional<XianqiaoInterfaceCompatHooks.ContainedExternalResource> contained =
                XianqiaoInterfaceCompatHooks.containedExternalResource(carried);
        if (deposit) {
            if (contained.isEmpty()) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                        "message.immortalstorage.terminal.chemical_deposit_mismatch"), true);
                return false;
            }
            return finishExternalResourceContainerTransfer(actor, true, null);
        }

        if (selected == null || !ExternalResourceChannels.MEKANISM_CHEMICAL_CHANNEL
                .equals(selected.key().channel())) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    "message.immortalstorage.terminal.chemical_entry_required"), true);
            return false;
        }
        if (contained.isPresent()) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    "message.immortalstorage.terminal.chemical_empty_container_required"), true);
            return false;
        }
        return finishExternalResourceContainerTransfer(actor, false, selected.key());
    }

    private boolean finishExternalResourceContainerTransfer(
            ServerPlayer actor, boolean deposit,
            com.immortalstorage.core.resource.ResourceChannelKey selectedKey) {
        ItemStack carried = getCarried();
        PlayerInvWrapper inventory = new PlayerInvWrapper(actor.getInventory());
        TerminalExternalResourceCompatHooks.TransferResult result = deposit
                ? TerminalExternalResourceCompatHooks.depositToStorage(
                        carried, externalResourceStorage, inventory)
                : TerminalExternalResourceCompatHooks.withdrawFromStorage(
                        carried, selectedKey, externalResourceStorage, inventory);
        if (!result.handled() || !result.success()) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    result.failure() == TerminalExternalResourceCompatHooks.Failure.EXECUTE_FAILED
                            ? "message.immortalstorage.terminal.chemical_transfer_failed"
                            : "message.immortalstorage.terminal.chemical_transfer_no_space"), true);
            return false;
        }
        setCarried(result.carried());
        rebuildExternalCatalog();
        broadcastChanges();
        return true;
    }

    private boolean finishFluidContainerTransfer(ServerPlayer actor, boolean deposit,
                                                  TerminalFluidStorage storage,
                                                  TerminalFluidKey selectedKey) {
        ItemStack carried = getCarried();
        PlayerInvWrapper inventory = new PlayerInvWrapper(actor.getInventory());
        TerminalFluidContainerTransfer.Result result = deposit
                ? TerminalFluidContainerTransfer.depositToStorage(carried, storage, inventory)
                : TerminalFluidContainerTransfer.withdrawFromStorage(
                        carried, storage, selectedKey, inventory);
        if (!result.success()) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(actor, net.minecraft.network.chat.Component.translatable(
                    result.failure() == TerminalFluidContainerTransfer.Failure.EXECUTE_FAILED
                            ? "message.immortalstorage.terminal.fluid_transfer_failed"
                            : "message.immortalstorage.terminal.fluid_transfer_no_space"), true);
            return false;
        }
        setCarried(result.carried());
        rebuildFluidCatalog();
        invalidateFluidSnapshot();
        broadcastChanges();
        return true;
    }

    private TerminalFluidEntry displayedFluidEntry(long entryId) {
        int itemCount = filteredEntries.size();
        int from = Math.min(filteredFluidEntries.size(), Math.max(0, baseRow * VISIBLE_COLS - itemCount));
        int bufferEnd = bufferBaseRow + bufferedRowCount();
        int interactiveEnd = Math.max(baseRow, Math.min(baseRow + visibleRows + 1, bufferEnd));
        int to = Math.min(filteredFluidEntries.size(), Math.max(0, interactiveEnd * VISIBLE_COLS - itemCount));
        for (int index = from; index < to; index++) {
            TerminalFluidEntry entry = filteredFluidEntries.get(index);
            if (entry.entryId() == entryId) return entry;
        }
        return null;
    }

    private boolean extractToCarried(Player actor, TerminalEntry entry, int amount) {
        ItemStack carried = getCarried();
        ItemStack prototype = entry.displayStack();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, prototype)) return false;
        int room = carried.isEmpty() ? prototype.getMaxStackSize() : carried.getMaxStackSize() - carried.getCount();
        int requested = Math.min(amount, room);
        if (requested <= 0) return false;
        ItemStack extracted = extractFromUnifiedStorage(prototype, requested);
        if (extracted.isEmpty()) return false;
        if (carried.isEmpty()) setCarried(extracted);
        else carried.grow(extracted.getCount());
        rebuildCatalog();
        broadcastChanges();
        return true;
    }

    private boolean insertCarried(TerminalEntry entry, boolean one) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return false;
        int amount = one ? 1 : carried.getCount();
        ItemStack offered = carried.copyWithCount(amount);
        ItemStack leftover = TerminalMenuSupport.insertXianqiao(data, offered);
        int inserted = amount - leftover.getCount();
        if (inserted <= 0) return false;
        carried.shrink(inserted);
        rebuildCatalog();
        broadcastChanges();
        return true;
    }

    private boolean insertCarriedAny(boolean one) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return false;
        int amount = one ? 1 : carried.getCount();
        ItemStack leftover = TerminalMenuSupport.insertXianqiao(data, carried.copyWithCount(amount));
        int inserted = amount - leftover.getCount();
        if (inserted <= 0) return false;
        carried.shrink(inserted);
        rebuildCatalog();
        broadcastChanges();
        return true;
    }

    private ItemStack quickMoveEntry(Player actor, TerminalEntry entry) {
        ItemStack result = data.batchXianqiaoMutations(() -> {
            ItemStack extracted = extractFromUnifiedStorage(
                    entry.displayStack(), entry.displayStack().getMaxStackSize());
            if (extracted.isEmpty()) return ItemStack.EMPTY;
            ItemStack moving = extracted.copy();
            moveItemStackTo(moving, PLAYER_START, slots.size(), true);
            int moved = extracted.getCount() - moving.getCount();
            if (!moving.isEmpty()) TerminalMenuSupport.insertXianqiao(data, moving);
            return moved <= 0 ? ItemStack.EMPTY : extracted.copyWithCount(moved);
        });
        if (!result.isEmpty()) {
            rebuildCatalog();
            broadcastChanges();
        }
        return result;
    }

    public void setViewport(int requestedRows, int requestedBaseRow) {
        int nextRows = TerminalViewport.clampRows(requestedRows);
        int nextBase = player.level().isClientSide()
                ? Math.max(0, requestedBaseRow)
                : TerminalViewport.clampBaseRow(requestedBaseRow, nextRows, getTotalRows());
        if (nextRows == visibleRows && nextBase == baseRow) return;
        visibleRows = nextRows;
        baseRow = nextBase;
        if (player.level().isClientSide()) clientRequestedBaseRow = nextBase;
        if (!player.level().isClientSide()) ensureBufferCoversViewport();
        if (!player.level().isClientSide()) broadcastChanges();
    }

    public void setTerminalQuery(TerminalQuery query) {
        TerminalQuery next = query == null ? TerminalQuery.DEFAULT : query;
        if (next.equals(terminalQuery)) return;
        terminalQuery = next;
        if (player.level().isClientSide()) return;
        baseRow = 0;
        if (!rebuildCatalog()) rebuildFilteredEntries(false);
        if (!rebuildFluidCatalog()) rebuildFilteredFluidEntries(false);
        rebuildExternalCatalog();
        resetBufferWindow();
        broadcastChanges();
    }

    public ImmortalStoragePlayerData getData() { return data; }
    public Container getStorageContainer() { return storage; }
    public void setActiveModule(int module) {
        activeModule = module >= 0 && module <= 3 ? module : -1;
        broadcastChanges();
    }
    public int getActiveModule() { return activeModule; }
    /** Compatibility facade: item and fluid entries now always share one directory page. */
    public boolean isFluidChannel() { return false; }
    public boolean isItemChannel() { return true; }
    static boolean isStorageProxyActive(boolean fluidChannel, int viewIndex, int bufferedRows) {
        return viewIndex >= 0 && viewIndex < bufferedRows * VISIBLE_COLS;
    }
    static boolean isStorageProxyViewportActive(int viewIndex, int bufferedBaseRow, int bufferedRows,
                                                int baseRow, int visibleRows) {
        if (!isStorageProxyActive(false, viewIndex, bufferedRows)) return false;
        long absoluteRow = (long) Math.max(0, bufferedBaseRow) + viewIndex / VISIBLE_COLS;
        long firstVisibleRow = Math.max(0, baseRow);
        long endExclusive = firstVisibleRow + TerminalViewport.maxIntersectingRows(visibleRows);
        return absoluteRow >= firstVisibleRow && absoluteRow < endExclusive;
    }
    static int combinedDirectoryRows(int itemEntries, int fluidEntries, int externalEntries) {
        long total = (long) Math.max(0, itemEntries) + Math.max(0, fluidEntries)
                + Math.max(0, externalEntries);
        return (int) Math.min(Integer.MAX_VALUE,
                (total + VISIBLE_COLS - 1L) / VISIBLE_COLS);
    }
    static int combinedItemIndex(int bufferedBaseRow, int viewIndex, int itemEntries) {
        long absolute = (long) Math.max(0, bufferedBaseRow) * VISIBLE_COLS + Math.max(0, viewIndex);
        return absolute < Math.max(0, itemEntries) ? (int) absolute : -1;
    }
    static int combinedFluidIndex(int bufferedBaseRow, int viewIndex, int itemEntries, int fluidEntries) {
        long absolute = (long) Math.max(0, bufferedBaseRow) * VISIBLE_COLS + Math.max(0, viewIndex);
        long fluidIndex = absolute - Math.max(0, itemEntries);
        return fluidIndex >= 0L && fluidIndex < Math.max(0, fluidEntries) ? (int) fluidIndex : -1;
    }
    static int combinedExternalIndex(int bufferedBaseRow, int viewIndex, int itemEntries,
                                     int fluidEntries, int externalEntries) {
        long absolute = (long) Math.max(0, bufferedBaseRow) * VISIBLE_COLS + Math.max(0, viewIndex);
        long index = absolute - Math.max(0, itemEntries) - Math.max(0, fluidEntries);
        return index >= 0L && index < Math.max(0, externalEntries) ? (int) index : -1;
    }
    public boolean isFurnaceVisible() { return activeModule == 2; }
    public boolean isSmithingUnlocked() { return data.getStage() >= 4; }
    public boolean isSmithingVisible() { return activeModule == 3 && isSmithingUnlocked(); }
    public boolean isFurnaceLit() { return furnace.isLit(); }
    public boolean isFurnaceAutoConsume() { return furnace.isAutoConsume(); }
    public boolean isFurnaceAutoFill() { return furnace.isAutoFill(); }
    public boolean isCraftAutofillMatchComponents() { return data.isCraftAutofillMatchComponents(); }
    public boolean isHandAutoRefill() { return data.isHandAutoRefill(); }
    public int getFurnaceLitProgress() { return furnace.litProgress(); }
    public int getFurnaceBurnProgress() { return getFurnaceBurnProgress(0); }
    public int getFurnaceBurnProgress(int channel) { return furnace.burnProgress(channel); }
    public int getFurnaceData(int index) { return furnace.data(index); }

    public void applyClientActiveModule(int module) {
        if (player.level().isClientSide()) {
            activeModule = module >= 0 && module <= 3 ? module : -1;
        }
    }

    public void setTerminalChannel(boolean fluid) {
        // Protocol-v2 clients may still send this button action. Keep it as a
        // harmless adapter while the v3 terminal presents one combined page.
    }

    public void applyClientTerminalChannel(boolean fluid) {
        // No-op compatibility adapter; there is no client-side channel state.
    }

    @Override
    public boolean clickMenuButton(Player actor, int buttonId) {
        if (!hasLiveTerminalAccess(actor)) return false;
        if (buttonId == 10) {
            if (!isCraftingVisible() || !isCraftingUnlocked()) return false;
            data.batchXianqiaoMutations(() ->
                    TerminalMenuSupport.returnCraftingItems(this, actor, craftSlots, data, true));
            slotsChanged(craftSlots);
            rebuildCatalog();
            return true;
        }
        if (buttonId == AUTO_FURNACE_FUEL_BUTTON) {
            if (!isFurnaceVisible() || !isCraftingUnlocked()) return false;
            furnace.setAutoConsume(!furnace.isAutoConsume());
            broadcastChanges();
            return true;
        }
        if (buttonId == CRAFT_MATCH_COMPONENTS_BUTTON) {
            if (!isCraftingVisible() || !isCraftingUnlocked()) return false;
            data.setCraftAutofillMatchComponents(!data.isCraftAutofillMatchComponents());
            broadcastChanges();
            return true;
        }
        if (buttonId == AUTO_FURNACE_FILL_BUTTON) {
            if (!isFurnaceVisible() || !isCraftingUnlocked()) return false;
            furnace.setAutoFill(!furnace.isAutoFill());
            broadcastChanges();
            return true;
        }
        if (buttonId == HAND_AUTO_REFILL_BUTTON) {
            if (activeModule != 1) return false;
            data.setHandAutoRefill(!data.isHandAutoRefill());
            broadcastChanges();
            return true;
        }
        if (buttonId == SORT_PLAYER_INVENTORY_BUTTON) {
            sortPlayerMainInventory();
            broadcastChanges();
            return true;
        }
        if (buttonId == DEPOSIT_PLAYER_INVENTORY_BUTTON) {
            depositPlayerInventory();
            return true;
        }
        if (buttonId == WITHDRAW_FILTERED_BUTTON) {
            withdrawFilteredEntries();
            return true;
        }
        if (buttonId == MAGNET_BUTTON) {
            if (activeModule != 1) return false;
            data.setMagnetEnabled(!data.isMagnetEnabled());
            broadcastChanges();
            return true;
        }
        if (buttonId < 0 || buttonId > 3) return false;
        if (buttonId == 3 && !isSmithingUnlocked()) return false;
        setActiveModule(activeModule == buttonId ? -1 : buttonId);
        return true;
    }

    private void sortPlayerMainInventory() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = PLAYER_START; slot < PLAYER_START + 27; slot++) {
            ItemStack current = slots.get(slot).getItem();
            if (!current.isEmpty()) stacks.add(current.copy());
            slots.get(slot).set(ItemStack.EMPTY);
        }
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            for (ItemStack target : merged) {
                if (stack.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(target, stack)) continue;
                int moved = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
                if (moved > 0) {
                    target.grow(moved);
                    stack.shrink(moved);
                }
            }
            if (!stack.isEmpty()) merged.add(stack);
        }
        merged.sort(Comparator
                .comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .thenComparing(stack -> stack.getComponentsPatch().toString()));
        for (int index = 0; index < merged.size() && index < 27; index++) {
            slots.get(PLAYER_START + index).set(merged.get(index));
        }
    }

    private void depositPlayerInventory() {
        data.batchXianqiaoMutations(() -> {
            for (int slot = PLAYER_START; slot < slots.size(); slot++) {
                ItemStack source = slots.get(slot).getItem();
                if (source.isEmpty()) continue;
                slots.get(slot).set(TerminalMenuSupport.insertXianqiao(data, source.copy()));
            }
        });
        rebuildCatalog();
        broadcastChanges();
    }

    private void withdrawFilteredEntries() {
        rebuildCatalog();
        List<TerminalEntry> ordered = List.copyOf(filteredEntries);
        data.batchXianqiaoMutations(() -> {
            for (TerminalEntry entry : ordered) {
                while (true) {
                    ItemStack extracted = extractFromUnifiedStorage(
                            entry.displayStack(), entry.displayStack().getMaxStackSize());
                    if (extracted.isEmpty()) break;
                    ItemStack moving = extracted.copy();
                    moveItemStackTo(moving, PLAYER_START, slots.size(), false);
                    if (!moving.isEmpty()) TerminalMenuSupport.insertXianqiao(data, moving);
                    if (moving.getCount() == extracted.getCount()) return;
                }
            }
        });
        rebuildCatalog();
        broadcastChanges();
    }
    public int getVisibleRows() { return visibleRows; }
    public int getBaseRow() { return baseRow; }
    public int getTotalRows() {
        return player.level().isClientSide() && clientTotalRows >= 0
                ? clientTotalRows
                : combinedDirectoryRows(filteredEntries.size(), filteredFluidEntries.size(),
                        filteredExternalEntries.size());
    }

    public int totalItemEntries() {
        return player.level().isClientSide() ? clientTotalItemEntries : filteredEntries.size();
    }

    public List<TerminalEntry> bufferedEntries() {
        int bufferStart = bufferBaseRow * VISIBLE_COLS;
        int bufferEnd = bufferStart + bufferedRowCount() * VISIBLE_COLS;
        int from = Math.min(filteredEntries.size(), bufferStart);
        int to = Math.min(filteredEntries.size(), bufferEnd);
        return List.copyOf(filteredEntries.subList(from, to));
    }

    public void applyClientSnapshot(long revision, int rows, int snapshotBaseRow, int snapshotBufferBaseRow,
                                    int totalRows, int totalItemEntries, List<TerminalEntry> entries) {
        if (!player.level().isClientSide()) return;
        visibleRows = TerminalViewport.clampRows(rows);
        clientSnapshotBufferBaseRow = Math.max(0, snapshotBufferBaseRow);
        clientTotalRows = Math.max(0, totalRows);
        clientTotalItemEntries = Math.max(0, totalItemEntries);
        int requestedBase = TerminalViewport.clampBaseRow(clientRequestedBaseRow, visibleRows, clientTotalRows);
        int bufferEnd = clientSnapshotBufferBaseRow + bufferedRowCount();
        baseRow = requestedBase >= clientSnapshotBufferBaseRow
                && requestedBase + visibleRows <= bufferEnd
                ? requestedBase : TerminalViewport.clampBaseRow(snapshotBaseRow, visibleRows, clientTotalRows);
        clientRequestedBaseRow = baseRow;
        clientRevision = Math.max(0L, revision);
        filteredEntries = entries == null ? List.of() : List.copyOf(entries);
        filteredEntriesInitialized = true;
    }

    public void applyClientFluidSnapshot(long revision, int rows, int snapshotBaseRow, int snapshotBufferBaseRow,
                                         int totalRows, int totalItemEntries, int totalFluidEntries,
                                         List<TerminalFluidEntry> entries) {
        if (!player.level().isClientSide()) return;
        visibleRows = TerminalViewport.clampRows(rows);
        clientSnapshotBufferBaseRow = Math.max(0, snapshotBufferBaseRow);
        clientTotalRows = Math.max(0, totalRows);
        clientTotalItemEntries = Math.max(0, totalItemEntries);
        clientTotalFluidEntries = Math.max(0, totalFluidEntries);
        int requestedBase = TerminalViewport.clampBaseRow(clientRequestedBaseRow, visibleRows, clientTotalRows);
        int bufferEnd = clientSnapshotBufferBaseRow + bufferedRowCount();
        baseRow = requestedBase >= clientSnapshotBufferBaseRow
                && requestedBase + visibleRows <= bufferEnd
                ? requestedBase : TerminalViewport.clampBaseRow(snapshotBaseRow, visibleRows, clientTotalRows);
        clientRequestedBaseRow = baseRow;
        clientFluidRevision = Math.max(0L, revision);
        filteredFluidEntries = entries == null ? List.of() : List.copyOf(entries);
        filteredFluidEntriesInitialized = true;
    }

    public void applyClientExternalSnapshot(long revision, int totalExternalEntries,
                                            List<TerminalExternalResourceEntry> entries) {
        if (!player.level().isClientSide()) return;
        clientExternalRevision = Math.max(0L, revision);
        clientTotalExternalEntries = Math.max(0, totalExternalEntries);
        filteredExternalEntries = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override public void slotsChanged(Container changed) {
        if (changed == craftSlots) refreshCraftingResult(null);
    }

    @Override public void removed(Player actor) {
        super.removed(actor);
        data.batchXianqiaoMutations(() -> {
            TerminalMenuSupport.returnCraftingItems(this, actor, craftSlots, data, true);
            smithing.returnInputs();
        });
        if (!actor.level().isClientSide()) rebuildCatalog();
    }

    public boolean hasLiveTerminalAccess(Player actor) {
        return actor != null
                && actor == player
                && actor.containerMenu == this
                && data.getStage() >= 6
                && ImmortalStoragePlayerData.get(actor) == data;
    }

    public boolean hasLiveFluidAccess(Player actor) {
        return hasLiveTerminalAccess(actor)
                && data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_FLUID_UNLOCK_STAGE;
    }

    public boolean hasLiveExternalAccess(Player actor) {
        return hasLiveTerminalAccess(actor)
                && data.getStage() >= ImmortalStoragePlayerData.XIANQIAO_EXTERNAL_UNLOCK_STAGE;
    }

    @Override public boolean stillValid(Player actor) { return hasLiveTerminalAccess(actor); }

    private void refreshCraftingResult(RecipeHolder<CraftingRecipe> lastRecipe) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(serverPlayer.level()).getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, serverPlayer.level(), lastRecipe);
        if (recipe.isPresent() && resultSlots.setRecipeUsed(serverPlayer, recipe.get())) {
            ItemStack assembled = recipe.get().value().assemble(input);
            if (assembled.isItemEnabled(serverPlayer.level().enabledFeatures())) result = assembled;
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(CRAFT_RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), CRAFT_RESULT_SLOT, result));
    }

    private boolean rebuildCatalog() {
        boolean catalogChanged = catalog.rebuildSummariesIfStale(
                itemStorage.snapshot(), itemStorage.revision(), List.of());
        if (catalogChanged || !filteredEntriesInitialized) {
            rebuildFilteredEntries(true);
            return true;
        }
        return false;
    }

    private void rebuildFilteredEntries(boolean adjustViewport) {
        filteredEntries = catalog.entries(terminalQuery);
        filteredEntriesInitialized = true;
        if (adjustViewport) {
            baseRow = TerminalViewport.clampBaseRow(baseRow, visibleRows, getTotalRows());
            ensureBufferCoversViewport();
        }
    }

    private boolean rebuildFluidCatalog() {
        long storageRevision = fluidStorage.revision();
        if (filteredFluidEntriesInitialized && storageRevision == lastObservedFluidStorageRevision) {
            return false;
        }
        lastObservedFluidStorageRevision = storageRevision;
        boolean catalogChanged = fluidCatalog.rebuildIfStale(fluidStorage.snapshot(), storageRevision);
        if (catalogChanged || !filteredFluidEntriesInitialized) {
            rebuildFilteredFluidEntries(true);
            return true;
        }
        return false;
    }

    private void rebuildFilteredFluidEntries(boolean adjustViewport) {
        filteredFluidEntries = fluidCatalog.entries(terminalQuery);
        filteredFluidEntriesInitialized = true;
        if (adjustViewport) {
            baseRow = TerminalViewport.clampBaseRow(baseRow, visibleRows, getTotalRows());
            ensureBufferCoversViewport();
        }
    }

    private void rebuildExternalCatalog() {
        if (player.level().isClientSide()) return;
        if (data.getStage() < ImmortalStoragePlayerData.XIANQIAO_EXTERNAL_UNLOCK_STAGE) {
            filteredExternalEntries = List.of();
            return;
        }
        String search = terminalQuery.normalizedText();
        Comparator<TerminalExternalResourceEntry> comparator = switch (terminalQuery.sortOrder()) {
            case AMOUNT -> Comparator.comparingLong(TerminalExternalResourceEntry::amount);
            case NAME, MOD_ID -> Comparator.comparing(entry -> entry.key().resourceId());
        };
        if (terminalQuery.sortDirection() == TerminalQuery.SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }
        filteredExternalEntries = data.getExternalResourceEntries().stream()
                .filter(entry -> entry.amount() > 0L && ExternalResourceCatalog.contains(entry.key()))
                .filter(entry -> search.isEmpty()
                        || entry.key().channel().toLowerCase(java.util.Locale.ROOT).contains(search)
                        || entry.key().resourceId().toLowerCase(java.util.Locale.ROOT).contains(search)
                        || ExternalResourceCatalog.displayName(entry.key()).getString()
                        .toLowerCase(java.util.Locale.ROOT).contains(search))
                .map(entry -> new TerminalExternalResourceEntry(externalEntryId(entry), entry.key(), entry.amount()))
                .sorted(comparator.thenComparing(entry -> entry.key().channel()))
                .toList();
    }

    private static long externalEntryId(ResourceChannelEntry entry) {
        long id = ((long) entry.key().channel().hashCode() << 32)
                ^ Integer.toUnsignedLong(entry.key().resourceId().hashCode()) ^ 0x4558545245534f55L;
        return id == 0L ? 1L : id;
    }

    private TerminalEntry entryAtViewIndex(int viewIndex) {
        int itemCount = totalItemEntries();
        int absolute = combinedItemIndex(bufferedBaseRow(), viewIndex, itemCount);
        if (absolute < 0) return null;
        int index = player.level().isClientSide()
                ? absolute - Math.min(itemCount, clientSnapshotBufferBaseRow * VISIBLE_COLS)
                : absolute;
        return index >= 0 && index < filteredEntries.size() ? filteredEntries.get(index) : null;
    }

    public TerminalEntry displayedEntryAtSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= bufferedRowCount() * VISIBLE_COLS) return null;
        int snapshotBase = bufferedBaseRow();
        int absoluteRow = snapshotBase + slotIndex / VISIBLE_COLS;
        int snapshotEnd = snapshotBase + bufferedRowCount();
        if (absoluteRow < baseRow || absoluteRow > baseRow + visibleRows
                || absoluteRow < snapshotBase || absoluteRow >= snapshotEnd) return null;
        return entryAtViewIndex(slotIndex);
    }

    /**
     * Returns the complete aggregated amount represented by a storage proxy slot.
     *
     * <p>The proxy {@link ItemStack} is intentionally kept at count one because an
     * {@code ItemStack} count is an {@code int} and is not a safe carrier for the
     * terminal's {@code long} amount. Client screens must use this method (or the
     * corresponding {@link AggregatedSlot#amount()}) for count overlays.</p>
     */
    public long aggregatedAmountAtSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= bufferedRowCount() * VISIBLE_COLS) return 0L;
        TerminalEntry entry = entryAtViewIndex(slotIndex);
        return entry == null ? 0L : entry.amount();
    }

    private TerminalFluidEntry fluidEntryAtViewIndex(int viewIndex) {
        int itemCount = totalItemEntries();
        int knownFluidCount = player.level().isClientSide()
                ? inferredClientFluidEntryCount() : filteredFluidEntries.size();
        int fluidAbsolute = combinedFluidIndex(fluidBufferedBaseRow(), viewIndex,
                itemCount, knownFluidCount);
        if (fluidAbsolute < 0) return null;
        int firstBufferedFluid = Math.max(0, fluidBufferedBaseRow() * VISIBLE_COLS - itemCount);
        int index = player.level().isClientSide() ? fluidAbsolute - firstBufferedFluid : fluidAbsolute;
        return index >= 0 && index < filteredFluidEntries.size() ? filteredFluidEntries.get(index) : null;
    }

    public TerminalFluidEntry displayedFluidEntryAtIndex(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= bufferedRowCount() * VISIBLE_COLS) return null;
        int snapshotBase = fluidBufferedBaseRow();
        int absoluteRow = snapshotBase + viewIndex / VISIBLE_COLS;
        int snapshotEnd = snapshotBase + bufferedRowCount();
        if (absoluteRow < baseRow || absoluteRow > baseRow + visibleRows
                || absoluteRow < snapshotBase || absoluteRow >= snapshotEnd) return null;
        return fluidEntryAtViewIndex(viewIndex);
    }

    public long fluidAmountAtIndex(int viewIndex) {
        TerminalFluidEntry entry = fluidEntryAtViewIndex(viewIndex);
        return entry == null ? 0L : entry.amountMb();
    }

    public List<TerminalFluidEntry> bufferedFluidEntries() {
        int itemCount = filteredEntries.size();
        int bufferStart = bufferBaseRow * VISIBLE_COLS;
        int bufferEnd = bufferStart + bufferedRowCount() * VISIBLE_COLS;
        int from = Math.min(filteredFluidEntries.size(), Math.max(0, bufferStart - itemCount));
        int to = Math.min(filteredFluidEntries.size(), Math.max(0, bufferEnd - itemCount));
        return List.copyOf(filteredFluidEntries.subList(from, to));
    }

    public int fluidBufferedBaseRow() {
        return bufferedBaseRow();
    }

    public long fluidRevision() {
        return player.level().isClientSide() ? clientFluidRevision : fluidCatalog.revision();
    }

    public boolean hasCurrentFluidRevision(long expectedRevision) {
        if (player.level().isClientSide()) return expectedRevision == clientFluidRevision;
        rebuildFluidCatalog();
        return expectedRevision == fluidCatalog.revision();
    }

    private TerminalEntry displayedEntry(long entryId) {
        for (TerminalEntry entry : interactiveEntries()) {
            if (entry.entryId() == entryId) return entry;
        }
        return null;
    }

    private boolean hasDisplayedEmptySlot() {
        int interactiveRows = Math.max(0, interactiveEndRowExclusive() - baseRow);
        int from = Math.min(totalDirectoryEntries(), baseRow * VISIBLE_COLS);
        int to = Math.min(totalDirectoryEntries(), interactiveEndRowExclusive() * VISIBLE_COLS);
        return Math.max(0, to - from) < interactiveRows * VISIBLE_COLS;
    }

    private int totalDirectoryEntries() {
        return totalItemEntries() + (player.level().isClientSide()
                ? clientTotalFluidEntries + clientTotalExternalEntries
                : filteredFluidEntries.size() + filteredExternalEntries.size());
    }

    private int inferredClientFluidEntryCount() {
        return clientTotalFluidEntries;
    }

    private TerminalExternalResourceEntry displayedExternalEntry(long entryId) {
        int slots = bufferedRowCount() * VISIBLE_COLS;
        for (int index = 0; index < slots; index++) {
            TerminalExternalResourceEntry entry = displayedExternalEntryAtIndex(index);
            if (entry != null && entry.entryId() == entryId) return entry;
        }
        return null;
    }

    public TerminalExternalResourceEntry displayedExternalEntryAtIndex(int viewIndex) {
        int itemCount = totalItemEntries();
        int fluidCount = player.level().isClientSide() ? clientTotalFluidEntries : filteredFluidEntries.size();
        int externalCount = player.level().isClientSide() ? clientTotalExternalEntries : filteredExternalEntries.size();
        int absolute = combinedExternalIndex(bufferedBaseRow(), viewIndex,
                itemCount, fluidCount, externalCount);
        if (absolute < 0) return null;
        int firstBuffered = Math.max(0, bufferedBaseRow() * VISIBLE_COLS - itemCount - fluidCount);
        int index = player.level().isClientSide() ? absolute - firstBuffered : absolute;
        return index >= 0 && index < filteredExternalEntries.size() ? filteredExternalEntries.get(index) : null;
    }

    public List<TerminalExternalResourceEntry> bufferedExternalEntries() {
        int itemCount = filteredEntries.size();
        int fluidCount = filteredFluidEntries.size();
        int bufferStart = bufferBaseRow * VISIBLE_COLS;
        int bufferEnd = bufferStart + bufferedRowCount() * VISIBLE_COLS;
        int from = Math.min(filteredExternalEntries.size(), Math.max(0, bufferStart - itemCount - fluidCount));
        int to = Math.min(filteredExternalEntries.size(), Math.max(0, bufferEnd - itemCount - fluidCount));
        return List.copyOf(filteredExternalEntries.subList(from, to));
    }

    public int totalFluidEntries() {
        return player.level().isClientSide() ? clientTotalFluidEntries : filteredFluidEntries.size();
    }

    public int totalExternalEntries() {
        return player.level().isClientSide() ? clientTotalExternalEntries : filteredExternalEntries.size();
    }

    public long externalRevision() {
        return player.level().isClientSide() ? clientExternalRevision : data.getExternalResourceRevision();
    }

    /** Current rows plus the clipped bottom row, never outside the sent 2R buffer. */
    private List<TerminalEntry> interactiveEntries() {
        int from = Math.min(filteredEntries.size(), baseRow * VISIBLE_COLS);
        int to = Math.min(filteredEntries.size(), interactiveEndRowExclusive() * VISIBLE_COLS);
        return from <= to ? List.copyOf(filteredEntries.subList(from, to)) : List.of();
    }

    private int interactiveEndRowExclusive() {
        int bufferEnd = bufferBaseRow + bufferedRowCount();
        return Math.max(baseRow, Math.min(baseRow + visibleRows + 1, bufferEnd));
    }

    @Override public TerminalViewport viewport() {
        return new TerminalViewport(visibleRows, visibleRows, baseRow, getTotalRows(),
                player.level().isClientSide() ? clientRevision : catalog.revision());
    }
    @Override public TerminalQuery query() { return terminalQuery; }
    @Override public int bufferedBaseRow() {
        return player.level().isClientSide() ? clientSnapshotBufferBaseRow : bufferBaseRow;
    }
    @Override public int bufferedRowCount() { return TerminalViewport.bufferedRows(visibleRows); }
    @Override public List<TerminalEntry> visibleEntries() {
        if (player.level().isClientSide()) {
            int viewStart = baseRow * VISIBLE_COLS;
            int firstBufferedItem = Math.min(clientTotalItemEntries,
                    clientSnapshotBufferBaseRow * VISIBLE_COLS);
            int from = Math.max(0, viewStart - firstBufferedItem);
            int to = Math.min(filteredEntries.size(),
                    Math.max(from, Math.min(clientTotalItemEntries, viewStart + visibleRows * VISIBLE_COLS)
                            - firstBufferedItem));
            return from <= to ? List.copyOf(filteredEntries.subList(from, to)) : List.of();
        }
        int from = Math.min(filteredEntries.size(), baseRow * VISIBLE_COLS);
        int to = Math.min(filteredEntries.size(), from + visibleRows * VISIBLE_COLS);
        return List.copyOf(filteredEntries.subList(from, to));
    }
    @Override public int storageSlotStart() { return 0; }
    @Override public int storageSlotCount() { return BUFFERED_STORAGE_SLOTS; }
    @Override public int craftingSlotStart() { return CRAFT_START; }
    @Override public int craftingResultSlot() { return CRAFT_RESULT_SLOT; }
    @Override public int playerInventoryStart() { return PLAYER_START; }
    @Override public boolean isCraftingUnlocked() { return data.getStage() >= 6; }
    @Override public boolean isCraftingVisible() { return activeModule == 0; }
    @Override public List<Slot> craftingInputSlots() { return List.copyOf(slots.subList(CRAFT_START, CRAFT_END)); }
    @Override public Slot craftingResultSlotView() { return slots.get(CRAFT_RESULT_SLOT); }
    @Override public List<Slot> craftingSourceSlots() {
        List<Slot> result = new ArrayList<>();
        result.addAll(slots.subList(PLAYER_START, slots.size()));
        return List.copyOf(result);
    }
    @Override public List<TransferIngredient> craftingStorageIngredients() {
        if (player.level().isClientSide()) return clientRecipeSources;
        rebuildCatalog();
        return filteredEntries.stream()
                .map(entry -> new TransferIngredient(entry.displayStack(), entry.amount()))
                .toList();
    }
    public List<TransferIngredient> serverCraftingStorageIngredients() {
        if (player.level().isClientSide()) return List.of();
        rebuildCatalog();
        return catalog.entries(TerminalQuery.DEFAULT).stream()
                .map(entry -> new TransferIngredient(entry.displayStack(), entry.amount()))
                .toList();
    }
    public boolean shouldSendRecipeSources(long revision) {
        return !player.level().isClientSide() && activeModule == 0
                && revision != lastRecipeSourcesSnapshotRevision;
    }
    public void markTerminalSnapshotSent(long revision, boolean recipeSourcesSent) {
        if (player.level().isClientSide()) return;
        lastSnapshotRevision = revision;
        lastSnapshotRows = visibleRows;
        lastSnapshotBufferBaseRow = bufferBaseRow;
        lastSnapshotQuery = terminalQuery;
        if (recipeSourcesSent) lastRecipeSourcesSnapshotRevision = revision;
    }
    public void markFluidTerminalSnapshotSent(long revision) {
        if (player.level().isClientSide()) return;
        lastFluidSnapshotRevision = revision;
        lastFluidSnapshotRows = visibleRows;
        lastFluidSnapshotBufferBaseRow = bufferBaseRow;
        lastFluidSnapshotQuery = terminalQuery;
    }
    public void markExternalTerminalSnapshotSent(long revision) {
        if (!player.level().isClientSide()) lastExternalSnapshotRevision = revision;
    }
    @Override public void applyRecipeSourceChunk(long sourceRevision, int chunkIndex, int chunkCount,
                                                 List<TransferIngredient> entries) {
        if (!player.level().isClientSide() || (clientRevision != 0L && sourceRevision != clientRevision)
                || chunkCount < 1 || chunkIndex < 0 || chunkIndex >= chunkCount) return;
        if (sourceRevision != pendingRecipeSourceRevision || chunkCount != pendingRecipeSourceChunkCount) {
            pendingRecipeSourceChunks.clear();
            pendingRecipeSourceRevision = sourceRevision;
            pendingRecipeSourceChunkCount = chunkCount;
        }
        pendingRecipeSourceChunks.put(chunkIndex, List.copyOf(entries));
        if (pendingRecipeSourceChunks.size() != chunkCount) return;
        List<TransferIngredient> complete = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            List<TransferIngredient> part = pendingRecipeSourceChunks.get(i);
            if (part == null) return;
            complete.addAll(part);
        }
        clientRecipeSources = List.copyOf(complete);
        pendingRecipeSourceChunks.clear();
    }
    @Override public boolean transferCraftingRecipe(RecipeHolder<CraftingRecipe> recipe,
                                                     int requestedSets, long expectedRevision) {
        if (!hasLiveTerminalAccess(player)) return false;
        boolean transferred = TerminalRecipeTransfer.place(this, player, data, craftSlots, recipe,
                requestedSets, expectedRevision, catalog.revision(), true);
        if (transferred) rebuildCatalog();
        return transferred;
    }
    @Override public List<Slot> smithingInputSlots() { return List.copyOf(slots.subList(SMITHING_START, SMITHING_RESULT_SLOT)); }
    @Override public Slot smithingResultSlotView() { return slots.get(SMITHING_RESULT_SLOT); }
    @Override public List<Slot> smithingSourceSlots() { return craftingSourceSlots(); }
    @Override public List<TransferIngredient> smithingStorageIngredients() { return craftingStorageIngredients(); }
    @Override public boolean transferSmithingRecipe(RecipeHolder<SmithingRecipe> recipe, long expectedRevision) {
        if (!(player instanceof ServerPlayer serverPlayer) || !hasLiveTerminalAccess(player) || !isSmithingVisible()) return false;
        boolean transferred = TerminalSmithingTransfer.place(serverPlayer, smithing, recipe,
                expectedRevision, catalog.revision(), serverCraftingStorageIngredients(), this::extractCraftingIngredient);
        if (transferred) rebuildCatalog();
        return transferred;
    }

    private final class AggregatedContainer implements Container {
        private final XianqiaoStorageMenu menu;
        private AggregatedContainer(XianqiaoStorageMenu menu) { this.menu = menu; }
        @Override public int getContainerSize() { return BUFFERED_STORAGE_SLOTS; }
        @Override public boolean isEmpty() { return filteredEntries.isEmpty(); }
        @Override public ItemStack getItem(int index) {
            TerminalEntry entry = menu.entryAtViewIndex(index);
            // This stack is a render/click identity only. The authoritative count is
            // a long exposed separately by AggregatedSlot#amount(); encoding it in
            // ItemStack would truncate at Integer.MAX_VALUE (and previously at 64).
            return storageProxyDisplayStack(player.level().isClientSide(), entry);
        }
        @Override public ItemStack removeItem(int index, int count) {
            if (!hasLiveTerminalAccess(player)) return ItemStack.EMPTY;
            TerminalEntry entry = menu.entryAtViewIndex(index);
            return entry == null ? ItemStack.EMPTY : extractFromUnifiedStorage(entry.displayStack(), count);
        }
        @Override public ItemStack removeItemNoUpdate(int index) { return removeItem(index, Integer.MAX_VALUE); }
        @Override public void setItem(int index, ItemStack stack) { }
        @Override public void setChanged() { rebuildCatalog(); }
        @Override public boolean stillValid(Player actor) { return true; }
        @Override public void clearContent() { }
    }

    public final class AggregatedSlot extends Slot {
        private final int viewIndex;
        private AggregatedSlot(Container container, int viewIndex, int x, int y) {
            super(container, viewIndex, x, y);
            this.viewIndex = viewIndex;
        }
        public TerminalEntry entry() { return entryAtViewIndex(viewIndex); }
        public long amount() {
            TerminalEntry entry = entry();
            return entry == null ? 0L : entry.amount();
        }
        @Override public boolean isActive() {
            return isStorageProxyViewportActive(viewIndex, bufferedBaseRow(), bufferedRowCount(),
                    baseRow, visibleRows);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player actor) { return false; }
        @Override public void set(ItemStack stack) { }
        @Override public ItemStack remove(int amount) { return ItemStack.EMPTY; }
    }

    private final class CraftModuleSlot extends Slot {
        private CraftModuleSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return isCraftingVisible(); }
    }

    private final class CraftModuleResultSlot extends ResultSlot {
        private CraftModuleResultSlot(Player actor, CraftingContainer crafting, Container result, int index, int x, int y) {
            super(actor, crafting, result, index, x, y);
        }
        @Override public boolean isActive() { return isCraftingVisible(); }

        @Override
        public void onTake(Player actor, ItemStack stack) {
            List<ItemStack> before = TerminalMenuSupport.snapshotCrafting(craftSlots);
            super.onTake(actor, stack);
            data.batchXianqiaoMutations(() -> TerminalMenuSupport.refillCraftingAfterTake(
                    craftSlots, before, data.isCraftAutofillMatchComponents(),
                    XianqiaoStorageMenu.this::extractCraftingIngredient));
            slotsChanged(craftSlots);
            rebuildCatalog();
            broadcastChanges();
        }
    }

    private class SmithingModuleSlot extends Slot {
        private SmithingModuleSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return isSmithingVisible(); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && smithing.accepts(index, stack); }
    }

    private final class SmithingModuleResultSlot extends Slot {
        private SmithingModuleResultSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return isSmithingVisible(); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player actor) { return isActive() && smithing.mayTake(); }
        @Override public void onTake(Player actor, ItemStack stack) {
            smithing.onTake(actor, stack);
            rebuildCatalog();
            super.onTake(actor, stack);
        }
    }

    static ItemStack storageProxyDisplayStack(boolean clientSide, TerminalEntry entry) {
        return !clientSide || entry == null ? ItemStack.EMPTY : entry.displayStack();
    }

    private class FurnaceModuleSlot extends Slot {
        private FurnaceModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean isActive() { return isFurnaceVisible(); }
    }

    private final class FurnaceModuleInputSlot extends FurnaceModuleSlot {
        private FurnaceModuleInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isActive() && furnace.isRecipeInput(player, stack);
        }
    }

    private final class FurnaceModuleFuelSlot extends FurnaceModuleSlot {
        private FurnaceModuleFuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return isActive() && furnace.isFuel(stack); }
    }

    private final class FurnaceModulePluginSlot extends FurnaceModuleSlot {
        private FurnaceModulePluginSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isActive() && com.immortalstorage.immortalstorage.block.entity.ReinforcementPluginHost.isPlugin(stack);
        }

        @Override public int getMaxStackSize() { return 1; }
    }

    private final class FurnaceModuleResultSlot extends FurnaceModuleSlot {
        private FurnaceModuleResultSlot(Player actor, Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public void onTake(Player actor, ItemStack stack) {
            super.onTake(actor, stack);
            furnace.awardUsedRecipes(actor);
        }
    }

    private void invalidateFluidSnapshot() {
        lastFluidSnapshotRevision = -1L;
    }

    private void invalidateItemSnapshot() {
        lastSnapshotRevision = -1L;
    }

    private ItemStack extractFromUnifiedStorage(ItemStack prototype, int requested) {
        if (prototype == null || prototype.isEmpty() || requested <= 0) return ItemStack.EMPTY;
        long extracted = itemStorage.extract(
                com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey.of(prototype),
                requested,
                com.immortalstorage.immortalstorage.api.storage.terminal.TerminalStorageAction.EXECUTE);
        return extracted <= 0L ? ItemStack.EMPTY
                : prototype.copyWithCount((int) Math.min(requested, extracted));
    }

    private ItemStack extractCraftingIngredient(ItemStack prototype, int amount, boolean matchComponents) {
        ItemStack selected = prototype;
        if (!matchComponents) {
            selected = itemStorage.snapshot().stream()
                    .map(com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary::prototype)
                    .filter(candidate -> candidate.is(prototype.getItem()))
                    .filter(candidate -> ItemStack.isSameItemSameComponents(candidate, prototype))
                    .findFirst()
                    .orElseGet(() -> itemStorage.snapshot().stream()
                            .map(com.immortalstorage.immortalstorage.api.storage.terminal.StorageItemSummary::prototype)
                            .filter(candidate -> candidate.is(prototype.getItem()))
                            .findFirst().orElse(ItemStack.EMPTY));
        }
        return selected.isEmpty() ? ItemStack.EMPTY : extractFromUnifiedStorage(selected, amount);
    }

    private void resetBufferWindow() {
        bufferBaseRow = TerminalViewport.recenterBufferBase(baseRow, visibleRows, getTotalRows());
    }

    private void ensureBufferCoversViewport() {
        if (player.level().isClientSide()) return;
        bufferBaseRow = TerminalViewport.ensureBufferBase(bufferBaseRow, baseRow, visibleRows, getTotalRows());
    }
}
