package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.TerminalFluidScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import com.immortalstorage.immortalstorage.config.ImmortalStorageClientConfig;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

@JeiPlugin
public final class ImmortalStorageJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "terminal_jei");
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void setSearchText(String text) {
        if (runtime != null && ImmortalStorageClientConfig.SYNC_RECIPE_VIEWER_SEARCH.get()) {
            runtime.getIngredientFilter().setFilterText(text == null ? "" : text);
        }
    }

    public static String getSearchText() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static boolean isSearchFocused() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(KongqiaoScreen.class, new TerminalGuiHandler<>());
        registration.addGuiContainerHandler(XianqiaoStorageScreen.class, new TerminalGuiHandler<>());
        registration.addGuiContainerHandler(XianqiaoInterfaceScreen.class,
                new XianqiaoInterfaceJeiGuiHandler());
        registration.addGhostIngredientHandler(XianqiaoInterfaceScreen.class,
                new XianqiaoInterfaceJeiGhostHandler());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ImmortalFurnaceJeiCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(ImmortalFurnaceJeiCategory.TYPE,
                level.getRecipeManager().getAllRecipesFor(
                        com.immortalstorage.immortalstorage.recipe.ModRecipes.IMMORTAL_FURNACE_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(com.immortalstorage.immortalstorage.block.ModBlocks.IMMORTAL_FURNACE.get(),
                ImmortalFurnaceJeiCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new TerminalTransferHandler<>(
                KongqiaoMenu.class, ModMenus.KONGQIAO.get(), helper), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new TerminalTransferHandler<>(
                XianqiaoStorageMenu.class, ModMenus.XIANQIAO_STORAGE.get(), helper), RecipeTypes.CRAFTING);
    }

    private static final class TerminalGuiHandler<S extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>
            implements IGuiContainerHandler<S> {
        @Override
        public List<Rect2i> getGuiExtraAreas(S screen) {
            return ((TerminalScreenAccess) screen).immortalstorage$getExtraAreas();
        }

        @Override
        public Optional<? extends mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                IClickableIngredientFactory factory, S screen, double mouseX, double mouseY) {
            if (screen instanceof TerminalFluidScreenAccess fluids) {
                Optional<TerminalFluidScreenAccess.FluidHover> hover =
                        fluids.immortalstorage$getFluidAt(mouseX, mouseY);
                if (hover.isPresent()) {
                    TerminalFluidScreenAccess.FluidHover fluid = hover.get();
                    return factory.createBuilder(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, fluid.stack())
                            .buildWithArea(fluid.bounds());
                }
            }
            Slot slot = ((TerminalScreenAccess) screen).immortalstorage$getSlotAt(mouseX, mouseY);
            if (slot == null || !slot.hasItem()) {
                return Optional.empty();
            }
            return factory.createBuilder(slot.getItem()).buildWithArea(
                    ((TerminalScreenAccess) screen).immortalstorage$getSlotBounds(slot));
        }
    }

    private static final class TerminalTransferHandler<M extends AbstractContainerMenu & StorageTerminalView & CraftingTransferTarget>
            implements IRecipeTransferHandler<M, RecipeHolder<CraftingRecipe>> {
        private final Class<M> menuClass;
        private final MenuType<M> menuType;
        private final IRecipeTransferHandlerHelper helper;

        private TerminalTransferHandler(Class<M> menuClass, MenuType<M> menuType, IRecipeTransferHandlerHelper helper) {
            this.menuClass = menuClass;
            this.menuType = menuType;
            this.helper = helper;
        }

        @Override public Class<? extends M> getContainerClass() { return menuClass; }
        @Override public Optional<MenuType<M>> getMenuType() { return Optional.of(menuType); }
        @Override public mezz.jei.api.recipe.RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public IRecipeTransferError transferRecipe(M menu, RecipeHolder<CraftingRecipe> recipe,
                                                   IRecipeSlotsView recipeSlots, net.minecraft.world.entity.player.Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            if (!menu.isCraftingUnlocked() || !menu.isCraftingVisible()) {
                return helper.createUserErrorWithTooltip(
                        Component.translatable("container.immortalstorage.terminal.recipe_transfer_locked"));
            }
            if (recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).size() > 9) {
                return helper.createInternalError();
            }
            int transferableSets = maxTransferableSets(menu, recipe.value().getIngredients());
            if (transferableSets <= 0) {
                return helper.createUserErrorForMissingSlots(
                        Component.translatable("container.immortalstorage.terminal.recipe_transfer_missing"),
                        recipeSlots.getSlotViews(RecipeIngredientRole.INPUT));
            }
            if (doTransfer) {
                PacketDistributor.sendToServer(new com.immortalstorage.immortalstorage.network.ModPayloads.TransferTerminalRecipe(
                        menu.containerId, menu.viewport().revision(), recipe.id(), maxTransfer ? transferableSets : 1));
            }
            return null;
        }

        private int maxTransferableSets(M menu, List<net.minecraft.world.item.crafting.Ingredient> ingredients) {
            List<AvailableStack> available = new java.util.ArrayList<>();
            for (Slot slot : menu.craftingSourceSlots()) if (slot.hasItem()) available.add(new AvailableStack(slot.getItem(), slot.getItem().getCount()));
            for (Slot slot : menu.craftingInputSlots()) if (slot.hasItem()) available.add(new AvailableStack(slot.getItem(), slot.getItem().getCount()));
            for (CraftingTransferTarget.TransferIngredient entry : menu.craftingStorageIngredients()) {
                available.add(new AvailableStack(entry.stack(), entry.amount()));
            }
            int limit = ingredients.stream().filter(ingredient -> !ingredient.isEmpty())
                    .flatMap(ingredient -> java.util.Arrays.stream(ingredient.getItems()))
                    .mapToInt(ItemStack::getMaxStackSize).max().orElse(1);
            for (int sets = limit; sets > 0; sets--) {
                List<AvailableStack> simulation = available.stream().map(AvailableStack::copy).toList();
                boolean complete = true;
                for (net.minecraft.world.item.crafting.Ingredient ingredient : ingredients) {
                    if (ingredient.isEmpty()) continue;
                    int remaining = sets;
                    for (AvailableStack stack : simulation) {
                        if (remaining <= 0) break;
                        if (!ingredient.test(stack.stack)) continue;
                        int take = (int) Math.min(remaining, stack.amount);
                        stack.amount -= take;
                        remaining -= take;
                    }
                    if (remaining > 0) { complete = false; break; }
                }
                if (complete) return sets;
            }
            return 0;
        }

        private static final class AvailableStack {
            private final ItemStack stack;
            private long amount;
            private AvailableStack(ItemStack stack, long amount) {
                this.stack = stack.copyWithCount(1);
                this.amount = amount;
            }
            private AvailableStack copy() { return new AvailableStack(stack, amount); }
        }
    }
}
