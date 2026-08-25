package com.immortalstorage.immortalstorage.menu.custom;
import net.minecraft.world.item.crafting.RecipeManager;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.List;

/** Server-authoritative smithing table embedded in a personal-storage terminal. */
final class EmbeddedSmithingBackend {
    static final int TEMPLATE = 0;
    static final int BASE = 1;
    static final int ADDITION = 2;

    private final AbstractContainerMenu menu;
    private final Player player;
    private final ImmortalStoragePlayerData data;
    private final boolean xianqiao;
    private final int resultMenuSlot;
    private final TerminalMenuSupport.CraftingExtractor extractor;
    private boolean refreshing;
    private RecipeHolder<SmithingRecipe> selectedRecipe;
    final SimpleContainer inputs = new SimpleContainer(3) {
        @Override public void setChanged() {
            super.setChanged();
            refreshResult();
        }
    };
    final ResultContainer result = new ResultContainer();

    EmbeddedSmithingBackend(AbstractContainerMenu menu, Player player, ImmortalStoragePlayerData data,
                             boolean xianqiao, int resultMenuSlot,
                             TerminalMenuSupport.CraftingExtractor extractor) {
        this.menu = menu;
        this.player = player;
        this.data = data;
        this.xianqiao = xianqiao;
        this.resultMenuSlot = resultMenuSlot;
        this.extractor = extractor;
    }

    boolean accepts(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.getAllRecipesFor((RecipeManager) player.level().recipeAccess(), RecipeType.SMITHING).stream().anyMatch(holder -> switch (slot) {
            case TEMPLATE -> holder.value().templateIngredient().map(ingredient -> ingredient.test(stack)).orElse(false);
            case BASE -> holder.value().baseIngredient().test(stack);
            case ADDITION -> holder.value().additionIngredient().map(ingredient -> ingredient.test(stack)).orElse(false);
            default -> false;
        });
    }

    void refreshResult() {
        if (refreshing || !(player instanceof ServerPlayer serverPlayer)) return;
        refreshing = true;
        try {
            SmithingRecipeInput input = input();
            List<RecipeHolder<SmithingRecipe>> matches = com.immortalstorage.immortalstorage.compat.mc2612.CompatRecipeAccess.getRecipesFor(com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(serverPlayer.level()).getRecipeManager(), RecipeType.SMITHING, input, serverPlayer.level());
            selectedRecipe = matches.isEmpty() ? null : matches.getFirst();
            ItemStack assembled = selectedRecipe == null ? ItemStack.EMPTY
                    : selectedRecipe.value().assemble(input);
            if (!assembled.isItemEnabled(serverPlayer.level().enabledFeatures())) assembled = ItemStack.EMPTY;
            result.setRecipeUsed(selectedRecipe);
            result.setItem(0, assembled);
            menu.setRemoteSlot(resultMenuSlot, assembled);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    menu.containerId, menu.incrementStateId(), resultMenuSlot, assembled));
        } finally {
            refreshing = false;
        }
    }

    boolean mayTake() {
        return selectedRecipe != null && selectedRecipe.value().matches(input(), player.level());
    }

    void onTake(Player actor, ItemStack crafted) {
        if (!mayTake()) return;
        List<ItemStack> before = TerminalMenuSupport.snapshotCrafting(inputs);
        crafted.onCraftedBy(actor, crafted.getCount());
        result.awardUsedRecipes(actor, before);
        for (int slot = 0; slot < inputs.getContainerSize(); slot++) {
            if (slot == BASE && crafted.is(com.immortalstorage.immortalstorage.item.ModItems.IMMORTAL_MASTER_TALISMAN.get())) {
                continue;
            }
            ItemStack stack = inputs.getItem(slot);
            if (!stack.isEmpty()) stack.shrink(1);
        }
        Runnable refill = () -> TerminalMenuSupport.refillCraftingAfterTake(
                inputs, before, data.isCraftAutofillMatchComponents(), extractor);
        if (xianqiao) data.batchXianqiaoMutations(refill); else refill.run();
        refreshResult();
        menu.broadcastChanges();
    }

    void returnInputs() {
        TerminalMenuSupport.returnCraftingItems(menu, player, inputs, data, xianqiao);
    }

    private SmithingRecipeInput input() {
        return new SmithingRecipeInput(inputs.getItem(TEMPLATE), inputs.getItem(BASE), inputs.getItem(ADDITION));
    }
}
