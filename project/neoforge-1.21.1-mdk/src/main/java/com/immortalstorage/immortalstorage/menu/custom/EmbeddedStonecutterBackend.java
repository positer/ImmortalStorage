package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative stonecutter embedded in a personal-storage terminal.
 * Mirrors the vanilla {@code StonecutterMenu} recipe-list behavior (multi-able
 * results, selectable index) so the client can reproduce the vanilla UI inside
 * the smithing module tab.
 */
final class EmbeddedStonecutterBackend {
    static final int INPUT = 0;
    static final int RESULT = 0;

    private final AbstractContainerMenu menu;
    private final Player player;
    private final ImmortalStoragePlayerData data;
    private final boolean xianqiao;
    private final int resultMenuSlot;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final Level level;
    private boolean refreshing;
    private List<RecipeHolder<StonecutterRecipe>> recipes = new ArrayList<>();
    private ItemStack trackedInput = ItemStack.EMPTY;

    final SimpleContainer input = new SimpleContainer(1) {
        @Override public void setChanged() {
            super.setChanged();
            slotsChanged();
        }
    };
    final ResultContainer result = new ResultContainer();

    EmbeddedStonecutterBackend(AbstractContainerMenu menu, Player player,
                               ImmortalStoragePlayerData data, boolean xianqiao,
                               int resultMenuSlot) {
        this.menu = menu;
        this.player = player;
        this.data = data;
        this.xianqiao = xianqiao;
        this.resultMenuSlot = resultMenuSlot;
        this.level = player.level();
        this.selectedRecipeIndex.set(-1);
    }

    boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING).stream()
                .anyMatch(holder -> holder.value().matches(new SingleRecipeInput(stack), level));
    }

    DataSlot selectedRecipeIndexSlot() {
        return selectedRecipeIndex;
    }

    int getSelectedRecipeIndex() {
        return selectedRecipeIndex.get();
    }

    List<RecipeHolder<StonecutterRecipe>> getRecipes() {
        return recipes;
    }

    int getNumRecipes() {
        return recipes.size();
    }

    boolean hasInputItem() {
        return !input.getItem(INPUT).isEmpty() && !recipes.isEmpty();
    }

    /** Client-initiated result selection; mirrors vanilla clickMenuButton. */
    boolean selectRecipe(int index) {
        if (!isValidRecipeIndex(index)) return false;
        selectedRecipeIndex.set(index);
        setupResultSlot();
        return true;
    }

    private boolean isValidRecipeIndex(int index) {
        return index >= 0 && index < recipes.size();
    }

    void slotsChanged() {
        ItemStack itemstack = input.getItem(INPUT);
        if (!itemstack.is(trackedInput.getItem())) {
            trackedInput = itemstack.copy();
            setupRecipeList(itemstack);
        }
    }

    private static SingleRecipeInput createRecipeInput(SimpleContainer container) {
        return new SingleRecipeInput(container.getItem(INPUT));
    }

    private void setupRecipeList(ItemStack itemstack) {
        recipes = new ArrayList<>();
        selectedRecipeIndex.set(-1);
        result.setItem(RESULT, ItemStack.EMPTY);
        pushResultToRemote();
        if (!itemstack.isEmpty()) {
            recipes = new ArrayList<>(level.getRecipeManager()
                    .getRecipesFor(RecipeType.STONECUTTING, createRecipeInput(input), level));
        }
    }

    void setupResultSlot() {
        ItemStack resultStack = ItemStack.EMPTY;
        if (!recipes.isEmpty() && isValidRecipeIndex(selectedRecipeIndex.get())) {
            RecipeHolder<StonecutterRecipe> holder = recipes.get(selectedRecipeIndex.get());
            ItemStack assembled = holder.value().assemble(createRecipeInput(input), level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                result.setRecipeUsed(holder);
                resultStack = assembled;
            }
        }
        result.setItem(RESULT, resultStack);
        pushResultToRemote();
    }

    private void pushResultToRemote() {
        ItemStack resultStack = result.getItem(RESULT);
        menu.setRemoteSlot(resultMenuSlot, resultStack);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    menu.containerId, menu.incrementStateId(), resultMenuSlot, resultStack));
        }
    }

    boolean mayTake() {
        return !recipes.isEmpty() && isValidRecipeIndex(selectedRecipeIndex.get())
                && !result.getItem(RESULT).isEmpty();
    }

    void onTake(Player actor, ItemStack crafted) {
        // Vanilla removes the result stack before Slot.onTake runs. Checking
        // mayTake() here would therefore reject every completed craft and leave
        // the selected recipe without a regenerated result.
        if (!isValidRecipeIndex(selectedRecipeIndex.get()) || input.getItem(INPUT).isEmpty()) return;
        if (!crafted.isEmpty()) crafted.onCraftedBy(actor.level(), actor, crafted.getCount());
        result.awardUsedRecipes(actor, List.of(input.getItem(INPUT)));
        ItemStack current = input.getItem(INPUT);
        if (!current.isEmpty()) current.shrink(1);
        if (current.isEmpty()) input.setItem(INPUT, ItemStack.EMPTY);
        setupResultSlot();
        menu.broadcastChanges();
    }

    void returnInputs() {
        TerminalMenuSupport.returnCraftingItems(menu, player, input, data, xianqiao);
    }
}
