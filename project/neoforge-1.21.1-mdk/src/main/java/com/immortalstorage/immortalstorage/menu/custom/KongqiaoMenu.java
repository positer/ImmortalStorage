package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.SmithingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalCraftingLayout;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntry;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalEntryKey;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kongqiao personal storage opened with the B key.
 *
 * Slot layout reserves twelve proxy rows so later stages can resize without
 * rebuilding the menu. The active viewport is still capped to the finite
 * stage's physical row count (with the shared two-row UI minimum).
 */
public class KongqiaoMenu extends AbstractContainerMenu implements StorageTerminalView, CraftingTransferTarget, SmithingTransferTarget {
    public static final int CRAFT_MATCH_COMPONENTS_BUTTON = 12;
    public static final int AUTO_FURNACE_FUEL_BUTTON = 11;
    public static final int AUTO_FURNACE_FILL_BUTTON = 13;
    public static final int MAGNET_BUTTON = 14;
    public static final int VISIBLE_ROWS = TerminalViewport.DEFAULT_ROWS;
    public static final int VISIBLE_COLS = 9;
    public static final int MAX_VISIBLE_ROWS = TerminalViewport.MAX_ROWS;
    public static final int VISIBLE_STORAGE_SLOTS = MAX_VISIBLE_ROWS * VISIBLE_COLS;
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
    public static final int FURNACE_END = FURNACE_RESULT_3_SLOT + 1;
    public static final int ARMOR_START = FURNACE_END;
    public static final int ARMOR_END = ARMOR_START + 4;
    public static final int PLAYER_START = ARMOR_END;
    private static final int FURNACE_INPUT_X = 48;
    private static final int FURNACE_INPUT_Y = 129;
    private static final int FURNACE_FUEL_X = 8;
    private static final int FURNACE_FUEL_Y = 147;
    private static final int FURNACE_RESULT_X = 134;
    private static final int FURNACE_RESULT_Y = 129;
    private static final int FURNACE_LANE_PITCH = 18;

    private final KongqiaoStorageContainer kongqiao;
    private final ImmortalStoragePlayerData data;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final EmbeddedSmithingBackend smithing;
    private final EmbeddedImmortalFurnaceBackend furnace;
    private final Player player;
    private int activeModule = -1;
    private int visibleRows = VISIBLE_ROWS;
    private int baseRow;
    private long revision = 1L;
    private List<TransferIngredient> clientRecipeSources = List.of();
    private final Map<Integer, List<TransferIngredient>> pendingRecipeSourceChunks = new java.util.HashMap<>();
    private long pendingRecipeSourceRevision = -1L;
    private int pendingRecipeSourceChunkCount;
    private long lastRecipeSourceRevision = -1L;
    private int lastRecipeSourceFingerprint;
    private int currentStorageFingerprint = Integer.MIN_VALUE;

    public KongqiaoMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player);
    }

    public KongqiaoMenu(int id, Inventory inv, Player player) {
        super(ModMenus.KONGQIAO.get(), id);
        this.data = ImmortalStoragePlayerData.get(player);
        this.furnace = data.getEmbeddedImmortalFurnace();
        this.kongqiao = new KongqiaoStorageContainer(data);
        this.player = player;
        this.smithing = new EmbeddedSmithingBackend(this, player, data, false,
                SMITHING_RESULT_SLOT, this::extractCraftingIngredient);
        this.visibleRows = clampViewportRows(VISIBLE_ROWS, getTotalRows());
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

        for (int r = 0; r < MAX_VISIBLE_ROWS; r++) {
            for (int c = 0; c < VISIBLE_COLS; c++) {
                int viewSlot = r * VISIBLE_COLS + c;
                this.addSlot(new KongqiaoSlot(kongqiao, this, viewSlot, 8 + c * 18, 18 + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new CraftModuleSlot(craftSlots, c + r * 3,
                        TerminalCraftingLayout.inputX(c),
                        TerminalCraftingLayout.inputY(TerminalCraftingLayout.MENU_BASELINE_IMAGE_HEIGHT, r)));
            }
        }
        this.addSlot(new CraftModuleResultSlot(player, craftSlots, resultSlots, 0,
                TerminalCraftingLayout.RESULT_X,
                TerminalCraftingLayout.resultY(TerminalCraftingLayout.MENU_BASELINE_IMAGE_HEIGHT)));
        this.addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.TEMPLATE, 30, 129));
        this.addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.BASE, 48, 129));
        this.addSlot(new SmithingModuleSlot(smithing.inputs, EmbeddedSmithingBackend.ADDITION, 66, 129));
        this.addSlot(new SmithingModuleResultSlot(smithing.result, 0, 120, 129));
        this.addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT,
                FURNACE_INPUT_X, FURNACE_INPUT_Y));
        this.addSlot(new FurnaceModuleFuelSlot(furnace, EmbeddedImmortalFurnaceBackend.FUEL,
                FURNACE_FUEL_X, FURNACE_FUEL_Y));
        this.addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT,
                FURNACE_RESULT_X, FURNACE_RESULT_Y));
        this.addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT_2,
                FURNACE_INPUT_X, FURNACE_INPUT_Y + FURNACE_LANE_PITCH));
        this.addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT_2,
                FURNACE_RESULT_X, FURNACE_RESULT_Y + FURNACE_LANE_PITCH));
        this.addSlot(new FurnaceModuleInputSlot(furnace, EmbeddedImmortalFurnaceBackend.INPUT_3,
                FURNACE_INPUT_X, FURNACE_INPUT_Y + FURNACE_LANE_PITCH * 2));
        this.addSlot(new FurnaceModuleResultSlot(player, furnace, EmbeddedImmortalFurnaceBackend.RESULT_3,
                FURNACE_RESULT_X, FURNACE_RESULT_Y + FURNACE_LANE_PITCH * 2));

        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET
        };
        net.minecraft.resources.ResourceLocation[] armorIcons = {
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
                net.minecraft.world.inventory.InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
        };
        for (int row = 0; row < 4; row++) {
            this.addSlot(new TerminalArmorSlot(inv, player, armorSlots[row], 39 - row,
                    8, 203 + row * 18, armorIcons[row]));
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inv, c + r * 9 + 9, 30 + c * 18, 203 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 30 + c * 18, 261));
        }
    }

    @Override public void broadcastChanges() {
        if ((activeModule == 0 && !isCraftingUnlocked())
                || (activeModule == 1 && !isSmithingUnlocked())
                || (activeModule == 2 && !isFurnaceUnlocked())) {
            activeModule = -1;
        }
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer) sendRecipeSourcesIfChanged(serverPlayer);
    }

    private void sendRecipeSourcesIfChanged(ServerPlayer serverPlayer) {
        List<TransferIngredient> sources = serverCraftingStorageIngredients();
        int fingerprint = 1;
        for (TransferIngredient source : sources) {
            fingerprint = 31 * fingerprint + ItemStack.hashItemAndComponents(source.stack());
            fingerprint = 31 * fingerprint + Long.hashCode(source.amount());
        }
        if (fingerprint != currentStorageFingerprint) {
            currentStorageFingerprint = fingerprint;
            revision++;
        }
        if (revision == lastRecipeSourceRevision && fingerprint == lastRecipeSourceFingerprint) return;
        lastRecipeSourceRevision = revision;
        lastRecipeSourceFingerprint = fingerprint;
        com.immortalstorage.immortalstorage.network.ModNetwork.sendRecipeSources(serverPlayer, this, sources, revision);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        if (slot < 0 || slot >= this.slots.size()) return ItemStack.EMPTY;
        ItemStack ret = ItemStack.EMPTY;
        Slot s = this.slots.get(slot);
        if (isGuardedSlot(slot) && !s.isActive()) return ItemStack.EMPTY;
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            ret = in.copy();
            final int kongqiaoEnd = VISIBLE_STORAGE_SLOTS;
            final int playerStart = PLAYER_START;
            final int playerEnd = this.slots.size();

            if (slot == CRAFT_RESULT_SLOT) {
                in.getItem().onCraftedBy(in, p.level(), p);
                if (!this.moveItemStackTo(in, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
                s.onQuickCraft(in, ret);
            } else if (slot < kongqiaoEnd) {
                if (!this.moveItemStackTo(in, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= CRAFT_START && slot <= CRAFT_RESULT_SLOT) {
                if (!this.moveItemStackTo(in, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isSmithingSlotIndex(slot)) {
                if (!isSmithingVisible() || !this.moveItemStackTo(in, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFurnaceSlotIndex(slot)) {
                if (!isFurnaceVisible() || !this.moveItemStackTo(in, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
                if (isFurnaceResultSlotIndex(slot)) s.onQuickCraft(in, ret);
            } else if (slot >= playerStart && slot < playerEnd) {
                if (isSmithingVisible() && moveItemStackToSmithingInput(in)) {
                    // The active smithing recipe determines the first compatible empty input.
                } else if (isFurnaceVisible() && furnace.isFuel(in)) {
                    if (!this.moveItemStackTo(in, FURNACE_FUEL_SLOT, FURNACE_FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isFurnaceVisible() && furnace.isRecipeInput(p, in)) {
                    if (!moveItemStackToFurnaceInputs(in)) return ItemStack.EMPTY;
                } else {
                    ItemStack leftover = TerminalMenuSupport.insertKongqiao(data, in);
                    in.setCount(leftover.getCount());
                    if (in.getCount() == ret.getCount()
                            && (this.activeModule != 0 || !this.moveItemStackTo(in, CRAFT_START, CRAFT_END, false))) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (in.isEmpty()) {
                s.set(ItemStack.EMPTY);
            } else {
                s.setChanged();
            }
            if (in.getCount() == ret.getCount()) {
                return ItemStack.EMPTY;
            }
            s.onTake(p, in);
            if (slot == CRAFT_RESULT_SLOT && !in.isEmpty()) {
                p.drop(in, false);
            }
        }
        return ret;
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
            if (!this.slots.get(menuSlot).hasItem() && smithing.accepts(input, stack)) {
                return this.moveItemStackTo(stack, menuSlot, menuSlot + 1, false);
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
    public void clicked(int slotId, int button, ClickType clickType, Player actor) {
        if (slotId >= 0 && slotId < PLAYER_START && !this.slots.get(slotId).isActive()) {
            return;
        }
        if (slotId >= 0 && slotId < VISIBLE_STORAGE_SLOTS) {
            Slot storageSlot = this.slots.get(slotId);
            ItemStack stored = storageSlot.getItem();
            if (clickType == ClickType.PICKUP && this.getCarried().isEmpty() && !stored.isEmpty()) {
                int requested = button == 1 ? 1 : Math.min(stored.getMaxStackSize(), stored.getCount());
                ItemStack extracted = storageSlot.remove(requested);
                if (!extracted.isEmpty()) {
                    this.setCarried(extracted);
                    storageSlot.onTake(actor, extracted);
                    this.broadcastChanges();
                }
                return;
            }
            if (clickType == ClickType.PICKUP && !stored.isEmpty()
                    && stored.getCount() > stored.getMaxStackSize()
                    && !this.getCarried().isEmpty()
                    && !ItemStack.isSameItemSameComponents(stored, this.getCarried())) {
                // Never swap an extended Kongqiao stack onto the cursor; the
                // player inventory cannot legally persist that stack size.
                return;
            }
            if (clickType == ClickType.THROW && this.getCarried().isEmpty() && !stored.isEmpty()) {
                int requested = button == 0 ? 1 : Math.min(stored.getMaxStackSize(), stored.getCount());
                ItemStack extracted = storageSlot.remove(requested);
                if (!extracted.isEmpty()) {
                    storageSlot.onTake(actor, extracted);
                    actor.drop(extracted, true);
                    this.broadcastChanges();
                }
                return;
            }
            if (clickType == ClickType.SWAP && !stored.isEmpty()
                    && stored.getCount() > stored.getMaxStackSize()) {
                if (button >= 0 && button < 9 && actor.getInventory().getItem(button).isEmpty()) {
                    ItemStack extracted = storageSlot.remove(stored.getMaxStackSize());
                    actor.getInventory().setItem(button, extracted);
                    storageSlot.onTake(actor, extracted);
                    this.broadcastChanges();
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, actor);
    }

    private static boolean isGuardedSlot(int slotId) {
        return slotId >= 0 && slotId < PLAYER_START;
    }

    @Override
    public boolean stillValid(Player p) {
        int stage = data.getStage();
        return stage >= 1 && stage < 6;
    }

    public ImmortalStoragePlayerData getData() {
        return data;
    }

    public KongqiaoStorageContainer getKongqiaoContainer() {
        return kongqiao;
    }

    public void setActiveModule(int activeModule) {
        this.activeModule = switch (activeModule) {
            case 0 -> isCraftingUnlocked() ? 0 : -1;
            case 1 -> isSmithingUnlocked() ? 1 : -1;
            case 2 -> isFurnaceUnlocked() ? 2 : -1;
            default -> -1;
        };
        this.broadcastChanges();
    }

    public int getActiveModule() {
        return activeModule;
    }

    public static boolean isFurnaceUnlockedAtStage(int stage) { return stage >= 5 && stage < 6; }
    public boolean isSmithingUnlocked() { return data.getStage() >= 4; }
    public boolean isSmithingVisible() { return activeModule == 1 && isSmithingUnlocked(); }
    public boolean isFurnaceUnlocked() { return isFurnaceUnlockedAtStage(data.getStage()); }
    public boolean isFurnaceVisible() { return activeModule == 2 && isFurnaceUnlocked(); }
    public boolean isFurnaceLit() { return furnace.isLit(); }
    public boolean isFurnaceAutoConsume() { return furnace.isAutoConsume(); }
    public boolean isFurnaceAutoFill() { return furnace.isAutoFill(); }
    public int getFurnaceLitProgress() { return furnace.litProgress(); }
    public int getFurnaceBurnProgress(int channel) { return furnace.burnProgress(channel); }

    public void applyClientActiveModule(int module) {
        if (player.level().isClientSide()) {
            activeModule = switch (module) {
                case 0 -> isCraftingUnlocked() ? 0 : -1;
                case 1 -> isSmithingUnlocked() ? 1 : -1;
                case 2 -> isFurnaceUnlocked() ? 2 : -1;
                default -> -1;
            };
        }
    }

    @Override
    public boolean clickMenuButton(Player actor, int buttonId) {
        if (buttonId == 10) {
            if (activeModule != 0 || !isCraftingUnlocked()) return false;
            TerminalMenuSupport.returnCraftingItems(this, actor, craftSlots, data, false);
            slotsChanged(craftSlots);
            return true;
        }
        if (buttonId == CRAFT_MATCH_COMPONENTS_BUTTON) {
            if (activeModule != 0 || !isCraftingUnlocked()) return false;
            data.setCraftAutofillMatchComponents(!data.isCraftAutofillMatchComponents());
            broadcastChanges();
            return true;
        }
        if (buttonId == AUTO_FURNACE_FUEL_BUTTON) {
            if (!isFurnaceVisible()) return false;
            furnace.setAutoConsume(!furnace.isAutoConsume());
            broadcastChanges();
            return true;
        }
        if (buttonId == AUTO_FURNACE_FILL_BUTTON) {
            if (!isFurnaceVisible()) return false;
            furnace.setAutoFill(!furnace.isAutoFill());
            broadcastChanges();
            return true;
        }
        if (buttonId == MAGNET_BUTTON) {
            if (data.getStage() < 4) return false;
            data.setMagnetEnabled(!data.isMagnetEnabled());
            broadcastChanges();
            return true;
        }
        if (buttonId < 0 || buttonId > 2) return false;
        if (buttonId == 0 && !isCraftingUnlocked()) return false;
        if (buttonId == 1 && !isSmithingUnlocked()) return false;
        if (buttonId == 2 && !isFurnaceUnlocked()) return false;
        setActiveModule(activeModule == buttonId ? -1 : buttonId);
        return true;
    }

    public void setViewport(int requestedRows, int requestedBaseRow) {
        int nextRows = clampViewportRows(requestedRows, getTotalRows());
        int nextBase = TerminalViewport.clampBaseRow(requestedBaseRow, nextRows, getTotalRows());
        if (nextRows == visibleRows && nextBase == baseRow) return;
        visibleRows = nextRows;
        baseRow = nextBase;
        if (player.level().isClientSide()) return;
        revision++;
        broadcastChanges();
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public int getBaseRow() {
        return baseRow;
    }

    public boolean isCraftAutofillMatchComponents() {
        return data.isCraftAutofillMatchComponents();
    }

    public int getTotalRows() {
        return (data.getKongqiaoMaxSlots() + VISIBLE_COLS - 1) / VISIBLE_COLS;
    }

    public static int clampViewportRows(int requestedRows, int totalRows) {
        return Math.min(TerminalViewport.clampRows(requestedRows),
                Math.max(TerminalViewport.MIN_ROWS, totalRows));
    }

    @Override
    public void slotsChanged(Container changed) {
        if (changed == this.craftSlots) {
            refreshCraftingResult(null);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        TerminalMenuSupport.returnCraftingItems(this, player, craftSlots, data, false);
        smithing.returnInputs();
    }

    private void refreshCraftingResult(RecipeHolder<CraftingRecipe> lastRecipe) {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CraftingInput input = this.craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = serverPlayer.server.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, serverPlayer.level(), lastRecipe);
        if (recipe.isPresent() && this.resultSlots.setRecipeUsed(serverPlayer.level(), serverPlayer, recipe.get())) {
            ItemStack assembled = recipe.get().value().assemble(input, serverPlayer.level().registryAccess());
            if (assembled.isItemEnabled(serverPlayer.level().enabledFeatures())) {
                result = assembled;
            }
        }
        this.resultSlots.setItem(0, result);
        this.setRemoteSlot(CRAFT_RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                this.containerId, this.incrementStateId(), CRAFT_RESULT_SLOT, result));
    }

    public static class KongqiaoStorageContainer implements Container {
        private final ImmortalStoragePlayerData data;

        public KongqiaoStorageContainer(ImmortalStoragePlayerData data) {
            this.data = data;
        }

        @Override
        public int getContainerSize() {
            return ImmortalStoragePlayerData.KONGQIAO_MAX_SLOTS_CEILING;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : data.getKongqiaoItems()) {
                if (!s.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int i) {
            return i >= 0 && i < data.getKongqiaoItems().size() ? data.getKongqiaoItems().get(i) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int i, int c) {
            ItemStack s = getItem(i);
            if (s.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack r = s.split(c);
            if (s.isEmpty()) {
                data.setKongqiaoSlot(i, ItemStack.EMPTY);
            }
            return r;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = data.getKongqiaoItems().get(i);
            data.setKongqiaoSlot(i, ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setItem(int i, ItemStack s) {
            data.setKongqiaoSlot(i, s);
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return data.getKongqiaoStackLimit(stack);
        }

        @Override public void setChanged() {}
        @Override public boolean stillValid(Player p) { return true; }

        @Override
        public void clearContent() {
            for (int i = 0; i < data.getKongqiaoItems().size(); i++) {
                data.setKongqiaoSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static class KongqiaoSlot extends Slot {
        private final ImmortalStoragePlayerData data;
        private final KongqiaoMenu menu;
        private final int viewIndex;

        public KongqiaoSlot(KongqiaoStorageContainer c, KongqiaoMenu menu, int viewIndex, int x, int y) {
            super(c, viewIndex, x, y);
            this.data = c.data;
            this.menu = menu;
            this.viewIndex = viewIndex;
        }

        @Override
        public boolean isActive() {
            int interactiveRows = Math.min(menu.bufferedRowCount(),
                    TerminalViewport.maxIntersectingRows(menu.visibleRows));
            return viewIndex < interactiveRows * VISIBLE_COLS && backingIndex() < data.getKongqiaoMaxSlots();
        }

        @Override public ItemStack getItem() { return isActive() ? container.getItem(backingIndex()) : ItemStack.EMPTY; }
        @Override public void set(ItemStack stack) { if (isActive()) container.setItem(backingIndex(), stack); }
        @Override public ItemStack remove(int amount) { return isActive() ? container.removeItem(backingIndex(), amount) : ItemStack.EMPTY; }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && super.mayPlace(stack); }
        @Override public boolean mayPickup(Player actor) { return isActive() && super.mayPickup(actor); }
        @Override public int getMaxStackSize() { return Integer.MAX_VALUE; }
        @Override public int getMaxStackSize(ItemStack stack) { return data.getKongqiaoStackLimit(stack); }
        @Override public void setChanged() { container.setChanged(); }
        @Override public int getContainerSlot() { return backingIndex(); }

        private int backingIndex() { return menu.baseRow * VISIBLE_COLS + viewIndex; }
    }

    @Override public TerminalViewport viewport() { return new TerminalViewport(visibleRows, visibleRows, baseRow, getTotalRows(), revision); }
    @Override public int bufferedRowCount() {
        // Kongqiao has at most nine physical rows, while the fixed menu already
        // reserves twelve proxy rows. Reuse that reservation as a two-view
        // look-ahead window so half-row scrolling never waits for a new binding.
        return Math.min(MAX_VISIBLE_ROWS, TerminalViewport.bufferedRows(visibleRows));
    }
    @Override public TerminalQuery query() { return TerminalQuery.DEFAULT; }
    @Override public List<TerminalEntry> visibleEntries() {
        Map<TerminalEntryKey, MutableAmount> grouped = new LinkedHashMap<>();
        int start = baseRow * VISIBLE_COLS;
        int end = Math.min(data.getKongqiaoMaxSlots(), start + visibleRows * VISIBLE_COLS);
        for (int i = start; i < end; i++) {
            ItemStack stack = data.getKongqiaoItems().get(i);
            if (stack.isEmpty()) continue;
            TerminalEntryKey key = TerminalEntryKey.of(stack);
            MutableAmount amount = grouped.get(key);
            if (amount == null) {
                amount = new MutableAmount(i + 1L, stack.copyWithCount(1));
                grouped.put(key, amount);
            }
            amount.amount += stack.getCount();
        }
        List<TerminalEntry> result = new ArrayList<>(grouped.size());
        for (MutableAmount value : grouped.values()) {
            result.add(new TerminalEntry(value.id, value.stack, value.amount));
        }
        return List.copyOf(result);
    }
    @Override public int storageSlotStart() { return 0; }
    @Override public int storageSlotCount() { return bufferedRowCount() * VISIBLE_COLS; }
    @Override public int craftingSlotStart() { return CRAFT_START; }
    @Override public int craftingResultSlot() { return CRAFT_RESULT_SLOT; }
    @Override public int playerInventoryStart() { return PLAYER_START; }
    @Override public boolean isCraftingUnlocked() { return data.getStage() >= 3; }
    @Override public boolean isCraftingVisible() { return activeModule == 0 && isCraftingUnlocked(); }
    @Override public List<Slot> craftingInputSlots() { return List.copyOf(slots.subList(CRAFT_START, CRAFT_END)); }
    @Override public Slot craftingResultSlotView() { return slots.get(CRAFT_RESULT_SLOT); }
    @Override public List<Slot> craftingSourceSlots() {
        List<Slot> source = new ArrayList<>();
        source.addAll(slots.subList(PLAYER_START, slots.size()));
        return List.copyOf(source);
    }
    @Override public List<TransferIngredient> craftingStorageIngredients() {
        if (player.level().isClientSide()) return clientRecipeSources;
        Map<TerminalEntryKey, MutableAmount> grouped = new LinkedHashMap<>();
        for (int i = 0; i < data.getKongqiaoMaxSlots(); i++) {
            ItemStack stack = data.getKongqiaoItems().get(i);
            if (stack.isEmpty()) continue;
            TerminalEntryKey key = TerminalEntryKey.of(stack);
            MutableAmount entry = grouped.get(key);
            if (entry == null) {
                entry = new MutableAmount(i + 1L, stack.copyWithCount(1));
                grouped.put(key, entry);
            }
            entry.amount += stack.getCount();
        }
        return grouped.values().stream().map(entry -> new TransferIngredient(entry.stack, entry.amount)).toList();
    }
    public List<TransferIngredient> serverCraftingStorageIngredients() { return craftingStorageIngredients(); }
    @Override public void applyRecipeSourceChunk(long sourceRevision, int chunkIndex, int chunkCount,
                                                 List<TransferIngredient> entries) {
        if (!player.level().isClientSide() || sourceRevision != revision || chunkCount < 1 || chunkIndex < 0 || chunkIndex >= chunkCount) return;
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
        return TerminalRecipeTransfer.place(this, player, data, craftSlots, recipe, requestedSets,
                expectedRevision, revision, false);
    }
    @Override public List<Slot> smithingInputSlots() { return List.copyOf(slots.subList(SMITHING_START, SMITHING_RESULT_SLOT)); }
    @Override public Slot smithingResultSlotView() { return slots.get(SMITHING_RESULT_SLOT); }
    @Override public List<Slot> smithingSourceSlots() { return craftingSourceSlots(); }
    @Override public List<TransferIngredient> smithingStorageIngredients() { return craftingStorageIngredients(); }
    @Override public boolean transferSmithingRecipe(RecipeHolder<SmithingRecipe> recipe, long expectedRevision) {
        return player instanceof ServerPlayer serverPlayer && isSmithingVisible()
                && TerminalSmithingTransfer.place(serverPlayer, smithing, recipe, expectedRevision, revision,
                serverCraftingStorageIngredients(), this::extractCraftingIngredient);
    }

    private static final class MutableAmount {
        private final long id;
        private final ItemStack stack;
        private long amount;
        private MutableAmount(long id, ItemStack stack) { this.id = id; this.stack = stack; }
    }

    private class CraftModuleSlot extends Slot {
        CraftModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return activeModule == 0 && data.getStage() >= 3;
        }

        @Override public boolean mayPlace(ItemStack stack) { return isActive() && super.mayPlace(stack); }
        @Override public boolean mayPickup(Player actor) { return isActive() && super.mayPickup(actor); }
        @Override public void set(ItemStack stack) { if (isActive()) super.set(stack); }
        @Override public ItemStack remove(int amount) { return isActive() ? super.remove(amount) : ItemStack.EMPTY; }
    }

    private class CraftModuleResultSlot extends ResultSlot {
        CraftModuleResultSlot(Player player, CraftingContainer craftingContainer, Container resultContainer, int index, int x, int y) {
            super(player, craftingContainer, resultContainer, index, x, y);
        }

        @Override
        public boolean isActive() {
            return activeModule == 0 && data.getStage() >= 3;
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player actor) { return isActive() && super.mayPickup(actor); }
        @Override public ItemStack remove(int amount) { return isActive() ? super.remove(amount) : ItemStack.EMPTY; }

        @Override
        public void onTake(Player actor, ItemStack stack) {
            List<ItemStack> before = TerminalMenuSupport.snapshotCrafting(craftSlots);
            super.onTake(actor, stack);
            TerminalMenuSupport.refillCraftingAfterTake(craftSlots, before,
                    data.isCraftAutofillMatchComponents(), KongqiaoMenu.this::extractCraftingIngredient);
            slotsChanged(craftSlots);
            broadcastChanges();
        }
    }

    private ItemStack extractCraftingIngredient(ItemStack prototype, int amount, boolean matchComponents) {
        ItemStack selected = prototype;
        if (!matchComponents) {
            selected = data.getKongqiaoItems().stream()
                    .filter(candidate -> !candidate.isEmpty() && candidate.is(prototype.getItem()))
                    .filter(candidate -> ItemStack.isSameItemSameComponents(candidate, prototype))
                    .findFirst()
                    .orElseGet(() -> data.getKongqiaoItems().stream()
                            .filter(candidate -> !candidate.isEmpty() && candidate.is(prototype.getItem()))
                            .findFirst().orElse(ItemStack.EMPTY));
        }
        return selected.isEmpty() ? ItemStack.EMPTY : data.extractStack(selected, amount);
    }

    private class SmithingModuleSlot extends Slot {
        SmithingModuleSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return isSmithingVisible(); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && smithing.accepts(index, stack); }
        @Override public boolean mayPickup(Player actor) { return isActive() && super.mayPickup(actor); }
    }

    private final class SmithingModuleResultSlot extends Slot {
        SmithingModuleResultSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean isActive() { return isSmithingVisible(); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player actor) { return isActive() && smithing.mayTake(); }
        @Override public void onTake(Player actor, ItemStack stack) {
            smithing.onTake(actor, stack);
            super.onTake(actor, stack);
        }
    }

    private class FurnaceModuleSlot extends Slot {
        FurnaceModuleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean isActive() { return isFurnaceVisible(); }
        @Override public boolean mayPickup(Player actor) { return isActive() && super.mayPickup(actor); }
    }

    private final class FurnaceModuleInputSlot extends FurnaceModuleSlot {
        FurnaceModuleInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isActive() && furnace.isRecipeInput(player, stack);
        }
    }

    private final class FurnaceModuleFuelSlot extends FurnaceModuleSlot {
        FurnaceModuleFuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isActive() && furnace.isFuel(stack);
        }
    }

    private final class FurnaceModuleResultSlot extends FurnaceModuleSlot {
        FurnaceModuleResultSlot(Player actor, Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public void onTake(Player actor, ItemStack stack) {
            super.onTake(actor, stack);
            furnace.awardUsedRecipes(actor);
        }
    }

}
