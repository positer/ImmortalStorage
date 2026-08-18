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
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.List;

/** 26.1.2 target override: stonecutter backend adapted to the 26.1 recipe APIs. */
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
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes = SelectableRecipe.SingleInputSet.empty();
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
        return level.recipeAccess().stonecutterRecipes().acceptsInput(stack);
    }

    DataSlot selectedRecipeIndexSlot() {
        return selectedRecipeIndex;
    }

    int getSelectedRecipeIndex() {
        return selectedRecipeIndex.get();
    }

    SelectableRecipe.SingleInputSet<StonecutterRecipe> getRecipes() {
        return recipes;
    }

    int getNumRecipes() {
        return recipes.size();
    }

    boolean hasInputItem() {
        return !input.getItem(INPUT).isEmpty() && !recipes.isEmpty();
    }

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
        recipes = SelectableRecipe.SingleInputSet.empty();
        selectedRecipeIndex.set(-1);
        result.setItem(RESULT, ItemStack.EMPTY);
        pushResultToRemote();
        if (!itemstack.isEmpty()) {
            recipes = level.recipeAccess().stonecutterRecipes().selectByInput(itemstack);
        }
    }

    void setupResultSlot() {
        ItemStack resultStack = ItemStack.EMPTY;
        if (!recipes.isEmpty() && isValidRecipeIndex(selectedRecipeIndex.get())) {
            java.util.Optional<RecipeHolder<StonecutterRecipe>> selected = recipes.entries()
                    .get(selectedRecipeIndex.get()).recipe().recipe();
            if (selected.isPresent()) {
                RecipeHolder<StonecutterRecipe> holder = selected.get();
                ItemStack assembled = holder.value().assemble(createRecipeInput(input));
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result.setRecipeUsed(holder);
                    resultStack = assembled;
                }
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
        if (!mayTake()) return;
        crafted.onCraftedBy(actor, crafted.getCount());
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
